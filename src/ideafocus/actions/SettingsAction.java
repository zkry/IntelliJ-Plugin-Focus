package ideafocus.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ui.Messages;
import ideafocus.config.FocusConfig;

public class SettingsAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        String lenStr = Messages.showInputDialog(e.getProject(), "Input length for focus session:", "Input Your Length", Messages.getQuestionIcon());
        try {
           int len = Integer.parseInt(lenStr);
            FocusConfig focusConfig = ServiceManager.getService(e.getProject(), FocusConfig.class);
            FocusConfig.State newState = focusConfig.getState();
            newState.focusLength = len;
            focusConfig.loadState(newState);
        } catch (NumberFormatException nfe) {
            Messages.showMessageDialog(e.getProject(), "The input you entered was invalid.", "Input Error", Messages.getErrorIcon());
        }
    }
}
