package co.phoenixlab.discord.util;

import co.phoenixlab.common.localization.LocaleStringProvider;

import java.util.Locale;

/**
 * End-of-line missing string provider. REGISTER THIS PROVIDER FIRST.
 */
public class NoMatchedStringLocaleStringProvider implements LocaleStringProvider {
    @Override
    public void setActiveLocale(Locale locale) {

    }

    @Override
    public String get(String key) {
        return key;
    }

    @Override
    public boolean contains(String key) {
        return false;
    }
}
