package com.drakebound.block;

import com.drakebound.entity.DragonEntity;
import com.drakebound.entity.DragonVariant;
import com.drakebound.registry.ModRegistries;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ovo de dragão. Só choca em cima de um bloco de magma e depois de aceso com um isqueiro.
 * Não usa BlockEntity nem ticks contínuos: enquanto está aceso agenda um tick a cada ~2,5 minutos,
 * verifica só o bloco de baixo e avança uma fase de incubação. Sem magma por baixo, apaga-se.
 */
public class DragonEggBlock extends Block {
    public static final MapCodec<DragonEggBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        DragonVariant.CODEC.fieldOf("variant").forGetter(b -> b.variant),
        propertiesCodec()
    ).apply(i, DragonEggBlock::new));

    public static final IntegerProperty HATCH = BlockStateProperties.AGE_4;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int MAX_HATCH = 4;
    /** 4 fases x 3000 ticks = cerca de 10 minutos aceso. */
    private static final int STAGE_TICKS = 3000;
    private static final int RANDOM_OFFSET = 300;
    private static final double OWNER_RADIUS = 16.0;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

    public final DragonVariant variant;

    public DragonEggBlock(DragonVariant variant, BlockBehaviour.Properties properties) {
        super(properties);
        this.variant = variant;
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH, LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Acender com isqueiro: só funciona se o ovo estiver em cima de magma. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL) || state.getValue(LIT)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (!isOnMagma(level, pos)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.drakebound.egg_needs_magma"));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            scheduleCheck(level, pos, this, level.getRandom());
        }
        return InteractionResult.SUCCESS;
    }

    public static void scheduleCheck(LevelAccessor level, BlockPos pos, Block block, RandomSource random) {
        level.scheduleTick(pos, block, STAGE_TICKS + random.nextInt(RANDOM_OFFSET));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (!isOnMagma(level, pos)) {
            // Tirar o magma apaga o ovo; o progresso fica guardado até ser aceso de novo.
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
            return;
        }
        int hatch = state.getValue(HATCH);
        if (hatch >= MAX_HATCH) {
            this.hatch(level, pos, random);
            return;
        }
        level.setBlock(pos, state.setValue(HATCH, hatch + 1), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.6F + random.nextFloat() * 0.2F);
        scheduleCheck(level, pos, this, random);
    }

    private static boolean isOnMagma(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Blocks.MAGMA_BLOCK);
    }

    /** Ovo aceso deita umas chamas pequenas (só no cliente, sem custo no servidor). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.1,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.02, 0.0);
        }
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
