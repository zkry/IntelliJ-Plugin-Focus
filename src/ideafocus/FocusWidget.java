package ideafocus;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Consumer;
import ideafocus.config.FocusConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.applet.AudioClip;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.applet.Applet;
import java.net.URL;

public class FocusWidget implements StatusBarWidget.TextPresentation, ActionListener, StatusBarWidget {

    private enum FocusPlayState {
        DONE, RUNNING, PAUSED
    }

    private enum FocusPhaseState {
        MAIN, BREAK
    }

    private Project project;

    private FocusPlayState myPlayState;
    private FocusPhaseState myPhaseState;
    private Timer timer = new Timer(1000, this);
    private StatusBar statusBar;
    private long prevClickTime;
    private int secondsRemaining;
    private int currentSessionMins; // Stores the value of the session running for case where user changes time min run


    private FocusConfig focusConfig;

    private AudioClip beepClip;

    private Consumer<MouseEvent> mouseEventConsumer = new Consumer<MouseEvent>() {

        @Override
        public void consume(MouseEvent mouseEvent) {
            long clickTime = getTime();
            if ( (clickTime - prevClickTime) < 200 ) {
                // Fast Double click occurred
                // Reset Focus
                myPlayState = FocusPlayState.DONE;
                myPhaseState = FocusPhaseState.MAIN;
            } else {
                if (myPlayState == FocusPlayState.DONE) {
                    currentSessionMins = focusConfig.getState().focusLength;
                    secondsRemaining = currentSessionMins * 60;
                    myPlayState = FocusPlayState.RUNNING;
                } else if (myPlayState == FocusPlayState.RUNNING) {
                    myPlayState = FocusPlayState.PAUSED;
                } else if (myPlayState == FocusPlayState.PAUSED) {
                    myPlayState = FocusPlayState.RUNNING;
                }
                statusBar.updateWidget(ID());
            }

            prevClickTime = clickTime;
        }
    };

    public FocusWidget(Project project) {
        // Set initial state values
        prevClickTime = getTime();
        secondsRemaining = -1;
        myPhaseState = FocusPhaseState.MAIN;
        myPlayState = FocusPlayState.DONE;

        // Get beep sound
        URL url = getClass().getClassLoader().getResource("beep.wav");
        beepClip = Applet.newAudioClip(url);

        // Load persistent data
        this.project = project;
        focusConfig = ServiceManager.getService(project, FocusConfig.class);

        // Reset the session count if more than 7 hours has passed
        if ((getTime() - focusConfig.getState().currentDay) / 1000.0 / 60 / 60 > 7.0) {
            // If more than seven hours have passed reset the count
            System.out.println("DEBUG: Resetting count from " + focusConfig.getState().completedSessionCount + " to 0");
            FocusConfig.State newState = focusConfig.getState();
            newState.currentDay = getTime();
            newState.completedSessionCount = 0;
            focusConfig.loadState(newState);
        }

        System.out.printf("DEBUG: Focus Loaded, completed sessions: %d%n", focusConfig.getState().completedSessionCount);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (myPlayState == FocusPlayState.RUNNING) {
            secondsRemaining--;
            if (secondsRemaining == 0) {
                if (myPhaseState == FocusPhaseState.MAIN) {
                    // Done with a focus session!
                   playFinishSound();
                   myPhaseState = FocusPhaseState.BREAK;
                   secondsRemaining = BREAK_MINS * 60;
                   popupAlert("Focus Session Complete (" + currentSessionMins + " mins)");
                   // Increase completed session count
                   FocusConfig.State s = focusConfig.getState();
                   s.completedSessionCount++;
                   s.currentDay = getTime();
                   focusConfig.loadState(s);
                    System.out.printf("DEBUG: Focus Loaded, completed sessions: %d%n", focusConfig.getState().completedSessionCount);
                } else {
                    // Break over
                    playFinishSound();
                   myPhaseState = FocusPhaseState.MAIN;
                   myPlayState = FocusPlayState.DONE;
                   secondsRemaining = -1;
                   popupAlert("Break Done. Time to get back to work.");
                }
            }
        }
        statusBar.updateWidget(ID());
    }


    // Play sound to signify end of focus
    private void playFinishSound() {
        beepClip.play();
    }

    // Function that makes a simple message appear based on the String text
    private void popupAlert(String text) {
        JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text, MessageType.INFO, null)
                .setFadeoutTime(7300)
                .createBalloon()
                .show(RelativePoint.getCenterOf(this.statusBar.getComponent()), Balloon.Position.atRight);
    }

    @NotNull
    @Override
    public String getText() {
        if (myPlayState == FocusPlayState.DONE) {
            return "Start Timer";
        } else {
            String retStr = "";
            if (myPlayState == FocusPlayState.RUNNING) {
                retStr += " ||  " + formatSecondsRemaining(secondsRemaining);
            } else {
                retStr += "▶  " + formatSecondsRemaining(secondsRemaining);
            }
            retStr += (myPhaseState == FocusPhaseState.BREAK ? "★" : "");
            return retStr;
        }
    }

    private String formatSecondsRemaining(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
    }

    @NotNull
    @Override
    public String ID() {
        return "IdeaFocus";
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation(@NotNull PlatformType platformType) {
        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.timer.start();
        this.statusBar = statusBar;
    }

    @Override
    public void dispose() {
        this.timer.stop();
        statusBar = null;
    }


    @NotNull
    @Override
    public String getMaxPossibleText() {
        return "0000000000000";
    }

    @Override
    public float getAlignment() {
        return 0.5f;
    }

    @Nullable
    @Override
    public String getTooltipText() {
        int ct = focusConfig.getState().completedSessionCount;
        return "Completed " + ct + (ct == 1 ? " sessions" : " sessions");
    }

    @Nullable
    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return mouseEventConsumer;
    }

    private Long getTime() {
        return System.currentTimeMillis();
    }

    final private int BREAK_MINS = 5;
}
