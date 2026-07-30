package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.datafixer.TypeReferences;

public class RenameEntityNbtKeyFix extends ChoiceFix {
    private final Map<String, String> oldToNewKeyNames;

    public RenameEntityNbtKeyFix(Schema outputSchema, String name, String entityId, Map<String, String> oldToNewKeyNames) {
        super(outputSchema, false, name, TypeReferences.ENTITY, entityId);
        this.oldToNewKeyNames = oldToNewKeyNames;
    }

    public Dynamic<?> fix(Dynamic<?> dynamic) {
        for (Entry<String, String> entry : this.oldToNewKeyNames.entrySet()) {
            dynamic = dynamic.renameField(entry.getKey(), entry.getValue());
        }

        return dynamic;
    }

    @Override
    protected Typed<?> transform(Typed<?> inputTyped) {
        return inputTyped.update(DSL.remainderFinder(), this::fix);
    }
}

