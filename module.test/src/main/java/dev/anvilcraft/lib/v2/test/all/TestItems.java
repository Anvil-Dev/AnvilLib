package dev.anvilcraft.lib.v2.test.all;

import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.item.TestItem;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.world.item.Items;

public class TestItems {
    static {
        AnvilLibTest.REGISTRUM.defaultCreativeTab(TestItemGroups.TEST_TAB);
    }

    public static final ItemEntry<TestItem> TEST_ITEM = AnvilLibTest.REGISTRUM
        .item("test_item", TestItem::new)
        .model(() -> (ctx, provider) -> provider.itemModelOutput.accept(
            ctx.get(),
            ItemModelUtils.plainModel(provider.createFlatItemModel(Items.CARROT, ModelTemplates.FLAT_ITEM))
        ))
        .register();

    public static void setupRegistration() {
    }
}
