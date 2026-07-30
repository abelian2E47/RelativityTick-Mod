package net.minecraft.client.render.item.property.select;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.model.SelectItemModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

@Environment(EnvType.CLIENT)
public interface SelectProperty<T> {
    @Nullable
    T getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode);

    SelectProperty.Type<? extends SelectProperty<T>, T> getType();

    @Environment(EnvType.CLIENT)
    record Type<P extends SelectProperty<T>, T>(MapCodec<SelectItemModel.UnbakedSwitch<P, T>> switchCodec) {
        public static <P extends SelectProperty<T>, T> SelectProperty.Type<P, T> create(MapCodec<P> propertyCodec, Codec<T> valueCodec) {
            Codec<List<SelectItemModel.SwitchCase<T>>> codec = SelectItemModel.SwitchCase.createCodec(valueCodec)
                .listOf()
                .validate(
                    cases -> {
                        if (cases.isEmpty()) {
                            return DataResult.error(() -> "Empty case list");
                        }

                        Multiset<T> multiset = HashMultiset.create();

                        for (SelectItemModel.SwitchCase<T> switchCase : cases) {
                            multiset.addAll(switchCase.values());
                        }

                        return multiset.size() != multiset.entrySet().size()
                            ? DataResult.error(
                                () -> "Duplicate case conditions: "
                                    + multiset.entrySet()
                                        .stream()
                                        .filter(entry -> entry.getCount() > 1)
                                        .map(entry -> entry.getElement().toString())
                                        .collect(Collectors.joining(", "))
                            )
                            : DataResult.success(cases);
                    }
                );
            MapCodec<SelectItemModel.UnbakedSwitch<P, T>> mapCodec = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        propertyCodec.forGetter(SelectItemModel.UnbakedSwitch::property),
                        codec.fieldOf("cases").forGetter(SelectItemModel.UnbakedSwitch::cases)
                    )
                    .apply(instance, SelectItemModel.UnbakedSwitch::new)
            );
            return new SelectProperty.Type<>(mapCodec);
        }
    }
}

