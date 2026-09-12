package com.abelian.client.render;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityInterpolationManager {
    public static final Map<Integer, EntityRenderInterpolation> ENTITY_INTERPOLATIONS = new ConcurrentHashMap<>();
    public record EntityRenderInterpolation(String regionId, Vec3d previousPos, Vec3d currentPos) {}

    //取实体在指定区域的插值区间
    public static EntityRenderInterpolation getInterpolation(Entity entity, String regionId) {
        EntityRenderInterpolation interpolation = ENTITY_INTERPOLATIONS.get(entity.getId());
        if (interpolation == null || !interpolation.regionId().equals(regionId)) {
            return null;
        }
        return interpolation;
    }

    //按tickDelta在该区间内线性插值出渲染坐标
    public static Vec3d interpolate(EntityRenderInterpolation interpolation, float tickDelta) {
        Vec3d previous = interpolation.previousPos();
        Vec3d current = interpolation.currentPos();
        return new Vec3d(
                previous.x + (current.x - previous.x) * tickDelta,
                previous.y + (current.y - previous.y) * tickDelta,
                previous.z + (current.z - previous.z) * tickDelta
        );
    }

}
