package net.minecraft.datafixer.fix;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import javax.annotation.Nullable;

public class LockComponentPredicateFix extends ComponentFix {
    public static final Escaper ESCAPER = Escapers.builder().addEscape('"', "\\\"").addEscape('\\', "\\\\").build();

    public LockComponentPredicateFix(Schema outputSchema) {
        super(outputSchema, "LockComponentPredicateFix", "minecraft:lock");
    }

    @Nullable
    @Override
    protected <T> Dynamic<T> fixComponent(Dynamic<T> dynamic) {
        return fixLock(dynamic);
    }

    @Nullable
    public static <T> Dynamic<T> fixLock(Dynamic<T> dynamic) {
        Optional<String> optional = dynamic.asString().result();
        if (optional.isEmpty()) {
            return null;
        }

        if (optional.get().isEmpty()) {
            return null;
        }

        Dynamic<T> dynamic2 = dynamic.createString("\"" + ESCAPER.escape(optional.get()) + "\"");
        Dynamic<T> dynamic3 = dynamic.emptyMap().set("minecraft:custom_name", dynamic2);
        return dynamic.emptyMap().set("components", dynamic3);
    }
}

