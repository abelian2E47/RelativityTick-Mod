package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.datafixer.TypeReferences;

public class BlockEntityLockToComponentFix extends DataFix {
    public BlockEntityLockToComponentFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "BlockEntityLockToComponentFix",
            this.getInputSchema().getType(TypeReferences.BLOCK_ENTITY),
            typed -> typed.update(DSL.remainderFinder(), dynamic -> {
                Optional<? extends Dynamic<?>> optional = dynamic.get("lock").result();
                if (optional.isEmpty()) {
                    return dynamic;
                }

                Dynamic<?> dynamic2 = InvalidLockComponentPredicateFix.validateLock((Dynamic<?>)optional.get());
                return dynamic2 != null ? dynamic.set("lock", dynamic2) : dynamic.remove("lock");
            })
        );
    }
}

