package net.minecraft.util.profiling.jfr.sample;

import java.time.Duration;
import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.util.math.ChunkPos;

public record StructureGenerationSample(Duration duration, ChunkPos chunkPos, String structureName, String level, boolean success) implements LongRunningSample {
    public static StructureGenerationSample fromEvent(RecordedEvent event) {
        return new StructureGenerationSample(
            event.getDuration(),
            new ChunkPos(event.getInt("chunkPosX"), event.getInt("chunkPosX")),
            event.getString("structure"),
            event.getString("level"),
            event.getBoolean("success")
        );
    }
}

