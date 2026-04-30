package cc.sufuzz.stkmkdetector.cfg;

import cc.sufuzz.stkmkdetector.service.DetectorService;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public record DetectorConfig(List<DetectorRule> rules) {
    public void validRules(DetectorService detectorService) throws ConfigurationLoadException {
        List<DetectorRule> detectorRules = detectorService.validRules(this);
        if (!detectorRules.isEmpty()) {
            List<String> invalidDetectors = detectorRules.stream().map(DetectorRule::detector).toList();
            throw new ConfigurationLoadException("配置文件有误，检测器" + StringUtils.joinWith("、", invalidDetectors));
        }
    }
}
