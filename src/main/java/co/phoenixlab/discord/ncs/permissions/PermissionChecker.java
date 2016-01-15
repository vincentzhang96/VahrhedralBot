package co.phoenixlab.discord.ncs.permissions;

import co.phoenixlab.discord.ncs.CommandContext;

@FunctionalInterface
public interface PermissionChecker {

    boolean test(CommandContext context);

    default PermissionChecker and(PermissionChecker other) {
        return c -> test(c) && other.test(c);
    }

    default PermissionChecker or(PermissionChecker other) {
        return c -> test(c) || other.test(c);
    }

    default PermissionChecker negate() {
        return c -> !test(c);
    }

}
