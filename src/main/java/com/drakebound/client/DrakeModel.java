package com.drakebound.client;

import com.drakebound.entity.DragonEntity;
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
    private static final float STEP_SWING = 0.35F;
    /** De pé o corpo sobe para as patas dobradas tocarem no chão (unidades do modelo, 16 = 1 bloco). */
    public static final float STAND_LIFT = DragonEntity.STAND_LIFT_UNITS;
    /** Pose agachada (rad): patas dobradas por baixo do corpo, pés assentes no chão. */
    private static final float FRONT_UPPER = 1.3F;
    private static final float FRONT_TIP = -2.34F;
    private static final float FRONT_FOOT = 1.04F;
    private static final float HIND_UPPER = -0.8F;
    private static final float HIND_TIP = 1.6F;
    private static final float HIND_FOOT = -0.8F;
    /** Patas de trás mais pequenas e patas da frente mais grossas (proporções de quadrúpede). */
    private static final float HIND_SCALE = 0.8F;
    private static final float FRONT_THICKNESS = 1.4F;
    /** Pescoço erguido em curva e cabeça direita. */
    private static final float[] NECK_STAND = {-0.7F, -0.45F, -0.2F, 0.0F, 0.15F};
    private static final float HEAD_STAND = 0.05F;
    private static final float TAIL_DROOP = -0.03F;
    private static final float SEGMENT = 10.0F;

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
    private final ModelPart[] frontTips;
    private final ModelPart[] frontFeet;
    private final ModelPart[] hindTips;
    private final ModelPart[] hindFeet;
    private final ModelPart[] neck = new ModelPart[5];
    private final ModelPart[] tail = new ModelPart[12];
    private final ModelPart head;

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
        this.frontTips = new ModelPart[]{this.leftFrontLeg.getChild("left_front_leg_tip"), this.rightFrontLeg.getChild("right_front_leg_tip")};
        this.frontFeet = new ModelPart[]{this.frontTips[0].getChild("left_front_foot"), this.frontTips[1].getChild("right_front_foot")};
        this.hindTips = new ModelPart[]{this.leftHindLeg.getChild("left_hind_leg_tip"), this.rightHindLeg.getChild("right_hind_leg_tip")};
        this.hindFeet = new ModelPart[]{this.hindTips[0].getChild("left_hind_foot"), this.hindTips[1].getChild("right_hind_foot")};
        for (int i = 0; i < this.neck.length; i++) {
            this.neck[i] = root.getChild("neck" + i);
        }
        for (int i = 0; i < this.tail.length; i++) {
            this.tail[i] = root.getChild("tail" + i);
        }
        this.head = root.getChild("head");
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
        this.leftHindLeg.xScale = this.leftHindLeg.yScale = this.leftHindLeg.zScale = HIND_SCALE;
        this.rightHindLeg.xScale = this.rightHindLeg.yScale = this.rightHindLeg.zScale = HIND_SCALE;
        this.leftFrontLeg.xScale = this.leftFrontLeg.zScale = FRONT_THICKNESS;
        this.rightFrontLeg.xScale = this.rightFrontLeg.zScale = FRONT_THICKNESS;
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

            this.standPose(ground);
            // Patas: diagonais alternadas (frente-esquerda com trás-direita), como um quadrúpede.
            float swing = Mth.sin(drake.walkPos * STEP_FREQ) * STEP_SWING * Math.min(1.0F, drake.walkSpeed * 2.0F) * ground;
            this.leftFrontLeg.xRot += swing;
            this.rightHindLeg.xRot += swing;
            this.rightFrontLeg.xRot -= swing;
            this.leftHindLeg.xRot -= swing;
        }
    }

    /** Mistura a pose de voo (do Ender Dragon) com a pose de pé: patas por baixo do corpo, pescoço erguido. */
    private void standPose(float ground) {
        this.root.y -= STAND_LIFT * ground;
        this.leftFrontLeg.xRot = Mth.lerp(ground, this.leftFrontLeg.xRot, FRONT_UPPER);
        this.rightFrontLeg.xRot = this.leftFrontLeg.xRot;
        this.leftHindLeg.xRot = Mth.lerp(ground, this.leftHindLeg.xRot, HIND_UPPER);
        this.rightHindLeg.xRot = this.leftHindLeg.xRot;
        for (int i = 0; i < 2; i++) {
            this.frontTips[i].xRot = Mth.lerp(ground, this.frontTips[i].xRot, FRONT_TIP);
            this.frontFeet[i].xRot = Mth.lerp(ground, this.frontFeet[i].xRot, FRONT_FOOT);
            this.hindTips[i].xRot = Mth.lerp(ground, this.hindTips[i].xRot, HIND_TIP);
            this.hindFeet[i].xRot = Mth.lerp(ground, this.hindFeet[i].xRot, HIND_FOOT);
        }
        // Pescoço: o modelo encadeia as posições a partir das rotações, por isso recalcula-se a cadeia.
        float x = this.neck[0].x;
        float y = this.neck[0].y;
        float z = this.neck[0].z;
        for (int i = 0; i < this.neck.length; i++) {
            ModelPart part = this.neck[i];
            part.xRot = Mth.lerp(ground, part.xRot, NECK_STAND[i]);
            part.x = x;
            part.y = y;
            part.z = z;
            x -= Mth.sin(part.yRot) * Mth.cos(part.xRot) * SEGMENT;
            y += Mth.sin(part.xRot) * SEGMENT;
            z -= Mth.cos(part.yRot) * Mth.cos(part.xRot) * SEGMENT;
        }
        this.head.x = x;
        this.head.y = y;
        this.head.z = z;
        this.head.xRot = Mth.lerp(ground, this.head.xRot, HEAD_STAND);
        // Cauda: desce um pouco em direção ao chão.
        x = this.tail[0].x;
        y = this.tail[0].y;
        z = this.tail[0].z;
        float droop = 0.0F;
        for (ModelPart part : this.tail) {
            droop += TAIL_DROOP * ground;
            part.xRot += droop;
            part.x = x;
            part.y = y;
            part.z = z;
            y += Mth.sin(part.xRot) * SEGMENT;
            z -= Mth.cos(part.yRot) * Mth.cos(part.xRot) * SEGMENT;
            x -= Mth.sin(part.yRot) * Mth.cos(part.xRot) * SEGMENT;
        }
    }
}
