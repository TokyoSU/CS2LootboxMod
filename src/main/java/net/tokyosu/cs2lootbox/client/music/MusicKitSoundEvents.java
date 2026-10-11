package net.tokyosu.cs2lootbox.client.music;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tokyosu.cs2lootbox.CS2LootBoxMod;

@Mod.EventBusSubscriber(modid = CS2LootBoxMod.MOD_ID, value = Dist.CLIENT)
public final class MusicKitSoundEvents {
    private MusicKitSoundEvents() { }
    @SubscribeEvent public static void onPlay(PlaySoundEvent event) {
        var sound = event.getSound();
        if (sound != null && sound.getSource() == SoundSource.RECORDS
                && sound.getLocation().getNamespace().equals(CS2LootBoxMod.MOD_ID)
                && sound.getLocation().getPath().startsWith("music.")
                && LootboxRegistrationService.isMusicKitSound(sound.getLocation())
                && !ClientMusicKitAvailability.hasSound(sound.getLocation())) event.setSound(null);
    }
    @SubscribeEvent public static void onStreaming(PlayStreamingSourceEvent event) {
        if (event.getSound() instanceof MusicKitPreview.PreviewSound preview) preview.audioStarted(event.getChannel());
    }
    @SubscribeEvent public static void onStatic(PlaySoundSourceEvent event) {
        if (event.getSound() instanceof MusicKitPreview.PreviewSound preview) preview.audioStarted(event.getChannel());
    }
}
