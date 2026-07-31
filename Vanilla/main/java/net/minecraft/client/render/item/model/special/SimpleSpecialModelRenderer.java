package net.minecraft.client.render.item.model.special;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public interface SimpleSpecialModelRenderer extends SpecialModelRenderer<Void> {
    @Nullable
    default Void getData(ItemStack itemStack) {
        return null;
    }

    default void render(
        @Nullable Void void_,
        ModelTransformationMode modelTransformationMode,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int i,
        int j,
        boolean bl
    ) {
        this.render(modelTransformationMode, matrixStack, vertexConsumerProvider, i, j, bl);
    }

    void render(
        ModelTransformationMode modelTransformationMode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, boolean glint
    );
}

