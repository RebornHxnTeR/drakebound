package com.drakebound.block;

import com.drakebound.entity.DragonEntity;
import com.drakebound.entity.DragonVariant;
import com.drakebound.registry.ModRegistries;
import com.drakebound.registry.ModTags;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ovo de dragão. Não usa BlockEntity nem ticks contínuos: agenda um tick a cada ~2,5 minutos,
 * verifica só os 6 blocos vizinhos à procura de calor e avança uma fase de incubação.
 */
public class DragonEggBlock extends Block {
    public static final MapCodec<DragonEggBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        DragonVariant.CODEC.fieldOf("variant").forGetter(b -> b.variant),
        propertiesCodec()
    ).apply(i, DragonEggBlock::new));

    public static final IntegerProperty HATCH = BlockStateProperties.AGE_4;
    public static final int MAX_HATCH = 4;
    /** 4 fases x 3000 ticks = cerca de 10 minutos de calor contínuo. */
    private static final int STAGE_TICKS = 3000;
    private static final int RANDOM_OFFSET = 300;
    private static final double OWNER_RADIUS = 16.0;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);
    private static final Direction[] DIRECTIONS = Direction.values();

    public final DragonVariant variant;

    public DragonEggBlock(DragonVariant variant, BlockBehaviour.Properties properties) {
        super(properties);
        this.variant = variant;
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!state.is(oldState.getBlock())) {
            scheduleCheck(level, pos, this, level.getRandom());
        }
    }

    public static void scheduleCheck(LevelAccessor level, BlockPos pos, Block block, RandomSource random) {
        level.scheduleTick(pos, block, STAGE_TICKS + random.nextInt(RANDOM_OFFSET));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isHeated(level, pos)) {
            int hatch = state.getValue(HATCH);
            if (hatch < MAX_HATCH) {
                level.setBlock(pos, state.setValue(HATCH, hatch + 1), Block.UPDATE_CLIENTS);
                level.playSound(null, pos, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.6F + random.nextFloat() * 0.2F);
            } else {
                this.hatch(level, pos, random);
                return;
            }
        }
        scheduleCheck(level, pos, this, random);
    }

    private static boolean isHeated(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (Direction direction : DIRECTIONS) {
            BlockState state = level.getBlockState(neighbour.setWithOffset(pos, direction));
            if (state.is(ModTags.EGG_HEAT_SOURCES) && (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT))) {
                return true;
            }
        }
        return false;
    }

    /** Ao nascer, o dono é o jogador mais próximo num raio de 16 blocos; sem ninguém, a cria fica selvagem. */
    private void hatch(ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        DragonEntity baby = ModRegistries.DRAGON.get().create(level, EntitySpawnReason.BREEDING);
        if (baby == null) {
            return;
        }
        baby.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        baby.setVariant(this.variant);
        baby.setMale(random.nextBoolean());
        baby.setAge(DragonEntity.BABY_START_AGE);
        baby.setPersistenceRequired();
        Player owner = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, OWNER_RADIUS, EntitySelector.NO_SPECTATORS);
        if (owner != null) {
            baby.tame(owner);
        }
        level.addFreshEntity(baby);
    }
}
