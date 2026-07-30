package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.ContainerComponentModifier;
import net.minecraft.loot.ContainerComponentModifiers;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;

public class ModifyContentsLootFunction extends ConditionalLootFunction {
    public static final MapCodec<ModifyContentsLootFunction> CODEC = RecordCodecBuilder.mapCodec(
        instance -> addConditionsField(instance)
            .and(
                instance.group(
                    ContainerComponentModifiers.MODIFIER_CODEC.fieldOf("component").forGetter(lootFunction -> lootFunction.component),
                    LootFunctionTypes.CODEC.fieldOf("modifier").forGetter(lootFunction -> lootFunction.modifier)
                )
            )
            .apply(instance, ModifyContentsLootFunction::new)
    );
    private final ContainerComponentModifier<?> component;
    private final LootFunction modifier;

    private ModifyContentsLootFunction(List<LootCondition> conditions, ContainerComponentModifier<?> component, LootFunction modifier) {
        super(conditions);
        this.component = component;
        this.modifier = modifier;
    }

    @Override
    public LootFunctionType<ModifyContentsLootFunction> getType() {
        return LootFunctionTypes.MODIFY_CONTENTS;
    }

    @Override
    public ItemStack process(ItemStack stack, LootContext context) {
        if (stack.isEmpty()) {
            return stack;
        }

        this.component.apply(stack, content -> this.modifier.apply(content, context));
        return stack;
    }

    @Override
    public void validate(LootTableReporter reporter) {
        super.validate(reporter);
        this.modifier.validate(reporter.makeChild(".modifier"));
    }
}

