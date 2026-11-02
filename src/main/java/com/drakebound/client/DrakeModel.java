package com.drakebound.client;

import com.drakebound.entity.SaddleType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;

/**
 * Modelo do Ender Dragon com as selas de ferro presas ao corpo (acompanham a animação).
 * As selas usam uma zona da textura que é transparente na pele do dragão, por isso só aparecem
 * na passagem desenhada com a textura da sela; aqui só se liga/desliga cada assento.
 */
public class DrakeModel extends EnderDragonModel {
    private final ModelPart saddleFront;
    private final ModelPart saddleBack;

    public DrakeModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        this.saddleFront = body.getChild("saddle_front");
        this.saddleBack = body.getChild("saddle_back");
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
        SaddleType saddle = state instanceof DragonRenderState drake ? drake.saddle : SaddleType.NONE;
        this.saddleFront.visible = saddle != SaddleType.NONE;
        this.saddleBack.visible = saddle == SaddleType.DOUBLE;
    }
}
