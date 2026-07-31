package net.minecraft.client.render.block.entity;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.OrderedText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public abstract class AbstractSignBlockEntityRenderer implements BlockEntityRenderer<SignBlockEntity> {
    private static final int GLOWING_BLACK_TEXT_COLOR = -988212;
    private static final int MAX_COLORED_TEXT_OUTLINE_RENDER_DISTANCE = MathHelper.square(16);
    private final TextRenderer textRenderer;

    public AbstractSignBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.textRenderer = context.getTextRenderer();
    }

    protected abstract Model getModel(BlockState state, WoodType woodType);

    protected abstract SpriteIdentifier getTextureId(WoodType woodType);

    protected abstract float getSignScale();

    protected abstract float getTextScale();

    protected abstract Vec3d getTextOffset();

    protected abstract void applyTransforms(MatrixStack matrices, float blockRotationDegrees, BlockState state);

    public void render(SignBlockEntity signBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
        BlockState blockState = signBlockEntity.getCachedState();
        AbstractSignBlock abstractSignBlock = (AbstractSignBlock)blockState.getBlock();
        Model model = this.getModel(blockState, abstractSignBlock.getWoodType());
        this.render(signBlockEntity, matrixStack, vertexConsumerProvider, i, j, blockState, abstractSignBlock, abstractSignBlock.getWoodType(), model);
    }

    private void render(
        SignBlockEntity blockEntity,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        int overlay,
        BlockState state,
        AbstractSignBlock block,
        WoodType woodType,
        Model model
    ) {
        matrices.push();
        this.applyTransforms(matrices, -block.getRotationDegrees(state), state);
        this.renderSign(matrices, vertexConsumers, light, overlay, woodType, model);
        this.renderText(
            blockEntity.getPos(),
            blockEntity.getFrontText(),
            matrices,
            vertexConsumers,
            light,
            blockEntity.getTextLineHeight(),
            blockEntity.getMaxTextWidth(),
            true
        );
        this.renderText(
            blockEntity.getPos(),
            blockEntity.getBackText(),
            matrices,
            vertexConsumers,
            light,
            blockEntity.getTextLineHeight(),
            blockEntity.getMaxTextWidth(),
            false
        );
        matrices.pop();
    }

    protected void renderSign(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, WoodType woodType, Model model) {
        matrices.push();
        float f = this.getSignScale();
        matrices.scale(f, -f, -f);
        SpriteIdentifier spriteIdentifier = this.getTextureId(woodType);
        VertexConsumer vertexConsumer = spriteIdentifier.getVertexConsumer(vertexConsumers, model::getLayer);
        model.render(matrices, vertexConsumer, light, overlay);
        matrices.pop();
    }

    private void renderText(
        BlockPos pos,
        SignText text,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        int textLineHeight,
        int maxTextWidth,
        boolean front
    ) {
        matrices.push();
        this.applyTextTransforms(matrices, front, this.getTextOffset());
        int i = getTextColor(text);
        int j = 4 * textLineHeight / 2;
        OrderedText[] orderedTexts = text.getOrderedMessages(MinecraftClient.getInstance().shouldFilterText(), textx -> {
            List<OrderedText> list = this.textRenderer.wrapLines(textx, maxTextWidth);
            return list.isEmpty() ? OrderedText.EMPTY : list.get(0);
        });
        int k;
        boolean bl;
        int l;
        if (text.isGlowing()) {
            k = text.getColor().getSignColor();
            bl = shouldRenderTextOutline(pos, k);
            l = 15728880;
        } else {
            k = i;
            bl = false;
            l = light;
        }

        for (int o = 0; o < 4; o++) {
            OrderedText orderedText = orderedTexts[o];
            float f = -this.textRenderer.getWidth(orderedText) / 2;
            if (bl) {
                this.textRenderer.drawWithOutline(orderedText, f, o * textLineHeight - j, k, i, matrices.peek().getPositionMatrix(), vertexConsumers, l);
            } else {
                this.textRenderer
                    .draw(
                        orderedText,
                        f,
                        o * textLineHeight - j,
                        k,
                        false,
                        matrices.peek().getPositionMatrix(),
                        vertexConsumers,
                        TextRenderer.TextLayerType.POLYGON_OFFSET,
                        0,
                        l
                    );
            }
        }

        matrices.pop();
    }

    private void applyTextTransforms(MatrixStack matrices, boolean front, Vec3d textOffset) {
        if (!front) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        }

        float f = 0.015625F * this.getTextScale();
        matrices.translate(textOffset);
        matrices.scale(f, -f, f);
    }

    private static boolean shouldRenderTextOutline(BlockPos pos, int color) {
        if (color == DyeColor.BLACK.getSignColor()) {
            return true;
        }

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
        if (clientPlayerEntity != null && minecraftClient.options.getPerspective().isFirstPerson() && clientPlayerEntity.isUsingSpyglass()) {
            return true;
        }

        Entity entity = minecraftClient.getCameraEntity();
        return entity != null && entity.squaredDistanceTo(Vec3d.ofCenter(pos)) < MAX_COLORED_TEXT_OUTLINE_RENDER_DISTANCE;
    }

    public static int getTextColor(SignText text) {
        int i = text.getColor().getSignColor();
        if (i == DyeColor.BLACK.getSignColor() && text.isGlowing()) {
            return -988212;
        }

        double d = 0.4;
        int j = (int)(ColorHelper.getRed(i) * 0.4);
        int k = (int)(ColorHelper.getGreen(i) * 0.4);
        int l = (int)(ColorHelper.getBlue(i) * 0.4);
        return ColorHelper.getArgb(0, j, k, l);
    }
}

