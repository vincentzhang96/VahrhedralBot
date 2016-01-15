package co.phoenixlab.discord.ncs;

public interface Enableable {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    default void enable() {
        setEnabled(true);
    }

    default void disable() {
        setEnabled(false);
    }

    default boolean toggle() {
        boolean newVal = !isEnabled();
        setEnabled(newVal);
        return newVal;
    }

}
