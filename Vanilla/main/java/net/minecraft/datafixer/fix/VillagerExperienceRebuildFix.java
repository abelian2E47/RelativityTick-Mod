package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.math.MathHelper;

public class VillagerExperienceRebuildFix extends DataFix {
    private static final int TRADES_PER_LEVEL = 2;
    private static final int[] LEVEL_TO_EXPERIENCE = new int[]{0, 10, 50, 100, 150};

    public static int levelToExperience(int level) {
        return LEVEL_TO_EXPERIENCE[MathHelper.clamp(level - 1, 0, LEVEL_TO_EXPERIENCE.length - 1)];
    }

    public VillagerExperienceRebuildFix(Schema outputSchema, boolean changesTyped) {
        super(outputSchema, changesTyped);
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getChoiceType(TypeReferences.ENTITY, "minecraft:villager");
        OpticFinder<?> opticFinder = DSL.namedChoice("minecraft:villager", type);
        OpticFinder<?> opticFinder2 = type.findField("Offers");
        Type<?> type2 = opticFinder2.type();
        OpticFinder<?> opticFinder3 = type2.findField("Recipes");
        ListType<?> listType = (ListType<?>)opticFinder3.type();
        OpticFinder<?> opticFinder4 = listType.getElement().finder();
        return this.fixTypeEverywhereTyped(
            "Villager level and xp rebuild",
            this.getInputSchema().getType(TypeReferences.ENTITY),
            entityTyped -> entityTyped.updateTyped(
                opticFinder,
                type,
                villagerTyped -> {
                    Dynamic<?> dynamic = villagerTyped.get(DSL.remainderFinder());
                    int i = dynamic.get("VillagerData").get("level").asInt(0);
                    Typed<?> typed = villagerTyped;
                    if (i == 0 || i == 1) {
                        int j = villagerTyped.getOptionalTyped(opticFinder2)
                            .flatMap(offersTyped -> offersTyped.getOptionalTyped(opticFinder3))
                            .map(recipesTyped -> recipesTyped.getAllTyped(opticFinder4).size())
                            .orElse(0);
                        i = MathHelper.clamp(j / 2, 1, 5);
                        if (i > 1) {
                            typed = fixLevel(typed, i);
                        }
                    }

                    Optional<Number> optional = dynamic.get("Xp").asNumber().result();
                    if (optional.isEmpty()) {
                        typed = fixExperience(typed, i);
                    }

                    return typed;
                }
            )
        );
    }

    private static Typed<?> fixLevel(Typed<?> villagerTyped, int level) {
        return villagerTyped.update(
            DSL.remainderFinder(),
            villager -> villager.update("VillagerData", villagerDataDynamic -> villagerDataDynamic.set("level", villagerDataDynamic.createInt(level)))
        );
    }

    private static Typed<?> fixExperience(Typed<?> villagerTyped, int level) {
        int i = levelToExperience(level);
        return villagerTyped.update(DSL.remainderFinder(), villager -> villager.set("Xp", villager.createInt(i)));
    }
}

