package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;

public class InvalidLockComponentPredicateFix extends ComponentFix {
    private static final Optional<String> DOUBLE_QUOTES = Optional.of("\"\"");

    public InvalidLockComponentPredicateFix(Schema outputSchema) {
        super(outputSchema, "InvalidLockComponentPredicateFix", "minecraft:lock");
    }

    @Nullable
    @Override
    protected <T> Dynamic<T> fixComponent(Dynamic<T> dynamic) {
        return validateLock(dynamic);
    }

    @Nullable
    public static <T> Dynamic<T> validateLock(Dynamic<T> dynamic) {
        return isLockInvalid(dynamic) ? null : dynamic;
    }

    private static <T> boolean isLockInvalid(Dynamic<T> dynamic) {
        return hasMatchingKey(
            dynamic,
            "components",
            componentsDynamic -> hasMatchingKey(
                componentsDynamic, "minecraft:custom_name", customNameDynamic -> customNameDynamic.asString().result().equals(DOUBLE_QUOTES)
            )
        );
    }

    private static <T> boolean hasMatchingKey(Dynamic<T> dynamic, String key, Predicate<Dynamic<T>> predicate) {
        Optional<Map<Dynamic<T>, Dynamic<T>>> optional = dynamic.getMapValues().result();
        return !optional.isEmpty() && optional.get().size() == 1 ? dynamic.get(key).result().filter(predicate).isPresent() : false;
    }
}

