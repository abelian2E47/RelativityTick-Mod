package net.minecraft.datafixer.fix;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.math.WordPackedArray;

public class LeavesFix extends DataFix {
    private static final int field_29886 = 128;
    private static final int field_29887 = 64;
    private static final int field_29888 = 32;
    private static final int field_29889 = 16;
    private static final int field_29890 = 8;
    private static final int field_29891 = 4;
    private static final int field_29892 = 2;
    private static final int field_29893 = 1;
    private static final int[][] AXIAL_OFFSETS = new int[][]{{-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}};
    private static final int field_29894 = 7;
    private static final int field_29895 = 12;
    private static final int field_29896 = 4096;
    static final Object2IntMap<String> LEAVES_MAP = DataFixUtils.make(new Object2IntOpenHashMap<>(), map -> {
        map.put("minecraft:acacia_leaves", 0);
        map.put("minecraft:birch_leaves", 1);
        map.put("minecraft:dark_oak_leaves", 2);
        map.put("minecraft:jungle_leaves", 3);
        map.put("minecraft:oak_leaves", 4);
        map.put("minecraft:spruce_leaves", 5);
    });
    static final Set<String> LOGS_MAP = ImmutableSet.of(
        "minecraft:acacia_bark",
        "minecraft:birch_bark",
        "minecraft:dark_oak_bark",
        "minecraft:jungle_bark",
        "minecraft:oak_bark",
        "minecraft:spruce_bark",
        "minecraft:acacia_log",
        "minecraft:birch_log",
        "minecraft:dark_oak_log",
        "minecraft:jungle_log",
        "minecraft:oak_log",
        "minecraft:spruce_log",
        "minecraft:stripped_acacia_log",
        "minecraft:stripped_birch_log",
        "minecraft:stripped_dark_oak_log",
        "minecraft:stripped_jungle_log",
        "minecraft:stripped_oak_log",
        "minecraft:stripped_spruce_log"
    );

    public LeavesFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(TypeReferences.CHUNK);
        OpticFinder<?> opticFinder = type.findField("Level");
        OpticFinder<?> opticFinder2 = opticFinder.type().findField("Sections");
        Type<?> type2 = opticFinder2.type();
        if (!(type2 instanceof ListType)) {
            throw new IllegalStateException("Expecting sections to be a list.");
        }

        Type<?> type3 = ((ListType)type2).getElement();
        OpticFinder<?> opticFinder3 = DSL.typeFinder(type3);
        return this.fixTypeEverywhereTyped(
            "Leaves fix",
            type,
            chunkTyped -> chunkTyped.updateTyped(
                opticFinder,
                levelTyped -> {
                    int[] is = new int[]{0};
                    Typed<?> typed = levelTyped.updateTyped(
                        opticFinder2,
                        sectionsTyped -> {
                            Int2ObjectMap<LeavesFix.LeavesLogFixer> int2ObjectMap = new Int2ObjectOpenHashMap<>(
                                sectionsTyped.getAllTyped(opticFinder3)
                                    .stream()
                                    .map(sectionTyped -> new LeavesFix.LeavesLogFixer((Typed<?>)sectionTyped, this.getInputSchema()))
                                    .collect(Collectors.toMap(LeavesFix.ListFixer::getY, fixer -> (LeavesFix.LeavesLogFixer)fixer))
                            );
                            if (int2ObjectMap.values().stream().allMatch(LeavesFix.ListFixer::isFixed)) {
                                return sectionsTyped;
                            }

                            List<IntSet> list = Lists.newArrayList();

                            for (int i = 0; i < 7; i++) {
                                list.add(new IntOpenHashSet());
                            }

                            for (LeavesFix.LeavesLogFixer leavesLogFixer : int2ObjectMap.values()) {
                                if (!leavesLogFixer.isFixed()) {
                                    for (int j = 0; j < 4096; j++) {
                                        int k = leavesLogFixer.blockStateAt(j);
                                        if (leavesLogFixer.isLog(k)) {
                                            list.get(0).add(leavesLogFixer.getY() << 12 | j);
                                        } else if (leavesLogFixer.isLeaf(k)) {
                                            int l = this.getX(j);
                                            int m = this.getZ(j);
                                            is[0] |= getBoundaryClassBit(l == 0, l == 15, m == 0, m == 15);
                                        }
                                    }
                                }
                            }

                            for (int n = 1; n < 7; n++) {
                                IntSet intSet = list.get(n - 1);
                                IntSet intSet2 = list.get(n);
                                IntIterator intIterator = intSet.iterator();

                                while (intIterator.hasNext()) {
                                    int o = intIterator.nextInt();
                                    int p = this.getX(o);
                                    int q = this.getY(o);
                                    int r = this.getZ(o);

                                    for (int[] js : AXIAL_OFFSETS) {
                                        int s = p + js[0];
                                        int t = q + js[1];
                                        int u = r + js[2];
                                        if (s >= 0 && s <= 15 && u >= 0 && u <= 15 && t >= 0 && t <= 255) {
                                            LeavesFix.LeavesLogFixer leavesLogFixer2 = int2ObjectMap.get(t >> 4);
                                            if (leavesLogFixer2 != null && !leavesLogFixer2.isFixed()) {
                                                int v = packLocalPos(s, t & 15, u);
                                                int w = leavesLogFixer2.blockStateAt(v);
                                                if (leavesLogFixer2.isLeaf(w)) {
                                                    int x = leavesLogFixer2.getDistanceToLog(w);
                                                    if (x > n) {
                                                        leavesLogFixer2.computeLeafStates(v, w, n);
                                                        intSet2.add(packLocalPos(s, t, u));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            return sectionsTyped.updateTyped(
                                opticFinder3,
                                sectionDynamic -> int2ObjectMap.get(sectionDynamic.get(DSL.remainderFinder()).get("Y").asInt(0)).finalizeFix(sectionDynamic)
                            );
                        }
                    );
                    if (is[0] != 0) {
                        typed = typed.update(DSL.remainderFinder(), dynamic -> {
                            Dynamic<?> dynamic2 = DataFixUtils.orElse(dynamic.get("UpgradeData").result(), dynamic.emptyMap());
                            return dynamic.set("UpgradeData", dynamic2.set("Sides", dynamic.createByte((byte)(dynamic2.get("Sides").asByte((byte)0) | is[0]))));
                        });
                    }

                    return typed;
                }
            )
        );
    }

    public static int packLocalPos(int localX, int localY, int localZ) {
        return localY << 8 | localZ << 4 | localX;
    }

    private int getX(int packedLocalPos) {
        return packedLocalPos & 15;
    }

    private int getY(int packedLocalPos) {
        return packedLocalPos >> 8 & 0xFF;
    }

    private int getZ(int packedLocalPos) {
        return packedLocalPos >> 4 & 15;
    }

    public static int getBoundaryClassBit(boolean westernmost, boolean easternmost, boolean northernmost, boolean southernmost) {
        int i = 0;
        if (northernmost) {
            if (easternmost) {
                i |= 2;
            } else if (westernmost) {
                i |= 128;
            } else {
                i |= 1;
            }
        } else if (southernmost) {
            if (westernmost) {
                i |= 32;
            } else if (easternmost) {
                i |= 8;
            } else {
                i |= 16;
            }
        } else if (easternmost) {
            i |= 4;
        } else if (westernmost) {
            i |= 64;
        }

        return i;
    }

    public static final class LeavesLogFixer extends LeavesFix.ListFixer {
        private static final String PERSISTENT = "persistent";
        private static final String DECAYABLE = "decayable";
        private static final String DISTANCE = "distance";
        @Nullable
        private IntSet leafIndices;
        @Nullable
        private IntSet logIndices;
        @Nullable
        private Int2IntMap leafStates;

        public LeavesLogFixer(Typed<?> typed, Schema schema) {
            super(typed, schema);
        }

        @Override
        protected boolean computeIsFixed() {
            this.leafIndices = new IntOpenHashSet();
            this.logIndices = new IntOpenHashSet();
            this.leafStates = new Int2IntOpenHashMap();

            for (int i = 0; i < this.properties.size(); i++) {
                Dynamic<?> dynamic = this.properties.get(i);
                String string = dynamic.get("Name").asString("");
                if (LeavesFix.LEAVES_MAP.containsKey(string)) {
                    boolean bl = Objects.equals(dynamic.get("Properties").get("decayable").asString(""), "false");
                    this.leafIndices.add(i);
                    this.leafStates.put(this.computeFlags(string, bl, 7), i);
                    this.properties.set(i, this.createLeafProperties(dynamic, string, bl, 7));
                }

                if (LeavesFix.LOGS_MAP.contains(string)) {
                    this.logIndices.add(i);
                }
            }

            return this.leafIndices.isEmpty() && this.logIndices.isEmpty();
        }

        private Dynamic<?> createLeafProperties(Dynamic<?> tag, String name, boolean persistent, int distance) {
            Dynamic<?> dynamic = tag.emptyMap();
            dynamic = dynamic.set("persistent", dynamic.createString(persistent ? "true" : "false"));
            dynamic = dynamic.set("distance", dynamic.createString(Integer.toString(distance)));
            Dynamic<?> dynamic2 = tag.emptyMap();
            dynamic2 = dynamic2.set("Properties", dynamic);
            return dynamic2.set("Name", dynamic2.createString(name));
        }

        public boolean isLog(int index) {
            return this.logIndices.contains(index);
        }

        public boolean isLeaf(int index) {
            return this.leafIndices.contains(index);
        }

        int getDistanceToLog(int index) {
            return this.isLog(index) ? 0 : Integer.parseInt(this.properties.get(index).get("Properties").get("distance").asString(""));
        }

        void computeLeafStates(int packedLocalPos, int propertyIndex, int distance) {
            Dynamic<?> dynamic = this.properties.get(propertyIndex);
            String string = dynamic.get("Name").asString("");
            boolean bl = Objects.equals(dynamic.get("Properties").get("persistent").asString(""), "true");
            int i = this.computeFlags(string, bl, distance);
            if (!this.leafStates.containsKey(i)) {
                int j = this.properties.size();
                this.leafIndices.add(j);
                this.leafStates.put(i, j);
                this.properties.add(this.createLeafProperties(dynamic, string, bl, distance));
            }

            int k = this.leafStates.get(i);
            if (1 << this.blockStateMap.getUnitSize() <= k) {
                WordPackedArray wordPackedArray = new WordPackedArray(this.blockStateMap.getUnitSize() + 1, 4096);

                for (int l = 0; l < 4096; l++) {
                    wordPackedArray.set(l, this.blockStateMap.get(l));
                }

                this.blockStateMap = wordPackedArray;
            }

            this.blockStateMap.set(packedLocalPos, k);
        }
    }

    public abstract static class ListFixer {
        protected static final String BLOCK_STATES_KEY = "BlockStates";
        protected static final String NAME_KEY = "Name";
        protected static final String PROPERTIES_KEY = "Properties";
        private final Type<Pair<String, Dynamic<?>>> blockStateType = DSL.named(TypeReferences.BLOCK_STATE.typeName(), DSL.remainderType());
        protected final OpticFinder<List<Pair<String, Dynamic<?>>>> paletteFinder = DSL.fieldFinder("Palette", DSL.list(this.blockStateType));
        protected final List<Dynamic<?>> properties;
        protected final int y;
        @Nullable
        protected WordPackedArray blockStateMap;

        public ListFixer(Typed<?> sectionTyped, Schema inputSchema) {
            if (!Objects.equals(inputSchema.getType(TypeReferences.BLOCK_STATE), this.blockStateType)) {
                throw new IllegalStateException("Block state type is not what was expected.");
            }

            Optional<List<Pair<String, Dynamic<?>>>> optional = sectionTyped.getOptional(this.paletteFinder);
            this.properties = optional.<List>map(palettes -> palettes.stream().map(Pair::getSecond).collect(Collectors.toList())).orElse(ImmutableList.of());
            Dynamic<?> dynamic = sectionTyped.get(DSL.remainderFinder());
            this.y = dynamic.get("Y").asInt(0);
            this.computeFixableBlockStates(dynamic);
        }

        protected void computeFixableBlockStates(Dynamic<?> dynamic) {
            if (this.computeIsFixed()) {
                this.blockStateMap = null;
            } else {
                long[] ls = dynamic.get("BlockStates").asLongStream().toArray();
                int i = Math.max(4, DataFixUtils.ceillog2(this.properties.size()));
                this.blockStateMap = new WordPackedArray(i, 4096, ls);
            }
        }

        public Typed<?> finalizeFix(Typed<?> typed) {
            return this.isFixed()
                ? typed
                : typed.update(
                        DSL.remainderFinder(),
                        remainder -> remainder.set("BlockStates", remainder.createLongList(Arrays.stream(this.blockStateMap.getAlignedArray())))
                    )
                    .set(
                        this.paletteFinder,
                        this.properties
                            .stream()
                            .map(propertiesDynamic -> Pair.of(TypeReferences.BLOCK_STATE.typeName(), propertiesDynamic))
                            .collect(Collectors.toList())
                    );
        }

        public boolean isFixed() {
            return this.blockStateMap == null;
        }

        public int blockStateAt(int index) {
            return this.blockStateMap.get(index);
        }

        protected int computeFlags(String leafBlockName, boolean persistent, int distance) {
            return LeavesFix.LEAVES_MAP.get(leafBlockName) << 5 | (persistent ? 16 : 0) | distance;
        }

        int getY() {
            return this.y;
        }

        protected abstract boolean computeIsFixed();
    }
}

