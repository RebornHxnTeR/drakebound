package com.drakebound.client;

import com.drakebound.entity.DragonVariant;
import com.drakebound.entity.SaddleType;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Reaproveita o estado do Ender Dragon (para o modelo) e acrescenta o que o Drakebound precisa. */
public class DragonRenderState extends EnderDragonRenderState {
    public DragonVariant variant = DragonVariant.GREEN;
    public boolean male;
    public float growthScale = 1.0F;
    public SaddleType saddle = SaddleType.NONE;
    /** Textura da armadura de dragão vestida, ou null. */
    public @Nullable Identifier armorTexture;
    /** 0 = a voar, 1 = pousado (suavizado). */
    public float groundAmount;
    public float walkPos;
    public float walkSpeed;

}
