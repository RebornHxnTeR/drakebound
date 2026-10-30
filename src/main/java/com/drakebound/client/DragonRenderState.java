package com.drakebound.client;

import com.drakebound.entity.DragonVariant;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;

/** Reaproveita o estado do Ender Dragon (para o modelo) e acrescenta o que o Drakebound precisa. */
public class DragonRenderState extends EnderDragonRenderState {
    public DragonVariant variant = DragonVariant.GREEN;
    public boolean male;
    public float growthScale = 1.0F;
}
