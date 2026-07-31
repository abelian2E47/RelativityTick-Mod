package net.minecraft.client.render.block.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;

@Environment(EnvType.CLIENT)
public class BannerBlockModel extends Model {
    public static final int field_55108 = 20;
    public static final int field_55109 = 40;
    public static final String FLAG = "flag";
    private static final String POLE = "pole";
    private static final String BAR = "bar";

    public BannerBlockModel(ModelPart root) {
        super(root, RenderLayer::getEntitySolid);
    }

    public static TexturedModelData getTexturedModelData(boolean standing) {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        if (standing) {
            modelPartData.addChild("pole", ModelPartBuilder.create().uv(44, 0).cuboid(-1.0F, -42.0F, -1.0F, 2.0F, 42.0F, 2.0F), ModelTransform.NONE);
        }

        modelPartData.addChild(
            "bar",
            ModelPartBuilder.create().uv(0, 42).cuboid(-10.0F, standing ? -44.0F : -20.5F, standing ? -1.0F : 9.5F, 20.0F, 2.0F, 2.0F),
            ModelTransform.NONE
        );
        return TexturedModelData.of(modelData, 64, 64);
    }
}

