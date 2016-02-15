package co.phoenixlab.discord.api2;

public class Verify {

    private Verify() {}

    public static void thatStringIsNotNull(String s, String name) {
        if (s == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    public static void thatStringIsNotEmpty(String s, String name) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
    }

    public static void thatStringIsNotEffectivelyEmpty(String s, String name) {
        if (s.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be effectively empty");
        }
    }

    public static void thatStringIsNotEffectivelyEmptyOrNull(String s, String name) {
        thatStringIsNotNull(s, name);
        thatStringIsNotEffectivelyEmpty(s, name);
    }

    public static void thatValueIsGreaterThan(int i, int min, String name) {
        if (i <= min) {
            throw new IllegalArgumentException(name + " must be greater than " + min);
        }
    }

    public static void thatValueIsAtLeast(int i, int min, String name) {
        if (i < min) {
            throw new IllegalArgumentException(name + " must be at least " + min);
        }
    }

    public static void thatValueIsEqualTo(int i, int val, String name) {
        if (i != val) {
            throw new IllegalArgumentException(name + " must be equal to " + val);
        }
    }

    public static void thatValueIsLessThan(int i, int max, String name) {
        if (i >= max) {
            throw new IllegalArgumentException(name + " must be less than " + max);
        }
    }

    public static void thatValueIsAtMost(int i, int max, String name) {
        if (i > max) {
            throw new IllegalArgumentException(name + " must be at most " + max);
        }
    }

}
