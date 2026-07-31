package net.minecraft.client.render.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.model.SpriteGetter;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class BasicBakedModel implements BakedModel {
    public static final String PARTICLE_TEXTURE_ID = "particle";
    private final List<BakedQuad> quads;
    private final Map<Direction, List<BakedQuad>> faceQuads;
    private final boolean usesAo;
    private final boolean hasDepth;
    private final boolean isSideLit;
    private final Sprite sprite;
    private final ModelTransformation transformation;

    public BasicBakedModel(
        List<BakedQuad> quads,
        Map<Direction, List<BakedQuad>> faceQuads,
        boolean usesAo,
        boolean isSideLit,
        boolean hasDepth,
        Sprite sprite,
        ModelTransformation transformation
    ) {
        this.quads = quads;
        this.faceQuads = faceQuads;
        this.usesAo = usesAo;
        this.hasDepth = hasDepth;
        this.isSideLit = isSideLit;
        this.sprite = sprite;
        this.transformation = transformation;
    }

    public static BakedModel bake(
        List<ModelElement> elements,
        ModelTextures textures,
        SpriteGetter spriteGetter,
        ModelBakeSettings settings,
        boolean ambientOcclusion,
        boolean isSideLit,
        boolean hasDepth,
        ModelTransformation transformation
    ) {
        Sprite sprite = getSprite(spriteGetter, textures, "particle");
        BasicBakedModel.Builder builder = new BasicBakedModel.Builder(ambientOcclusion, isSideLit, hasDepth, transformation).setParticle(sprite);

        for (ModelElement modelElement : elements) {
            for (Direction direction : modelElement.faces.keySet()) {
                ModelElementFace modelElementFace = modelElement.faces.get(direction);
                Sprite sprite2 = getSprite(spriteGetter, textures, modelElementFace.textureId());
                if (modelElementFace.cullFace() == null) {
                    builder.addQuad(bake(modelElement, modelElementFace, sprite2, direction, settings));
                } else {
                    builder.addQuad(
                        Direction.transform(settings.getRotation().getMatrix(), modelElementFace.cullFace()),
                        bake(modelElement, modelElementFace, sprite2, direction, settings)
                    );
                }
            }
        }

        return builder.build();
    }

    private static BakedQuad bake(ModelElement element, ModelElementFace face, Sprite sprite, Direction direction, ModelBakeSettings settings) {
        return BakedQuadFactory.bake(element.from, element.to, face, sprite, direction, settings, element.rotation, element.shade, element.lightEmission);
    }

    private static Sprite getSprite(SpriteGetter spriteGetter, ModelTextures textures, String textureId) {
        SpriteIdentifier spriteIdentifier = textures.get(textureId);
        return spriteIdentifier != null ? spriteGetter.get(spriteIdentifier) : spriteGetter.getMissing(textureId);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return face == null ? this.quads : this.faceQuads.get(face);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.usesAo;
    }

    @Override
    public boolean hasDepth() {
        return this.hasDepth;
    }

    @Override
    public boolean isSideLit() {
        return this.isSideLit;
    }

    @Override
    public Sprite getParticleSprite() {
        return this.sprite;
    }

    @Override
    public ModelTransformation getTransformation() {
        return this.transformation;
    }

    @Environment(EnvType.CLIENT)
    public static class Builder {
        private final ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
        private final EnumMap<Direction, ImmutableList.Builder<BakedQuad>> faceQuads = Maps.newEnumMap(Direction.class);
        private final boolean usesAo;
        @Nullable
        private Sprite particleTexture;
        private final boolean isSideLit;
        private final boolean hasDepth;
        private final ModelTransformation transformation;

        public Builder(boolean usesAo, boolean isSideLit, boolean hasDepth, ModelTransformation transformation) {
            this.usesAo = usesAo;
            this.isSideLit = isSideLit;
            this.hasDepth = hasDepth;
            this.transformation = transformation;

            for (Direction direction : Direction.values()) {
                this.faceQuads.put(direction, ImmutableList.builder());
            }
        }

        public BasicBakedModel.Builder addQuad(Direction side, BakedQuad quad) {
            this.faceQuads.get(side).add(quad);
            return this;
        }

        public BasicBakedModel.Builder addQuad(BakedQuad quad) {
            this.quads.add(quad);
            return this;
        }

        public BasicBakedModel.Builder setParticle(Sprite sprite) {
            this.particleTexture = sprite;
            return this;
        }

        public BasicBakedModel.Builder method_35809() {
            return this;
        }

        public BakedModel build() {
            if (this.particleTexture == null) {
                throw new RuntimeException("Missing particle!");
            }

            Map<Direction, List<BakedQuad>> map = Maps.transformValues(this.faceQuads, ImmutableList.Builder::build);
            return new BasicBakedModel(
                this.quads.build(), new EnumMap<>(map), this.usesAo, this.isSideLit, this.hasDepth, this.particleTexture, this.transformation
            );
        }
    }
}

