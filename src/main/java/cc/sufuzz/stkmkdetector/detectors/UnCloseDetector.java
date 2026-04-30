package cc.sufuzz.stkmkdetector.detectors;

import cc.sufuzz.stkmkdetector.UnClosedStaticMockIssue;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.util.List;

public interface UnCloseDetector {
    String name();

    List<UnClosedStaticMockIssue> doDetect(Project project, ProgressIndicator progressIndicator);
}
