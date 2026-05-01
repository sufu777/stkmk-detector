package cc.sufuzz.stkmkdetector.detectors;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.util.List;

public interface UnCloseDetector {
    String name();

    String description();

    List<UnClosedStaticMockIssue> doDetect(Project project, ProgressIndicator progressIndicator);
}
