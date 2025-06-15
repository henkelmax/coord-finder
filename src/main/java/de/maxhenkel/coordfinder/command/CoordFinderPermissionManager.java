package de.maxhenkel.coordfinder.command;

import de.maxhenkel.admiral.permissions.PermissionManager;
import de.maxhenkel.coordfinder.CoordFinder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;

public class CoordFinderPermissionManager implements PermissionManager<CommandSourceStack> {

    public static final CoordFinderPermissionManager INSTANCE = new CoordFinderPermissionManager();

    private static final Permission PLAYER_LOCATION = new Permission("coordfinder.playerlocation", PermissionType.EVERYONE);
    private static final Permission MODIFY_PLACES = new Permission("coordfinder.modifyplaces", PermissionType.EVERYONE);
    private static final Permission GET_PLACES = new Permission("coordfinder.getplaces", PermissionType.EVERYONE);
    private static final Permission HIDE = new Permission("coordfinder.hide", PermissionType.EVERYONE);

    private static final List<Permission> PERMISSIONS = List.of(
            PLAYER_LOCATION,
            MODIFY_PLACES,
            GET_PLACES,
            HIDE
    );

    @Override
    public boolean hasPermission(CommandSourceStack stack, String permission) {
        for (Permission p : PERMISSIONS) {
            if (!p.permission.equals(permission)) {
                continue;
            }
            if (stack.isPlayer()) {
                return p.hasPermission(stack.getPlayer());
            }
            if (p.getType().equals(PermissionType.OPS)) {
                return stack.hasPermission(stack.getServer().getFunctionCompilationLevel());
            } else {
                return p.hasPermission(null);
            }
        }
        return false;
    }

    private static Boolean loaded;

    private static boolean isFabricPermissionsAPILoaded() {
        if (loaded == null) {
            loaded = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
            if (loaded) {
                CoordFinder.LOGGER.info("Using Fabric Permissions API");
            }
        }
        return loaded;
    }

    private static class Permission {
        private final String permission;
        private final PermissionType type;

        public Permission(String permission, PermissionType type) {
            this.permission = permission;
            this.type = type;
        }

        public boolean hasPermission(@Nullable ServerPlayer player) {
            if (isFabricPermissionsAPILoaded()) {
                return checkFabricPermission(player);
            }
            return type.hasPermission(player);
        }

        private boolean checkFabricPermission(@Nullable ServerPlayer player) {
            if (player == null) {
                return false;
            }
            TriState permissionValue = Permissions.getPermissionValue(player, permission);
            switch (permissionValue) {
                case DEFAULT:
                    return type.hasPermission(player);
                case TRUE:
                    return true;
                case FALSE:
                default:
                    return false;
            }
        }

        public PermissionType getType() {
            return type;
        }
    }

    private static enum PermissionType {

        EVERYONE, NOONE, OPS;

        boolean hasPermission(@Nullable ServerPlayer player) {
            return switch (this) {
                case EVERYONE -> true;
                case NOONE -> false;
                case OPS -> player != null && player.hasPermissions(player.getServer().getOperatorUserPermissionLevel());
            };
        }

    }

}
