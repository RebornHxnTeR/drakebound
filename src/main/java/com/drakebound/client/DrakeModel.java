package com.drakebound.client;

import com.drakebound.entity.SaddleType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.util.Mth;

/**
 * Modelo do Ender Dragon com:
 * <ul>
 *   <li>selas presas ao corpo (acompanham a animação);</li>
 *   <li>asas fechadas junto ao corpo quando pousado (sem bater);</li>
 *   <li>patas a dar passos quando caminha.</li>
 * </ul>
 */
public class DrakeModel extends EnderDragonModel {
    /** Asa fechada: osso virado para trás, membrana pendurada ao lado do corpo, ponta dobrada por cima. */
    private static final float FOLD_WING_X = 0.05F;
    private static final float FOLD_WING_Y = -1.45F;
    private static final float FOLD_WING_Z = -1.75F;
    private static final float FOLD_TIP_Z = Mth.PI - 0.08F;
    /** Passada: frequência e amplitude (radianos) do balanço das patas. */
    private static final float STEP_FREQ = 0.35F;
    private static final float STEP_SWING = 0.45F;

    private final ModelPart saddleFront;
    private final ModelPart saddleBack;
    private final ModelPart leftWing;
    private final ModelPart leftWingTip;
    private final ModelPart rightWing;
    private final ModelPart rightWingTip;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;

    public DrakeModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        this.saddleFront = body.getChild("saddle_front");
        this.saddleBack = body.getChild("saddle_back");
        this.leftWing = body.getChild("left_wing");
        this.leftWingTip = this.leftWing.getChild("left_wing_tip");
        this.rightWing = body.getChild("right_wing");
        this.rightWingTip = this.rightWing.getChild("right_wing_tip");
        this.leftFrontLeg = body.getChild("left_front_leg");
        this.rightFrontLeg = body.getChild("right_front_leg");
        this.leftHindLeg = body.getChild("left_hind_leg");
        this.rightHindLeg = body.getChild("right_hind_leg");
    }

    /** Modelo que só desenha as selas (o resto continua a ser posicionado, mas não é desenhado). */
    public static DrakeModel saddleOnly(ModelPart root) {
        DrakeModel model = new DrakeModel(root);
        for (ModelPart part : root.getAllParts()) {
            part.skipDraw = true;
        }
        model.saddleFront.skipDraw = false;
        model.saddleBack.skipDraw = false;
        return model;
    }

    /** Acrescenta as duas selas ao corpo (coordenadas do corpo do Ender Dragon; o topo está em y = 1). */
    public static MeshDefinition addSaddles(MeshDefinition mesh) {
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild("saddle_front", CubeListBuilder.create()
                .addBox("seat", -7.0F, -6.0F, -12.0F, 14, 7, 16, 64, 144)
                .addBox("pommel", -3.0F, -9.0F, -12.0F, 6, 3, 3, 64, 170)
                .addBox("cantle", -6.0F, -8.0F, 1.0F, 12, 2, 3, 88, 170)
                .addBox("strap", -13.0F, 1.0F, 2.0F, 1, 16, 4, 64, 180)
                .mirror()
                .addBox("strap", 12.0F, 1.0F, 2.0F, 1, 16, 4, 64, 180),
            PartPose.ZERO);
        body.addOrReplaceChild("saddle_back", CubeListBuilder.create()
                .addBox("seat", -7.0F, -6.0F, 8.0F, 14, 7, 16, 128, 144)
                .addBox("cantle", -6.0F, -8.0F, 21.0F, 12, 2, 3, 120, 170),
            PartPose.ZERO);
        return mesh;
    }

    @Override
    public void setupAnim(EnderDragonRenderState state) {
        super.setupAnim(state);
        if (!(state instanceof DragonRenderState drake)) {
            this.saddleFront.visible = false;
            this.saddleBack.visible = false;
            return;
        }
        this.saddleFront.visible = drake.saddle != SaddleType.NONE;
        this.saddleBack.visible = drake.saddle == SaddleType.DOUBLE;

        float ground = drake.groundAmount;
        if (ground > 0.0F) {
            // Asas: da pose de voo (já calculada) para fechadas, proporcionalmente a "quão pousado" está.
            this.leftWing.xRot = Mth.lerp(ground, this.leftWing.xRot, FOLD_WING_X);
            this.leftWing.yRot = Mth.lerp(ground, this.leftWing.yRot, FOLD_WING_Y);
            this.leftWing.zRot = Mth.lerp(ground, this.leftWing.zRot, FOLD_WING_Z);
            this.leftWingTip.zRot = Mth.lerp(ground, this.leftWingTip.zRot, FOLD_TIP_Z);
            this.rightWing.xRot = this.leftWing.xRot;
            this.rightWing.yRot = -this.leftWing.yRot;
            this.rightWing.zRot = -this.leftWing.zRot;
            this.rightWingTip.zRot = -this.leftWingTip.zRot;

            // Patas: diagonais alternadas (frente-esquerda com trás-direita), como um quadrúpede.
            float swing = Mth.sin(drake.walkPos * STEP_FREQ) * STEP_SWING * Math.min(1.0F, drake.walkSpeed * 2.0F) * ground;
            this.leftFrontLeg.xRot += swing;
            this.rightHindLeg.xRot += swing;
            this.rightFrontLeg.xRot -= swing;
            this.leftHindLeg.xRot -= swing;
        }
    }
}
