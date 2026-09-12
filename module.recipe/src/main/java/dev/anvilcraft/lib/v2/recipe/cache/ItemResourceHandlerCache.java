package dev.anvilcraft.lib.v2.recipe.cache;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * 物品处理器缓存接口，用于定义输入和输出物品处理器的访问方法
 * 实现该接口的类可以提供对输入和输出物品处理器的访问
 */
public interface ItemResourceHandlerCache {
    /**
     * 获取输入物品处理器
     *
     * @return 输入物品处理器
     */
    ResourceHandler<ItemResource> getInput();

    /**
     * 获取输出物品处理器
     *
     * @return 输出物品处理器
     */
    ResourceHandler<ItemResource> getOutput();
}
