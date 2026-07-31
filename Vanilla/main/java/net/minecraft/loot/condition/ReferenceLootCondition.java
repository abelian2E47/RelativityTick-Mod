package net.minecraft.loot.condition;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import org.slf4j.Logger;

public record ReferenceLootCondition(RegistryKey<LootCondition> id) implements LootCondition {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<ReferenceLootCondition> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(RegistryKey.createCodec(RegistryKeys.PREDICATE).fieldOf("name").forGetter(ReferenceLootCondition::id))
            .apply(instance, ReferenceLootCondition::new)
    );

    @Override
    public LootConditionType getType() {
        return LootConditionTypes.REFERENCE;
    }

    @Override
    public void validate(LootTableReporter reporter) {
        if (!reporter.canUseReferences()) {
            reporter.report("Uses reference to " + this.id.getValue() + ", but references are not allowed");
        } else if (reporter.isInStack(this.id)) {
            reporter.report("Condition " + this.id.getValue() + " is recursively called");
        } else {
            LootCondition.super.validate(reporter);
            reporter.getDataLookup()
                .getOptionalEntry(this.id)
                .ifPresentOrElse(
                    entry -> entry.value().validate(reporter.makeChild(".{" + this.id.getValue() + "}", this.id)),
                    () -> reporter.report("Unknown condition table called " + this.id.getValue())
                );
        }
    }

    public boolean test(LootContext lootContext) {
        LootCondition lootCondition = lootContext.getLookup().getOptionalEntry(this.id).map(RegistryEntry.Reference::value).orElse(null);
        if (lootCondition == null) {
            LOGGER.warn("Tried using unknown condition table called {}", this.id.getValue());
            return false;
        }

        LootContext.Entry<?> entry = LootContext.predicate(lootCondition);
        if (lootContext.markActive(entry)) {
            try {
                return lootCondition.test(lootContext);
            } finally {
                lootContext.markInactive(entry);
            }
        } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return false;
        }
    }

    public static LootCondition.Builder builder(RegistryKey<LootCondition> key) {
        return () -> new ReferenceLootCondition(key);
    }
}

