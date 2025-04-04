package de.maxhenkel.coordfinder.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import de.maxhenkel.coordfinder.CoordFinder;
import de.maxhenkel.coordfinder.Location;
import de.maxhenkel.coordfinder.config.PlaceConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Command("coords")
public class CoordCommands {

    @RequiresPermission("coordfinder.playerlocation")
    @Command("player")
    public void playerCoords(CommandContext<CommandSourceStack> context, @Name("player") ServerPlayer player) {
        if (CoordFinder.HIDDEN_PLAYERS.getOrDefault(player.getUUID(), false)) {
            context.getSource().sendSuccess(() -> Component.literal("Player ").append(player.getDisplayName()).append(Component.literal(" is hidden.")), false);
            return;
        }

        context.getSource().sendSuccess(() -> Component.literal("Player ")
                        .append(player.getDisplayName())
                        .append(Component.literal(" is at "))
                        .append(fromLocation(Location.fromPlayer(player)))
                        .append(".")
                , false);
    }

    @RequiresPermission("coordfinder.modifyplaces")
    @Command("setplace")
    public void setPlace(CommandContext<CommandSourceStack> context, @Name("name") String placeName, @Name("location") Optional<Vec3> location, @Name("dimension") Optional<ServerLevel> dimension) {
        setPlace(context, placeName, dimension.orElseGet(() -> context.getSource().getLevel()), location.orElseGet(() -> context.getSource().getPosition()));
    }

    @RequiresPermission("coordfinder.modifyplaces")
    @Command("removeplace")
    public void removePlace(CommandContext<CommandSourceStack> context, @Name("name") String placeName) {
        if (!PlaceConfig.isValidPlaceName(placeName)) {
            context.getSource().sendSuccess(() -> Component.literal("Invalid place name ")
                    .append(Component.literal(placeName).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(".")
                    ), false);
            return;
        }
        CoordFinder.PLACE_CONFIG.removePlace(placeName);
        context.getSource().sendSuccess(() -> Component.literal("Successfully removed ")
                .append(Component.literal(placeName).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(".")
                ), false);
    }

    @RequiresPermission("coordfinder.getplaces")
    @Command("place")
    public void place(CommandContext<CommandSourceStack> context, @Name("name") String placeName) {
        Location place = CoordFinder.PLACE_CONFIG.getPlace(placeName);

        if (place == null) {
            context.getSource().sendSuccess(() -> Component.literal("Place with name ")
                            .append(Component.literal(placeName).withStyle(ChatFormatting.GREEN))
                            .append(" not found.")
                    , false);
            return;
        }

        context.getSource().sendSuccess(() -> Component.literal("Place ")
                        .append(Component.literal(placeName).withStyle(ChatFormatting.GREEN))
                        .append(" is at ")
                        .append(fromLocation(place))
                        .append(".")
                , false);
    }

    @RequiresPermission("coordfinder.getplaces")
    @Command("listplaces")
    public void listPlaces(CommandContext<CommandSourceStack> context) {
        Set<Map.Entry<String, Location>> entries = CoordFinder.PLACE_CONFIG.getPlaces().entrySet();
        for (Map.Entry<String, Location> entry : entries) {
            context.getSource().sendSuccess(() ->
                            Component.literal("Place ")
                                    .append(Component.literal(entry.getKey()).withStyle(ChatFormatting.GREEN))
                                    .append(" is located at ")
                                    .append(fromLocation(entry.getValue()))
                    , false);
        }
        if (entries.size() <= 0) {
            context.getSource().sendSuccess(() -> Component.literal("There are no places"), false);
        }
    }

    @RequiresPermission("coordfinder.hide")
    @Command("hide")
    public void hide(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CoordFinder.HIDDEN_PLAYERS.put(player.getUUID(), true);
        context.getSource().sendSuccess(() -> Component.literal("Coordinates successfully hidden. You will stay hidden until the next server restart."), false);
    }

    @RequiresPermission("coordfinder.hide")
    @Command("unhide")
    public void unhide(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CoordFinder.HIDDEN_PLAYERS.remove(player.getUUID());
        context.getSource().sendSuccess(() -> Component.literal("Coordinates successfully unhidden."), false);
    }

    public static Component fromLocation(Location location) {
        return ComponentUtils.wrapInSquareBrackets(
                        Component.translatable("chat.coordinates", location.position().getX(), location.position().getY(), location.position().getZ())
                                .append(" in ")
                                .append(Component.literal(location.dimension().toString()))
                                .withStyle((style) -> {
                                    return style

                                            .withClickEvent(new ClickEvent.SuggestCommand("/execute in %s run tp @s %s %s %s".formatted(location.dimension(), location.position().getX(), location.position().getY(), location.position().getZ())))
                                            .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")));
                                })
                )
                .withStyle(ChatFormatting.GREEN);
    }

    private static void setPlace(CommandContext<CommandSourceStack> context, String placeName, ServerLevel dimension, Vec3 location) {
        if (!PlaceConfig.isValidPlaceName(placeName)) {
            context.getSource().sendSuccess(() ->
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
        Location loc = new Location(dimension.dimension().location(), new BlockPos((int) location.x, (int) location.y, (int) location.z));
        CoordFinder.PLACE_CONFIG.setPlace(placeName, loc);

        context.getSource().sendSuccess(() ->
                        Component.literal("Successfully set the location of ")
                                .append(Component.literal(placeName).withStyle(ChatFormatting.GREEN))
                                .append(" to ")
                                .append(fromLocation(loc))
                                .append(".")
                , false);
    }


}
