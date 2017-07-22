package ideafocus;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;

public class FocusPluginRegistration implements ProjectComponent {

    private Project project;

    public FocusPluginRegistration(Project p) {
        project = p;
    }

    @Override
    public void projectOpened() {
       StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
       if (statusBar != null)
           statusBar.addWidget(new FocusWidget(project), "before ReadOnlyAttribute");
    }

    @Override
    public void projectClosed() {

    }

    @Override
    public String getComponentName() {
        return "Focus";
    }

    @Override
    public void disposeComponent() {

    }

}
