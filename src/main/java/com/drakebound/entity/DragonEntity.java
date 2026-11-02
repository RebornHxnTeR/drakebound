package com.drakebound.entity;

import com.drakebound.item.DragonArmorItem;
import com.drakebound.item.DragonSaddleItem;
import com.drakebound.registry.ModRegistries;
import com.drakebound.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Um único tipo de entidade para todos os dragões. Variante, sexo, sela e fase de crescimento
 * vivem em SynchedEntityData; tudo o que varia por variante vem da tabela {@link DragonVariant}.
 */
public class DragonEntity extends TamableAnimal implements FlyingAnimal {
    public static final String TAG_VARIANT = "variant";
    private static final String TAG_MALE = "male";
    private static final String TAG_SADDLE = "saddle";
    private static final String TAG_STAMINA = "stamina";
    private static final String TAG_WANDER = "wander";

    public static final int BABY_START_AGE = -72000;
    /** Um quarto do crescimento total (em segundos), por cada bloco de carniça. */
    private static final int CARRION_GROWTH_SECONDS = -BABY_START_AGE / 20 / 4;
    public static final float REJECT_CHANCE = 0.05F;
    public static final int NEST_RADIUS = 24;

    public static final int MAX_STAMINA = 1200;
    private static final int STAMINA_REGEN = 3;
    private static final int BREATH_DURATION = 40;
    private static final int BREATH_COOLDOWN = 120;
    private static final double BREATH_RANGE = 20.0;
    private static final double BREATH_CONE_DOT = 0.75;
    private static final float RIDDEN_FLY_ACCEL = 0.1F;
    private static final float RIDDEN_WALK_ACCEL = 0.12F;
    /** Atrito no chão (bloco normal 0.6 x ar 0.91), igual ao de qualquer mob a andar. */
    private static final double GROUND_FRICTION = 0.546;
    private static final int PREY_SEARCH_INTERVAL = 300;
    private static final float PREY_HEAL = 20.0F;

    private static final byte FLAG_MALE = 1;
    private static final byte FLAG_EXHAUSTED = 2;
    private static final byte FLAG_WANDER = 4;
    /** Raio (blocos) em que um dragão em modo "passear" anda à solta. */
    public static final int WANDER_RADIUS = 32;
    /** Só se teleporta para o dono a partir desta distância (o dragão voa e é rápido). */
    private static final double TELEPORT_DISTANCE_SQR = 48.0 * 48.0;
    /** Velocidade com que o modelo passa de "a voar" para "pousado" (por tick). */
    private static final float GROUND_BLEND_STEP = 0.1F;
    private static final int FLY_MIN_HEIGHT = 10;
    private static final int FLY_EXTRA_HEIGHT = 9;

    private static final EntityDataAccessor<Byte> DATA_VARIANT = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_DRAGON_FLAGS = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_SADDLE = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_STAGE = SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);

    /** Histórico de voo usado pelo modelo do Ender Dragon para animar pescoço e cauda (só cliente). */
    public final DragonFlightHistory flightHistory = new DragonFlightHistory();
    public float flapTime;
    public float oFlapTime;
    /** Quanto o modelo sobe quando está de pé (unidades do modelo; 16 = 1 bloco). */
    public static final float STAND_LIFT_UNITS = 12.0F;
    /** 0 = a voar, 1 = pousado; suavizado para o modelo não saltar entre poses (só cliente). */
    public float groundAmount;
    public float oGroundAmount;

    private int stamina = MAX_STAMINA;
    private int breathTicks;
    private int breathCooldown;
    private @Nullable Vec3 riderBreathDir;

    public DragonEntity(EntityType<? extends DragonEntity> type, Level level) {
        super(type, level);
        // Sem pairar parado: quando não tem destino a gravidade puxa-o para o chão, onde também anda.
        this.moveControl = new DragonMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 240.0)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.ATTACK_KNOCKBACK, 1.5)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FLYING_SPEED, 0.6)
            .add(Attributes.STEP_HEIGHT, 1.5)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.CAMERA_DISTANCE, 10.0);
    }

    // ---------------------------------------------------------------- dados sincronizados

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, (byte) 0);
        builder.define(DATA_DRAGON_FLAGS, FLAG_MALE);
        builder.define(DATA_SADDLE, (byte) 0);
        builder.define(DATA_STAGE, (byte) GrowthStage.ADULT.ordinal());
    }

    public DragonVariant getVariant() {
        return DragonVariant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(DragonVariant variant) {
        this.entityData.set(DATA_VARIANT, (byte) variant.ordinal());
        this.applyStats();
    }

    public boolean isMale() {
        return (this.entityData.get(DATA_DRAGON_FLAGS) & FLAG_MALE) != 0;
    }

    public void setMale(boolean male) {
        this.setDragonFlag(FLAG_MALE, male);
        this.applyStats();
    }

    public boolean isExhausted() {
        return (this.entityData.get(DATA_DRAGON_FLAGS) & FLAG_EXHAUSTED) != 0;
    }

    /** Modo "passear": anda à solta perto de onde foi deixado em vez de seguir o dono. */
    public boolean isWandering() {
        return (this.entityData.get(DATA_DRAGON_FLAGS) & FLAG_WANDER) != 0;
    }

    private void setWandering(boolean wander) {
        this.setDragonFlag(FLAG_WANDER, wander);
        if (wander) {
            this.setHomeTo(this.blockPosition(), WANDER_RADIUS);
        } else {
            this.clearHome();
        }
    }

    private void setDragonFlag(byte flag, boolean value) {
        byte flags = this.entityData.get(DATA_DRAGON_FLAGS);
        this.entityData.set(DATA_DRAGON_FLAGS, (byte) (value ? flags | flag : flags & ~flag));
    }

    public SaddleType getSaddle() {
        return SaddleType.byId(this.entityData.get(DATA_SADDLE));
    }

    public void setSaddle(SaddleType saddle) {
        this.entityData.set(DATA_SADDLE, (byte) saddle.ordinal());
    }

    public GrowthStage getStage() {
        return GrowthStage.byId(this.entityData.get(DATA_STAGE));
    }

    private void setStage(GrowthStage stage) {
        this.entityData.set(DATA_STAGE, (byte) stage.ordinal());
        this.applyStats();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_STAGE.equals(accessor) || DATA_VARIANT.equals(accessor)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public float getAgeScale() {
        // Adultos têm o tamanho do Ender Dragon; o Ancião do Nether é anão (~1,5 blocos mais baixo).
        return this.getStage().scale * (this.getVariant() == DragonVariant.NETHER_ELDER ? 0.7F : 1.0F);
    }

    @Override
    protected int getBabyStartAge() {
        return BABY_START_AGE;
    }

    @Override
    public void setAge(int age) {
        super.setAge(age);
        GrowthStage stage = GrowthStage.fromAge(age);
        if (stage != this.getStage()) {
            this.setStage(stage);
        }
    }

    /** Vida e dano dependem de sexo, variante e fase; recalculados só quando um destes muda. */
    private void applyStats() {
        if (this.level().isClientSide()) {
            return;
        }
        GrowthStage stage = this.getStage();
        float power = this.getVariant().powerMultiplier;
        boolean male = this.isMale();
        boolean wasFull = this.getHealth() >= this.getMaxHealth();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((male ? 240.0 : 200.0) * power * stage.healthFactor);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((male ? 8.0 : 12.0) * power * stage.healthFactor);
        if (wasFull || this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    /** Nome mostrado no jogo: "Dragão Verdejante", "Peaks Dragon", etc. */
    @Override
    protected Component getTypeName() {
        return Component.translatable(this.getType().getDescriptionId() + "." + this.getVariant().getSerializedName());
    }

    // ---------------------------------------------------------------- spawn e gravação

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        this.setMale(level.getRandom().nextBoolean());
        // Nunca aparecem crias no mundo: só nascem de ovos chocados.
        return super.finalizeSpawn(level, difficulty, reason, groupData != null ? groupData : new AgeableMob.AgeableMobGroupData(false));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString(TAG_VARIANT, this.getVariant().getSerializedName());
        output.putBoolean(TAG_MALE, this.isMale());
        output.putByte(TAG_SADDLE, (byte) this.getSaddle().ordinal());
        output.putInt(TAG_STAMINA, this.stamina);
        output.putBoolean(TAG_WANDER, this.isWandering());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_VARIANT, (byte) input.read(TAG_VARIANT, DragonVariant.CODEC).orElse(this.getVariant()).ordinal());
        this.setDragonFlag(FLAG_MALE, input.getBooleanOr(TAG_MALE, this.isMale()));
        this.setSaddle(SaddleType.byId(input.getByteOr(TAG_SADDLE, (byte) this.getSaddle().ordinal())));
        this.stamina = input.getIntOr(TAG_STAMINA, MAX_STAMINA);
        this.setDragonFlag(FLAG_WANDER, input.getBooleanOr(TAG_WANDER, false));
        this.entityData.set(DATA_STAGE, (byte) GrowthStage.fromAge(this.getAge()).ordinal());
        this.applyStats();
    }

    // ---------------------------------------------------------------- IA

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0, 12.0F, 4.0F) {
            @Override
            public boolean canUse() {
                return !DragonEntity.this.isWandering() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !DragonEntity.this.isWandering() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new EatCarrionGoal(this));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        // Passeia mais vezes a pé e de vez em quando levanta voo.
        this.goalSelector.addGoal(6, new DragonWalkGoal(this, 0.7));
        // Voo alto (10 a 18 blocos acima do chão): o voo "rasteiro" do goal vanilla parecia o dragão a flutuar.
        WaterAvoidingRandomFlyingGoal fly = new WaterAvoidingRandomFlyingGoal(this, 1.0) {
            @Override
            protected @Nullable Vec3 getPosition() {
                Vec3 pos = super.getPosition();
                if (pos == null) {
                    return null;
                }
                int ground = DragonEntity.this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(pos.x), Mth.floor(pos.z));
                return new Vec3(pos.x, Math.max(pos.y, ground + FLY_MIN_HEIGHT + DragonEntity.this.random.nextInt(FLY_EXTRA_HEIGHT)), pos.z);
            }
        };
        fly.setInterval(400);
        this.goalSelector.addGoal(6, fly);
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, true, (target, level) -> this.isHostileTo(target)));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true,
            (target, level) -> !this.isTame() && this.getVariant() == DragonVariant.GREY));
        // Caça: dragões selvagens comem galinhas, cabras, vacas e ovelhas. Intervalo alto = procura rara e barata.
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Animal.class, PREY_SEARCH_INTERVAL, true, false,
            (target, level) -> !this.isTame() && !this.isBaby() && target.is(ModTags.DRAGON_PREY)));
    }

    /** Dragões selvagens adultos defendem o ninho; os Cinzentos (raids) atacam sempre. */
    private boolean isHostileTo(LivingEntity target) {
        if (this.isTame() || this.isBaby()) {
            return false;
        }
        return this.getVariant() == DragonVariant.GREY
            || this.hasHome() && this.getHomePosition().distToCenterSqr(target.position()) < NEST_RADIUS * NEST_RADIUS;
    }

    /** Comer a presa recupera vida. */
    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
        if (entity.is(ModTags.DRAGON_PREY)) {
            this.heal(PREY_HEAL);
            this.playSound(SoundEvents.GENERIC_EAT.value(), 1.5F, 0.6F);
        }
        return super.killedEntity(level, entity, source);
    }

    @Override
    protected boolean canFlyToOwner() {
        return true;
    }

    @Override
    public boolean shouldTryTeleportToOwner() {
        LivingEntity owner = this.getOwner();
        return owner != null && !this.isWandering() && this.distanceToSqr(owner) >= TELEPORT_DISTANCE_SQR;
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.breathCooldown > 0) {
            this.breathCooldown--;
        }
        if (this.breathTicks > 0) {
            this.tickBreath(level);
        } else if (this.breathCooldown == 0 && !this.isBaby() && !(this.getControllingPassenger() instanceof Player)) {
            LivingEntity target = this.getTarget();
            // A linha de visão só é calculada quando o sopro está pronto e o alvo ao alcance.
            if (target != null && this.distanceToSqr(target) < BREATH_RANGE * BREATH_RANGE && this.getSensing().hasLineOfSight(target)) {
                this.startBreath(null);
            }
        }
        this.tickStamina();
    }

    // ---------------------------------------------------------------- sopro

    /** Inicia o sopro. Com {@code direction} nulo aponta ao alvo da IA. */
    public boolean startBreath(@Nullable Vec3 direction) {
        if (this.breathCooldown > 0 || this.getStage() != GrowthStage.ADULT) {
            return false;
        }
        this.riderBreathDir = direction;
        this.breathTicks = BREATH_DURATION;
        this.breathCooldown = BREATH_COOLDOWN;
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
        return true;
    }

    private void tickBreath(ServerLevel level) {
        this.breathTicks--;
        Vec3 dir = this.breathDirection();
        Vec3 mouth = this.position().add(0.0, this.getBbHeight() * 0.75, 0.0).add(dir.scale(this.getBbWidth() * 1.2));
        DragonVariant variant = this.getVariant();

        if ((this.breathTicks & 1) == 0) {
            Vec3 mid = mouth.add(dir.scale(BREATH_RANGE * 0.5));
            double spread = BREATH_RANGE * 0.2;
            double sx = Math.abs(dir.x) * spread + 0.5;
            double sy = Math.abs(dir.y) * spread + 0.5;
            double sz = Math.abs(dir.z) * spread + 0.5;
            // Todos deitam fogo; a partícula própria da variante marca o efeito secundário.
            SimpleParticleType fire = variant == DragonVariant.NETHER_ELDER ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
            level.sendParticles(fire, mid.x, mid.y, mid.z, 20, sx, sy, sz, 0.02);
            if (variant.breathParticle != fire) {
                level.sendParticles(variant.breathParticle, mid.x, mid.y, mid.z, 8, sx, sy, sz, 0.02);
            }
        }
        if (this.breathTicks % 5 != 0) {
            return;
        }

        if (variant.breathFireSeconds > 0) {
            this.scorchGround(level, mouth, dir);
        }

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4F;
        DamageSource source = this.damageSources().mobAttack(this);
        AABB area = new AABB(mouth, mouth.add(dir.scale(BREATH_RANGE))).inflate(1.5);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, area, e -> this.canBreathHit(e, mouth, dir))) {
            hit.hurtServer(level, source, damage);
            if (variant.breathFireSeconds > 0) {
                hit.igniteForSeconds(variant.breathFireSeconds);
            }
            if (variant.breathEffect != null) {
                hit.addEffect(new MobEffectInstance(variant.breathEffect, 100, variant.breathAmplifier), this);
            }
        }
    }

    /**
     * Sopro de fogo prolongado queima a terra: a cada impacto escolhe um ponto ao acaso no jato e
     * converte o bloco de terra por baixo com 25% de probabilidade, por isso só muda com exposição longa.
     */
    private void scorchGround(ServerLevel level, Vec3 mouth, Vec3 dir) {
        if (!net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, this)) {
            return;
        }
        Vec3 point = mouth.add(dir.scale(3.0 + this.random.nextDouble() * (BREATH_RANGE - 3.0)));
        BlockPos.MutableBlockPos pos = BlockPos.containing(point).mutable();
        for (int i = 0; i < 4 && level.getBlockState(pos).isAir(); i++) {
            pos.move(0, -1, 0);
        }
        if (level.getBlockState(pos).is(ModTags.SCORCHABLE) && this.random.nextFloat() < 0.25F) {
            level.setBlockAndUpdate(pos, ModRegistries.SCORCHED_EARTH.get().defaultBlockState());
        }
    }

    private Vec3 breathDirection() {
        if (this.riderBreathDir != null) {
            LivingEntity rider = this.getControllingPassenger();
            return rider != null ? rider.getLookAngle() : this.riderBreathDir;
        }
        LivingEntity target = this.getTarget();
        if (target != null) {
            return target.getEyePosition().subtract(this.getEyePosition()).normalize();
        }
        return Vec3.directionFromRotation(this.getXRot(), this.getYRot());
    }

    private boolean canBreathHit(LivingEntity entity, Vec3 mouth, Vec3 dir) {
        if (entity == this || !entity.isAlive() || entity.getVehicle() == this || this.isAlliedTo(entity)) {
            return false;
        }
        if (this.isTame() && entity instanceof TamableAnimal pet && pet.isTame() && pet.getOwnerReference() != null
            && pet.getOwnerReference().equals(this.getOwnerReference())) {
            return false;
        }
        Vec3 to = entity.getBoundingBox().getCenter().subtract(mouth);
        double lengthSqr = to.lengthSqr();
        return lengthSqr < 4.0 || to.dot(dir) / Math.sqrt(lengthSqr) > BREATH_CONE_DOT;
    }

    // ---------------------------------------------------------------- stamina e voo montado

    private void tickStamina() {
        boolean ridden = this.getControllingPassenger() instanceof Player;
        int old = this.stamina;
        if (ridden && !this.onGround()) {
            this.stamina = Math.max(0, this.stamina - 1);
        } else if (this.onGround() || !ridden) {
            this.stamina = Math.min(MAX_STAMINA, this.stamina + STAMINA_REGEN);
        }
        if (this.stamina == 0 && !this.isExhausted()) {
            this.setDragonFlag(FLAG_EXHAUSTED, true);
        } else if (this.isExhausted() && this.stamina >= MAX_STAMINA / 2) {
            this.setDragonFlag(FLAG_EXHAUSTED, false);
        }
        // Mostra a energia na barra de ação só a cada segundo e só quando mudou.
        if (ridden && old != this.stamina && this.tickCount % 20 == 0 && this.getControllingPassenger() instanceof ServerPlayer player) {
            int percent = this.stamina * 100 / MAX_STAMINA;
            player.sendOverlayMessage(Component.translatable(this.isExhausted() ? "message.drakebound.exhausted" : "message.drakebound.stamina", percent));
        }
    }

    public int getStamina() {
        return this.stamina;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player && this.isOwnedBy(player) && this.getSaddle() != SaddleType.NONE ? player : null;
    }

    @Override
    public boolean isFlyingVehicle() {
        return this.getStage() == GrowthStage.ADULT;
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float strafe = controller.xxa * 0.6F;
        if (this.onGround() && !controller.isJumping()) {
            // No chão anda: ignora a inclinação da câmara; saltar (espaço) levanta voo.
            return new Vec3(strafe, 0.0, controller.zza < 0.0F ? controller.zza * 0.5F : controller.zza);
        }
        float forward = 0.0F;
        float up = 0.0F;
        if (controller.zza != 0.0F) {
            float pitch = controller.getXRot() * Mth.DEG_TO_RAD;
            forward = Mth.cos(pitch);
            up = -Mth.sin(pitch);
            if (controller.zza < 0.0F) {
                forward *= -0.5F;
                up *= -0.5F;
            }
        }
        if (controller.isJumping()) {
            up += 0.6F;
        }
        if (this.isExhausted()) {
            // Sem energia não consegue subir: plana até ao chão.
            up = Math.min(up, 0.0F);
        }
        return new Vec3(strafe, up, forward);
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        Vec2 rotation = new Vec2(controller.getXRot() * 0.5F, controller.getYRot());
        float yRot = this.getYRot() + Mth.wrapDegrees(rotation.y - this.getYRot()) * 0.15F;
        this.setRot(yRot, rotation.x);
        this.yRotO = this.yBodyRot = this.yHeadRot = yRot;
        if (this.isExhausted() && !this.onGround()) {
            Vec3 motion = this.getDeltaMovement();
            if (motion.y < -0.25) {
                this.setDeltaMovement(motion.x, -0.25, motion.z);
            }
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (this.getControllingPassenger() instanceof Player) {
            float accel = RIDDEN_FLY_ACCEL * this.getVariant().powerMultiplier;
            if (this.onGround() && input.y <= 0.0) {
                this.setNoGravity(false);
                this.moveRelative(RIDDEN_WALK_ACCEL, input);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().multiply(GROUND_FRICTION, 0.98, GROUND_FRICTION).add(0.0, -0.08, 0.0));
            } else if (this.isExhausted()) {
                this.setNoGravity(false);
                this.moveRelative(accel * 0.5F, input);
                this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.91, 0.98, 0.91).add(0.0, -0.04, 0.0));
            } else {
                this.travelFlying(input, accel);
            }
            return;
        }
        super.travel(input);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        int seats = passenger instanceof Player ? this.getSaddle().seats : 1;
        return this.getPassengers().size() < seats;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int index = Math.max(0, this.getPassengers().indexOf(passenger));
        double z = index == 0 ? dimensions.width() * 0.15 : -dimensions.width() * 0.35;
        // De pé o modelo sobe (patas esticadas), por isso a sela também.
        double lift = STAND_LIFT_UNITS / 16.0 * this.getAgeScale() * this.standAmount();
        return new Vec3(0.0, dimensions.height() * 0.85 + lift, z).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 side = new Vec3(this.getBbWidth() * 0.8, 0.0, 0.0).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
        return this.position().add(side);
    }

    // ---------------------------------------------------------------- interação, domesticação e reprodução

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean server = !this.level().isClientSide();

        if (!this.isTame()) {
            // Adultos selvagens não podem ser domesticados; só crias, com carne crua e 5% de rejeição.
            if (this.isBaby() && stack.is(ModTags.DRAGON_FOOD)) {
                if (server) {
                    this.usePlayerItem(player, hand, stack);
                    if (this.random.nextFloat() < REJECT_CHANCE) {
                        this.level().broadcastEntityEvent(this, (byte) 6);
                    } else {
                        this.tame(player);
                        this.navigation.stop();
                        this.setTarget(null);
                        this.level().broadcastEntityEvent(this, (byte) 7);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        if (!this.isOwnedBy(player)) {
            // Outro jogador só pode ocupar o segundo lugar da sela dupla.
            if (this.isVehicle() && this.canAddPassenger(player)) {
                if (server) {
                    player.startRiding(this);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Bloco de carniça: alimento forte, faz a cria avançar um quarto do crescimento de uma vez.
        if (stack.is(ModRegistries.CARRION_BLOCK_ITEM.get()) && (this.isBaby() || this.getHealth() < this.getMaxHealth())) {
            if (server) {
                this.feed(player, hand, stack, 1.0F, 40.0F);
                if (this.isBaby()) {
                    this.ageUp(CARRION_GROWTH_SECONDS, true);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(ModTags.DRAGON_FOOD) && (this.isBaby() || this.getHealth() < this.getMaxHealth())) {
            if (server) {
                this.feed(player, hand, stack, 1.0F, 8.0F);
                if (this.isBaby()) {
                    this.ageUp(getSpeedUpSecondsWhenFeeding(-this.getAge()), true);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof DragonSaddleItem saddle && this.getSaddle() == SaddleType.NONE && this.getStage() == GrowthStage.ADULT) {
            if (server) {
                this.setSaddle(saddle.type);
                stack.consume(1, player);
                this.playSound(SoundEvents.HORSE_SADDLE.value(), 1.0F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }

        if (this.isFood(stack)) {
            if (!this.isInLava() && this.isTame() && this.getStage() == GrowthStage.ADULT) {
                if (server) {
                    player.sendOverlayMessage(Component.translatable("message.drakebound.breed_needs_lava"));
                }
                return InteractionResult.FAIL;
            }
            return super.mobInteract(player, hand);
        }

        // Armadura de dragão: só adultos, uma de cada vez.
        if (stack.getItem() instanceof DragonArmorItem && !this.isWearingBodyArmor() && this.getStage() == GrowthStage.ADULT) {
            if (server) {
                this.setBodyArmorItem(stack.copyWithCount(1));
                stack.consume(1, player);
                this.playSound(SoundEvents.HORSE_ARMOR.value(), 1.0F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }

        // Tesoura tira a sela (como nos cavalos); sem sela, tira a armadura.
        if (stack.is(Items.SHEARS) && !this.isVehicle() && (this.getSaddle() != SaddleType.NONE || this.isWearingBodyArmor())) {
            if (server) {
                ServerLevel level = (ServerLevel) this.level();
                if (this.getSaddle() != SaddleType.NONE) {
                    this.spawnAtLocation(level, new ItemStack(ModRegistries.saddleItem(this.getSaddle())));
                    this.setSaddle(SaddleType.NONE);
                    this.playSound(SoundEvents.SADDLE_UNEQUIP.value(), 1.0F, 0.8F);
                } else {
                    this.spawnAtLocation(level, this.getBodyArmorItem().copy());
                    this.setBodyArmorItem(ItemStack.EMPTY);
                    this.playSound(SoundEvents.HORSE_ARMOR_UNEQUIP.value(), 1.0F, 0.8F);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.isEmpty()) {
            if (!player.isSecondaryUseActive() && this.getSaddle() != SaddleType.NONE && this.getStage() == GrowthStage.ADULT) {
                if (server) {
                    this.setOrderedToSit(false);
                    this.setInSittingPose(false);
                    player.startRiding(this);
                }
                return InteractionResult.SUCCESS;
            }
            // Mão vazia (ou Shift + mão vazia se tiver sela): seguir -> sentar -> passear -> seguir.
            if (server) {
                this.cycleCommand(player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.DRAGON_BREEDING_FOOD);
    }

    @Override
    public boolean canFallInLove() {
        return this.isTame() && this.getStage() == GrowthStage.ADULT && this.isInLava() && super.canFallInLove();
    }

    /** Adulto domesticado, livre e ainda sem vontade de acasalar: come carniça junto à lava se a encontrar. */
    public boolean wantsCarrion() {
        return this.isTame() && this.getStage() == GrowthStage.ADULT && this.getAge() == 0 && !this.isInLove() && !this.isOrderedToSit()
            && !this.isVehicle();
    }

    /** Reprodução exige dois dragões domesticados, adultos, da mesma variante, de sexos opostos e ambos dentro de lava. */
    @Override
    public boolean canMate(Animal partner) {
        return partner instanceof DragonEntity other
            && other.isTame() && this.isTame()
            && other.isMale() != this.isMale()
            && other.getVariant() == this.getVariant()
            && this.isInLava() && other.isInLava()
            && super.canMate(partner);
    }

    /** Em vez de uma cria, a fêmea põe um ovo da sua variante, que terá de ser chocado. */
    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        DragonEntity mother = this.isMale() ? (DragonEntity) partner : this;
        mother.spawnAtLocation(level, new ItemStack(ModRegistries.eggItem(this.getVariant())));
        this.finalizeSpawnChildFromBreeding(level, partner, null);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        // Cada cor larga a sua própria escama.
        int scales = (this.getStage() == GrowthStage.ADULT ? 2 : 0) + this.random.nextInt(4);
        if (scales > 0) {
            this.spawnAtLocation(level, new ItemStack(ModRegistries.SCALES.get(this.getVariant()).get(), scales));
        }
        if (this.getSaddle() != SaddleType.NONE) {
            this.spawnAtLocation(level, new ItemStack(ModRegistries.saddleItem(this.getSaddle())));
        }
    }

    /** 0 a voar, 1 de pé: no cliente suavizado, no servidor direto. */
    public float standAmount() {
        return this.level().isClientSide() ? this.groundAmount : this.onGround() || this.isInSittingPose() ? 1.0F : 0.0F;
    }

    private void cycleCommand(Player player) {
        String key;
        if (this.isOrderedToSit()) {
            this.setOrderedToSit(false);
            this.setWandering(true);
            key = "message.drakebound.command.wander";
        } else if (this.isWandering()) {
            this.setWandering(false);
            key = "message.drakebound.command.follow";
        } else {
            this.setOrderedToSit(true);
            key = "message.drakebound.command.sit";
        }
        this.setInSittingPose(this.isOrderedToSit());
        this.navigation.stop();
        this.setTarget(null);
        this.jumping = false;
        player.sendOverlayMessage(Component.translatable(key, this.getDisplayName()));
    }

    // ---------------------------------------------------------------- cliente

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            this.oFlapTime = this.flapTime;
            boolean grounded = this.onGround() || this.isInSittingPose();
            this.flapTime += grounded ? 0.02F : this.isVehicle() ? 0.08F : 0.06F;
            this.oGroundAmount = this.groundAmount;
            this.groundAmount = Mth.approach(this.groundAmount, grounded ? 1.0F : 0.0F, GROUND_BLEND_STEP);
            // Guarda a rotação "ao contrário", como o Ender Dragon, para reaproveitar o seu modelo.
            this.flightHistory.record(this.getY(), this.yBodyRot + 180.0F);
        }
    }

    // ---------------------------------------------------------------- sons

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isBaby() ? SoundEvents.ENDER_DRAGON_AMBIENT : SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (this.isBaby() ? 1.6F : this.isMale() ? 0.9F : 1.1F);
    }

    @Override
    protected float getSoundVolume() {
        return this.isBaby() ? 0.6F : 2.0F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }
}
