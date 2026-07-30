package net.minecraft.client.render.model;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class MultipartBakedModel extends WrapperBakedModel {
    private final List<MultipartBakedModel.Selector> selectors;
    private final Map<BlockState, BitSet> stateCache = new Reference2ObjectOpenHashMap<>();

    private static BakedModel getFirst(List<MultipartBakedModel.Selector> selectors) {
        if (selectors.isEmpty()) {
            throw new IllegalArgumentException("Model must have at least one selector");
        } else {
            return selectors.getFirst().model();
        }
    }

    public MultipartBakedModel(List<MultipartBakedModel.Selector> selectors) {
        super(getFirst(selectors));
        this.selectors = selectors;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        if (state == null) {
            return Collections.emptyList();
        }

        BitSet bitSet = this.stateCache.get(state);
        if (bitSet == null) {
            bitSet = new BitSet();

            for (int i = 0; i < this.selectors.size(); i++) {
                if (this.selectors.get(i).condition.test(state)) {
                    bitSet.set(i);
                }
            }

            this.stateCache.put(state, bitSet);
        }

        List<BakedQuad> list = new ArrayList<>();
        long l = random.nextLong();

        for (int j = 0; j < bitSet.length(); j++) {
            if (bitSet.get(j)) {
                random.setSeed(l);
                list.addAll(this.selectors.get(j).model.getQuads(state, face, random));
            }
        }

        return list;
    }

    @Environment(EnvType.CLIENT)
    public record Selector(Predicate<BlockState> condition, BakedModel model) {
    }
}

