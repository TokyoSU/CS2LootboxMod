package net.tokyosu.cs2lootbox.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;

/** Read-only access: this does not alter Minecraft or other mods' rendering. */
@Mixin(MultiBufferSource.BufferSource.class)
public interface BufferSourceAccessor {
    @Accessor("fixedBuffers") Map<RenderType, BufferBuilder> cs2lootbox$getFixedBuffers();
    @Accessor("startedBuffers") Set<BufferBuilder> cs2lootbox$getStartedBuffers();
}
