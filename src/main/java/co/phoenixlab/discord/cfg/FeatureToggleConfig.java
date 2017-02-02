package co.phoenixlab.discord.cfg;

import java.util.HashMap;
import java.util.Map;

public class FeatureToggleConfig {

    private Map<String, FeatureToggle> toggles;

    public FeatureToggleConfig() {
        toggles = new HashMap<>();
    }

    public Map<String, FeatureToggle> getToggles() {
        return toggles;
    }

    public FeatureToggle getToggle(String key) {
        FeatureToggle featureToggle = toggles.get(key);
        if (featureToggle == null) {
            featureToggle = new FeatureToggle();
            toggles.put(key, featureToggle);
        }
        return featureToggle;
    }
}
