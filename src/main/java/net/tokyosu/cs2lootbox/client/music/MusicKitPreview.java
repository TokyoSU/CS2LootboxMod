package net.tokyosu.cs2lootbox.client.music;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.sounds.SoundSource;
import net.tokyosu.cs2lootbox.api.music.MusicKitDefinition;
import net.tokyosu.cs2lootbox.mixin.client.SoundEngineAccessor;
import net.tokyosu.cs2lootbox.mixin.client.SoundManagerAccessor;

/** One local preview per open screen. Uses Minecraft's managed audio thread and RECORDS volume. */
public final class MusicKitPreview implements AutoCloseable {
    public enum State { STOPPED, LOADING, PLAYING, PAUSED, ENDED, ERROR }
    private PreviewSound sound;
    private State state = State.STOPPED;
    private long requestedAt;

    public void play(MusicKitDefinition.Track track) {
        close();
        var manager = Minecraft.getInstance().getSoundManager();
        if (manager.getSoundEvent(track.sound()) == null) { state = State.ERROR; return; }
        sound = new PreviewSound(track);
        requestedAt = Util.getMillis();
        state = State.LOADING;
        manager.play(sound);
    }

    public void tick() {
        if (sound == null || state == State.ENDED || state == State.ERROR) return;
        boolean active = Minecraft.getInstance().getSoundManager().isActive(sound);
        if (sound.clock.hasStarted()) {
            if (!active) { sound.clock.finish(); state = State.ENDED; }
            else state = sound.paused ? State.PAUSED : State.PLAYING;
        } else if (!active || Util.getMillis() - requestedAt > 15000) {
            close(); state = State.ERROR;
        }
    }

    public boolean canPause() { return channel() != null; }

    public void togglePause() {
        if (sound == null || state == State.ENDED || state == State.ERROR) return;
        var channel = channel();
        if (channel == null) return;
        sound.paused = !sound.paused;
        if (sound.paused) {
            sound.clock.pause(); channel.execute(Channel::pause); state = State.PAUSED;
        } else {
            sound.clock.resume(); channel.execute(Channel::unpause);
            state = sound.clock.hasStarted() ? State.PLAYING : State.LOADING;
        }
    }

    private ChannelAccess.ChannelHandle channel() {
        if (sound == null) return null;
        Object manager = Minecraft.getInstance().getSoundManager();
        if (!(manager instanceof SoundManagerAccessor access)) return null;
        Object engine = access.cs2lootbox$getSoundEngine();
        return engine instanceof SoundEngineAccessor channels ? channels.cs2lootbox$getChannels().get(sound) : null;
    }

    public State state() { return state; }
    public double seconds() { return sound == null ? 0 : sound.clock.elapsedMillis() / 1000.0; }
    public boolean hasPlayback() { return state == State.PLAYING || state == State.PAUSED || state == State.LOADING; }

    @Override public void close() {
        if (sound != null) {
            sound.cancelled = true;
            Minecraft.getInstance().getSoundManager().stop(sound);
            sound = null;
        }
        state = State.STOPPED;
    }

    /** Start notification is delivered on the sound thread after async OGG decoding. */
    public static final class PreviewSound extends SimpleSoundInstance {
        private final MusicPlaybackClock clock;
        private volatile boolean paused, cancelled;

        private PreviewSound(MusicKitDefinition.Track track) {
            super(track.sound(), SoundSource.RECORDS, 1, 1, SoundInstance.createUnseededRandom(),
                    false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
            clock = new MusicPlaybackClock(Util::getMillis, track.lengthInTicks() * 50L);
        }

        public void audioStarted(Channel channel) {
            if (cancelled) { channel.stop(); return; }
            clock.started();
            if (paused) channel.pause();
        }
    }
}
