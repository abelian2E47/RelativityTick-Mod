package com.abelian.mixin;

import com.abelian.regionTick.Region;
import com.abelian.regionTick.RegionsManager;
import net.minecraft.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin extends Chunk {

    public WorldChunkMixin(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> biomeRegistry, long inhabitedTime, @Nullable ChunkSection[] sectionArray, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightLimitView, biomeRegistry, inhabitedTime, sectionArray, blendingData);
    }

    @Inject(method = "canTickBlockEntity", at = @At("HEAD"), cancellable = true)
    private void onCanTickBlockEntity(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        World world = ((WorldChunk) (Object) this).getWorld();
        long posLong = this.getPos().toLong();
        Region manager = world instanceof net.minecraft.server.world.ServerWorld serverWorld
                ? RegionsManager.getRegionByChunk(serverWorld, posLong)
                : null;

        if (manager != null && manager.isControlled()) {
            cir.setReturnValue(false);
        }
    }
}
