package cc.sufuzz.stkmkdetector.service;

import cc.sufuzz.stkmkdetector.detectors.UnCloseDetector;
import cc.sufuzz.stkmkdetector.detectors.impl.ClassPropertiesAllCleanUpCloseDetector;
import cc.sufuzz.stkmkdetector.detectors.impl.ClassPropertiesEachCleanUpCloseDetector;
import com.intellij.openapi.components.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理项目中注册的所有检测器
 */
@Service(Service.Level.PROJECT)
public final class DetectorService {
    private final Map<String, UnCloseDetector> DETECTOR_MAP = new HashMap<>();
    private boolean inited;

    public void ensureInit() {
        if (!inited) {
            this.initDetector(new ClassPropertiesAllCleanUpCloseDetector());
            this.initDetector(new ClassPropertiesEachCleanUpCloseDetector());
            inited = true;
        }
    }

    private void initDetector(UnCloseDetector unCloseDetector) {
        DETECTOR_MAP.put(unCloseDetector.name(), unCloseDetector);
    }

    public UnCloseDetector get(String name) {
        return DETECTOR_MAP.get(name);
    }
}
