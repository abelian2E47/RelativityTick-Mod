package net.minecraft.client.render.item.model;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.tint.TintSource;
import net.minecraft.client.render.item.tint.TintSourceTypes;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class BasicItemModel implements ItemModel {
    private final BakedModel model;
    private final List<TintSource> tints;

    BasicItemModel(BakedModel model, List<TintSource> tints) {
        this.model = model;
        this.tints = tints;
    }

    @Override
    public void update(
        ItemRenderState state,
        ItemStack stack,
        ItemModelManager resolver,
        ModelTransformationMode transformationMode,
        @Nullable ClientWorld world,
        @Nullable LivingEntity user,
        int seed
    ) {
        ItemRenderState.LayerRenderState layerRenderState = state.newLayer();
        if (stack.hasGlint()) {
            layerRenderState.setGlint(shouldUseSpecialGlint(stack) ? ItemRenderState.Glint.SPECIAL : ItemRenderState.Glint.STANDARD);
        }

        int i = this.tints.size();
        int[] is = layerRenderState.initTints(i);

        for (int j = 0; j < i; j++) {
            is[j] = this.tints.get(j).getTint(stack, world, user);
        }

        RenderLayer renderLayer = RenderLayers.getItemLayer(stack);
        layerRenderState.setModel(this.model, renderLayer);
    }

    private static boolean shouldUseSpecialGlint(ItemStack stack) {
        return stack.isIn(ItemTags.COMPASSES) || stack.isOf(Items.CLOCK);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(Identifier model, List<TintSource> tints) implements ItemModel.Unbaked {
        public static final MapCodec<BasicItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Identifier.CODEC.fieldOf("model").forGetter(BasicItemModel.Unbaked::model),
                    TintSourceTypes.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(BasicItemModel.Unbaked::tints)
                )
                .apply(instance, BasicItemModel.Unbaked::new)
        );

        @Override
        public void resolve(ResolvableModel.Resolver resolver) {
            resolver.resolve(this.model);
        }

        @Override
        public ItemModel bake(ItemModel.BakeContext context) {
            BakedModel bakedModel = context.bake(this.model);
            return new BasicItemModel(bakedModel, this.tints);
        }

        @Override
        public MapCodec<BasicItemModel.Unbaked> getCodec() {
            return CODEC;
        }
    }
}

