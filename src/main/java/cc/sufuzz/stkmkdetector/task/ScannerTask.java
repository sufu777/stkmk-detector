package cc.sufuzz.stkmkdetector.task;

import cc.sufuzz.stkmkdetector.StaticMockCloseVisitor;
import cc.sufuzz.stkmkdetector.cfg.ConfigurationLoadException;
import cc.sufuzz.stkmkdetector.cfg.DetectorConfig;
import cc.sufuzz.stkmkdetector.cfg.DetectorConfigLoader;
import cc.sufuzz.stkmkdetector.service.DetectedResultViewManager;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScopes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ScannerTask extends Task.Backgroundable {
    private final Project project;

    public ScannerTask(@Nullable Project project) {
        super(project, "扫描未关闭的StaticMock", true);
        this.project = project;
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        if (Objects.isNull(project)) {
            return;
        }
        try {
            DetectorConfig detectorConfig = DetectorConfigLoader.loadConfig();
        } catch (ConfigurationLoadException e) {
            NotificationGroupManager.getInstance().getNotificationGroup("stkmk-detector")
                    .createNotification("配置文件加载失败", e.getMessage(), NotificationType.ERROR);
            return;
        }
        // 遍历文件期间不能中断任务
        progressIndicator.setIndeterminate(false);
        Collection<VirtualFile> testFiles = ReadAction.compute(() -> FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScopes.projectTestScope(project)));
        List<JavaFileIssue> issues = testFiles.stream()
                .map(f -> this.analyzeFile(f, progressIndicator))
                .filter(Objects::nonNull)
                .toList();
        showIssues(issues);
    }

    private void showIssues(List<JavaFileIssue> issues) {
        if (Objects.isNull(project)) {
            return;
        }
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow("unClosedStaticMockScanResult");
        ApplicationManager.getApplication().invokeLater(() -> {
            tw.show();
        });
        DetectedResultViewManager service = project.getService(DetectedResultViewManager.class);
        service.updateIssues(issues);
    }

    private JavaFileIssue analyzeFile(VirtualFile testFile, ProgressIndicator indicator) {
        indicator.setText2("Analyzing " + testFile.getName());
        PsiJavaFile psiFile = ReadAction.compute(() -> (PsiJavaFile) PsiManager.getInstance(project).findFile(testFile));
        if (psiFile == null) {
            return null;
        }
        JavaFileIssue issue = ReadAction.compute(() -> {
            JavaFileIssue javaFileIssue = new JavaFileIssue(psiFile);
            psiFile.accept(new StaticMockCloseVisitor(javaFileIssue, project));
            return javaFileIssue;
        });

        return Objects.nonNull(issue) && issue.hasIssue() ? issue : null;
    }
}
