package com.drakebound.entity;

import com.drakebound.registry.ModRegistries;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Dragões adultos domesticados comem, quando lhes apetece, um bloco de carniça pousado junto à lava
 * e ficam prontos para acasalar. Procura só de 10 em 10 segundos, numa caixa limitada à volta do dragão.
 */
public class EatCarrionGoal extends Goal {
    private static final int SEARCH_INTERVAL = 200;
    private static final int RANGE = 12;
    private static final int HEIGHT = 4;
    private static final float HEAL = 20.0F;

    private final DragonEntity dragon;
    private @Nullable BlockPos target;
    private int nextSearch;

    public EatCarrionGoal(DragonEntity dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--this.nextSearch > 0 || !this.dragon.wantsCarrion()) {
            return false;
        }
        this.nextSearch = SEARCH_INTERVAL + this.dragon.getRandom().nextInt(SEARCH_INTERVAL);
        Level level = this.dragon.level();
        this.target = BlockPos.findClosestMatch(this.dragon.blockPosition(), RANGE, HEIGHT,
            pos -> isCarrionByLava(level, pos)).orElse(null);
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.dragon.wantsCarrion() && isCarrionByLava(this.dragon.level(), this.target);
    }

    @Override
    public void start() {
        this.moveTo();
    }

    @Override
    public void stop() {
        this.target = null;
        this.dragon.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        BlockPos pos = this.target;
        if (pos == null) {
            return;
        }
        this.dragon.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        double reach = this.dragon.getBbWidth() * 0.5 + 2.5;
        if (this.dragon.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > reach * reach) {
            if (this.dragon.getNavigation().isDone()) {
                this.moveTo();
            }
            return;
        }
        Level level = this.dragon.level();
        level.destroyBlock(pos, false, this.dragon);
        this.dragon.playSound(SoundEvents.GENERIC_EAT.value(), 1.5F, 0.6F);
        this.dragon.heal(HEAL);
        this.dragon.setInLove(null);
        this.target = null;
    }

    private void moveTo() {
        BlockPos pos = this.target;
        if (pos != null) {
            this.dragon.getNavigation().moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1.0);
        }
    }

    /** Carniça "perto da lava": encostada a lava por um dos lados ou por baixo. */
    private static boolean isCarrionByLava(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModRegistries.CARRION_BLOCK.get())) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (direction != Direction.UP && level.getFluidState(pos.relative(direction)).is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }
}
