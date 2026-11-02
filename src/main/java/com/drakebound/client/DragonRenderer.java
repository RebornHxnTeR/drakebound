package com.drakebound.client;

import com.drakebound.Drakebound;
import com.drakebound.entity.DragonEntity;
import com.drakebound.entity.SaddleType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Desenha os dragões com o modelo do Ender Dragon, à escala, recolorido por variante.
 * Os machos usam uma camada com cornos extra na cabeça; as fêmeas usam o modelo original.
 */
public class DragonRenderer extends EntityRenderer<DragonEntity, DragonRenderState> {
    public static final ModelLayerLocation MALE_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "dragon"), "male");
    public static final ModelLayerLocation FEMALE_LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "dragon"), "female");
    /** Adultos têm exatamente o tamanho do Ender Dragon. */
    private static final float MODEL_SCALE = 1.0F;
    /** Desce o modelo para que as patas toquem no chão (em blocos, antes da escala). */
    private static final float MODEL_Y_OFFSET = -1.0F;

    private static final Identifier SADDLE_TEXTURE = Identifier.fromNamespaceAndPath(Drakebound.MOD_ID, "textures/entity/dragon_saddle.png");

    private final DrakeModel maleModel;
    private final DrakeModel femaleModel;
    /** Só as selas: a textura da sela não pode ser aplicada às asas, que partilham a mesma zona UV. */
    private final DrakeModel saddleModel;

    public DragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 3.0F;
        this.maleModel = new DrakeModel(context.bakeLayer(MALE_LAYER));
        this.femaleModel = new DrakeModel(context.bakeLayer(FEMALE_LAYER));
        this.saddleModel = DrakeModel.saddleOnly(context.bakeLayer(FEMALE_LAYER));
        DragonTextures.rebuild(context.getResourceManager());
    }

    public static LayerDefinition createMaleLayer() {
        return EnderDragonModel.createBodyLayer().apply(mesh -> {
            PartDefinition head = mesh.getRoot().getChild("head");
            head.addOrReplaceChild("horns", CubeListBuilder.create()
                    .addBox("horn", -7.0F, -3.0F, 0.0F, 3, 3, 18, 0, 0)
                    .mirror()
                    .addBox("horn", 4.0F, -3.0F, 0.0F, 3, 3, 18, 0, 0),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.45F, 0.0F, 0.0F));
            return DrakeModel.addSaddles(mesh);
        });
    }

    public static LayerDefinition createFemaleLayer() {
        return EnderDragonModel.createBodyLayer().apply(DrakeModel::addSaddles);
    }

    @Override
    public DragonRenderState createRenderState() {
        return new DragonRenderState();
    }

    @Override
    public void extractRenderState(DragonEntity dragon, DragonRenderState state, float partialTicks) {
        // Antes do super: a sombra (calculada no super) depende da escala.
        state.growthScale = dragon.getAgeScale();
        super.extractRenderState(dragon, state, partialTicks);
        state.variant = dragon.getVariant();
        state.male = dragon.isMale();
        state.saddle = dragon.getSaddle();
        state.flapTime = Mth.lerp(partialTicks, dragon.oFlapTime, dragon.flapTime);
        state.deathTime = 0.0F;
        state.hasRedOverlay = dragon.hurtTime > 0 || dragon.deathTime > 0;
        state.beamOffset = null;
        state.isLandingOrTakingOff = false;
        state.groundAmount = Mth.lerp(partialTicks, dragon.oGroundAmount, dragon.groundAmount);
        state.isSitting = false;
        state.walkPos = dragon.walkAnimation.position(partialTicks);
        state.walkSpeed = dragon.walkAnimation.speed(partialTicks);
        state.partialTicks = partialTicks;
        state.flightHistory.copyFrom(dragon.flightHistory);
    }

    @Override
    public void submit(DragonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        float scale = MODEL_SCALE * state.growthScale;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, MODEL_Y_OFFSET, 0.0F);
        float yRot = state.getHistoricalPos(7).yRot();
        float pitch = (float) (state.getHistoricalPos(5).y() - state.getHistoricalPos(10).y());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch * 10.0F));
        poseStack.translate(0.0F, 0.0F, 1.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        DrakeModel model = state.male ? this.maleModel : this.femaleModel;
        int overlay = OverlayTexture.pack(0.0F, state.hasRedOverlay);
        collector.submitModel(model, state, poseStack, DragonTextures.skin(state.variant), state.lightCoords, overlay, state.outlineColor, null);
        collector.submitModel(model, state, poseStack, RenderTypes.eyes(DragonTextures.eyes(state.variant)), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        if (state.saddle != SaddleType.NONE) {
            collector.submitModel(this.saddleModel, state, poseStack, SADDLE_TEXTURE, state.lightCoords, overlay, state.outlineColor, null);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    /** A sombra acompanha o tamanho do dragão. */
    @Override
    protected float getShadowRadius(DragonRenderState state) {
        return this.shadowRadius * state.growthScale;
    }

    /** O modelo (asas e cauda) é muito maior do que a caixa de colisão. */
    @Override
    protected AABB getBoundingBoxForCulling(DragonEntity dragon) {
        return dragon.getBoundingBox().inflate(10.0 * dragon.getAgeScale());
    }
}
