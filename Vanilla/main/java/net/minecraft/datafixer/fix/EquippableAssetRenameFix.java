package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.datafixer.TypeReferences;

public class EquippableAssetRenameFix extends DataFix {
    public EquippableAssetRenameFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(TypeReferences.DATA_COMPONENTS);
        OpticFinder<?> opticFinder = type.findField("minecraft:equippable");
        return this.fixTypeEverywhereTyped(
            "equippable asset rename fix",
            type,
            typed -> typed.updateTyped(opticFinder, typedx -> typedx.update(DSL.remainderFinder(), dynamic -> dynamic.renameField("model", "asset_id")))
        );
    }
}

