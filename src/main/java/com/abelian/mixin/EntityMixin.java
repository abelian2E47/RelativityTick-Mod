package com.abelian.mixin;

import com.abelian.RelativityTickUtils;
import com.abelian.network.PassengerSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract int getId();

    @Shadow @Nullable public abstract Entity getVehicle();

    @Shadow public abstract World getWorld();

    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At("RETURN"))
    private void startRidingSync(Entity vehicle, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (this.getWorld().isClient() || !cir.getReturnValue()) return;

        PassengerSyncPayload payload = new PassengerSyncPayload(this.getId(), vehicle.getId());
        sendPayload(payload, vehicle.getWorld().getRegistryKey());
    }

    @Inject(method = "stopRiding", at = @At("HEAD"))
    private void stopRidingSync(CallbackInfo ci) {
        if (this.getWorld().isClient() || this.getVehicle() == null) return;

        PassengerSyncPayload payload = new PassengerSyncPayload(this.getId(), -1);
        sendPayload(payload, this.getWorld().getRegistryKey());
    }

    @Unique
    private void sendPayload(PassengerSyncPayload payload, RegistryKey<World> registerKey){
        Objects.requireNonNull(RelativityTickUtils.getServer().getWorld(registerKey)).getPlayers().forEach(player -> ServerPlayNetworking.send(player, payload));
    }
}

