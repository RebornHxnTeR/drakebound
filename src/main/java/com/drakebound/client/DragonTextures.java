package com.drakebound.client;

import com.drakebound.Drakebound;
import com.drakebound.entity.DragonVariant;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Gera as texturas de cada variante a partir da textura do Ender Dragon do próprio jogo,
 * recolorindo-a uma vez por recarga de recursos. Assim o mod não distribui texturas da Mojang
 * e um pack de recursos que mude o Ender Dragon muda também os dragões.
 */
public final class DragonTextures {
    private static final Identifier BASE = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
    private static final Identifier BASE_EYES = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");
    /** Intervalo de cinzentos usado pela textura base (medido no ficheiro vanilla). */
    private static final float GREY_MIN = 14.0F, GREY_MAX = 133.0F;

    private static final Map<DragonVariant, Identifier> SKIN = new EnumMap<>(DragonVariant.class);
    private static final Map<DragonVariant, Identifier> EYES = new EnumMap<>(DragonVariant.class);

    public static Identifier skin(DragonVariant variant) {
        return SKIN.getOrDefault(variant, BASE);
    }

    public static Identifier eyes(DragonVariant variant) {
        return EYES.getOrDefault(variant, BASE_EYES);
    }

    static void rebuild(ResourceManager resources) {
        for (DragonVariant variant : DragonVariant.values()) {
            String name = variant.getSerializedName();
            register(resources, BASE, "dragon_" + name, SKIN, variant, (argb, v) -> recolorSkin(argb, v));
            register(resources, BASE_EYES, "dragon_eyes_" + name, EYES, variant, (argb, v) -> recolorEyes(argb, v));
        }
    }

    private interface Recolor {
        int apply(int argb, DragonVariant variant);
    }

    private static void register(ResourceManager resources, Identifier source, String name, Map<DragonVariant, Identifier> target,
                                 DragonVariant variant, Recolor recolor) {
        Optional<Resource> resource = resources.getResource(source);
        if (resource.isEmpty()) {
            return;
        }
        try (InputStream in = resource.get().open()) {
            NativeImage image = NativeImage.read(in);
            int width = image.getWidth();
            int height = image.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getPixel(x, y);
                    if (ARGB.alpha(argb) != 0) {
                        image.setPixel(x, y, recolor.apply(argb, variant));
                    }
                }
            }
            Identifier id = Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "dynamic/" + name);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> id.toString(), image));
            target.put(variant, id);
        } catch (IOException e) {
            target.remove(variant);
        }
    }

    private static int recolorSkin(int argb, DragonVariant variant) {
        float t = Mth.clamp((ARGB.red(argb) - GREY_MIN) / (GREY_MAX - GREY_MIN), 0.0F, 1.0F);
        return ARGB.color(ARGB.alpha(argb), ARGB.srgbLerp(t, variant.darkColor, variant.lightColor));
    }

    private static int recolorEyes(int argb, DragonVariant variant) {
        int brightness = Math.max(ARGB.red(argb), Math.max(ARGB.green(argb), ARGB.blue(argb)));
        return ARGB.color(ARGB.alpha(argb), ARGB.scaleRGB(variant.eyeColor, brightness / 255.0F));
    }

    private DragonTextures() {
    }
}
