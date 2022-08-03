package de.maxhenkel.coordfinder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.coordfinder.CoordFinder;
import de.maxhenkel.coordfinder.Location;
import de.maxhenkel.coordfinder.config.PlaceConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;

public class CoordCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, net.minecraft.commands.Commands.CommandSelection environment) {
        LiteralArgumentBuilder<CommandSourceStack> coordsBuilder = Commands.literal("coords");

        coordsBuilder.then(Commands.literal("player").then(Commands.argument("target", EntityArgument.player()).executes(context -> {
            ServerPlayer player = EntityArgument.getPlayer(context, "target");

            if (CoordFinder.HIDDEN_PLAYERS.getOrDefault(player.getUUID(), false)) {
                context.getSource().sendSuccess(Component.literal("Player ").append(player.getDisplayName()).append(Component.literal(" is hidden.")), false);
                return 1;
            }

            context.getSource().sendSuccess(Component.literal("Player ")
                            .append(player.getDisplayName())
                            .append(Component.literal(" is at "))
                            .append(fromLocation(Location.fromPlayer(player)))
                            .append(".")
                    , false);

            return 1;
        })));

        coordsBuilder.then(Commands.literal("setplace")
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("location", Vec3Argument.vec3())
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(context -> {
                                            String placeName = StringArgumentType.getString(context, "name");
                                            ServerLevel dimension = DimensionArgument.getDimension(context, "dimension");
                                            Vec3 location = Vec3Argument.getVec3(context, "location");
                                            setPlace(context, placeName, dimension, location);
                                            return 1;
                                        }))))
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("location", Vec3Argument.vec3()).executes(context -> {
                            String placeName = StringArgumentType.getString(context, "name");
                            Vec3 location = Vec3Argument.getVec3(context, "location");
                            setPlace(context, placeName, context.getSource().getLevel(), location);
                            return 1;
                        })))
                .then(Commands.argument("name", StringArgumentType.string()).executes(context -> {
                    String placeName = StringArgumentType.getString(context, "name");
                    setPlace(context, placeName, context.getSource().getLevel(), context.getSource().getPosition());
                    return 1;
                }))
        );

        coordsBuilder.then(Commands.literal("removeplace")
                .then(Commands.argument("name", StringArgumentType.string()).executes(context -> {
                    String placeName = StringArgumentType.getString(context, "name");
                    if (!PlaceConfig.isValidPlaceName(placeName)) {
                        context.getSource().sendSuccess(Component.literal("Invalid place name ")
                                .append(Component.literal(placeName).withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(".")
                                ), false);
                        return 1;
                    }
                    CoordFinder.PLACE_CONFIG.removePlace(placeName);
                    context.getSource().sendSuccess(Component.literal("Successfully removed ")
                            .append(Component.literal(placeName).withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(".")
                            ), false);
                    return 1;
                }))
        );

        coordsBuilder.then(Commands.literal("place")
                .then(Commands.argument("name", StringArgumentType.string()).executes(context -> {
                    String placeName = StringArgumentType.getString(context, "name");

                    Location place = CoordFinder.PLACE_CONFIG.getPlace(placeName);

                    if (place == null) {
                        context.getSource().sendSuccess(Component.literal("Place with name ")
                                        .append(Component.literal(placeName).withStyle(ChatFormatting.GREEN))
                                        .append(" not found.")
                                , false);
                        return 1;
                    }

                    context.getSource().sendSuccess(Component.literal("Place ")
                                    .append(Component.literal(placeName).withStyle(ChatFormatting.GREEN))
                                    .append(" is at ")
                                    .append(fromLocation(place))
                                    .append(".")
                            , false);

                    return 1;
                })));

        coordsBuilder.then(Commands.literal("listplaces").executes(context -> {
            Set<Map.Entry<String, Location>> entries = CoordFinder.PLACE_CONFIG.getPlaces().entrySet();
            for (Map.Entry<String, Location> entry : entries) {
                context.getSource().sendSuccess(
                        Component.literal("Place ")
                                .append(Component.literal(entry.getKey()).withStyle(ChatFormatting.GREEN))
                                .append(" is located at ")
                                .append(fromLocation(entry.getValue()))
                        , false);
            }
            if (entries.size() <= 0) {
                context.getSource().sendSuccess(Component.literal("There are no places"), false);
            }

            return 1;
        }));

        coordsBuilder.then(Commands.literal("hide").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            CoordFinder.HIDDEN_PLAYERS.put(player.getUUID(), true);
            context.getSource().sendSuccess(Component.literal("Coordinates successfully hidden. You will stay hidden until the next server restart."), false);
            return 1;
        }));

        coordsBuilder.then(Commands.literal("unhide").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            CoordFinder.HIDDEN_PLAYERS.remove(player.getUUID());
            context.getSource().sendSuccess(Component.literal("Coordinates successfully unhidden."), false);
            return 1;
        }));

        dispatcher.register(coordsBuilder);
    }

    public static Component fromLocation(Location location) {
        return ComponentUtils.wrapInSquareBrackets(
                        Component.translatable("chat.coordinates", location.position().getX(), location.position().getY(), location.position().getZ())
                                .append(" in ")
                                .append(Component.literal(location.dimension().toString()))
                                .withStyle((style) -> {
                                    return style

                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in %s run tp @s %s %s %s".formatted(location.dimension(), location.position().getX(), location.position().getY(), location.position().getZ())))
                                            .withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")));
                                })
                )
                .withStyle(ChatFormatting.GREEN);
    }

    private static void setPlace(CommandContext<CommandSourceStack> context, String placeName, ServerLevel dimension, Vec3 location) {
        if (!PlaceConfig.isValidPlaceName(placeName)) {
            context.getSource().sendSuccess(
                    Component.literal("Invalid place name. Valid characters are ")
                            .append(Component.literal("A-Z").withStyle(ChatFormatting.GREEN))
                            .append(", ")
                            .append(Component.literal("a-z").withStyle(ChatFormatting.GREEN))
                            .append(", ")
                            .append(Component.literal("-").withStyle(ChatFormatting.GREEN))
                            .append(" and ")
                            .append(Component.literal("_").withStyle(ChatFormatting.GREEN))
                            .append(".")
                    , false);
            return;
        }
        Location loc = new Location(dimension.dimension().location(), new BlockPos(location.x, location.y, location.z));
        CoordFinder.PLACE_CONFIG.setPlace(placeName, loc);

        context.getSource().sendSuccess(
                Component.literal("Successfully set the location of ")
                        .append(Component.literal(placeName).withStyle(ChatFormatting.GREEN))
                        .append(" to ")
                        .append(fromLocation(loc))
                        .append(".")
                , false);
    }


}
