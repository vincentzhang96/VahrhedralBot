package co.phoenixlab.discord.util;

import java.util.Locale;
import java.util.ResourceBundle;

import co.phoenixlab.common.localization.LocaleStringProvider;
import co.phoenixlab.common.localization.Localizer;
import co.phoenixlab.common.localization.LocalizerImpl;

public class StringUtilities {

	public static Localizer getLocalizer() {
		Localizer localizer = new LocalizerImpl(Locale.getDefault());
        localizer.registerPluralityRules(LocalizerImpl.defaultPluralityRules());
        LocaleStringProvider provider = new LocaleStringProvider() {
            ResourceBundle bundle;
            @Override
            public void setActiveLocale(Locale locale) {
                bundle = ResourceBundle.getBundle("co.phoenixlab.discord.resources.locale", locale);
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
        };
        localizer.addLocaleStringProvider(provider);
        return localizer;
	}
}
