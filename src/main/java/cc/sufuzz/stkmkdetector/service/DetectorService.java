package cc.sufuzz.stkmkdetector.service;

import cc.sufuzz.stkmkdetector.cfg.DetectorConfig;
import cc.sufuzz.stkmkdetector.cfg.DetectorRule;
import cc.sufuzz.stkmkdetector.detectors.DetectResult;
import cc.sufuzz.stkmkdetector.detectors.UnCloseDetector;
import cc.sufuzz.stkmkdetector.detectors.UnClosedStaticMockIssue;
import cc.sufuzz.stkmkdetector.detectors.impl.ClassPropertiesCleanUpCloseDetector;
import cc.sufuzz.stkmkdetector.detectors.impl.TestMethodCloseDetector;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理项目中注册的所有检测器
 */
@Service(Service.Level.PROJECT)
public final class DetectorService {
    private final Map<String, UnCloseDetector> DETECTOR_MAP = new HashMap<>();

    public DetectorService() {
        this.initDetector(new ClassPropertiesCleanUpCloseDetector());
        this.initDetector(new TestMethodCloseDetector());
    }

    /**
     * 验证配置文件中的检测器是否都存在
     *
     * @param detectorConfig 本轮扫描配置文件
     * @return 不存在检测器的规则
     */
    public List<DetectorRule> validRules(DetectorConfig detectorConfig) {
        return detectorConfig.rules().stream().filter(r -> !DETECTOR_MAP.containsKey(r.detector())).toList();
    }

    private void initDetector(UnCloseDetector unCloseDetector) {
        DETECTOR_MAP.put(unCloseDetector.name(), unCloseDetector);
    }

    /**
     * 根据配置开始检测
     *
     * @param project           当前项目
     * @param progressIndicator 进度指示器，用于停止检测和设置当前检测规则
     * @param detectorConfig    配置
     * @return 扫描到的未关闭的 staticMock
     */
    public DetectResult doDetect(Project project, ProgressIndicator progressIndicator, DetectorConfig detectorConfig) {
        DetectResult detectResult = new DetectResult();
        List<UnClosedStaticMockIssue> list = detectorConfig.rules()
                .stream()
                .filter(DetectorRule::enable)
                .flatMap(r -> this.doDetectWithRule(project, progressIndicator, r).stream())
                .toList();
        detectResult.setIssues(list);
        detectResult.setEndTime(LocalDateTime.now());
        return detectResult;
    }

    /**
     * 根据单个检测规则开始检测
     *
     * @param project           当前项目
     * @param progressIndicator 进度指示器
     * @param detectorRule      配置
     * @return 扫描到的未关闭的 staticMock
     */
    private List<UnClosedStaticMockIssue> doDetectWithRule(Project project, ProgressIndicator progressIndicator, DetectorRule detectorRule) {
        if (!detectorRule.enable()) {
            return Collections.emptyList();
        }
        UnCloseDetector unCloseDetector = DETECTOR_MAP.get(detectorRule.detector());
        if (unCloseDetector == null) {
            throw new IllegalStateException("Detector " + detectorRule.detector() + " not found");
        }
        return unCloseDetector.doDetect(project, progressIndicator);
    }
}
