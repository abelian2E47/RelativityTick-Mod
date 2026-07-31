package net.minecraft.client.render.block.entity;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class BlockEntityRenderDispatcher implements SynchronousResourceReloader {
    private Map<BlockEntityType<?>, BlockEntityRenderer<?>> renderers = ImmutableMap.of();
    private final TextRenderer textRenderer;
    private final Supplier<LoadedEntityModels> entityModelsGetter;
    public World world;
    public Camera camera;
    public HitResult crosshairTarget;
    private final BlockRenderManager blockRenderManager;
    private final ItemModelManager itemModelManager;
    private final ItemRenderer itemRenderer;
    private final EntityRenderDispatcher entityRenderDispatcher;

    public BlockEntityRenderDispatcher(
        TextRenderer textRenderer,
        Supplier<LoadedEntityModels> entityModelsGetter,
        BlockRenderManager blockRenderManager,
        ItemModelManager itemModelManager,
        ItemRenderer itemRenderer,
        EntityRenderDispatcher entityRenderDispatcher
    ) {
        this.itemRenderer = itemRenderer;
        this.itemModelManager = itemModelManager;
        this.entityRenderDispatcher = entityRenderDispatcher;
        this.textRenderer = textRenderer;
        this.entityModelsGetter = entityModelsGetter;
        this.blockRenderManager = blockRenderManager;
    }

    @Nullable
    public <E extends BlockEntity> BlockEntityRenderer<E> get(E blockEntity) {
        return (BlockEntityRenderer<E>)this.renderers.get(blockEntity.getType());
    }

    public void configure(World world, Camera camera, HitResult crosshairTarget) {
        if (this.world != world) {
            this.setWorld(world);
        }

        this.camera = camera;
        this.crosshairTarget = crosshairTarget;
    }

    public <E extends BlockEntity> void render(E blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        BlockEntityRenderer<E> blockEntityRenderer = this.get(blockEntity);
        if (blockEntityRenderer != null) {
            if (blockEntity.hasWorld() && blockEntity.getType().supports(blockEntity.getCachedState())) {
                if (blockEntityRenderer.isInRenderDistance(blockEntity, this.camera.getPos())) {
                    try {
                        render(blockEntityRenderer, blockEntity, tickDelta, matrices, vertexConsumers);
                    } catch (Throwable throwable) {
                        CrashReport crashReport = CrashReport.create(throwable, "Rendering Block Entity");
                        CrashReportSection crashReportSection = crashReport.addElement("Block Entity Details");
                        blockEntity.populateCrashReport(crashReportSection);
                        throw new CrashException(crashReport);
                    }
                }
            }
        }
    }

    private static <T extends BlockEntity> void render(
        BlockEntityRenderer<T> renderer, T blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers
    ) {
        World world = blockEntity.getWorld();
        int i;
        if (world != null) {
            i = WorldRenderer.getLightmapCoordinates(world, blockEntity.getPos());
        } else {
            i = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }

        renderer.render(blockEntity, tickDelta, matrices, vertexConsumers, i, OverlayTexture.DEFAULT_UV);
    }

    public void setWorld(@Nullable World world) {
        this.world = world;
        if (world == null) {
            this.camera = null;
        }
    }

    @Override
    public void reload(ResourceManager manager) {
        BlockEntityRendererFactory.Context context = new BlockEntityRendererFactory.Context(
            this,
            this.blockRenderManager,
            this.itemModelManager,
            this.itemRenderer,
            this.entityRenderDispatcher,
            this.entityModelsGetter.get(),
            this.textRenderer
        );
        this.renderers = BlockEntityRendererFactories.reload(context);
    }
}

