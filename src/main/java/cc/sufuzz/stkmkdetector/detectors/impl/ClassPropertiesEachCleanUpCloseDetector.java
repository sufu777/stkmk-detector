package cc.sufuzz.stkmkdetector.detectors.impl;

import cc.sufuzz.stkmkdetector.detectors.UnCloseDetector;
import com.intellij.openapi.project.Project;

public class ClassPropertiesEachCleanUpCloseDetector implements UnCloseDetector {
    @Override
    public String name() {
        return "classPropertiesEachCleanUpCloseDetector";
    }

    @Override
    public void doDetect(Project project) {

    }
}
