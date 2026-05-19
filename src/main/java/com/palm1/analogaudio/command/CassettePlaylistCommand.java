package com.palm1.analogaudio.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;
import com.palm1.analogaudio.util.ModPermissions;
import com.palm1.analogaudio.util.PlaylistResolver;
import com.palm1.analogaudio.client.audio.api.IPlaylistResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.server.permission.PermissionAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CassettePlaylistCommand {
    private static final DyeColor[] DYE_COLORS = DyeColor.values();
    private static final Random RANDOM = new Random();

    private static int getRandomDyeColor() {
        DyeColor color = DYE_COLORS[RANDOM.nextInt(DYE_COLORS.length)];
        return 0xFF000000 | color.getFireworkColor();
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal("playlist")
                .requires(source -> {
                    if (source.getEntity() instanceof ServerPlayer player) {
                        return player.hasPermissions(4)
                                || PermissionAPI.getPermission(player, ModPermissions.CASSETTE_PLAYLIST);
                    }
                    return source.hasPermission(4);
                })
                .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(context -> execute(context, StringArgumentType.getString(context, "url")))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, String url) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        final String finalUrl = ModCommands.stripQuotes(url);

        player.sendSystemMessage(Component.translatable("commands.analogaudio.cassette.playlist.resolving")
                .withStyle(ChatFormatting.YELLOW));
        PlaylistResolver.resolvePlaylist(finalUrl).thenAccept(tracks -> {
            if (tracks.isEmpty()) {
                player.sendSystemMessage(Component.translatable("commands.analogaudio.cassette.playlist.empty")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            player.getServer().execute(() -> {
                ItemStack bag = new ItemStack(ModItems.CASSETTE_BAG.get());
                List<ItemStack> cassettes = new ArrayList<>();
                int count = 0;

                for (IPlaylistResolver.ResolvedTrack track : tracks) {
                    if (count >= 18) {
                        break;
                    }

                    ItemStack cassette = new ItemStack(ModItems.CASSETTE_TAPE.get());
                    CassetteData data = new CassetteData(
                            UUID.randomUUID().toString(),
                            track.url(),
                            track.name(),
                            getRandomDyeColor(),
                            -1.0f,
                            track.duration(),
                            player.getUUID().toString());
                    cassette.set(ModDataComponents.CASSETTE_DATA.get(), data);
                    cassettes.add(cassette);
                    count++;
                }

                bag.set(ModDataComponents.BAG_CONTENTS.get(), ItemContainerContents.fromItems(cassettes));

                boolean added = player.getInventory().add(bag);
                if (!added) {
                    ItemEntity itemEntity = player.drop(bag, false);
                    if (itemEntity != null) {
                        itemEntity.setNoPickUpDelay();
                        itemEntity.setTarget(player.getUUID());
                    }
                }

                player.sendSystemMessage(Component.translatable("commands.analogaudio.cassette.playlist.success", count)
                        .withStyle(ChatFormatting.GREEN));
            });
        }).exceptionally(ex -> {
            player.getServer().execute(() -> {
                player.sendSystemMessage(
                        Component.translatable("commands.analogaudio.cassette.playlist.failed", ex.getMessage())
                                .withStyle(ChatFormatting.RED));
            });
            return null;
        });
        return 1;
    }
}
