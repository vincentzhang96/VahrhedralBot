package co.phoenixlab.discord.util;

import co.phoenixlab.common.localization.LocaleStringProvider;

import java.util.Locale;
import java.util.ResourceBundle;

public class ResourceBundleLocaleStringProvider implements LocaleStringProvider {


    ResourceBundle bundle;
    private String bundleName;

    public ResourceBundleLocaleStringProvider(String bundleName) {
        this.bundleName = bundleName;
    }

    @Override
    public void setActiveLocale(Locale locale) {
        bundle = ResourceBundle.getBundle(bundleName, locale);
    }

    @Override
    public String get(String key) {
        if (!contains(key)) {
            return key;
        }
        return bundle.getString(key);
    }

    @Override
    public boolean contains(String key) {
        return bundle.containsKey(key);
    }
}
