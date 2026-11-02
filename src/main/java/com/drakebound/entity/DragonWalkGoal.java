package com.drakebound.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Passeio a pé: escolhe um ponto em terra perto e vai até lá em linha reta pelo controlo de movimento
 * (sem pathfinding, que falha com criaturas desta largura). Desiste ao fim de {@link #MAX_TICKS}.
 */
public class DragonWalkGoal extends Goal {
    private static final int MAX_TICKS = 200;
    private static final int CHANCE = 120;
    private static final double ARRIVED_SQR = 4.0;

    private final DragonEntity dragon;
    private final double speed;
    private @Nullable Vec3 target;
    private int ticks;

    public DragonWalkGoal(DragonEntity dragon, double speed) {
        this.dragon = dragon;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.dragon.onGround() || this.dragon.isVehicle() || this.dragon.isOrderedToSit()
            || this.dragon.getRandom().nextInt(reducedTickDelay(CHANCE)) != 0) {
            return false;
        }
        this.target = LandRandomPos.getPos(this.dragon, 16, 4);
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.ticks < MAX_TICKS && this.dragon.onGround() && !this.dragon.isOrderedToSit()
            && this.dragon.distanceToSqr(this.target.x, this.dragon.getY(), this.target.z) > ARRIVED_SQR;
    }

    @Override
    public void start() {
        this.ticks = 0;
    }

    @Override
    public void stop() {
        this.target = null;
        this.dragon.setZza(0.0F);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.target != null) {
            this.dragon.getMoveControl().setWantedPosition(this.target.x, this.target.y, this.target.z, this.speed);
        }
    }
}
