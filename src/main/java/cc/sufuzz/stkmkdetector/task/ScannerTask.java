package cc.sufuzz.stkmkdetector.task;

import cc.sufuzz.stkmkdetector.cfg.ConfigurationLoadException;
import cc.sufuzz.stkmkdetector.cfg.DetectorConfig;
import cc.sufuzz.stkmkdetector.cfg.DetectorConfigLoader;
import cc.sufuzz.stkmkdetector.detectors.DetectResult;
import cc.sufuzz.stkmkdetector.i18n.I18nBundle;
import cc.sufuzz.stkmkdetector.service.DetectResultManager;
import cc.sufuzz.stkmkdetector.service.DetectorService;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ScannerTask extends Task.Backgroundable {
    private final Project project;

    public ScannerTask(@Nullable Project project) {
        super(project, I18nBundle.message("plugin.task.title"), true);
        this.project = project;
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        if (Objects.isNull(project)) {
            return;
        }
        DetectorService detectorService = project.getService(DetectorService.class);
        DetectorConfig detectorConfig;
        try {
            detectorConfig = DetectorConfigLoader.loadConfig();
            detectorConfig.validRules(detectorService);
        } catch (ConfigurationLoadException e) {
            NotificationGroupManager.getInstance().getNotificationGroup("stkmk-detector")
                    .createNotification(I18nBundle.message("plugin.config.load.err.text"), e.getMessage(), NotificationType.ERROR);
            return;
        }
        DetectResult detectResult = detectorService.doDetect(project, progressIndicator, detectorConfig);
        DetectResultManager service = project.getService(DetectResultManager.class);
        service.addDetectResult(detectResult);
    }
}
