/**
 * 动态多方块包（dynamic multiblock）级别注释。
 *
 * <p>此包提供对动态多方块的核心类型与运行时管理支持。主要类包括：
 * <ul>
 *   <li>{@link dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState} - 多方块运行时状态。</li>
 *   <li>{@link dev.anvilcraft.lib.v2.multiblock.dynamic.DynamicMultiblockManager} - 管理器，负责注册/注销、持久化与周期性检测。</li>
 *   <li>控制器相关接口/记录用于自定义多方块控制器行为（onFormed/onUnformed）。</li>
 * </ul>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
package dev.anvilcraft.lib.v2.multiblock.dynamic;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;