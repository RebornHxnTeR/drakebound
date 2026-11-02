package com.drakebound.client;

import com.drakebound.entity.DragonVariant;
import com.drakebound.entity.SaddleType;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;

/** Reaproveita o estado do Ender Dragon (para o modelo) e acrescenta o que o Drakebound precisa. */
public class DragonRenderState extends EnderDragonRenderState {
    public DragonVariant variant = DragonVariant.GREEN;
    public boolean male;
    public float growthScale = 1.0F;
    public SaddleType saddle = SaddleType.NONE;
    /** 0 = a voar, 1 = pousado (suavizado). */
    public float groundAmount;
    public float walkPos;
    public float walkSpeed;

    /**
     * Pescoço e cabeça: mistura suave entre a pose de voo e a pose pousada, em vez de saltar
     * entre as duas sempre que o "no chão" muda (era isso que fazia a cabeça piscar ao pousar).
     */
    @Override
    public float getHeadPartYOffset(int part, DragonFlightHistory.Sample bodyPos, DragonFlightHistory.Sample partPos) {
        float flying = part == 6 ? 0.0F : (float) (partPos.y() - bodyPos.y());
        return flying + (part - flying) * this.groundAmount;
    }
}
