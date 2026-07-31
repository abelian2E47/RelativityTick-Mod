package net.minecraft.client.render.item.model;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.property.bool.BooleanProperties;
import net.minecraft.client.render.item.property.bool.BooleanProperty;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public class ConditionItemModel implements ItemModel {
    private final BooleanProperty property;
    private final ItemModel onTrue;
    private final ItemModel onFalse;

    public ConditionItemModel(BooleanProperty property, ItemModel onTrue, ItemModel onFalse) {
        this.property = property;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
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
        (this.property.getValue(stack, world, user, seed, transformationMode) ? this.onTrue : this.onFalse)
            .update(state, stack, resolver, transformationMode, world, user, seed);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(BooleanProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) implements ItemModel.Unbaked {
        public static final MapCodec<ConditionItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BooleanProperties.CODEC.forGetter(ConditionItemModel.Unbaked::property),
                    ItemModelTypes.CODEC.fieldOf("on_true").forGetter(ConditionItemModel.Unbaked::onTrue),
                    ItemModelTypes.CODEC.fieldOf("on_false").forGetter(ConditionItemModel.Unbaked::onFalse)
                )
                .apply(instance, ConditionItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<ConditionItemModel.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakeContext context) {
            return new ConditionItemModel(this.property, this.onTrue.bake(context), this.onFalse.bake(context));
        }

        @Override
        public void resolve(ResolvableModel.Resolver resolver) {
            this.onTrue.resolve(resolver);
            this.onFalse.resolve(resolver);
        }
    }
}

