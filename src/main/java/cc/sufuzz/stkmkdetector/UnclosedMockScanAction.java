package cc.sufuzz.stkmkdetector;

import cc.sufuzz.stkmkdetector.task.ScannerTask;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class UnclosedMockScanAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        if (project == null) {
            return;
        }
        ToolWindow unClosedStaticMockScanResult = ToolWindowManager.getInstance(project).getToolWindow("unClosedStaticMockScanResult");
        if (unClosedStaticMockScanResult != null) {
            unClosedStaticMockScanResult.show();
        }
        ScannerTask unclosedStaticMockScannerTask = new ScannerTask(project);
        ProgressManager.getInstance().run(unclosedStaticMockScannerTask);
    }
}
