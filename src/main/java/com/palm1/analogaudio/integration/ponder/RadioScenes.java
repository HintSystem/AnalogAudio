package com.palm1.analogaudio.integration.ponder;

import com.palm1.analogaudio.block.entity.RadioBlockEntity;
import com.palm1.analogaudio.item.CassetteData;
import com.palm1.analogaudio.registry.ModDataComponents;
import com.palm1.analogaudio.registry.ModItems;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;

public class RadioScenes {

    public static void intro(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("radio_intro", "analogaudio.ponder.radio_intro.header");
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);

        BlockPos radioPos = util.grid().at(2, 1, 2);
        scene.world().modifyBlockEntity(radioPos, RadioBlockEntity.class, be -> {
            be.clearContent();
            try {
                java.lang.reflect.Field insertTimeField = RadioBlockEntity.class
                        .getDeclaredField("insertTime");
                insertTimeField.setAccessible(true);
                insertTimeField.setLong(be, 0);
            } catch (Exception e) {
            }
        });

        scene.idle(20);
        scene.addKeyframe();

        scene.overlay().showText(60)
                .text("analogaudio.ponder.radio_intro.text_1")
                .pointAt(util.vector().topOf(radioPos))
                .placeNearTarget();
        scene.idle(60);
        scene.addKeyframe();

        scene.overlay().showControls(util.vector().topOf(radioPos), Pointing.DOWN, 40)
                .rightClick()
                .withItem(new ItemStack(
                        ModItems.CASSETTE_TAPE.get()));

        scene.idle(10);

        for (int frame = 0; frame <= 10; frame++) {
            final int f = frame;
            scene.world().modifyBlockEntity(radioPos, RadioBlockEntity.class, be -> {
                if (be.getItem(0).isEmpty()) {
                    ItemStack cassette = new ItemStack(
                            ModItems.CASSETTE_TAPE.get());
                    CassetteData data = new CassetteData(
                            java.util.UUID.randomUUID().toString(), "", "Tutorial Tape", 0xFFFFFFFF, 0.75f, 60000,
                            java.util.UUID.randomUUID().toString());
                    cassette.set(ModDataComponents.CASSETTE_DATA.get(), data);
                    be.setItem(0, cassette);
                }

                try {
                    java.lang.reflect.Field insertTimeField = RadioBlockEntity.class
                            .getDeclaredField("insertTime");
                    insertTimeField.setAccessible(true);
                    insertTimeField.setLong(be, be.getLevel().getGameTime() - f);

                    java.lang.reflect.Field removeTimeField = RadioBlockEntity.class
                            .getDeclaredField("removeTime");
                    removeTimeField.setAccessible(true);
                    removeTimeField.setLong(be, 0);
                } catch (Exception e) {
                }
            });
            scene.idle(1);
        }

        for (int i = 0; i < 3; i++) {
            pulse(scene, util, radioPos);
            scene.idle(10);
            pulse(scene, util, radioPos);
            scene.idle(10);
        }
        scene.idle(20);
    }

    public static void redstone(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("radio_redstone", "analogaudio.ponder.radio_redstone.header");
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(60)
                .text("analogaudio.ponder.radio_redstone.text_1")
                .pointAt(util.vector().topOf(2, 2, 1))
                .placeNearTarget();

        scene.idle(20);

        BlockPos radioPos = util.grid().at(2, 2, 1);
        for (int i = 0; i < 2; i++) {
            pulse(scene, util, radioPos);
            scene.idle(10);
            pulse(scene, util, radioPos);
            scene.idle(20);
            pulse(scene, util, radioPos);
            scene.idle(10);
        }
        scene.idle(20);
    }

    public static void displayLink(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("radio_display_link", "analogaudio.ponder.radio_display_link.header");
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(60)
                .text("analogaudio.ponder.radio_display_link.text_1")
                .pointAt(util.vector().topOf(2, 1, 2))
                .placeNearTarget();

        scene.idle(20);
        BlockPos radioPos = util.grid().at(2, 1, 2);
        for (int i = 0; i < 2; i++) {
            pulse(scene, util, radioPos);
            scene.idle(10);
            pulse(scene, util, radioPos);
            scene.idle(20);
            pulse(scene, util, radioPos);
            scene.idle(10);
        }
        scene.idle(20);
    }

    public static void mechanicalArms(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("radio_mechanical_arms", "analogaudio.ponder.radio_mechanical_arms.header");
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(80)
                .text("analogaudio.ponder.radio_mechanical_arms.text_1")
                .pointAt(util.vector().topOf(2, 1, 2))
                .placeNearTarget();

        scene.idle(20);
        BlockPos radioPos = util.grid().at(2, 1, 2);
        for (int i = 0; i < 2; i++) {
            pulse(scene, util, radioPos);
            scene.idle(10);
            pulse(scene, util, radioPos);
            scene.idle(20);
            pulse(scene, util, radioPos);
            scene.idle(10);
        }
        scene.idle(20);
    }

    private static void pulse(SceneBuilder scene, SceneBuildingUtil util, BlockPos pos) {
        scene.effects().emitParticles(
                util.vector().centerOf(pos).add(0, 0.3, 0),
                scene.effects().simpleParticleEmitter(ParticleTypes.NOTE,
                        util.vector().of(0, 0.2, 0)),
                1.0f, 1);
    }
}
