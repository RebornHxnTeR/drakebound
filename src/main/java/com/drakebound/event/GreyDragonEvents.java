package com.drakebound.event;

import com.drakebound.entity.DragonEntity;
import com.drakebound.entity.DragonVariant;
import com.drakebound.entity.SaddleType;
import com.drakebound.registry.ModRegistries;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Dragão Cinzento: 15% por vaga de raid e 10% por Pillager que aparece num posto avançado,
 * sempre montado por um Pillager. Só eventos de entrada no mundo, nada corre por tick.
 */
public final class GreyDragonEvents {
    private static final float RAID_CHANCE = 0.15F;
    private static final float OUTPOST_CHANCE = 0.10F;
    private static final double OUTPOST_LIMIT_RADIUS = 64.0;
    /** Última vaga já sorteada por raid, para sortear uma vez por vaga e não por Pillager. */
    private static final Map<Raid, Integer> ROLLED_WAVES = new WeakHashMap<>();

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof Pillager pillager) || pillager.isPassenger()
            || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Raid raid = pillager.getCurrentRaid();
        if (raid != null) {
            Integer rolled = ROLLED_WAVES.get(raid);
            if (rolled != null && rolled >= pillager.getWave()) {
                return;
            }
            ROLLED_WAVES.put(raid, pillager.getWave());
            if (level.getRandom().nextFloat() < RAID_CHANCE) {
                mountLater(level, pillager);
            }
            return;
        }
        // Verificações baratas primeiro; a procura da estrutura só acontece nos 10% sorteados.
        if (level.getRandom().nextFloat() >= OUTPOST_CHANCE) {
            return;
        }
        var outpost = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getValue(BuiltinStructures.PILLAGER_OUTPOST);
        if (outpost == null || !level.structureManager().getStructureWithPieceAt(pillager.blockPosition(), outpost).isValid()) {
            return;
        }
        boolean alreadyGuarded = !level.getEntitiesOfClass(DragonEntity.class, pillager.getBoundingBox().inflate(OUTPOST_LIMIT_RADIUS),
            d -> d.getVariant() == DragonVariant.GREY).isEmpty();
        if (!alreadyGuarded) {
            mountLater(level, pillager);
        }
    }

    /** O Pillager ainda está a ser adicionado; monta-o no próximo tick do servidor. */
    private static void mountLater(ServerLevel level, Pillager pillager) {
        MinecraftServer server = level.getServer();
        server.schedule(new TickTask(server.getTickCount(), () -> {
            if (!pillager.isAlive() || pillager.isPassenger()) {
                return;
            }
            DragonEntity dragon = ModRegistries.DRAGON.get().create(level, EntitySpawnReason.EVENT);
            if (dragon == null) {
                return;
            }
            dragon.snapTo(pillager.getX(), pillager.getY(), pillager.getZ(), pillager.getYRot(), 0.0F);
            dragon.finalizeSpawn(level, level.getCurrentDifficultyAt(pillager.blockPosition()), EntitySpawnReason.EVENT, null);
            dragon.setVariant(DragonVariant.GREY);
            // Montado pelo saqueador: já traz a sela de ferro (e larga-a ao morrer).
            dragon.setSaddle(SaddleType.SINGLE);
            dragon.setPersistenceRequired();
            level.addFreshEntity(dragon);
            pillager.startRiding(dragon, true, false);
        }));
    }

    private GreyDragonEvents() {
    }
}
