package com.palm1.analogaudio.integration.ponder;

import com.palm1.analogaudio.block.entity.SpeakerBlockEntity;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;

public class SpeakerScenes {

    public static void intro(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("speaker_intro", "analogaudio.ponder.speaker_intro.header");
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        BlockPos speakerPos = util.grid().at(2, 1, 2);

        scene.overlay().showText(60)
                .text("analogaudio.ponder.speaker_intro.text_1")
                .pointAt(util.vector().topOf(speakerPos))
                .placeNearTarget();

        for (int i = 0; i < 6; i++) {
            pulse(scene, speakerPos);
            scene.idle(1);
            pulse(scene, speakerPos);
            scene.idle(2);
            pulse(scene, speakerPos);
            scene.idle(4);
        }

        scene.idle(50);
        scene.addKeyframe();
        scene.overlay().showText(80)
                .text("analogaudio.ponder.speaker_intro.text_2")
                .pointAt(util.vector().topOf(speakerPos))
                .placeNearTarget();

        scene.idle(10);

        for (int i = 0; i < 110; i++) {
            scene.world().modifyBlockEntity(speakerPos, SpeakerBlockEntity.class, be -> {
                try {
                    java.lang.reflect.Field opacityField = SpeakerBlockEntity.class.getDeclaredField("displayOpacity");
                    opacityField.setAccessible(true);
                    opacityField.setFloat(be, 1.0f);
                    be.setFrequency(14);
                } catch (Exception e) {
                }
            });
            scene.idle(1);
        }
    }

    public static void redstone(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("speaker_redstone", "analogaudio.ponder.speaker_redstone.header");
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);

        BlockPos speakerPos = util.grid().at(1, 1, 2);

        scene.overlay().showText(60)
                .text("analogaudio.ponder.speaker_redstone.text_1")
                .pointAt(util.vector().topOf(speakerPos))
                .placeNearTarget();
        for (int i = 0; i < 6; i++) {
            pulse(scene, speakerPos);
            if (i == 0) {
                scene.world().modifyBlock(util.grid().at(3, 1, 2),
                        state -> state.hasProperty(RedstoneLampBlock.LIT)
                                ? state.setValue(RedstoneLampBlock.LIT, true)
                                : state,
                        false);

                scene.world().modifyBlock(util.grid().at(2, 1, 2),
                        state -> state.hasProperty(ComparatorBlock.POWERED)
                                ? state.setValue(ComparatorBlock.POWERED, true)
                                : state,
                        false);
            }

            scene.idle(1);
            pulse(scene, speakerPos);
            scene.idle(2);
            pulse(scene, speakerPos);
            scene.idle(4);
        }

        scene.idle(25);
    }

    public static void displayLink(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("speaker_display_link", "analogaudio.ponder.speaker_display_link.header");
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(20);
        BlockPos speakerPos = util.grid().at(2, 1, 2);
        scene.overlay().showText(60)
                .text("analogaudio.ponder.speaker_display_link.text_1")
                .pointAt(util.vector().topOf(speakerPos))
                .placeNearTarget();
        for (int i = 0; i < 6; i++) {
            pulse(scene, speakerPos);
            scene.idle(1);
            pulse(scene, speakerPos);
            scene.idle(2);
            pulse(scene, speakerPos);
            scene.idle(4);
        }

        scene.idle(30);
    }

    private static void pulse(SceneBuilder scene, BlockPos pos) {
        scene.world().modifyBlockEntity(pos, SpeakerBlockEntity.class, be -> {
            be.setScale(1.15f);
        });
    }
}
