package cc.sufuzz.stkmkdetector;

import cc.sufuzz.stkmkdetector.task.ScannerTask;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class UnclosedMockScanAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }
        ScannerTask unclosedStaticMockScannerTask = new ScannerTask(project);
        ProgressManager.getInstance().run(unclosedStaticMockScannerTask);
    }
}
