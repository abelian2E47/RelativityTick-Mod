package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import java.util.stream.Collectors;
import net.minecraft.datafixer.TypeReferences;

public class OptionsKeyTranslationFix extends DataFix {
    public OptionsKeyTranslationFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "OptionsKeyTranslationFix",
            this.getInputSchema().getType(TypeReferences.OPTIONS),
            optionsTyped -> optionsTyped.update(
                DSL.remainderFinder(),
                optionsDynamic -> optionsDynamic.getMapValues().map(optionsMap -> optionsDynamic.createMap(optionsMap.entrySet().stream().map(entry -> {
                    if (entry.getKey().asString("").startsWith("key_")) {
                        String string = entry.getValue().asString("");
                        if (!string.startsWith("key.mouse") && !string.startsWith("scancode.")) {
                            return Pair.of(entry.getKey(), optionsDynamic.createString("key.keyboard." + string.substring("key.".length())));
                        }
                    }

                    return Pair.of(entry.getKey(), entry.getValue());
                }).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)))).result().orElse(optionsDynamic)
            )
        );
    }
}

