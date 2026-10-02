package net.tokyosu.cs2lootbox.client.widget;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.tokyosu.cs2lootbox.api.lootbox.LootEntry;
import net.tokyosu.cs2lootbox.api.lootbox.LootboxDefinition;
import net.tokyosu.cs2lootbox.client.renderer.ClientRenderRevision;
import net.tokyosu.cs2lootbox.loot.LootboxLootRoller;

import java.util.IdentityHashMap;
import java.util.Map;

/** One screen's immutable configured previews. Never shares mutable reward stacks. */
final class LootPreviewCache {
    private final Map<LootEntry, Preview> entries = new IdentityHashMap<>();
    private LootboxDefinition definition;
    private long resources = -1, tags = -1;

    Preview get(LootboxDefinition current, LootEntry entry) {
        long nextResources = ClientRenderRevision.get(), nextTags = LootboxLootRoller.tagRevision();
        if (definition != current || resources != nextResources || tags != nextTags) {
            entries.clear();
            definition = current;
            resources = nextResources;
            tags = nextTags;
        }
        return entries.computeIfAbsent(entry, key -> new Preview(key, LootboxLootRoller.createPreviewStack(key)));
    }

    static final class Preview {
        final LootEntry entry;
        final ItemStack stack;
        final String translatedName;
        int width = -1;
        String fullName, clippedName;
        Preview(LootEntry entry, ItemStack stack) {
            this.entry = entry;
            this.stack = stack;
            translatedName = entry.nameTranslationKey() == null ? null
                    : Component.translatable(entry.nameTranslationKey()).getString();
        }
        String name() {
            // Custom items may derive names from live game state; keep that behavior.
            return translatedName != null ? translatedName : stack.isEmpty()
                    ? (entry.itemTagSource() ? "#" : "") + entry.itemId() : stack.getHoverName().getString();
        }
    }
}
