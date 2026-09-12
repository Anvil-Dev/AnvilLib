package dev.anvilcraft.lib.v2.multiblock.dynamic.controller;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ControllerRecord {
    private static final Map<ControllerInfo, IController> CONTROLLERS = new HashMap<>();

    public static void register(SimpleController controller) {
        CONTROLLERS.put(new ControllerInfo(controller.getBlock(), controller.getDefinitionId()), controller);
    }

    public static IController get(Block block, Identifier definitionId) {
        ControllerInfo info = new ControllerInfo(block, definitionId);
        IController controller = CONTROLLERS.get(info);
        if (controller == null) {
            if (block instanceof IController controller1) {
                CONTROLLERS.put(info, controller1);
                return controller1;
            }
            throw new IllegalArgumentException(
                "Attempt to get non IController and unregistered block."
                + " Block Id: " + BuiltInRegistries.BLOCK.getKey(block) + ","
                + " Multiblock Id: " + definitionId
            );
        }
        return controller;
    }
}
