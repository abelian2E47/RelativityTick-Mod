package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;

@Environment(EnvType.CLIENT)
public class ArmedEntityRenderState extends LivingEntityRenderState {
    public Arm mainArm = Arm.RIGHT;
    public BipedEntityModel.ArmPose rightArmPose = BipedEntityModel.ArmPose.EMPTY;
    public final ItemRenderState rightHandItemState = new ItemRenderState();
    public BipedEntityModel.ArmPose leftArmPose = BipedEntityModel.ArmPose.EMPTY;
    public final ItemRenderState leftHandItemState = new ItemRenderState();

    public ItemRenderState getMainHandItemState() {
        return this.mainArm == Arm.RIGHT ? this.rightHandItemState : this.leftHandItemState;
    }

    public static void updateRenderState(LivingEntity entity, ArmedEntityRenderState state, ItemModelManager itemModelManager) {
        state.mainArm = entity.getMainArm();
        itemModelManager.updateForLivingEntity(
            state.rightHandItemState, entity.getStackInArm(Arm.RIGHT), ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, false, entity
        );
        itemModelManager.updateForLivingEntity(
            state.leftHandItemState, entity.getStackInArm(Arm.LEFT), ModelTransformationMode.THIRD_PERSON_LEFT_HAND, true, entity
        );
    }
}

