package net.minecraft.client.render.block.entity;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class HangingSignBlockEntityRenderer extends AbstractSignBlockEntityRenderer {
    private static final String PLANK = "plank";
    private static final String V_CHAINS = "vChains";
    private static final String NORMAL_CHAINS = "normalChains";
    private static final String CHAIN_L1 = "chainL1";
    private static final String CHAIN_L2 = "chainL2";
    private static final String CHAIN_R1 = "chainR1";
    private static final String CHAIN_R2 = "chainR2";
    private static final String BOARD = "board";
    private static final float MODEL_SCALE = 1.0F;
    private static final float TEXT_SCALE = 0.9F;
    private static final Vec3d TEXT_OFFSET = new Vec3d(0.0, -0.32F, 0.073F);
    private final Map<HangingSignBlockEntityRenderer.Variant, Model> models;

    public HangingSignBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
        Stream<HangingSignBlockEntityRenderer.Variant> stream = WoodType.stream()
            .flatMap(
                woodType -> Arrays.stream(HangingSignBlockEntityRenderer.AttachmentType.values())
                    .map(attachmentType -> new HangingSignBlockEntityRenderer.Variant(woodType, attachmentType))
            );
        this.models = stream.collect(
            ImmutableMap.toImmutableMap(
                variant -> (HangingSignBlockEntityRenderer.Variant)variant,
                variant -> createModel(context.getLoadedEntityModels(), variant.woodType, variant.attachmentType)
            )
        );
    }

    public static Model createModel(LoadedEntityModels models, WoodType woodType, HangingSignBlockEntityRenderer.AttachmentType attachmentType) {
        return new Model.SinglePartModel(models.getModelPart(EntityModelLayers.createHangingSign(woodType, attachmentType)), RenderLayer::getEntityCutoutNoCull);
    }

    @Override
    protected float getSignScale() {
        return 1.0F;
    }

    @Override
    protected float getTextScale() {
        return 0.9F;
    }

    private static void setAngles(MatrixStack matrices, float blockRotationDegrees) {
        matrices.translate(0.5, 0.9375, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(blockRotationDegrees));
        matrices.translate(0.0F, -0.3125F, 0.0F);
    }

    @Override
    protected void applyTransforms(MatrixStack matrices, float blockRotationDegrees, BlockState state) {
        setAngles(matrices, blockRotationDegrees);
    }

    @Override
    protected Model getModel(BlockState state, WoodType woodType) {
        HangingSignBlockEntityRenderer.AttachmentType attachmentType = HangingSignBlockEntityRenderer.AttachmentType.from(state);
        return this.models.get(new HangingSignBlockEntityRenderer.Variant(woodType, attachmentType));
    }

    @Override
    protected SpriteIdentifier getTextureId(WoodType woodType) {
        return TexturedRenderLayers.getHangingSignTextureId(woodType);
    }

    @Override
    protected Vec3d getTextOffset() {
        return TEXT_OFFSET;
    }

    public static void renderAsItem(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, Model model, SpriteIdentifier texture) {
        matrices.push();
        setAngles(matrices, 0.0F);
        matrices.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer vertexConsumer = texture.getVertexConsumer(vertexConsumers, model::getLayer);
        model.render(matrices, vertexConsumer, light, overlay);
        matrices.pop();
    }

    public static TexturedModelData getTexturedModelData(HangingSignBlockEntityRenderer.AttachmentType attachmentType) {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild("board", ModelPartBuilder.create().uv(0, 12).cuboid(-7.0F, 0.0F, -1.0F, 14.0F, 10.0F, 2.0F), ModelTransform.NONE);
        if (attachmentType == HangingSignBlockEntityRenderer.AttachmentType.WALL) {
            modelPartData.addChild("plank", ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -6.0F, -2.0F, 16.0F, 2.0F, 4.0F), ModelTransform.NONE);
        }

        if (attachmentType == HangingSignBlockEntityRenderer.AttachmentType.WALL || attachmentType == HangingSignBlockEntityRenderer.AttachmentType.CEILING) {
            ModelPartData modelPartData2 = modelPartData.addChild("normalChains", ModelPartBuilder.create(), ModelTransform.NONE);
            modelPartData2.addChild(
                "chainL1",
                ModelPartBuilder.create().uv(0, 6).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
                ModelTransform.of(-5.0F, -6.0F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
            );
            modelPartData2.addChild(
                "chainL2",
                ModelPartBuilder.create().uv(6, 6).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
                ModelTransform.of(-5.0F, -6.0F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
            );
            modelPartData2.addChild(
                "chainR1",
                ModelPartBuilder.create().uv(0, 6).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
                ModelTransform.of(5.0F, -6.0F, 0.0F, 0.0F, (float) (-Math.PI / 4), 0.0F)
            );
            modelPartData2.addChild(
                "chainR2",
                ModelPartBuilder.create().uv(6, 6).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 6.0F, 0.0F),
                ModelTransform.of(5.0F, -6.0F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F)
            );
        }

        if (attachmentType == HangingSignBlockEntityRenderer.AttachmentType.CEILING_MIDDLE) {
            modelPartData.addChild("vChains", ModelPartBuilder.create().uv(14, 6).cuboid(-6.0F, -6.0F, 0.0F, 12.0F, 6.0F, 0.0F), ModelTransform.NONE);
        }

        return TexturedModelData.of(modelData, 64, 32);
    }

    @Environment(EnvType.CLIENT)
    public enum AttachmentType implements StringIdentifiable {
        WALL("wall"),
        CEILING("ceiling"),
        CEILING_MIDDLE("ceiling_middle");

        private final String id;

        AttachmentType(final String id) {
            this.id = id;
        }

        public static HangingSignBlockEntityRenderer.AttachmentType from(BlockState state) {
            if (state.getBlock() instanceof HangingSignBlock) {
                return state.get(Properties.ATTACHED) ? CEILING_MIDDLE : CEILING;
            } else {
                return WALL;
            }
        }

        @Override
        public String asString() {
            return this.id;
        }
    }

    @Environment(EnvType.CLIENT)
    public record Variant(WoodType woodType, HangingSignBlockEntityRenderer.AttachmentType attachmentType) {
    }
}

