package net.minecraft.client.texture;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.resource.metadata.GuiResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class GuiAtlasManager extends SpriteAtlasHolder {
    private static final Set<ResourceMetadataSerializer<?>> METADATA_SERIALIZERS = Set.of(AnimationResourceMetadata.SERIALIZER, GuiResourceMetadata.SERIALIZER);

    public GuiAtlasManager(TextureManager manager) {
        super(manager, Identifier.ofVanilla("textures/atlas/gui.png"), Identifier.ofVanilla("gui"), METADATA_SERIALIZERS);
    }

    @Override
    public Sprite getSprite(Identifier objectId) {
        return super.getSprite(objectId);
    }

    public Scaling getScaling(Sprite sprite) {
        return this.getGuiMetadata(sprite).scaling();
    }

    private GuiResourceMetadata getGuiMetadata(Sprite sprite) {
        return sprite.getContents().getMetadata().decode(GuiResourceMetadata.SERIALIZER).orElse(GuiResourceMetadata.DEFAULT);
    }
}

