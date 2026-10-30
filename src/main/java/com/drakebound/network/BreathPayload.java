package com.drakebound.network;

import com.drakebound.Drakebound;
import com.drakebound.entity.DragonEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Enviado pelo cliente quando o cavaleiro carrega na tecla de sopro. Não leva dados. */
public record BreathPayload() implements CustomPacketPayload {
    public static final BreathPayload INSTANCE = new BreathPayload();
    public static final Type<BreathPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "breath"));
    public static final StreamCodec<ByteBuf, BreathPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BreathPayload payload, IPayloadContext context) {
        if (context.player().getVehicle() instanceof DragonEntity dragon && dragon.getControllingPassenger() == context.player()) {
            dragon.startBreath(context.player().getLookAngle());
        }
    }
}
