package com.palm1.analogaudio.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import org.joml.Matrix4f;

import com.palm1.analogaudio.AnalogAudio;
import com.palm1.analogaudio.integration.voicechat.VoicechatApiHandle;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModSounds;

public class WalkieTalkieRenderer extends BlockEntityWithoutLevelRenderer {

    public static final double SCREEN_X_CENTER = 0.5D;
    public static final double SCREEN_Y_OFFSET = 3.2D / 16.0D;
    public static final double SCREEN_Z_OFFSET = 3.15D / 16.0D;

    public static final float TEXT_SCALE = 0.025f;
    public static final float INDICATOR_SCALE = 0.015f;

    public static final float FREQ_X_OFFSET = 0f;
    public static final float FREQ_Y_OFFSET = -3.5f;

    public static final float INDICATOR_X_POS = 7f;
    public static final float INDICATOR_Y_POS = -5f;

    public static final float BUTTON_TRAVEL_MAX = 0.9f / 16.0f;

    public static final ModelResourceLocation BODY_MODEL = ModelResourceLocation
            .standalone(ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "item/walkie_talkie_body"));
    public static final ModelResourceLocation BUTTON_MODEL = ModelResourceLocation
            .standalone(ResourceLocation.fromNamespaceAndPath(AnalogAudio.MODID, "item/walkie_talkie_button"));

    private static float currentButtonOffset = 0f;
    private static float buttonVelocity = 0f;
    private static long lastFrameTime = -1;
    private boolean wasPttActive = false;

    public WalkieTalkieRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @SuppressWarnings("unused")
    private int renderCount = 0;

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            poseStack.popPose();
            return;
        }

        boolean isFirstPerson = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        boolean isThirdPerson = displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        boolean isGui = displayContext == ItemDisplayContext.GUI;

        BakedModel bodyModel = mc.getModelManager().getModel(BODY_MODEL);
        BakedModel buttonModel = mc.getModelManager().getModel(BUTTON_MODEL);

        mc.getItemRenderer().renderModelLists(bodyModel, stack, packedLight, packedOverlay, poseStack,
                buffer.getBuffer(RenderType.cutout()));

        poseStack.pushPose();

        boolean pttActive = false;
        boolean isMuted = false;
        boolean isTalking = false;
        @SuppressWarnings("unused")
        boolean usingFallback = false;

        var clientApiOpt = VoicechatApiHandle.getClientApi();
        if (clientApiOpt.isPresent()) {
            var clientApi = clientApiOpt.get();
            pttActive = clientApi.isPushToTalkKeyPressed();
            isMuted = clientApi.isMuted();
            isTalking = clientApi.isTalking();
        } else {
            usingFallback = true;
            try {
                pttActive = getPttFallback();
                isMuted = getMuteFallback();
                isTalking = getTalkingFallback();
            } catch (Exception ignored) {
            }
        }

        boolean isHeldInHand = mc.player != null
                && (mc.player.getMainHandItem() == stack || mc.player.getOffhandItem() == stack);
        boolean isHandContext = isFirstPerson || isThirdPerson;

        if (!isHeldInHand) {
            pttActive = false;
            isTalking = false;
        }

        boolean shouldTriggerSound = isHeldInHand && isHandContext;

        if (shouldTriggerSound && mc.player != null) {
            if (pttActive && !wasPttActive) {
                mc.player.playSound(ModSounds.WALKIE_PRESS.get(), 0.4f,
                        (float) (Math.random() * 0.125f + 1.0f));
            } else if (!pttActive && wasPttActive) {
                mc.player.playSound(ModSounds.WALKIE_UNPRESS.get(), 0.4f,
                        (float) (Math.random() * 0.0625f + 1.0f));
            }
            wasPttActive = pttActive;
        }

        long now = System.currentTimeMillis();
        if (now != lastFrameTime) {
            float dt = (lastFrameTime == -1) ? 0 : (now - lastFrameTime) / 1000f;
            lastFrameTime = now;

            if (dt > 0.1f)
                dt = 0.1f;
            if (dt > 0) {
                boolean physicalPtt = false;
                var apiOpt = VoicechatApiHandle.getClientApi();
                if (apiOpt.isPresent()) {
                    physicalPtt = apiOpt.get().isPushToTalkKeyPressed();
                } else {
                    try {
                        physicalPtt = getPttFallback();
                    } catch (Exception ignored) {
                    }
                }

                float target = physicalPtt ? BUTTON_TRAVEL_MAX : 0.0f;
                float stiffness = 180.0f;
                float damping = 15.0f;

                float force = (target - currentButtonOffset) * stiffness;
                buttonVelocity += (force - buttonVelocity * damping) * dt;
                currentButtonOffset += buttonVelocity * dt;
            }
        }

        renderCount++;

        float visualOffset = isHeldInHand ? currentButtonOffset : 0;
        poseStack.translate(0, 0, visualOffset);

        mc.getItemRenderer().renderModelLists(buttonModel, stack, packedLight, packedOverlay, poseStack,
                buffer.getBuffer(RenderType.cutout()));
        poseStack.popPose();

        if (isFirstPerson || isThirdPerson || isGui) {
            poseStack.pushPose();

            int frequency = stack.getOrDefault(ModDataComponents.FREQUENCY.get(), 1);
            String freqStr = String.valueOf(frequency);

            poseStack.translate(SCREEN_X_CENTER, 0.5D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            poseStack.translate(0.0D, SCREEN_Y_OFFSET, SCREEN_Z_OFFSET);

            boolean isTransmitting = isTalking || (pttActive && !isMuted);
            boolean isBlinking = !isTransmitting && !isMuted && (mc.level != null && mc.level.getGameTime() % 20 < 10);
            @SuppressWarnings("unused")
            boolean showBrightRed = isTransmitting || isBlinking;

            Font font = mc.font;
            int fullBright = 15728880;

            float textWidth = (float) font.width(freqStr);
            float xPos = -textWidth / 2f + FREQ_X_OFFSET;

            poseStack.pushPose();
            poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

            int nixieBright = 0xFFFF6A00;
            int nixieCore = 0xFFFFA133;
            int nixieDark = 0xFF9E4300;

            poseStack.pushPose();
            poseStack.translate(0, 0, 0.01f);
            font.drawInBatch(freqStr, xPos + 0.5f, FREQ_Y_OFFSET + 0.5f, nixieDark, false, poseStack.last().pose(),
                    buffer, Font.DisplayMode.NORMAL, 0x000000, fullBright);

            poseStack.translate(0, 0, 0.01f);
            font.drawInBatch(freqStr, xPos, FREQ_Y_OFFSET, nixieBright, false, poseStack.last().pose(),
                    buffer, Font.DisplayMode.NORMAL, 0x000000, fullBright);

            poseStack.translate(0, 0, 0.01f);
            font.drawInBatch(freqStr, xPos, FREQ_Y_OFFSET, nixieCore, false, poseStack.last().pose(),
                    buffer, Font.DisplayMode.NORMAL, 0x000000, fullBright);

            poseStack.popPose();
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.scale(INDICATOR_SCALE, -INDICATOR_SCALE, INDICATOR_SCALE);
            Matrix4f matrix4f = poseStack.last().pose();
            int dotColor = showBrightRed ? 0xFFFF0000 : 0xFF440000;

            float scaleAdjustment = TEXT_SCALE / INDICATOR_SCALE;
            font.drawInBatch("\u25cf", INDICATOR_X_POS * scaleAdjustment, INDICATOR_Y_POS * scaleAdjustment, dotColor,
                    false,
                    matrix4f, buffer,
                    Font.DisplayMode.NORMAL, 0x000000, fullBright);
            poseStack.popPose();

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static Boolean getPttFallback() {
        try {
            Class<?> clientManagerClass = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
            Object instance = clientManagerClass.getMethod("instance").invoke(null);
            Object pttHandler = clientManagerClass.getMethod("getPttKeyHandler").invoke(instance);
            return (boolean) pttHandler.getClass().getMethod("isPTTDown").invoke(pttHandler);
        } catch (Exception e) {
            return false;
        }
    }

    private static Boolean getMuteFallback() {
        try {
            Class<?> clientManagerClass = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
            Object instance = clientManagerClass.getMethod("instance").invoke(null);
            Object playerState = clientManagerClass.getMethod("getPlayerStateManager").invoke(instance);
            return (boolean) playerState.getClass().getMethod("isMuted").invoke(playerState);
        } catch (Exception e) {
            return false;
        }
    }

    private static Boolean getTalkingFallback() {
        try {
            Class<?> clientManagerClass = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
            Object client = clientManagerClass.getMethod("getClient").invoke(null);
            if (client == null)
                return false;
            Object micThread = client.getClass().getMethod("getMicThread").invoke(client);
            if (micThread == null)
                return false;
            return (boolean) micThread.getClass().getMethod("shouldTransmitAudio").invoke(micThread);
        } catch (Exception e) {
            return false;
        }
    }
}
