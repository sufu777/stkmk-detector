package cc.sufuzz.stkmkdetector.detectors.impl;

import cc.sufuzz.stkmkdetector.UnClosedStaticMockIssue;
import cc.sufuzz.stkmkdetector.detectors.UnCloseDetector;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.util.List;

public class TestMethodCloseDetector extends BaseDetector implements UnCloseDetector {
    @Override
    public String name() {
        return "testMethodCloseDetector";
    }

    @Override
    public List<UnClosedStaticMockIssue> doDetect(Project project, ProgressIndicator progressIndicator) {
        return List.of();
    }
}
