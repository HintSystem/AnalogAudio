package com.palm1.analogaudio.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.client.audio.api.IRadioStreamer;
import com.palm1.analogaudio.client.audio.lavaplayer.LavaplayerLoader;
import com.palm1.analogaudio.config.ModConfig;
import com.palm1.analogaudio.util.AudioUploader;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.inventory.CassetteDeckMenu;
import com.palm1.analogaudio.network.packet.EraseCassetteC2SPacket;
import com.palm1.analogaudio.network.packet.WriteCassetteC2SPacket;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;
import com.palm1.analogaudio.registry.ModSounds;

import java.io.File;
import java.util.UUID;

public class CassetteDeckScreen extends AbstractContainerScreen<CassetteDeckMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/cassette_deck.png");

    private static final ResourceLocation WRITE_NORMAL = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/write.png");
    private static final ResourceLocation WRITE_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/write_hover.png");
    private static final ResourceLocation WRITE_SELECTED = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/write_selected.png");

    private static final ResourceLocation ERASE_NORMAL = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/erase.png");
    private static final ResourceLocation ERASE_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/erase_hover.png");
    private static final ResourceLocation ERASE_SELECTED = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/erase_selected.png");

    private static final ResourceLocation COLOR_NORMAL = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/color.png");
    private static final ResourceLocation COLOR_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/color_hover.png");
    private static final ResourceLocation COLOR_SELECTED = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/color_selected.png");

    private static final ResourceLocation SLOT_NORMAL = ResourceLocation.fromNamespaceAndPath(
            AnalogAudio.MODID, "icons/cassette_slot");
    private static final ResourceLocation SLOT_HOVER = ResourceLocation.fromNamespaceAndPath(
            AnalogAudio.MODID, "icons/cassette_slot_hover");

    private static final ResourceLocation BROWSE_NORMAL = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/browse.png");
    private static final ResourceLocation BROWSE_HOVER = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/browse_hover.png");
    private static final ResourceLocation BROWSE_SELECTED = ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID,
            "textures/gui/sprites/icons/browse_selected.png");

    private EditBox urlBox;
    private EditBox nameBox;
    private int selectedColor = 0xFFFFFF;

    public enum StatusType {
        SUCCESS(0x55FF55),
        ERROR(0xFF5555),
        INFO(0xFFAA00),
        NONE(0xFFFFFF);

        public final int color;

        StatusType(int color) {
            this.color = color;
        }
    }

    public static Component statusMessage = Component.empty();
    public static StatusType statusType = StatusType.NONE;
    private int statusTimer = 0;

    public static void setStatus(Component message, StatusType type, int duration) {
        statusMessage = message;
        statusType = type;
        if (Minecraft.getInstance().screen instanceof CassetteDeckScreen screen) {
            screen.statusTimer = duration;
        }
    }

    private final java.util.Map<String, String> localPathCache = new java.util.HashMap<>();
    private ItemStack lastTape = ItemStack.EMPTY;

    public CassetteDeckScreen(CassetteDeckMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 172;
    }

    @Override
    protected void init() {
        super.init();

        this.urlBox = new EditBox(this.font, this.leftPos + 10, this.topPos + 26, 97, 10,
                Component.translatable("gui.analogaudio.cassette_deck.url")) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                int outlineColor = this.isHovered() ? 0xFF4A4441 : 0xFF352F2C;
                int bgColor = this.isHovered() ? 0xFF4E3B33 : 0xFF3E2723;

                guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1,
                        this.getY() + this.height + 1, outlineColor);
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                        bgColor);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(this.getX(), this.getY() + 1.25f, 0);
                guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
                guiGraphics.pose().translate(-this.getX(), -this.getY(), 0);

                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                guiGraphics.pose().popPose();
            }
        };
        this.urlBox.setMaxLength(256);
        this.urlBox.setHint(Component.literal("https://youtube.com/..."));
        this.urlBox.setBordered(false);
        this.urlBox.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.cassette_deck.url_disclaimer")));
        this.addRenderableWidget(this.urlBox);

        this.nameBox = new EditBox(this.font, this.leftPos + 10, this.topPos + 48, 97, 10,
                Component.translatable("gui.analogaudio.cassette_deck.name")) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                int outlineColor = this.isHovered() ? 0xFF4A4441 : 0xFF352F2C;
                int bgColor = this.isHovered() ? 0xFF4E3B33 : 0xFF3E2723;

                guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1,
                        this.getY() + this.height + 1, outlineColor);
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                        bgColor);

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(this.getX(), this.getY() + 1.25f, 0);
                guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
                guiGraphics.pose().translate(-this.getX(), -this.getY(), 0);

                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                guiGraphics.pose().popPose();
            }
        };
        this.nameBox.setMaxLength(32);
        this.nameBox.setHint(Component.literal("Fear's Mixtape"));
        this.nameBox.setBordered(false);
        this.nameBox
                .setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.cassette_deck.name_disclaimer")));
        this.addRenderableWidget(this.nameBox);

        DyeColor[] orderedColors = {
                DyeColor.WHITE, DyeColor.LIGHT_GRAY,
                DyeColor.GRAY, DyeColor.BLACK,
                DyeColor.BROWN, DyeColor.RED,
                DyeColor.ORANGE, DyeColor.YELLOW,
                DyeColor.LIME, DyeColor.GREEN,
                DyeColor.CYAN, DyeColor.LIGHT_BLUE,
                DyeColor.BLUE, DyeColor.PURPLE,
                DyeColor.MAGENTA, DyeColor.PINK
        };

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                final DyeColor color = orderedColors[row * 4 + col];
                final int colorVal = 0xFF000000 | color.getFireworkColor();
                ImageButton colorBtn = new ImageButton(this.leftPos + 113 + (col * 13), this.topPos + 26 + (row * 14),
                        12, 13,
                        new WidgetSprites(COLOR_NORMAL, COLOR_NORMAL), button -> {
                            this.selectedColor = colorVal;
                            ItemStack stack = this.menu.getSlot(0).getItem();
                            if (!stack.isEmpty()
                                    && stack.is(ModItems.CASSETTE_TAPE.get())) {
                                CassetteData oldData = stack
                                        .get(ModDataComponents.CASSETTE_DATA.get());
                                String uuid = oldData != null ? oldData.uuid() : UUID.randomUUID().toString();
                                String url = oldData != null ? oldData.url() : "";
                                String name = oldData != null ? oldData.name() : "";
                                stack.set(ModDataComponents.CASSETTE_DATA.get(),
                                        new CassetteData(uuid, url, name, colorVal,
                                                oldData != null ? oldData.volume() : -1.0f,
                                                oldData != null ? oldData.duration() : 0L,
                                                oldData != null ? oldData.authorUuid() : ""));
                            }
                        }) {
                    @Override
                    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                        int r = ((colorVal >> 16) & 0xFF);
                        int g = ((colorVal >> 8) & 0xFF);
                        int b = (colorVal & 0xFF);
                        guiGraphics.setColor(r / 255.0f, g / 255.0f, b / 255.0f, 1.0f);

                        ResourceLocation texture = COLOR_NORMAL;
                        if (selectedColor == colorVal) {
                            texture = COLOR_SELECTED;
                        } else if (this.isHovered()) {
                            texture = COLOR_HOVER;
                        }

                        guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width,
                                this.height);
                        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    }
                };
                this.addRenderableWidget(colorBtn);
            }
        }

        ImageButton writeBtn = new ImageButton(this.leftPos + 8, this.topPos + 67, 18, 18,
                new WidgetSprites(WRITE_NORMAL, WRITE_NORMAL), button -> {
                    if (this.menu.getSlot(0).hasItem()) {
                        String url = this.urlBox.getValue();
                        if (url.startsWith("client:")) {
                            url = localPathCache.getOrDefault(url, url);
                        }
                        final String finalUrl = url;

                        if (url.isEmpty()) {
                            PacketDistributor.sendToServer(
                                    new WriteCassetteC2SPacket(url, this.nameBox.getValue(),
                                            this.selectedColor, 0L));
                            setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.writing"),
                                    StatusType.INFO, 60);
                            return;
                        }

                        if (validateUrl(url) == UrlValidationResult.DISALLOWED) {
                            setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.disallowed_url"),
                                    StatusType.ERROR, 60);
                            return;
                        }

                        setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.fetching_metadata"),
                                StatusType.INFO, 1000);

                        IRadioStreamer streamer = LavaplayerLoader.getUtilityStreamer();
                        if (streamer == null) {
                            setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.write_fail"),
                                    StatusType.ERROR, 60);
                            return;
                        }

                        streamer.fetchDuration(finalUrl, duration -> {
                            Minecraft.getInstance().execute(() -> {
                                PacketDistributor.sendToServer(
                                        new WriteCassetteC2SPacket(finalUrl, this.nameBox.getValue(),
                                                this.selectedColor, duration));
                                setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.writing"),
                                        StatusType.INFO, 60);
                            });
                        });
                    } else {
                        setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.insert_cassette"),
                                StatusType.ERROR, 60);
                    }
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                ResourceLocation texture = WRITE_NORMAL;
                if (this.isHovered()) {
                    if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                        texture = WRITE_SELECTED;
                    } else {
                        texture = WRITE_HOVER;
                    }
                }
                guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width,
                        this.height);
            }

            @Override
            public void playDownSound(SoundManager handler) {
                handler.play(SimpleSoundInstance.forUI(ModSounds.WRITE.get(), 1.0f));
            }
        };
        writeBtn.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.cassette_deck.write")
                .append("\n§7")
                .append(Component.translatable("gui.analogaudio.cassette_deck.write_disclaimer"))));
        this.addRenderableWidget(writeBtn);

        ImageButton eraseBtn = new ImageButton(this.leftPos + 26, this.topPos + 67, 18, 18,
                new WidgetSprites(ERASE_NORMAL, ERASE_NORMAL), button -> {
                    if (this.menu.getSlot(0).hasItem()) {
                        PacketDistributor.sendToServer(
                                new EraseCassetteC2SPacket());
                        setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.erased"),
                                StatusType.SUCCESS, 60);
                    } else {
                        setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.insert_cassette"),
                                StatusType.ERROR, 60);
                    }
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                ResourceLocation texture = ERASE_NORMAL;
                if (this.isHovered()) {
                    if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                        texture = ERASE_SELECTED;
                    } else {
                        texture = ERASE_HOVER;
                    }
                }
                guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width,
                        this.height);
            }

            @Override
            public void playDownSound(SoundManager handler) {
                handler.play(SimpleSoundInstance.forUI(ModSounds.ERASE.get(), 1.0f));
            }
        };
        eraseBtn.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.cassette_deck.erase")
                .append("\n§7")
                .append(Component.translatable("gui.analogaudio.cassette_deck.erase_disclaimer"))));
        this.addRenderableWidget(eraseBtn);

        ImageButton browseBtn = new ImageButton(this.leftPos + 44, this.topPos + 67, 18, 18,
                new WidgetSprites(BROWSE_NORMAL, BROWSE_NORMAL), button -> {
                    PointerBuffer filters = MemoryUtil.memAllocPointer(7);
                    filters.put(MemoryUtil.memUTF8("*.ogg"));
                    filters.put(MemoryUtil.memUTF8("*.mp3"));
                    filters.put(MemoryUtil.memUTF8("*.wav"));
                    filters.put(MemoryUtil.memUTF8("*.flac"));
                    filters.put(MemoryUtil.memUTF8("*.aac"));
                    filters.put(MemoryUtil.memUTF8("*.m4a"));
                    filters.put(MemoryUtil.memUTF8("*.opus"));
                    filters.flip();

                    String path = TinyFileDialogs.tinyfd_openFileDialog(
                            Component.translatable("gui.analogaudio.cassette_deck.browse").getString(),
                            "",
                            filters,
                            "Audio Files (*.ogg, *.mp3, *.wav, *.flac, *.aac, *.m4a, *.opus)",
                            false);

                    org.lwjgl.system.MemoryUtil.memFree(filters);

                    if (path != null) {
                        File file = new File(path);
                        if (file.exists()) {
                            long maxBytes = ModConfig.FileServer.maxFileSize * 1024 * 1024;
                            if (file.length() > maxBytes) {
                                setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.file_too_large"),
                                        StatusType.ERROR, 100);
                                return;
                            }

                            setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.uploading"),
                                    StatusType.INFO, 1000);
                            AudioUploader.upload(file).thenAccept(url -> {
                                if (url.startsWith("client:")) {
                                    localPathCache.put(url, "file:///" + file.getAbsolutePath().replace("\\", "/"));
                                }
                                this.minecraft.execute(() -> {
                                    this.urlBox.setValue(url);
                                    setStatus(
                                            Component.translatable(
                                                    "gui.analogaudio.cassette_deck.status.upload_success"),
                                            StatusType.SUCCESS, 60);
                                });
                            }).exceptionally(ex -> {
                                this.minecraft.execute(() -> {
                                    String msg = ex.getMessage();
                                    if (msg != null && msg.contains("File too large")) {
                                        setStatus(
                                                Component.translatable(
                                                        "gui.analogaudio.cassette_deck.status.file_too_large"),
                                                StatusType.ERROR, 60);
                                    } else if (msg != null && msg.contains("File format not allowed")) {
                                        setStatus(
                                                Component.translatable(
                                                        "gui.analogaudio.cassette_deck.status.disallowed_format"),
                                                StatusType.ERROR, 60);
                                    } else {
                                        setStatus(
                                                Component.translatable(
                                                        "gui.analogaudio.cassette_deck.status.upload_fail"),
                                                StatusType.ERROR, 60);
                                    }
                                });
                                return null;
                            });
                        }
                    }
                }) {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                ResourceLocation texture = BROWSE_NORMAL;
                if (this.active && this.isHovered()) {
                    if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(),
                            GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                        texture = BROWSE_SELECTED;
                    } else {
                        texture = BROWSE_HOVER;
                    }
                }
                if (!this.active) {
                    guiGraphics.setColor(0.5f, 0.5f, 0.5f, 1.0f);
                }
                guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width,
                        this.height);
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        };

        boolean fileUploads = ModConfig.Synced.allowFileUploads;
        browseBtn.active = fileUploads;
        if (fileUploads) {
            browseBtn.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.cassette_deck.browse")
                    .append("\n§7")
                    .append(Component.translatable("gui.analogaudio.cassette_deck.browse_disclaimer"))));
        } else {
            browseBtn.setTooltip(Tooltip.create(Component.translatable("gui.analogaudio.cassette_deck.browse")
                    .append("\n§c")
                    .append(Component.translatable("gui.analogaudio.cassette_deck.browse_disabled"))));
        }
        this.addRenderableWidget(browseBtn);
    }

    private enum UrlValidationResult {
        ALLOWED,
        DISALLOWED
    }

    private UrlValidationResult validateUrl(String url) {
        if (url.isEmpty())
            return UrlValidationResult.ALLOWED;
        String lower = url.toLowerCase();

        if (lower.startsWith("server:") || lower.startsWith("file:///")) {
            return UrlValidationResult.ALLOWED;
        }

        boolean match = false;
        java.util.List<String> domains = ModConfig.Synced.whitelistedUrls;
        for (String domain : domains) {
            if (lower.contains(domain.toLowerCase())) {
                match = true;
                break;
            }
        }

        boolean isBlacklist = ModConfig.Synced.whitelistAsBlacklist;
        if (isBlacklist) {
            return match ? UrlValidationResult.DISALLOWED : UrlValidationResult.ALLOWED;
        } else {
            if (lower.endsWith(".ogg") || lower.contains(".ogg?") ||
                    lower.endsWith(".mp3") || lower.contains(".mp3?") ||
                    lower.endsWith(".wav") || lower.contains(".wav?") ||
                    lower.endsWith(".flac") || lower.contains(".flac?") ||
                    lower.endsWith(".aac") || lower.contains(".aac?") ||
                    lower.endsWith(".m4a") || lower.contains(".m4a?") ||
                    lower.endsWith(".opus") || lower.contains(".opus?")) {
                return UrlValidationResult.ALLOWED;
            }
            return match ? UrlValidationResult.ALLOWED : UrlValidationResult.DISALLOWED;
        }
    }

    public void handleResult(int status) {
        switch (status) {
            case 0 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.write_success"),
                    StatusType.SUCCESS, 100);
            case 1 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.write_fail"),
                    StatusType.ERROR, 100);
            case 2 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.erase_success"),
                    StatusType.SUCCESS, 100);
            case 3 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.erase_fail"),
                    StatusType.ERROR, 100);
            case 4 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.invalid_url"),
                    StatusType.ERROR, 100);
            case 5 -> setStatus(Component.translatable("gui.analogaudio.cassette_deck.status.disallowed_format"),
                    StatusType.ERROR, 100);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (statusTimer > 0) {
            statusTimer--;
            if (statusTimer == 0) {
                statusMessage = Component.empty();
                statusType = StatusType.NONE;
            }
        }

        ItemStack stack = this.menu.getSlot(0).getItem();
        CassetteData currentData = stack.get(ModDataComponents.CASSETTE_DATA.get());
        CassetteData lastData = lastTape.get(ModDataComponents.CASSETTE_DATA.get());

        boolean itemChanged = (stack.getItem() != lastTape.getItem());
        boolean dataPresenceChanged = ((currentData == null) != (lastData == null));
        boolean uuidChanged = (currentData != null && lastData != null
                && !java.util.Objects.equals(currentData.uuid(), lastData.uuid()));
        boolean urlChanged = (currentData != null && lastData != null
                && !java.util.Objects.equals(currentData.url(), lastData.url()));
        boolean nameChanged = (currentData != null && lastData != null
                && !java.util.Objects.equals(currentData.name(), lastData.name()));

        boolean isCurrentBlank = (currentData == null || (currentData.url().isEmpty() && currentData.name().isEmpty()));
        boolean isLastBlank = (lastData == null || (lastData.url().isEmpty() && lastData.name().isEmpty()));
        boolean bothBlank = isCurrentBlank && isLastBlank;
        boolean dataChanged = dataPresenceChanged || uuidChanged || urlChanged || nameChanged;

        if (itemChanged || (dataChanged && !bothBlank)) {
            lastTape = stack.copy();
            if (stack.isEmpty()) {
                this.urlBox.setValue("");
                this.nameBox.setValue("");
                this.selectedColor = 0xFFFFFFFF;
            } else if (stack.is(ModItems.CASSETTE_TAPE.get())) {
                if (currentData != null) {
                    String url = currentData.url();
                    if (url.startsWith("file:///")) {
                        String filename = url.substring(url.lastIndexOf('/') + 1);
                        String clientUrl = "client:" + filename;
                        localPathCache.put(clientUrl, url);
                        url = clientUrl;
                    }
                    this.urlBox.setValue(url);
                    this.nameBox.setValue(currentData.name());
                    this.selectedColor = currentData.color();
                } else {
                    this.urlBox.setValue("");
                    this.nameBox.setValue("");
                    this.selectedColor = 0xFFFFFF;
                }
            }
        } else {
            lastTape = stack.copy();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        if (this.urlBox.isFocused() && this.urlBox.keyPressed(keyCode, scanCode, modifiers))
            return true;
        if (this.nameBox.isFocused() && this.nameBox.keyPressed(keyCode, scanCode, modifiers))
            return true;
        if (this.urlBox.isFocused() || this.nameBox.isFocused())
            return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (slot.index == 0) {
            if (slot.getItem().isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(slot.x, slot.y, 0);
                guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
                guiGraphics.pose().translate(-slot.x, -slot.y, 0);
                renderGhostItem(guiGraphics, ModItems.CASSETTE_TAPE.get().getDefaultInstance(), slot.x, slot.y,
                        this.hoveredSlot == slot);
                guiGraphics.pose().popPose();
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(slot.x, slot.y, 0);
            guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
            guiGraphics.pose().translate(-slot.x, -slot.y, 0);
            super.renderSlot(guiGraphics, slot);
            guiGraphics.pose().popPose();
        } else {
            super.renderSlot(guiGraphics, slot);
        }
    }

    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, boolean hovered) {
        float alpha = hovered ? 0.6f : 0.3f;
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        guiGraphics.renderItem(stack, x, y);
        if (hovered) {
            guiGraphics.fill(RenderType.guiGhostRecipeOverlay(), x, y, x + 16, y + 16, 0x30FFFFFF);
        }
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        Slot slot0 = this.menu.slots.get(0);
        if (x == slot0.x && y == slot0.y) {
            return super.isHovering(slot0.x - 1, slot0.y + 5, 34, 22, mouseX, mouseY);
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight,
                this.imageWidth, this.imageHeight);

        Slot slot0 = this.menu.slots.get(0);
        int slotX = this.leftPos + slot0.x - 1;
        int slotY = this.topPos + slot0.y + 5;

        ResourceLocation slotTexture = (this.hoveredSlot == slot0) ? SLOT_HOVER : SLOT_NORMAL;
        guiGraphics.blitSprite(slotTexture, slotX, slotY, 34, 22);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(10, 17, 0);
        guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
        guiGraphics.drawString(this.font, Component.translatable("gui.analogaudio.cassette_deck.url"), 0, 0, 0xFFFFFF,
                false);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(10, 39, 0);
        guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
        guiGraphics.drawString(this.font, Component.translatable("gui.analogaudio.cassette_deck.name"), 0, 0, 0xFFFFFF,
                false);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(112, 17, 0);
        guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
        guiGraphics.drawString(this.font, Component.translatable("gui.analogaudio.cassette_deck.select_color"), 0, 0,
                0xFFFFFF, false);
        guiGraphics.pose().popPose();

        if (statusMessage != Component.empty()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(10, 61, 0);
            guiGraphics.pose().scale(0.6f, 0.6f, 1.0f);

            guiGraphics.drawString(this.font, Component.literal("§o").append(statusMessage), 0, 0, statusType.color,
                    false);
            guiGraphics.pose().popPose();
        }
    }
}
