package net.tokyosu.cs2lootbox.mixin.client;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeTabs.class)
public interface CreativeModeTabsAccessor {
    @Accessor("CACHED_PARAMETERS")
    static void cs2lootbox$invalidateContents(CreativeModeTab.ItemDisplayParameters parameters) {
        // Mixin replaces this body with access to the private static field at runtime.
        throw new AssertionError("CreativeModeTabs accessor was not transformed by Mixin");
    }
}
