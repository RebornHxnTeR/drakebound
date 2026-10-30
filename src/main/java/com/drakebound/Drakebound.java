package com.drakebound;

import com.drakebound.entity.DragonEntity;
import com.drakebound.event.GreyDragonEvents;
import com.drakebound.network.BreathPayload;
import com.drakebound.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Drakebound.MOD_ID)
public final class Drakebound {
    public static final String MOD_ID = "drakebound";

    public Drakebound(IEventBus modBus) {
        ModRegistries.register(modBus);
        modBus.addListener(Drakebound::onAttributes);
        modBus.addListener(Drakebound::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(GreyDragonEvents::onEntityJoin);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModRegistries.DRAGON.get(), DragonEntity.createAttributes().build());
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(BreathPayload.TYPE, BreathPayload.CODEC, BreathPayload::handle);
    }
}
