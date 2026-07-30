package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class HeadModelRenderer implements SpecialModelRenderer<ProfileComponent> {
    private final SkullBlock.SkullType kind;
    private final SkullBlockEntityModel model;
    @Nullable
    private final Identifier texture;
    private final float animation;

    public HeadModelRenderer(SkullBlock.SkullType kind, SkullBlockEntityModel model, @Nullable Identifier texture, float animation) {
        this.kind = kind;
        this.model = model;
        this.texture = texture;
        this.animation = animation;
    }

    @Nullable
    public ProfileComponent getData(ItemStack itemStack) {
        return itemStack.get(DataComponentTypes.PROFILE);
    }

    public void render(
        @Nullable ProfileComponent profileComponent,
        ModelTransformationMode modelTransformationMode,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider,
        int i,
        int j,
        boolean bl
    ) {
        RenderLayer renderLayer = SkullBlockEntityRenderer.getRenderLayer(this.kind, profileComponent, this.texture);
        SkullBlockEntityRenderer.renderSkull(null, 180.0F, this.animation, matrixStack, vertexConsumerProvider, i, this.model, renderLayer);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(SkullBlock.SkullType kind, Optional<Identifier> textureOverride, float animation) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<HeadModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    SkullBlock.SkullType.CODEC.fieldOf("kind").forGetter(HeadModelRenderer.Unbaked::kind),
                    Identifier.CODEC.optionalFieldOf("texture").forGetter(HeadModelRenderer.Unbaked::textureOverride),
                    Codec.FLOAT.optionalFieldOf("animation", 0.0F).forGetter(HeadModelRenderer.Unbaked::animation)
                )
                .apply(instance, HeadModelRenderer.Unbaked::new)
        );

        public Unbaked(SkullBlock.SkullType kind) {
            this(kind, Optional.empty(), 0.0F);
        }

        @Override
        public MapCodec<HeadModelRenderer.Unbaked> getCodec() {
            return CODEC;
        }

        @Nullable
        @Override
        public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
            SkullBlockEntityModel skullBlockEntityModel = SkullBlockEntityRenderer.getModels(entityModels, this.kind);
            Identifier identifier = this.textureOverride.<Identifier>map(id -> id.withPath(texture -> "textures/entity/" + texture + ".png")).orElse(null);
            return skullBlockEntityModel != null ? new HeadModelRenderer(this.kind, skullBlockEntityModel, identifier, this.animation) : null;
        }
    }
}

