package com.drakebound.item;

import com.drakebound.Drakebound;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Armadura que se veste no próprio dragão (espaço do corpo), como a armadura de cavalo. */
public class DragonArmorItem extends Item {
    /** Textura desenhada por cima do modelo do dragão. */
    public final Identifier texture;

    public DragonArmorItem(String material, Item.Properties properties) {
        super(properties);
        this.texture = Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "textures/entity/dragon_armor/" + material + ".png");
    }
}
