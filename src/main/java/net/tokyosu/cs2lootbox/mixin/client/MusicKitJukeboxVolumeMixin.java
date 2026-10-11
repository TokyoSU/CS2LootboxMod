package net.tokyosu.cs2lootbox.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.tokyosu.cs2lootbox.registry.LootboxRegistrationService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Normalize record gain without changing the volume used for attenuation distance. */
@Mixin(SoundEngine.class)
public abstract class MusicKitJukeboxVolumeMixin {
    @Shadow private float calculateVolume(float volume, SoundSource source) {
        throw new AssertionError();
    }

    private static boolean cs2lootbox$isJukeboxMusic(SoundInstance sound) {
        return sound.getSource() == SoundSource.RECORDS
                && sound.getAttenuation() == SoundInstance.Attenuation.LINEAR
                && !sound.isRelative()
                && LootboxRegistrationService.isMusicKitSound(sound.getLocation());
    }

    @Redirect(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float cs2lootbox$initialRecordGain(SoundEngine engine, float volume,
                                              SoundSource source, SoundInstance sound) {
        return calculateVolume(cs2lootbox$isJukeboxMusic(sound) ? volume / 4.0F : volume, source);
    }

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("HEAD"), cancellable = true)
    private void cs2lootbox$updatedRecordGain(SoundInstance sound, CallbackInfoReturnable<Float> result) {
        if (cs2lootbox$isJukeboxMusic(sound)) {
            result.setReturnValue(calculateVolume(sound.getVolume() / 4.0F, sound.getSource()));
        }
    }
}
