package com.abelian.client;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityInterpolationManager {
    public static final Map<Integer, EntityRenderInterpolation> ENTITY_INTERPOLATIONS = new ConcurrentHashMap<>();
    public record EntityRenderInterpolation(String regionId, Vec3d previousPos, Vec3d currentPos) {}

    public static Vec3d getInterpolatedEntityPos(Entity entity, String regionId, float tickDelta) {
        EntityRenderInterpolation interpolation = ENTITY_INTERPOLATIONS.get(entity.getId());
        if (interpolation == null || !interpolation.regionId().equals(regionId)) {
            return entity.getLerpedPos(tickDelta);
        }

        Vec3d previous = interpolation.previousPos();
        Vec3d current = interpolation.currentPos();
        return new Vec3d(
                previous.x + (current.x - previous.x) * tickDelta,
                previous.y + (current.y - previous.y) * tickDelta,
                previous.z + (current.z - previous.z) * tickDelta
        );
    }


}
