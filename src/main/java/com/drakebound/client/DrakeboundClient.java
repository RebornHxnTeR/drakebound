package com.drakebound.client;

import com.drakebound.Drakebound;
import com.drakebound.entity.DragonEntity;
import com.drakebound.network.BreathPayload;
import com.drakebound.registry.ModRegistries;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = Drakebound.MOD_ID, dist = Dist.CLIENT)
public final class DrakeboundClient {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "main"));
    private static final KeyMapping BREATH_KEY = new KeyMapping("key.drakebound.breath", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    public DrakeboundClient(IEventBus modBus) {
        modBus.addListener(DrakeboundClient::onRegisterRenderers);
        modBus.addListener(DrakeboundClient::onRegisterLayers);
        modBus.addListener(DrakeboundClient::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(DrakeboundClient::onClientTick);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistries.DRAGON.get(), DragonRenderer::new);
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DragonRenderer.MALE_LAYER, DragonRenderer::createMaleLayer);
        event.registerLayerDefinition(DragonRenderer.FEMALE_LAYER, EnderDragonModel::createBodyLayer);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(BREATH_KEY);
    }

    /** Só envia o pacote quando a tecla é premida e o jogador está a montar um dragão. */
    private static void onClientTick(ClientTickEvent.Post event) {
        while (BREATH_KEY.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.player.getVehicle() instanceof DragonEntity) {
                ClientPacketDistributor.sendToServer(BreathPayload.INSTANCE);
            }
        }
    }
}
