package dev.anvilcraft.lib.v2.registrum.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;

/** 创造物品栏变体组注册表；原版独立 16 色物品自动识别，其他模组使用 register 显式声明。 */
public final class CreativeVariantPickerRegistry {
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final BooleanSupplier ALWAYS_ENABLED = () -> true;
    private static final List<DyeColor> COLOR_ORDER = List.of(
        DyeColor.WHITE,
        DyeColor.LIGHT_GRAY,
        DyeColor.GRAY,
        DyeColor.BLACK,
        DyeColor.BROWN,
        DyeColor.RED,
        DyeColor.ORANGE,
        DyeColor.YELLOW,
        DyeColor.LIME,
        DyeColor.GREEN,
        DyeColor.CYAN,
        DyeColor.LIGHT_BLUE,
        DyeColor.BLUE,
        DyeColor.PURPLE,
        DyeColor.MAGENTA,
        DyeColor.PINK
    );
    private static final Map<Item, List<VariantGroup>> GROUPS = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<PendingGroup> PENDING_GROUPS = new ConcurrentLinkedQueue<>();
    private static volatile BooleanSupplier vanillaColorVariantPickerEnabled = () -> false;
    private static volatile boolean colorFamiliesDiscovered;

    private CreativeVariantPickerRegistry() {
    }

    /** 返回叠加层使用的稳定颜色顺序。 */
    public static List<DyeColor> colorOrder() {
        return COLOR_ORDER;
    }

    /**
     * 设置原版独立十六色物品组是否折叠为创造物品栏选择器，默认不折叠
     *
     * <p>实现模组可传入客户端配置字段的读取器，配置重载后的值会在下次构建创造标签时读取</p>
     *
     * @param enabled 返回 {@code true} 时折叠完整的原版十六色物品组
     */
    public static void setVanillaColorVariantPickerEnabled(BooleanSupplier enabled) {
        vanillaColorVariantPickerEnabled = Objects.requireNonNull(enabled, "enabled");
    }

    /** 注册由独立物品组成的变体组，例如原版式的彩色方块。 */
    public static void register(ItemLike... variants) {
        register(ALWAYS_ENABLED, variants);
    }

    /** 注册可由实现模组配置开关控制的独立物品变体组。 */
    public static void register(BooleanSupplier enabled, ItemLike... variants) {
        // 26.1 的物品默认组件在世界注册表加载后才绑定，客户端初始化阶段只保存物品定义。
        PENDING_GROUPS.add(new PendingGroup(Objects.requireNonNull(enabled, "enabled"),
            Arrays.stream(variants).filter(Objects::nonNull).toList()));
    }

    /** 注册由独立物品栈组成的变体组。 */
    public static void registerStacks(ItemStack... variants) {
        registerStacks(ALWAYS_ENABLED, Arrays.asList(variants));
    }

    /** 注册可由实现模组配置开关控制的独立物品栈变体组。 */
    public static void registerStacks(BooleanSupplier enabled, ItemStack... variants) {
        registerStacks(enabled, Arrays.asList(variants));
    }

    /** 注册由独立物品栈组成的变体组。 */
    public static void registerStacks(Collection<ItemStack> variants) {
        registerStacks(ALWAYS_ENABLED, variants);
    }

    /** 注册可由实现模组配置开关控制的独立物品栈变体组。 */
    public static void registerStacks(BooleanSupplier enabled, Collection<ItemStack> variants) {
        Objects.requireNonNull(enabled, "enabled");
        List<ItemStack> copies = variants.stream()
            .filter(stack -> stack != null && !stack.isEmpty())
            .map(stack -> stack.copyWithCount(1))
            .toList();
        if (copies.size() < 2 || copies.size() > 16) {
            throw new IllegalArgumentException("Creative variant groups must contain between 2 and 16 items");
        }
        VariantGroup group = new VariantGroup(copies, enabled);
        for (ItemStack stack : copies) {
            GROUPS.compute(stack.getItem(), (item, existing) -> {
                List<VariantGroup> groups = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
                if (groups.stream().noneMatch(group::sameItems)) groups.add(group);
                return List.copyOf(groups);
            });
        }
    }

    /** 查询物品栈对应的变体。实现类接口优先于独立物品注册组。 */
    public static Optional<List<ItemStack>> createVariants(ItemStack source) {
        if (source.isEmpty()) return Optional.empty();
        if (source.getItem() instanceof CreativeVariantPickerItem provider) {
            if (!provider.isCreativePickerEnabled(source)) return Optional.empty();
            return normalize(provider.createCreativePickerVariants(source));
        }
        discoverVanillaColorFamilies();
        VariantGroup group = findGroup(source);
        return group == null ? Optional.empty() : Optional.of(group.copies());
    }

    /** 只检查变体选择器是否仍可用，不重新生成实现类提供的物品栈。 */
    public static boolean isCreativePickerEnabled(ItemStack source) {
        if (source.isEmpty()) return false;
        if (source.getItem() instanceof CreativeVariantPickerItem provider) {
            return provider.isCreativePickerEnabled(source);
        }
        discoverVanillaColorFamilies();
        return findGroup(source) != null;
    }

    /**
     * 从创造标签内容中移除完整的独立物品变体组，并保留第一项作为代表物品
     */
    public static Collection<ItemStack> fold(Collection<ItemStack> items) {
        discoverVanillaColorFamilies();
        List<ItemStack> original = List.copyOf(items);
        Set<VariantGroup> eligible = new HashSet<>();
        for (List<VariantGroup> groups : GROUPS.values()) {
            for (VariantGroup group : groups) {
                if (group.isEnabled() && group.presentIn(original)) eligible.add(group);
            }
        }
        if (eligible.isEmpty()) return new ArrayList<>(original);

        Set<VariantGroup> emitted = new HashSet<>();
        List<ItemStack> folded = new ArrayList<>(original.size());
        for (ItemStack stack : original) {
            VariantGroup group = findGroup(stack, eligible);
            if (group == null) {
                folded.add(stack);
            } else if (emitted.add(group)) {
                folded.add(group.source());
            }
        }
        return folded;
    }

    private static Optional<List<ItemStack>> normalize(List<ItemStack> variants) {
        if (variants == null) return Optional.empty();
        List<ItemStack> normalized = variants.stream()
            .filter(stack -> stack != null && !stack.isEmpty())
            .limit(16)
            .map(stack -> stack.copyWithCount(1))
            .toList();
        return normalized.size() < 2 ? Optional.empty() : Optional.of(normalized);
    }

    private static VariantGroup findGroup(ItemStack stack) {
        if (stack.getItem() instanceof CreativeVariantPickerItem) return null;
        List<VariantGroup> groups = GROUPS.get(stack.getItem());
        if (groups == null) return null;
        for (VariantGroup group : groups) {
            if (group.isEnabled() && group.matches(stack)) return group;
        }
        return null;
    }

    private static VariantGroup findGroup(ItemStack stack, Set<VariantGroup> allowed) {
        if (stack.getItem() instanceof CreativeVariantPickerItem) return null;
        List<VariantGroup> groups = GROUPS.get(stack.getItem());
        if (groups == null) return null;
        for (VariantGroup group : groups) {
            if (allowed.contains(group) && group.matches(stack)) return group;
        }
        return null;
    }

    private static void discoverVanillaColorFamilies() {
        PendingGroup pending;
        while ((pending = PENDING_GROUPS.poll()) != null) {
            registerStacks(pending.enabled(), pending.variants().stream().map(item -> item.asItem().getDefaultInstance()).toList());
        }
        if (colorFamiliesDiscovered) return;
        synchronized (CreativeVariantPickerRegistry.class) {
            if (colorFamiliesDiscovered) return;
            Map<FamilyKey, EnumMap<DyeColor, Item>> candidates = new HashMap<>();
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                if (!VANILLA_NAMESPACE.equals(id.getNamespace())) continue;
                String path = id.getPath();
                for (DyeColor color : COLOR_ORDER) {
                    String prefix = color.getName() + "_";
                    if (!path.startsWith(prefix) || path.length() == prefix.length()) continue;
                    FamilyKey key = new FamilyKey(id.getNamespace(), path.substring(prefix.length()));
                    candidates.computeIfAbsent(key, ignored -> new EnumMap<>(DyeColor.class))
                        .put(color, BuiltInRegistries.ITEM.getValue(id));
                }
            }
            for (EnumMap<DyeColor, Item> family : candidates.values()) {
                if (family.size() != COLOR_ORDER.size()) continue;
                List<ItemStack> variants = COLOR_ORDER.stream()
                    .map(color -> new ItemStack(family.get(color)))
                    .toList();
                registerStacks(CreativeVariantPickerRegistry::isVanillaColorVariantPickerEnabled, variants);
            }
            colorFamiliesDiscovered = true;
        }
    }

    private static boolean isVanillaColorVariantPickerEnabled() {
        return vanillaColorVariantPickerEnabled.getAsBoolean();
    }

    private record FamilyKey(String namespace, String basePath) {
    }

    private record PendingGroup(BooleanSupplier enabled, List<ItemLike> variants) { }

    private record VariantGroup(List<ItemStack> variants, BooleanSupplier enabled) {
        private VariantGroup {
            variants = List.copyOf(variants);
            Objects.requireNonNull(enabled, "enabled");
        }

        private boolean isEnabled() {
            return this.enabled.getAsBoolean();
        }

        private boolean sameItems(VariantGroup other) {
            if (this.variants.size() != other.variants.size()) return false;
            for (int index = 0; index < this.variants.size(); index++) {
                if (!ItemStack.isSameItemSameComponents(this.variants.get(index), other.variants.get(index))) {
                    return false;
                }
            }
            return true;
        }

        private boolean matches(ItemStack source) {
            return this.variants.stream().anyMatch(variant -> ItemStack.isSameItemSameComponents(variant, source));
        }

        private boolean presentIn(Collection<ItemStack> items) {
            return this.variants.stream().allMatch(variant -> items.stream()
                .anyMatch(stack -> ItemStack.isSameItemSameComponents(variant, stack)));
        }

        private ItemStack source() {
            return this.variants.getFirst().copy();
        }

        private List<ItemStack> copies() {
            return this.variants.stream().map(ItemStack::copy).toList();
        }
    }
}
