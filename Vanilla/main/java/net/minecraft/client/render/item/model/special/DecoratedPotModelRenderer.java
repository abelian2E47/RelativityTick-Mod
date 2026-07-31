package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import java.util.Objects;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.Sherds;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.DecoratedPotBlockEntityRenderer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public class DecoratedPotModelRenderer implements SpecialModelRenderer<Sherds> {
    private final DecoratedPotBlockEntityRenderer blockEntityRenderer;

    public DecoratedPotModelRenderer(DecoratedPotBlockEntityRenderer blockEntityRenderer) {
        this.blockEntityRenderer = blockEntityRenderer;
    }

    @Nullable
    public Sherds getData(ItemStack itemStack) {
        return itemStack.get(DataComponentTypes.POT_DECORATIONS);
    }

    public void render(
        @Nullable Sherds sherds,
        ModelTransformationMode modelTransformationMode,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int i,
        int j,
        boolean bl
    ) {
        this.blockEntityRenderer.renderAsItem(matrixStack, vertexConsumerProvider, i, j, Objects.requireNonNullElse(sherds, Sherds.DEFAULT));
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<DecoratedPotModelRenderer.Unbaked> CODEC = MapCodec.unit(new DecoratedPotModelRenderer.Unbaked());

        @Override
        public MapCodec<DecoratedPotModelRenderer.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
            return new DecoratedPotModelRenderer(new DecoratedPotBlockEntityRenderer(entityModels));
        }
    }
}

