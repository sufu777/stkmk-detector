package cc.sufuzz.stkmkdetector.i18n;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.PropertyKey;

public class I18nBundle {
    private static final String BUNDLE_NAME = "message.i18n";
    private static final DynamicBundle INSTANCE = new DynamicBundle(I18nBundle.class, BUNDLE_NAME);

    private I18nBundle() {
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... args) {
        return INSTANCE.getMessage(key, args);
    }
}
