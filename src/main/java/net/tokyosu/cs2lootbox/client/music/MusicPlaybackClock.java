package net.tokyosu.cs2lootbox.client.music;

import java.util.function.LongSupplier;

/** Starts when the audio channel actually starts; loading and paused time do not advance progress. */
public final class MusicPlaybackClock {
    private final LongSupplier millis;
    private final long duration;
    private long elapsed, runningSince;
    private boolean started, paused, finished;

    public MusicPlaybackClock(LongSupplier millis, long duration) { this.millis = millis; this.duration = duration; }
    public synchronized void started() {
        if (!started) { started = true; runningSince = millis.getAsLong(); }
    }
    public synchronized void pause() {
        if (!paused) { elapsed = elapsedMillis(); paused = true; }
    }
    public synchronized void resume() {
        if (paused) { paused = false; runningSince = millis.getAsLong(); }
    }
    public synchronized long elapsedMillis() {
        long running = started && !paused && !finished ? Math.max(0, millis.getAsLong() - runningSince) : 0;
        return Math.min(duration, elapsed + running);
    }
    public synchronized void finish() { elapsed = elapsedMillis(); finished = true; }
    public synchronized boolean hasStarted() { return started; }
}
