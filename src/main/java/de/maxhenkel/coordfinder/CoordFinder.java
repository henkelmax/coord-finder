package de.maxhenkel.coordfinder;

import de.maxhenkel.coordfinder.command.CoordCommands;
import de.maxhenkel.coordfinder.config.PlaceConfig;
import de.maxhenkel.coordfinder.config.ServerConfig;
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
    public static ServerConfig SERVER_CONFIG;

    public static PlaceConfig PLACE_CONFIG;
    public static Map<UUID, Boolean> HIDDEN_PLAYERS = new HashMap<>();

    @Override
    public void onInitialize() {
        // SERVER_CONFIG = ConfigBuilder.build(FabricLoader.getInstance().getConfigDir().resolve(MODID).resolve("%s-server.properties".formatted(MODID)), ServerConfig::new);
        PLACE_CONFIG = new PlaceConfig(FabricLoader.getInstance().getConfigDir().resolve(MODID).resolve("places.properties"));
        CommandRegistrationCallback.EVENT.register(CoordCommands::register);
    }

}
