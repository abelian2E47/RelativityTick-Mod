package net.minecraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.HashMap;
import java.util.Map;
import java.util.SequencedMap;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.BufferAllocator;

/**
 * Manages rendering with multiple {@linkplain RenderLayer render layers}.
 */
@Environment(EnvType.CLIENT)
public interface VertexConsumerProvider {
    /**
     * {@return a vertex consumer provider that immediately draws the current
     * buffer builder when a different render layer is requested}.
     */
    static VertexConsumerProvider.Immediate immediate(BufferAllocator buffer) {
        return immediate(Object2ObjectSortedMaps.emptyMap(), buffer);
    }

    /**
     * {@return a vertex consumer provider that immediately draws the current
     * buffer builder when a different render layer is requested except {@code
     * layerBuffers}}.
     * 
     * <p>{@code layerBuffers} will not be drawn immediately after switching
     * the current render layer. The caller of this method can control when to
     * draw these render layers. For example, {@linkplain
     * RenderLayer#TRANSLUCENT the translucent render layer} should be drawn in
     * a later stage so the other things behind translucent objects are
     * visible.
     */
    static VertexConsumerProvider.Immediate immediate(SequencedMap<RenderLayer, BufferAllocator> layerBuffers, BufferAllocator fallbackBuffer) {
        return new VertexConsumerProvider.Immediate(fallbackBuffer, layerBuffers);
    }

    /**
     * Starts building a buffer that will be drawn with {@code layer}.
     * 
     * <p>The returned vertex consumer can only be safely used until this
     * method is called with a different render layer.
     * 
     * <p>Note that the user of this method cannot modify states controlled
     * by the render layer. Those states will be overridden by the render layer
     * before drawing them. If you need to set states yourself, consider using
     * {@link Tessellator} or making a custom render layer.
     */
    VertexConsumer getBuffer(RenderLayer layer);

    /**
     * A vertex consumer provider that immediately draws the current buffer
     * builder when a different render layer is requested except for render
     * layers specified in the constructor.
     * 
     * @see VertexConsumerProvider#immediate(BufferAllocator)
     * @see VertexConsumerProvider#immediate(Map, BufferAllocator)
     */
    @Environment(EnvType.CLIENT)
    class Immediate implements VertexConsumerProvider {
        protected final BufferAllocator allocator;
        protected final SequencedMap<RenderLayer, BufferAllocator> layerBuffers;
        protected final Map<RenderLayer, BufferBuilder> pending = new HashMap<>();
        @Nullable
        protected RenderLayer currentLayer;

        protected Immediate(BufferAllocator allocator, SequencedMap<RenderLayer, BufferAllocator> sequencedMap) {
            this.allocator = allocator;
            this.layerBuffers = sequencedMap;
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            BufferBuilder bufferBuilder = this.pending.get(layer);
            if (bufferBuilder != null && !layer.areVerticesNotShared()) {
                this.draw(layer, bufferBuilder);
                bufferBuilder = null;
            }

            if (bufferBuilder != null) {
                return bufferBuilder;
            }

            BufferAllocator bufferAllocator = this.layerBuffers.get(layer);
            if (bufferAllocator != null) {
                bufferBuilder = new BufferBuilder(bufferAllocator, layer.getDrawMode(), layer.getVertexFormat());
            } else {
                if (this.currentLayer != null) {
                    this.draw(this.currentLayer);
                }

                bufferBuilder = new BufferBuilder(this.allocator, layer.getDrawMode(), layer.getVertexFormat());
                this.currentLayer = layer;
            }

            this.pending.put(layer, bufferBuilder);
            return bufferBuilder;
        }

        /**
         * Draws the current render layer if it's not in {@code layerBuffers}
         * specified in the constructor.
         */
        public void drawCurrentLayer() {
            if (this.currentLayer != null) {
                this.draw(this.currentLayer);
                this.currentLayer = null;
            }
        }

        /**
         * Draws all remaining render layers including {@code layerBuffers}
         * specified in the constructor.
         */
        public void draw() {
            this.drawCurrentLayer();

            for (RenderLayer renderLayer : this.layerBuffers.keySet()) {
                this.draw(renderLayer);
            }
        }

        /**
         * Draws the contents in the {@code layer}'s buffer.
         */
        public void draw(RenderLayer layer) {
            BufferBuilder bufferBuilder = this.pending.remove(layer);
            if (bufferBuilder != null) {
                this.draw(layer, bufferBuilder);
            }
        }

        private void draw(RenderLayer layer, BufferBuilder builder) {
            BuiltBuffer builtBuffer = builder.endNullable();
            if (builtBuffer != null) {
                if (layer.isTranslucent()) {
                    BufferAllocator bufferAllocator = this.layerBuffers.getOrDefault(layer, this.allocator);
                    builtBuffer.sortQuads(bufferAllocator, RenderSystem.getProjectionType().getVertexSorter());
                }

                layer.draw(builtBuffer);
            }

            if (layer.equals(this.currentLayer)) {
                this.currentLayer = null;
            }
        }
    }
}

