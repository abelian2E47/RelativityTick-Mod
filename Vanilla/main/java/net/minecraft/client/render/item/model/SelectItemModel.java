package net.minecraft.client.render.item.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.property.select.SelectProperties;
import net.minecraft.client.render.item.property.select.SelectProperty;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public class SelectItemModel<T> implements ItemModel {
    private final SelectProperty<T> property;
    private final Object2ObjectMap<T, ItemModel> cases;

    public SelectItemModel(SelectProperty<T> property, Object2ObjectMap<T, ItemModel> cases) {
        this.property = property;
        this.cases = cases;
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
        T object = this.property.getValue(stack, world, user, seed, transformationMode);
        ItemModel itemModel = this.cases.get(object);
        if (itemModel != null) {
            itemModel.update(state, stack, resolver, transformationMode, world, user, seed);
        }
    }

    @Environment(EnvType.CLIENT)
    public record SwitchCase<T>(List<T> values, ItemModel.Unbaked model) {
        public static <T> Codec<SelectItemModel.SwitchCase<T>> createCodec(Codec<T> conditionCodec) {
            return RecordCodecBuilder.create(
                instance -> instance.group(
                        Codecs.nonEmptyList(Codecs.listOrSingle(conditionCodec)).fieldOf("when").forGetter(SelectItemModel.SwitchCase::values),
                        ItemModelTypes.CODEC.fieldOf("model").forGetter(SelectItemModel.SwitchCase::model)
                    )
                    .apply(instance, SelectItemModel.SwitchCase::new)
            );
        }
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(SelectItemModel.UnbakedSwitch<?, ?> unbakedSwitch, Optional<ItemModel.Unbaked> fallback) implements ItemModel.Unbaked {
        public static final MapCodec<SelectItemModel.Unbaked> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    SelectItemModel.UnbakedSwitch.CODEC.forGetter(SelectItemModel.Unbaked::unbakedSwitch),
                    ItemModelTypes.CODEC.optionalFieldOf("fallback").forGetter(SelectItemModel.Unbaked::fallback)
                )
                .apply(instance, SelectItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<SelectItemModel.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakeContext context) {
            ItemModel itemModel = this.fallback.<ItemModel>map(model -> model.bake(context)).orElse(context.missingItemModel());
            return this.unbakedSwitch.bake(context, itemModel);
        }

        @Override
        public void resolve(ResolvableModel.Resolver resolver) {
            this.unbakedSwitch.resolveCases(resolver);
            this.fallback.ifPresent(model -> model.resolve(resolver));
        }
    }

    @Environment(EnvType.CLIENT)
    public record UnbakedSwitch<P extends SelectProperty<T>, T>(P property, List<SelectItemModel.SwitchCase<T>> cases) {
        public static final MapCodec<SelectItemModel.UnbakedSwitch<?, ?>> CODEC = SelectProperties.CODEC
            .dispatchMap("property", unbakedSwitch -> unbakedSwitch.property().getType(), SelectProperty.Type::switchCodec);

        public ItemModel bake(ItemModel.BakeContext context, ItemModel fallback) {
            Object2ObjectMap<T, ItemModel> object2ObjectMap = new Object2ObjectOpenHashMap<>();

            for (SelectItemModel.SwitchCase<T> switchCase : this.cases) {
                ItemModel.Unbaked unbaked = switchCase.model;
                ItemModel itemModel = unbaked.bake(context);

                for (T object : switchCase.values) {
                    object2ObjectMap.put(object, itemModel);
                }
            }

            object2ObjectMap.defaultReturnValue(fallback);
            return new SelectItemModel<>(this.property, object2ObjectMap);
        }

        public void resolveCases(ResolvableModel.Resolver resolver) {
            for (SelectItemModel.SwitchCase<?> switchCase : this.cases) {
                switchCase.model.resolve(resolver);
            }
        }
    }
}

