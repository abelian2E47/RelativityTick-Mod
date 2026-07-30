package net.minecraft.client.render.model.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.SpriteGetter;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.BasicBakedModel;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelTextures;
import net.minecraft.client.render.model.ResolvableModel;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class GeneratedItemModel implements UnbakedModel {
    public static final Identifier GENERATED = Identifier.ofVanilla("builtin/generated");
    public static final List<String> LAYERS = List.of("layer0", "layer1", "layer2", "layer3", "layer4");
    private static final float field_32806 = 7.5F;
    private static final float field_32807 = 8.5F;
    private static final ModelTextures.Textures TEXTURES = new ModelTextures.Textures.Builder().addTextureReference("particle", "layer0").build();

    @Override
    public ModelTextures.Textures getTextures() {
        return TEXTURES;
    }

    @Override
    public void resolve(ResolvableModel.Resolver resolver) {
    }

    @Nullable
    @Override
    public UnbakedModel.GuiLight getGuiLight() {
        return UnbakedModel.GuiLight.ITEM;
    }

    @Override
    public BakedModel bake(
        ModelTextures textures, Baker baker, ModelBakeSettings settings, boolean ambientOcclusion, boolean isSideLit, ModelTransformation transformation
    ) {
        return this.create(textures, baker.getSpriteGetter(), settings, ambientOcclusion, isSideLit, transformation);
    }

    private BakedModel create(
        ModelTextures textures,
        SpriteGetter spriteGetter,
        ModelBakeSettings settings,
        boolean ambientOcclusion,
        boolean isSideLit,
        ModelTransformation transformation
    ) {
        ModelTextures.Textures.Builder builder = new ModelTextures.Textures.Builder();
        List<ModelElement> list = new ArrayList<>();

        for (int i = 0; i < LAYERS.size(); i++) {
            String string = LAYERS.get(i);
            SpriteIdentifier spriteIdentifier = textures.get(string);
            if (spriteIdentifier == null) {
                break;
            }

            builder.addSprite(string, spriteIdentifier);
            SpriteContents spriteContents = spriteGetter.get(spriteIdentifier).getContents();
            list.addAll(this.addLayerElements(i, string, spriteContents));
        }

        return BasicBakedModel.bake(list, textures, spriteGetter, settings, ambientOcclusion, isSideLit, false, transformation);
    }

    private List<ModelElement> addLayerElements(int layer, String key, SpriteContents sprite) {
        Map<Direction, ModelElementFace> map = Map.of(
            Direction.SOUTH,
            new ModelElementFace(null, layer, key, new ModelElementTexture(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0)),
            Direction.NORTH,
            new ModelElementFace(null, layer, key, new ModelElementTexture(new float[]{16.0F, 0.0F, 0.0F, 16.0F}, 0))
        );
        List<ModelElement> list = new ArrayList<>();
        list.add(new ModelElement(new Vector3f(0.0F, 0.0F, 7.5F), new Vector3f(16.0F, 16.0F, 8.5F), map));
        list.addAll(this.addSubComponents(sprite, key, layer));
        return list;
    }

    private List<ModelElement> addSubComponents(SpriteContents sprite, String key, int layer) {
        float f = sprite.getWidth();
        float g = sprite.getHeight();
        List<ModelElement> list = new ArrayList<>();

        for (GeneratedItemModel.Frame frame : this.getFrames(sprite)) {
            float h = 0.0F;
            float i = 0.0F;
            float j = 0.0F;
            float k = 0.0F;
            float l = 0.0F;
            float m = 0.0F;
            float n = 0.0F;
            float o = 0.0F;
            float p = 16.0F / f;
            float q = 16.0F / g;
            float r = frame.getMin();
            float s = frame.getMax();
            float t = frame.getLevel();
            GeneratedItemModel.Side side = frame.getSide();
            switch (side) {
                case UP:
                    l = r;
                    h = r;
                    j = m = s + 1.0F;
                    n = t;
                    i = t;
                    k = t;
                    o = t + 1.0F;
                    break;
                case DOWN:
                    n = t;
                    o = t + 1.0F;
                    l = r;
                    h = r;
                    j = m = s + 1.0F;
                    i = t + 1.0F;
                    k = t + 1.0F;
                    break;
                case LEFT:
                    l = t;
                    h = t;
                    j = t;
                    m = t + 1.0F;
                    o = r;
                    i = r;
                    k = n = s + 1.0F;
                    break;
                case RIGHT:
                    l = t;
                    m = t + 1.0F;
                    h = t + 1.0F;
                    j = t + 1.0F;
                    o = r;
                    i = r;
                    k = n = s + 1.0F;
            }

            h *= p;
            j *= p;
            i *= q;
            k *= q;
            i = 16.0F - i;
            k = 16.0F - k;
            l *= p;
            m *= p;
            n *= q;
            o *= q;
            Map<Direction, ModelElementFace> map = Map.of(
                side.getDirection(), new ModelElementFace(null, layer, key, new ModelElementTexture(new float[]{l, n, m, o}, 0))
            );
            switch (side) {
                case UP:
                    list.add(new ModelElement(new Vector3f(h, i, 7.5F), new Vector3f(j, i, 8.5F), map));
                    break;
                case DOWN:
                    list.add(new ModelElement(new Vector3f(h, k, 7.5F), new Vector3f(j, k, 8.5F), map));
                    break;
                case LEFT:
                    list.add(new ModelElement(new Vector3f(h, i, 7.5F), new Vector3f(h, k, 8.5F), map));
                    break;
                case RIGHT:
                    list.add(new ModelElement(new Vector3f(j, i, 7.5F), new Vector3f(j, k, 8.5F), map));
            }
        }

        return list;
    }

    private List<GeneratedItemModel.Frame> getFrames(SpriteContents sprite) {
        int i = sprite.getWidth();
        int j = sprite.getHeight();
        List<GeneratedItemModel.Frame> list = new ArrayList<>();
        sprite.getDistinctFrameCount().forEach(frame -> {
            for (int k = 0; k < j; k++) {
                for (int l = 0; l < i; l++) {
                    boolean bl = !this.isPixelTransparent(sprite, frame, l, k, i, j);
                    this.buildCube(GeneratedItemModel.Side.UP, list, sprite, frame, l, k, i, j, bl);
                    this.buildCube(GeneratedItemModel.Side.DOWN, list, sprite, frame, l, k, i, j, bl);
                    this.buildCube(GeneratedItemModel.Side.LEFT, list, sprite, frame, l, k, i, j, bl);
                    this.buildCube(GeneratedItemModel.Side.RIGHT, list, sprite, frame, l, k, i, j, bl);
                }
            }
        });
        return list;
    }

    private void buildCube(
        GeneratedItemModel.Side side, List<GeneratedItemModel.Frame> cubes, SpriteContents sprite, int frame, int x, int y, int width, int height, boolean bl
    ) {
        boolean bl2 = this.isPixelTransparent(sprite, frame, x + side.getOffsetX(), y + side.getOffsetY(), width, height) && bl;
        if (bl2) {
            this.buildCube(cubes, side, x, y);
        }
    }

    private void buildCube(List<GeneratedItemModel.Frame> cubes, GeneratedItemModel.Side side, int x, int y) {
        GeneratedItemModel.Frame frame = null;

        for (GeneratedItemModel.Frame frame2 : cubes) {
            if (frame2.getSide() == side) {
                int i = side.isVertical() ? y : x;
                if (frame2.getLevel() == i) {
                    frame = frame2;
                    break;
                }
            }
        }

        int j = side.isVertical() ? y : x;
        int k = side.isVertical() ? x : y;
        if (frame == null) {
            cubes.add(new GeneratedItemModel.Frame(side, k, j));
        } else {
            frame.expand(k);
        }
    }

    private boolean isPixelTransparent(SpriteContents sprite, int frame, int x, int y, int width, int height) {
        return x >= 0 && y >= 0 && x < width && y < height ? sprite.isPixelTransparent(frame, x, y) : true;
    }

    @Environment(EnvType.CLIENT)
    static class Frame {
        private final GeneratedItemModel.Side side;
        private int min;
        private int max;
        private final int level;

        public Frame(GeneratedItemModel.Side side, int width, int depth) {
            this.side = side;
            this.min = width;
            this.max = width;
            this.level = depth;
        }

        public void expand(int newValue) {
            if (newValue < this.min) {
                this.min = newValue;
            } else if (newValue > this.max) {
                this.max = newValue;
            }
        }

        public GeneratedItemModel.Side getSide() {
            return this.side;
        }

        public int getMin() {
            return this.min;
        }

        public int getMax() {
            return this.max;
        }

        public int getLevel() {
            return this.level;
        }
    }

    @Environment(EnvType.CLIENT)
    enum Side {
        UP(Direction.UP, 0, -1),
        DOWN(Direction.DOWN, 0, 1),
        LEFT(Direction.EAST, -1, 0),
        RIGHT(Direction.WEST, 1, 0);

        private final Direction direction;
        private final int offsetX;
        private final int offsetY;

        Side(final Direction direction, final int offsetX, final int offsetY) {
            this.direction = direction;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }

        public Direction getDirection() {
            return this.direction;
        }

        public int getOffsetX() {
            return this.offsetX;
        }

        public int getOffsetY() {
            return this.offsetY;
        }

        boolean isVertical() {
            return this == DOWN || this == UP;
        }
    }
}

