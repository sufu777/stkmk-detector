package cc.sufuzz.stkmkdetector.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class DetectorConfigLoader {
    private static final String ENV_CFG_KEY = "STKMK_DETECTOR_CFG";
    private static DetectorConfig cfgCache;

    /**
     * 从环境变量或者 resource 加载配置文件
     */
    public static DetectorConfig loadConfig() throws ConfigurationLoadException {
        String stkmkDetectorCfg = System.getenv(ENV_CFG_KEY);
        if (stkmkDetectorCfg == null) {
            return loadDefaultConfig();
        }
        Path cfgPath = Paths.get(stkmkDetectorCfg);
        if (!Files.isRegularFile(cfgPath)) {
            throw new ConfigurationLoadException("通过环境变量指定了配置文件，但配置文件不存在");
        }
        try (InputStream is = Files.newInputStream(cfgPath)) {
            return new ObjectMapper().readValue(is, DetectorConfig.class);
        } catch (IOException e) {
            throw new ConfigurationLoadException("读取配置文件异常：" + e.getLocalizedMessage(), e);
        }
    }

    private static DetectorConfig loadDefaultConfig() throws ConfigurationLoadException {
        if (Objects.isNull(cfgCache)) {
            try (InputStream is = DetectorConfigLoader.class.getClassLoader().getResourceAsStream("asserts/config.json")) {
                cfgCache = new ObjectMapper().readValue(is, DetectorConfig.class);
            } catch (IOException e) {
                throw new ConfigurationLoadException("读取默认配置失败！", e);
            }
        }
        return cfgCache;
    }
}
