package com.drakebound.event;

import com.drakebound.entity.DragonVariant;
import com.drakebound.registry.ModRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * O ovo do Ender Dragon do jogo passa a chocar um Dragão do Ender domesticável: quando um jogador
 * o coloca, é trocado pelo bloco de incubação do mod (com o mesmo aspeto, e que volta a largar o
 * ovo do jogo se for partido). O ovo gerado em cima do portal fica como está até ser apanhado.
 */
public final class EnderEggEvents {
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getPlacedBlock().is(Blocks.DRAGON_EGG) && !event.getLevel().isClientSide()) {
            event.getLevel().setBlock(event.getPos(), ModRegistries.EGG_BLOCKS.get(DragonVariant.ENDER).get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private EnderEggEvents() {
    }
}
