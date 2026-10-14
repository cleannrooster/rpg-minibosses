package com.cleannrooster.anarchy_rpg.client.combat;

import com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackFrame;
import com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackGeometry;
import com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackVolume;
import dev.eann.atlasvfx.trail.SweptSurface;
import org.joml.Vector3f;

/**
 * An attack's damage surface, as AtlasVFX's trails read one: {@code s} through the swing, {@code t} from
 * the inner radius out to the blade's reach. Every point comes from {@link AttackGeometry#surfacePoint} —
 * the call the server damages through — so the trail is drawn exactly over what can be hit, speed curve,
 * asymmetric arc and inner radius included.
 *
 * <p>For swept arcs only. Their frame is frozen at commit, so nothing here moves once built.</p>
 */
final class AttackSurface implements SweptSurface {
    private final AttackVolume volume;
    private final AttackFrame frame;
    private final Vector3f normal;
    private final float reach;

    AttackSurface(AttackVolume volume, AttackFrame frame) {
        this.volume = volume;
        this.frame = frame;
        var n = frame.planeNormal(volume.planeRoll());
        this.normal = new Vector3f((float) n.x, (float) n.y, (float) n.z).normalize();
        this.reach = (float) AttackGeometry.rangeOf(volume, frame);
    }

    @Override
    public boolean prepare(float tickDelta) {
        return this.reach > 0.0f;
    }

    @Override
    public void point(float s, float t, double[] out) {
        var point = AttackGeometry.surfacePoint(this.volume, this.frame, s, t);
        out[0] = point.x;
        out[1] = point.y;
        out[2] = point.z;
    }

    @Override
    public Vector3f normal() {
        return this.normal;
    }

    @Override
    public float reach() {
        return this.reach;
    }

    @Override
    public boolean closed() {
        return this.volume.swing().sweepDegrees() >= 360.0f;
    }

    @Override
    public boolean viewer() {
        return false;
    }
}
