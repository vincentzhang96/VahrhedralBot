package co.phoenixlab.discord.util;

import java.util.Locale;
import java.util.ResourceBundle;

import co.phoenixlab.common.lang.number.ParseLong;
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

	public static double parseAlphanumeric(String value, Localizer localizer) {
	//  Check if it ends in thousand or million
        String thousandSuffix = localizer.localize("commands.dn.defense.suffix.thousand");
        int start = value.indexOf(thousandSuffix);
        double working;
        double ret = 0;
        if (start == 0) {
            //  Invalid, cannot be just "k"
            throw new NumberFormatException();
        }
        if (start != -1) {
            String num = value.substring(0, start);
            try {
                working = Double.parseDouble(num);
            } 
            catch (NumberFormatException nfe) {
                throw new NumberFormatException();
            }
            ret = working * 1000.0;
        }
        else {
            String millionSuffix = localizer.localize("commands.dn.defense.suffix.million");
            start = value.indexOf(millionSuffix);
            if (start == 0) {
                //  Invalid, cannot be just "m"
                throw new NumberFormatException();
            }
            if (start != -1) {
                String num = value.substring(0, start);
                try {
                    working = Double.parseDouble(num);
                } 
                catch (NumberFormatException nfe) {
                    throw new NumberFormatException();
                }
                ret = working * 1000000.0;
            }
            else {
                String billionSuffix = localizer.localize("commands.dn.defense.suffix.billion");
                start = value.indexOf(billionSuffix);
                if (start == 0) {
                    //  Invalid, cannot be just "b"
                    throw new NumberFormatException();
                }
                if (start != -1) {
                    String num = value.substring(0, start);
                    try {
                        working = Double.parseDouble(num);
                    }
                    catch (NumberFormatException nfe) {
                        throw new NumberFormatException();
                    }
                    ret = working * 1000000000.0;
                } 
                else {
                    ret = ParseLong.parseDec(value);
                }
            }
        }
        return ret;
	}
	
}
