package com.drakebound.registry;

import com.drakebound.Drakebound;
import com.drakebound.block.DragonEggBlock;
import com.drakebound.entity.DragonEntity;
import com.drakebound.entity.DragonVariant;
import com.drakebound.entity.SaddleType;
import com.drakebound.item.DragonSaddleItem;
import com.drakebound.world.DragonNestPiece;
import com.drakebound.world.DragonNestStructure;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistries {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Drakebound.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Drakebound.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Drakebound.MOD_ID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Drakebound.MOD_ID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Drakebound.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Drakebound.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<DragonEntity>> DRAGON = ENTITY_TYPES.register("dragon",
        id -> EntityType.Builder.of(DragonEntity::new, MobCategory.CREATURE)
            .sized(5.0F, 3.5F)
            // Todos os dragões ignoram fogo, lava, magma e qualquer dano de calor.
            .fireImmune()
            .eyeHeight(2.8F)
            .clientTrackingRange(16)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** Terra queimada: forma-se sob sopro de fogo prolongado e cobre o chão dos ninhos. */
    public static final DeferredBlock<Block> SCORCHED_EARTH = BLOCKS.registerSimpleBlock("scorched_earth",
        p -> p.mapColor(MapColor.TERRACOTTA_BLACK).strength(0.6F).sound(SoundType.ROOTED_DIRT).lightLevel(s -> 3));
    public static final DeferredItem<BlockItem> SCORCHED_EARTH_ITEM = ITEMS.registerSimpleBlockItem(SCORCHED_EARTH);

    public static final Map<DragonVariant, DeferredBlock<DragonEggBlock>> EGG_BLOCKS = new EnumMap<>(DragonVariant.class);
    public static final Map<DragonVariant, DeferredItem<BlockItem>> EGG_ITEMS = new EnumMap<>(DragonVariant.class);
    public static final Map<DragonVariant, DeferredItem<SpawnEggItem>> SPAWN_EGGS = new EnumMap<>(DragonVariant.class);
    /** Escamas: cada cor de dragão larga a sua; também aparecem nos baús dos ninhos. */
    public static final Map<DragonVariant, DeferredItem<Item>> SCALES = new EnumMap<>(DragonVariant.class);

    static {
        for (DragonVariant variant : DragonVariant.values()) {
            String name = variant.getSerializedName();
            DeferredBlock<DragonEggBlock> egg = BLOCKS.registerBlock(name + "_dragon_egg", p -> new DragonEggBlock(variant, p),
                p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.0F, 9.0F).lightLevel(s -> 1).noOcclusion().sound(SoundType.METAL));
            EGG_BLOCKS.put(variant, egg);
            // O ovo do Dragão do Ender é o ovo do próprio jogo; o bloco do mod só existe enquanto choca.
            if (variant != DragonVariant.ENDER) {
                EGG_ITEMS.put(variant, ITEMS.registerSimpleBlockItem(egg, p -> p.rarity(Rarity.EPIC).stacksTo(16)));
            }
            SCALES.put(variant, ITEMS.registerSimpleItem(name + "_dragon_scale", p -> p.rarity(Rarity.RARE)));
            CompoundTag data = new CompoundTag();
            data.putString(DragonEntity.TAG_VARIANT, name);
            SPAWN_EGGS.put(variant, ITEMS.registerItem(name + "_dragon_spawn_egg", SpawnEggItem::new,
                p -> p.component(DataComponents.ENTITY_DATA, TypedEntityData.<EntityType<?>>of(DRAGON.get(), data))));
        }
    }

    /** Armaduras de escamas: uma por cor, protegem como netherite (acima do diamante) e aceitam moldes de embelezamento. */
    public static final Map<DragonVariant, Map<ArmorType, DeferredItem<Item>>> ARMOR = new EnumMap<>(DragonVariant.class);
    private static final ArmorType[] ARMOR_PIECES = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};

    static {
        Map<ArmorType, Integer> defense = Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8,
            ArmorType.HELMET, 3, ArmorType.BODY, 11);
        for (DragonVariant variant : DragonVariant.values()) {
            String name = variant.getSerializedName() + "_dragon";
            ArmorMaterial material = new ArmorMaterial(37, defense, 15, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F,
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "repairs_" + name + "_armor")),
                ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, name)));
            Map<ArmorType, DeferredItem<Item>> pieces = new EnumMap<>(ArmorType.class);
            for (ArmorType type : ARMOR_PIECES) {
                pieces.put(type, ITEMS.registerSimpleItem(name + "_" + type.getName(),
                    p -> p.humanoidArmor(material, type).fireResistant().rarity(Rarity.EPIC)));
            }
            ARMOR.put(variant, pieces);
        }
    }

    public static final DeferredItem<DragonSaddleItem> SADDLE = ITEMS.registerItem("dragon_saddle",
        p -> new DragonSaddleItem(SaddleType.SINGLE, p), p -> p.stacksTo(1));
    public static final DeferredItem<DragonSaddleItem> DOUBLE_SADDLE = ITEMS.registerItem("double_dragon_saddle",
        p -> new DragonSaddleItem(SaddleType.DOUBLE, p), p -> p.stacksTo(1));

    public static final DeferredHolder<StructureType<?>, StructureType<DragonNestStructure>> DRAGON_NEST_STRUCTURE =
        STRUCTURE_TYPES.register("dragon_nest", () -> () -> DragonNestStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> DRAGON_NEST_PIECE =
        STRUCTURE_PIECES.register("dragon_nest", () -> (StructurePieceType.ContextlessType) DragonNestPiece::new);

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.drakebound"))
            .icon(() -> EGG_ITEMS.get(DragonVariant.RED).toStack())
            .displayItems((params, output) -> {
                EGG_ITEMS.values().forEach(output::accept);
                output.accept(SCORCHED_EARTH_ITEM);
                SCALES.values().forEach(output::accept);
                output.accept(SADDLE);
                output.accept(DOUBLE_SADDLE);
                ARMOR.values().forEach(pieces -> pieces.values().forEach(output::accept));
                SPAWN_EGGS.values().forEach(output::accept);
            })
            .build());
    }

    /** Item do ovo de cada variante; o Dragão do Ender usa o ovo do Ender Dragon do jogo. */
    public static Item eggItem(DragonVariant variant) {
        return variant == DragonVariant.ENDER ? net.minecraft.world.item.Items.DRAGON_EGG : EGG_ITEMS.get(variant).get();
    }

    public static DragonSaddleItem saddleItem(SaddleType type) {
        return (type == SaddleType.DOUBLE ? DOUBLE_SADDLE : SADDLE).get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECES.register(modBus);
        TABS.register(modBus);
    }

    private ModRegistries() {
    }
}
