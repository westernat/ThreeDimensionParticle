package org.mesdag.thr_dim_particle.client.impl;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import org.mesdag.thr_dim_particle.TDP;

public class TestHardcodeModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(TDP.asResource("test"), "main");

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, 0.0F, -8.0F, 16.0F, 16.0F, 16.0F, CubeDeformation.NONE), PartPose.ZERO);

        bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-8.0F, 16.0F, 8.0F, 0.0F, -0.7854F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
