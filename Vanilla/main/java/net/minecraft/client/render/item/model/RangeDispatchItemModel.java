package net.minecraft.client.render.item.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.property.numeric.NumericProperties;
import net.minecraft.client.render.item.property.numeric.NumericProperty;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public class RangeDispatchItemModel implements ItemModel {
    private static final int field_55353 = 16;
    private final NumericProperty property;
    private final float scale;
    private final float[] thresholds;
    private final ItemModel[] models;
    private final ItemModel fallback;

    RangeDispatchItemModel(NumericProperty property, float scale, float[] thresholds, ItemModel[] models, ItemModel fallback) {
        this.property = property;
        this.thresholds = thresholds;
        this.models = models;
        this.fallback = fallback;
        this.scale = scale;
    }

    private static int getIndex(float[] thresholds, float value) {
        if (thresholds.length < 16) {
            for (int i = 0; i < thresholds.length; i++) {
                if (thresholds[i] > value) {
                    return i - 1;
                }
            }

            return thresholds.length - 1;
        } else {
            int j = Arrays.binarySearch(thresholds, value);
            if (j < 0) {
                int k = ~j;
                return k - 1;
            } else {
                return j;
            }
        }
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
        float f = this.property.getValue(stack, world, user, seed) * this.scale;
        ItemModel itemModel;
        if (Float.isNaN(f)) {
            itemModel = this.fallback;
        } else {
            int i = getIndex(this.thresholds, f);
            itemModel = i == -1 ? this.fallback : this.models[i];
        }

        itemModel.update(state, stack, resolver, transformationMode, world, user, seed);
    }

    @Environment(EnvType.CLIENT)
    public record Entry(float threshold, ItemModel.Unbaked model) {
        public static final Codec<RangeDispatchItemModel.Entry> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.FLOAT.fieldOf("threshold").forGetter(RangeDispatchItemModel.Entry::threshold),
                    ItemModelTypes.CODEC.fieldOf("model").forGetter(RangeDispatchItemModel.Entry::model)
                )
                .apply(instance, RangeDispatchItemModel.Entry::new)
        );
        public static final Comparator<RangeDispatchItemModel.Entry> COMPARATOR = Comparator.comparingDouble(RangeDispatchItemModel.Entry::threshold);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(NumericProperty property, float scale, List<RangeDispatchItemModel.Entry> entries, Optional<ItemModel.Unbaked> fallback)
        implements ItemModel.Unbaked {
        public static final MapCodec<RangeDispatchItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    NumericProperties.CODEC.forGetter(RangeDispatchItemModel.Unbaked::property),
                    Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(RangeDispatchItemModel.Unbaked::scale),
                    RangeDispatchItemModel.Entry.CODEC.listOf().fieldOf("entries").forGetter(RangeDispatchItemModel.Unbaked::entries),
                    ItemModelTypes.CODEC.optionalFieldOf("fallback").forGetter(RangeDispatchItemModel.Unbaked::fallback)
                )
                .apply(instance, RangeDispatchItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<RangeDispatchItemModel.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakeContext context) {
            float[] fs = new float[this.entries.size()];
            ItemModel[] itemModels = new ItemModel[this.entries.size()];
            List<RangeDispatchItemModel.Entry> list = new ArrayList<>(this.entries);
            list.sort(RangeDispatchItemModel.Entry.COMPARATOR);

            for (int i = 0; i < list.size(); i++) {
                RangeDispatchItemModel.Entry entry = list.get(i);
                fs[i] = entry.threshold;
                itemModels[i] = entry.model.bake(context);
            }

            ItemModel itemModel = this.fallback.<ItemModel>map(model -> model.bake(context)).orElse(context.missingItemModel());
            return new RangeDispatchItemModel(this.property, this.scale, fs, itemModels, itemModel);
        }

        @Override
        public void resolve(ResolvableModel.Resolver resolver) {
            this.fallback.ifPresent(model -> model.resolve(resolver));
            this.entries.forEach(entry -> entry.model.resolve(resolver));
        }
    }
}

