package de.maxhenkel.coordfinder;

import de.maxhenkel.admiral.MinecraftAdmiral;
import de.maxhenkel.coordfinder.command.CoordCommands;
import de.maxhenkel.coordfinder.command.CoordFinderPermissionManager;
import de.maxhenkel.coordfinder.config.PlaceConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CoordFinder implements ModInitializer {

    public static final String MODID = "coordfinder";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static PlaceConfig PLACE_CONFIG;
    public static Map<UUID, Boolean> HIDDEN_PLAYERS = new HashMap<>();

    @Override
    public void onInitialize() {
        PLACE_CONFIG = new PlaceConfig(FabricLoader.getInstance().getConfigDir().resolve(MODID).resolve("places.properties"));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MinecraftAdmiral.builder(dispatcher, registryAccess).addCommandClasses(
                    CoordCommands.class
            ).setPermissionManager(CoordFinderPermissionManager.INSTANCE).build();
        });
    }

}
