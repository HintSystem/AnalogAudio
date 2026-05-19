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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.permission.PermissionAPI;

import java.util.UUID;

public class CassetteWriteCommand {
    public static void register(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal("write")
                .requires(source -> {
                    if (source.getEntity() instanceof ServerPlayer player) {
                        return player.hasPermissions(4)
                                || PermissionAPI.getPermission(player, ModPermissions.CASSETTE_WRITE);
                    }
                    return source.hasPermission(4);
                })
                .then(Commands.argument("url", StringArgumentType.string())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> execute(context, StringArgumentType.getString(context, "url"),
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(context -> execute(context, StringArgumentType.getString(context, "url"), ""))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, String url, String name)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        final String finalUrl = ModCommands.stripQuotes(url);
        final String finalName = ModCommands.stripQuotes(name);

        if (finalName.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.analogaudio.cassette.write.resolving")
                    .withStyle(ChatFormatting.YELLOW));
            PlaylistResolver.resolvePlaylist(finalUrl).thenAccept(tracks -> {
                if (!tracks.isEmpty()) {
                    IPlaylistResolver.ResolvedTrack track = tracks.get(0);
                    giveCassette(player, track.url(), track.name(), track.duration());
                } else {
                    giveCassette(player, finalUrl, finalUrl, 0L);
                }
            }).exceptionally(ex -> {
                player.getServer().execute(() -> {
                    giveCassette(player, finalUrl, finalUrl, 0L);
                });
                return null;
            });
        } else {
            giveCassette(player, finalUrl, finalName, 0L);
        }
        return 1;
    }

    private static void giveCassette(ServerPlayer player, String url, String name, long duration) {
        player.getServer().execute(() -> {
            ItemStack cassette = new ItemStack(ModItems.CASSETTE_TAPE.get());
            CassetteData data = new CassetteData(
                    UUID.randomUUID().toString(),
                    url,
                    name,
                    0xFFFFFF,
                    -1.0f,
                    duration,
                    player.getUUID().toString());
            cassette.set(ModDataComponents.CASSETTE_DATA.get(), data);

            boolean added = player.getInventory().add(cassette);
            if (!added) {
                ItemEntity itemEntity = player.drop(cassette, false);
                if (itemEntity != null) {
                    itemEntity.setNoPickUpDelay();
                    itemEntity.setTarget(player.getUUID());
                }
            }

            player.sendSystemMessage(Component.translatable("commands.analogaudio.cassette.write.success",
                    Component.literal(name).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC))
                    .withStyle(ChatFormatting.GREEN));
        });
    }
}
