package cc.sufuzz.stkmkdetector.detectors;

import com.intellij.openapi.project.Project;

public interface UnCloseDetector {
    String name();
    void doDetect(Project project);
}
