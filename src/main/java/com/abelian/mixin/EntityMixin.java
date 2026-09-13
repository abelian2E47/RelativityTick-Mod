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

    @Shadow public abstract World getEntityWorld();

    //1.21.11 的骑乘入口改为 startRiding(Entity, boolean force, boolean emitEvent)，单参版本内部委托到它
    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;ZZ)Z", at = @At("RETURN"))
    private void startRidingSync(Entity vehicle, boolean force, boolean emitEvent, CallbackInfoReturnable<Boolean> cir) {
        if (this.getEntityWorld().isClient() || !cir.getReturnValue()) return;

        PassengerSyncPayload payload = new PassengerSyncPayload(this.getId(), vehicle.getId());
        sendPayload(payload, vehicle.getEntityWorld().getRegistryKey());
    }

    @Inject(method = "stopRiding", at = @At("HEAD"))
    private void stopRidingSync(CallbackInfo ci) {
        if (this.getEntityWorld().isClient() || this.getVehicle() == null) return;

        PassengerSyncPayload payload = new PassengerSyncPayload(this.getId(), -1);
        sendPayload(payload, this.getEntityWorld().getRegistryKey());
    }

    @Unique
    private void sendPayload(PassengerSyncPayload payload, RegistryKey<World> registerKey){
        Objects.requireNonNull(RelativityTickUtils.getServer().getWorld(registerKey)).getPlayers().forEach(player -> ServerPlayNetworking.send(player, payload));
    }
}

