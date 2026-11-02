package com.drakebound.registry;

import com.drakebound.Drakebound;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Carne crua: alimenta e domestica crias. */
    public static final TagKey<Item> DRAGON_FOOD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "dragon_food"));
    /** Comida especial que põe dois dragões domesticados em modo de reprodução. */
    public static final TagKey<Item> DRAGON_BREEDING_FOOD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "dragon_breeding_food"));
    /** Blocos de terra que o sopro de fogo transforma em terra queimada. */
    public static final TagKey<Block> SCORCHABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "scorchable"));

    /** Animais que os dragões selvagens caçam e comem. */
    public static final TagKey<net.minecraft.world.entity.EntityType<?>> DRAGON_PREY = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "dragon_prey"));

    private ModTags() {
    }
}
