package net.tokyosu.cs2lootbox.client.renderer;

import software.bernie.geckolib.cache.object.BakedGeoModel;

/** One renderer-owned pose; never shares animation managers between item stacks. */
public final class StaticHeldCasePose {
    private BakedGeoModel model;
    private long instanceId, revision;
    private CaseMeshPose pose;
    private double stableSince, lastTick;
    private boolean ready;

    public boolean reuse(BakedGeoModel model, long instanceId, long revision, double tick) {
        if (!sameKey(model, instanceId, revision) || !Double.isFinite(tick) || tick < lastTick) {
            clear();
            return false;
        }
        if (!ready || !pose.restore(model)) return false;
        lastTick = tick;
        return true;
    }

    public void observe(BakedGeoModel model, long instanceId, long revision, double tick) {
        if (!Double.isFinite(tick)) { clear(); return; }
        if (!sameKey(model, instanceId, revision) || tick < lastTick || !pose.matches(model)) {
            this.model = model; this.instanceId = instanceId; this.revision = revision;
            pose = new CaseMeshPose(model); stableSince = tick; ready = false;
        } else if (tick > lastTick && tick - stableSince >= 1) {
            // Several render calls in the same frame cannot accidentally freeze
            // an uninitialised/transition pose. Require a full tick of stability.
            ready = true;
        }
        lastTick = tick;
    }

    private boolean sameKey(BakedGeoModel model, long instanceId, long revision) {
        return pose != null && this.model == model && this.instanceId == instanceId && this.revision == revision;
    }

    public void clear() { model = null; pose = null; ready = false; }
}
