package com.drakebound.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;

/**
 * Controlo de movimento que voa como o {@link FlyingMoveControl} mas, quando o dragão está no chão
 * e o destino está mais ou menos à mesma altura, caminha com gravidade em vez de levantar voo.
 */
public class DragonMoveControl extends FlyingMoveControl {
    /** Diferença de altura (blocos) que ainda se faz a pé; acima disto levanta voo. */
    private static final double MAX_WALK_RISE = 2.0;

    public DragonMoveControl(Mob mob) {
        super(mob, 20, false);
    }

    @Override
    public void tick() {
        if (this.operation != Operation.MOVE_TO || !this.mob.onGround() || this.wantedY - this.mob.getY() > MAX_WALK_RISE) {
            super.tick();
            return;
        }
        this.operation = Operation.WAIT;
        this.mob.setNoGravity(false);
        this.mob.setYya(0.0F);
        double xd = this.wantedX - this.mob.getX();
        double zd = this.wantedZ - this.mob.getZ();
        if (xd * xd + zd * zd < 0.25) {
            this.mob.setZza(0.0F);
            return;
        }
        float yRotD = (float) (Mth.atan2(zd, xd) * Mth.RAD_TO_DEG) - 90.0F;
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yRotD, 10.0F));
        this.mob.setXRot(0.0F);
        // setSpeed também define o avanço (zza) da criatura.
        this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
    }
}
