package net.minecraft.client.render.entity.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class CreakingAnimations {
    public static final Animation WALKING = Animation.Builder.create(1.125F)
        .looping()
        .addBoneAnimation(
            "upper_body",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(26.8802F, -23.399F, -9.0616F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.125F, AnimationHelper.createRotationalVector(-2.2093F, 5.9119F, 0.0675F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5417F, AnimationHelper.createRotationalVector(23.0778F, 14.2906F, 4.6066F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.875F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.125F, AnimationHelper.createRotationalVector(26.8802F, -23.399F, -9.0616F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "head",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0417F, AnimationHelper.createRotationalVector(-17.5F, -62.5F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.4167F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.4583F, AnimationHelper.createRotationalVector(0.0F, 15.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.0417F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.0833F, AnimationHelper.createRotationalVector(-37.1532F, 81.1131F, -28.3621F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_arm",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createRotationalVector(-32.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.875F, AnimationHelper.createRotationalVector(12.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.125F, AnimationHelper.createRotationalVector(-15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_arm",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(-15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.125F, AnimationHelper.createRotationalVector(10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5417F, AnimationHelper.createRotationalVector(-25.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.75F, AnimationHelper.createRotationalVector(-9.0923F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7917F, AnimationHelper.createRotationalVector(-15.137F, -66.7758F, 13.9603F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-9.0923F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.0F, AnimationHelper.createRotationalVector(10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.125F, AnimationHelper.createRotationalVector(-15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_leg",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createRotationalVector(30.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.375F, AnimationHelper.createRotationalVector(49.8924F, -3.8282F, 3.2187F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5F, AnimationHelper.createRotationalVector(17.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createRotationalVector(-56.5613F, -12.2403F, -8.7374F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.9167F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.125F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_leg",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 2.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createTranslationalVector(0.0F, 0.1846F, 0.5979F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.375F, AnimationHelper.createTranslationalVector(0.0F, -0.0665F, -2.2177F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5F, AnimationHelper.createTranslationalVector(0.0F, 1.3563F, -4.3474F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createTranslationalVector(0.0F, 0.1047F, -1.6556F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.9167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -1.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.125F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 2.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_leg",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(25.5305F, 11.3125F, 5.3525F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.125F, AnimationHelper.createRotationalVector(-49.5628F, 7.3556F, 6.7933F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.4583F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.9167F, AnimationHelper.createRotationalVector(30.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.0417F, AnimationHelper.createRotationalVector(55.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.125F, AnimationHelper.createRotationalVector(25.5305F, 11.3125F, 5.3525F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_leg",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.9674F, -3.6578F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.125F, AnimationHelper.createTranslationalVector(0.0F, -0.2979F, -0.9411F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createTranslationalVector(0.0F, -0.3F, -0.94F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.4583F, AnimationHelper.createTranslationalVector(0.0F, -0.3F, 1.06F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.125F, AnimationHelper.createTranslationalVector(0.0F, 0.9674F, -3.6578F), Transformation.Interpolations.LINEAR)
            )
        )
        .build();
    public static final Animation ATTACKING = Animation.Builder.create(0.7083F)
        .looping()
        .addBoneAnimation(
            "upper_body",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createRotationalVector(0.0F, 45.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-115.0F, 67.5F, -90.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.375F, AnimationHelper.createRotationalVector(67.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5417F, AnimationHelper.createRotationalVector(0.0F, 45.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "upper_body",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createTranslationalVector(0.0F, -2.7716F, -1.1481F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.375F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5417F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "upper_body",
            new Transformation(
                Transformation.Targets.SCALE,
                new Keyframe(0.0F, AnimationHelper.createScalingVector(1.0, 1.0, 1.0), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createScalingVector(1.0, 1.0, 1.0), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "head",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createRotationalVector(0.0F, -45.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createRotationalVector(-11.25F, -45.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-117.3939F, 76.6331F, -130.1483F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.4167F, AnimationHelper.createRotationalVector(-45.0F, -45.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5F, AnimationHelper.createRotationalVector(60.0F, -45.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5833F, AnimationHelper.createRotationalVector(60.0F, -45.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createRotationalVector(0.0F, -45.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "head",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.4167F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5F, AnimationHelper.createTranslationalVector(0.3827F, 0.5133F, -0.7682F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5833F, AnimationHelper.createTranslationalVector(0.3827F, 0.5133F, -0.7682F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "head",
            new Transformation(
                Transformation.Targets.SCALE,
                new Keyframe(0.1667F, AnimationHelper.createScalingVector(1.0, 1.0, 1.0), Transformation.Interpolations.LINEAR),
                new Keyframe(0.4167F, AnimationHelper.createScalingVector(1.0, 1.0, 1.0), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5F, AnimationHelper.createScalingVector(1.0, 1.3F, 1.0), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createScalingVector(1.0, 1.0, 1.0), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_arm",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.4583F, AnimationHelper.createRotationalVector(55.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_arm",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_leg",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_leg",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -2.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, -2.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_leg",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createRotationalVector(0.0F, 45.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createRotationalVector(0.0F, 45.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_leg",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createTranslationalVector(0.7071F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createTranslationalVector(0.7071F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_arm",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createRotationalVector(10.3453F, 14.7669F, 2.664F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.4583F, AnimationHelper.createRotationalVector(57.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.625F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_arm",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.7083F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .build();
    public static final Animation INVULNERABLE = Animation.Builder.create(0.2917F)
        .addBoneAnimation(
            "upper_body",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createRotationalVector(5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "upper_body",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_arm",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createRotationalVector(17.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_arm",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_arm",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createRotationalVector(20.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-15.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_arm",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.25F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .build();
    public static final Animation CRUMBLING = Animation.Builder.create(2.25F)
        .addBoneAnimation(
            "upper_body",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createRotationalVector(-40.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createRotationalVector(7.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5833F, AnimationHelper.createRotationalVector(16.25F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.6667F, AnimationHelper.createRotationalVector(29.0814F, 62.5516F, 26.5771F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.75F, AnimationHelper.createRotationalVector(12.2115F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.0F, AnimationHelper.createRotationalVector(10.25F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.0417F, AnimationHelper.createRotationalVector(-47.64F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.125F, AnimationHelper.createRotationalVector(21.96F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.25F, AnimationHelper.createRotationalVector(12.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(2.25F, AnimationHelper.createRotationalVector(17.3266F, 7.9022F, -0.1381F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "upper_body",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createTranslationalVector(0.0F, 0.557F, 1.2659F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createTranslationalVector(0.0F, -2.0889F, -0.3493F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "upper_body",
            new Transformation(
                Transformation.Targets.SCALE,
                new Keyframe(0.0F, AnimationHelper.createScalingVector(1.0, 1.0, 1.0), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createScalingVector(1.0, 1.1F, 1.0), Transformation.Interpolations.LINEAR),
                new Keyframe(0.1667F, AnimationHelper.createScalingVector(1.0, 0.9F, 1.0), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createScalingVector(1.0, 1.0, 1.0), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_arm",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.25F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.5417F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.5833F, AnimationHelper.createRotationalVector(-12.1479F, -34.3927F, 6.9326F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.6667F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "right_arm",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_arm",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.8333F, AnimationHelper.createRotationalVector(-4.4444F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.875F, AnimationHelper.createRotationalVector(-26.7402F, -78.831F, 26.3025F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.9583F, AnimationHelper.createRotationalVector(-5.5556F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.25F, AnimationHelper.createRotationalVector(-10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "left_arm",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "head",
            new Transformation(
                Transformation.Targets.ROTATE,
                new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.0833F, AnimationHelper.createRotationalVector(-5.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createRotationalVector(10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5F, AnimationHelper.createRotationalVector(2.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5417F, AnimationHelper.createRotationalVector(5.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.5833F, AnimationHelper.createRotationalVector(-67.4168F, -12.9552F, -8.0231F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.6667F, AnimationHelper.createRotationalVector(8.5F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.0F, AnimationHelper.createRotationalVector(10.773F, -29.5608F, -5.3627F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.25F, AnimationHelper.createRotationalVector(10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.7917F, AnimationHelper.createRotationalVector(10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.8333F, AnimationHelper.createRotationalVector(12.9625F, 39.2735F, 8.2901F), Transformation.Interpolations.LINEAR),
                new Keyframe(1.9167F, AnimationHelper.createRotationalVector(10.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .addBoneAnimation(
            "head",
            new Transformation(
                Transformation.Targets.TRANSLATE,
                new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                new Keyframe(0.2917F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            )
        )
        .build();
}

