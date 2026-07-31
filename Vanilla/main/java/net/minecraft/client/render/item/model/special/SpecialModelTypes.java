package net.minecraft.client.render.item.model.special;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.WoodType;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public class SpecialModelTypes {
    private static final Codecs.IdMapper<Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked>> ID_MAPPER = new Codecs.IdMapper<>();
    public static final Codec<SpecialModelRenderer.Unbaked> CODEC = ID_MAPPER.getCodec(Identifier.CODEC)
        .dispatch(SpecialModelRenderer.Unbaked::getCodec, codec -> codec);
    private static final Map<Block, SpecialModelRenderer.Unbaked> BLOCK_TO_MODEL_TYPE = ImmutableMap.<Block, SpecialModelRenderer.Unbaked>builder()
        .put(Blocks.SKELETON_SKULL, new HeadModelRenderer.Unbaked(SkullBlock.Type.SKELETON))
        .put(Blocks.ZOMBIE_HEAD, new HeadModelRenderer.Unbaked(SkullBlock.Type.ZOMBIE))
        .put(Blocks.CREEPER_HEAD, new HeadModelRenderer.Unbaked(SkullBlock.Type.CREEPER))
        .put(Blocks.DRAGON_HEAD, new HeadModelRenderer.Unbaked(SkullBlock.Type.DRAGON))
        .put(Blocks.PIGLIN_HEAD, new HeadModelRenderer.Unbaked(SkullBlock.Type.PIGLIN))
        .put(Blocks.PLAYER_HEAD, new HeadModelRenderer.Unbaked(SkullBlock.Type.PLAYER))
        .put(Blocks.WITHER_SKELETON_SKULL, new HeadModelRenderer.Unbaked(SkullBlock.Type.WITHER_SKELETON))
        .put(Blocks.SKELETON_WALL_SKULL, new HeadModelRenderer.Unbaked(SkullBlock.Type.SKELETON))
        .put(Blocks.ZOMBIE_WALL_HEAD, new HeadModelRenderer.Unbaked(SkullBlock.Type.ZOMBIE))
        .put(Blocks.CREEPER_WALL_HEAD, new HeadModelRenderer.Unbaked(SkullBlock.Type.CREEPER))
        .put(Blocks.DRAGON_WALL_HEAD, new HeadModelRenderer.Unbaked(SkullBlock.Type.DRAGON))
        .put(Blocks.PIGLIN_WALL_HEAD, new HeadModelRenderer.Unbaked(SkullBlock.Type.PIGLIN))
        .put(Blocks.PLAYER_WALL_HEAD, new HeadModelRenderer.Unbaked(SkullBlock.Type.PLAYER))
        .put(Blocks.WITHER_SKELETON_WALL_SKULL, new HeadModelRenderer.Unbaked(SkullBlock.Type.WITHER_SKELETON))
        .put(Blocks.WHITE_BANNER, new BannerModelRenderer.Unbaked(DyeColor.WHITE))
        .put(Blocks.ORANGE_BANNER, new BannerModelRenderer.Unbaked(DyeColor.ORANGE))
        .put(Blocks.MAGENTA_BANNER, new BannerModelRenderer.Unbaked(DyeColor.MAGENTA))
        .put(Blocks.LIGHT_BLUE_BANNER, new BannerModelRenderer.Unbaked(DyeColor.LIGHT_BLUE))
        .put(Blocks.YELLOW_BANNER, new BannerModelRenderer.Unbaked(DyeColor.YELLOW))
        .put(Blocks.LIME_BANNER, new BannerModelRenderer.Unbaked(DyeColor.LIME))
        .put(Blocks.PINK_BANNER, new BannerModelRenderer.Unbaked(DyeColor.PINK))
        .put(Blocks.GRAY_BANNER, new BannerModelRenderer.Unbaked(DyeColor.GRAY))
        .put(Blocks.LIGHT_GRAY_BANNER, new BannerModelRenderer.Unbaked(DyeColor.LIGHT_GRAY))
        .put(Blocks.CYAN_BANNER, new BannerModelRenderer.Unbaked(DyeColor.CYAN))
        .put(Blocks.PURPLE_BANNER, new BannerModelRenderer.Unbaked(DyeColor.PURPLE))
        .put(Blocks.BLUE_BANNER, new BannerModelRenderer.Unbaked(DyeColor.BLUE))
        .put(Blocks.BROWN_BANNER, new BannerModelRenderer.Unbaked(DyeColor.BROWN))
        .put(Blocks.GREEN_BANNER, new BannerModelRenderer.Unbaked(DyeColor.GREEN))
        .put(Blocks.RED_BANNER, new BannerModelRenderer.Unbaked(DyeColor.RED))
        .put(Blocks.BLACK_BANNER, new BannerModelRenderer.Unbaked(DyeColor.BLACK))
        .put(Blocks.WHITE_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.WHITE))
        .put(Blocks.ORANGE_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.ORANGE))
        .put(Blocks.MAGENTA_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.MAGENTA))
        .put(Blocks.LIGHT_BLUE_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.LIGHT_BLUE))
        .put(Blocks.YELLOW_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.YELLOW))
        .put(Blocks.LIME_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.LIME))
        .put(Blocks.PINK_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.PINK))
        .put(Blocks.GRAY_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.GRAY))
        .put(Blocks.LIGHT_GRAY_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.LIGHT_GRAY))
        .put(Blocks.CYAN_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.CYAN))
        .put(Blocks.PURPLE_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.PURPLE))
        .put(Blocks.BLUE_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.BLUE))
        .put(Blocks.BROWN_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.BROWN))
        .put(Blocks.GREEN_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.GREEN))
        .put(Blocks.RED_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.RED))
        .put(Blocks.BLACK_WALL_BANNER, new BannerModelRenderer.Unbaked(DyeColor.BLACK))
        .put(Blocks.WHITE_BED, new BedModelRenderer.Unbaked(DyeColor.WHITE))
        .put(Blocks.ORANGE_BED, new BedModelRenderer.Unbaked(DyeColor.ORANGE))
        .put(Blocks.MAGENTA_BED, new BedModelRenderer.Unbaked(DyeColor.MAGENTA))
        .put(Blocks.LIGHT_BLUE_BED, new BedModelRenderer.Unbaked(DyeColor.LIGHT_BLUE))
        .put(Blocks.YELLOW_BED, new BedModelRenderer.Unbaked(DyeColor.YELLOW))
        .put(Blocks.LIME_BED, new BedModelRenderer.Unbaked(DyeColor.LIME))
        .put(Blocks.PINK_BED, new BedModelRenderer.Unbaked(DyeColor.PINK))
        .put(Blocks.GRAY_BED, new BedModelRenderer.Unbaked(DyeColor.GRAY))
        .put(Blocks.LIGHT_GRAY_BED, new BedModelRenderer.Unbaked(DyeColor.LIGHT_GRAY))
        .put(Blocks.CYAN_BED, new BedModelRenderer.Unbaked(DyeColor.CYAN))
        .put(Blocks.PURPLE_BED, new BedModelRenderer.Unbaked(DyeColor.PURPLE))
        .put(Blocks.BLUE_BED, new BedModelRenderer.Unbaked(DyeColor.BLUE))
        .put(Blocks.BROWN_BED, new BedModelRenderer.Unbaked(DyeColor.BROWN))
        .put(Blocks.GREEN_BED, new BedModelRenderer.Unbaked(DyeColor.GREEN))
        .put(Blocks.RED_BED, new BedModelRenderer.Unbaked(DyeColor.RED))
        .put(Blocks.BLACK_BED, new BedModelRenderer.Unbaked(DyeColor.BLACK))
        .put(Blocks.SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked())
        .put(Blocks.WHITE_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.WHITE))
        .put(Blocks.ORANGE_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.ORANGE))
        .put(Blocks.MAGENTA_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.MAGENTA))
        .put(Blocks.LIGHT_BLUE_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.LIGHT_BLUE))
        .put(Blocks.YELLOW_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.YELLOW))
        .put(Blocks.LIME_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.LIME))
        .put(Blocks.PINK_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.PINK))
        .put(Blocks.GRAY_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.GRAY))
        .put(Blocks.LIGHT_GRAY_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.LIGHT_GRAY))
        .put(Blocks.CYAN_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.CYAN))
        .put(Blocks.PURPLE_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.PURPLE))
        .put(Blocks.BLUE_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.BLUE))
        .put(Blocks.BROWN_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.BROWN))
        .put(Blocks.GREEN_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.GREEN))
        .put(Blocks.RED_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.RED))
        .put(Blocks.BLACK_SHULKER_BOX, new ShulkerBoxModelRenderer.Unbaked(DyeColor.BLACK))
        .put(Blocks.OAK_SIGN, new SignModelRenderer.Unbaked(WoodType.OAK))
        .put(Blocks.SPRUCE_SIGN, new SignModelRenderer.Unbaked(WoodType.SPRUCE))
        .put(Blocks.BIRCH_SIGN, new SignModelRenderer.Unbaked(WoodType.BIRCH))
        .put(Blocks.ACACIA_SIGN, new SignModelRenderer.Unbaked(WoodType.ACACIA))
        .put(Blocks.CHERRY_SIGN, new SignModelRenderer.Unbaked(WoodType.CHERRY))
        .put(Blocks.JUNGLE_SIGN, new SignModelRenderer.Unbaked(WoodType.JUNGLE))
        .put(Blocks.DARK_OAK_SIGN, new SignModelRenderer.Unbaked(WoodType.DARK_OAK))
        .put(Blocks.PALE_OAK_SIGN, new SignModelRenderer.Unbaked(WoodType.PALE_OAK))
        .put(Blocks.MANGROVE_SIGN, new SignModelRenderer.Unbaked(WoodType.MANGROVE))
        .put(Blocks.BAMBOO_SIGN, new SignModelRenderer.Unbaked(WoodType.BAMBOO))
        .put(Blocks.CRIMSON_SIGN, new SignModelRenderer.Unbaked(WoodType.CRIMSON))
        .put(Blocks.WARPED_SIGN, new SignModelRenderer.Unbaked(WoodType.WARPED))
        .put(Blocks.OAK_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.OAK))
        .put(Blocks.SPRUCE_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.SPRUCE))
        .put(Blocks.BIRCH_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.BIRCH))
        .put(Blocks.ACACIA_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.ACACIA))
        .put(Blocks.CHERRY_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.CHERRY))
        .put(Blocks.JUNGLE_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.JUNGLE))
        .put(Blocks.DARK_OAK_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.DARK_OAK))
        .put(Blocks.PALE_OAK_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.PALE_OAK))
        .put(Blocks.MANGROVE_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.MANGROVE))
        .put(Blocks.BAMBOO_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.BAMBOO))
        .put(Blocks.CRIMSON_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.CRIMSON))
        .put(Blocks.WARPED_WALL_SIGN, new SignModelRenderer.Unbaked(WoodType.WARPED))
        .put(Blocks.OAK_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.OAK))
        .put(Blocks.SPRUCE_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.SPRUCE))
        .put(Blocks.BIRCH_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.BIRCH))
        .put(Blocks.ACACIA_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.ACACIA))
        .put(Blocks.CHERRY_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.CHERRY))
        .put(Blocks.JUNGLE_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.JUNGLE))
        .put(Blocks.DARK_OAK_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.DARK_OAK))
        .put(Blocks.PALE_OAK_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.PALE_OAK))
        .put(Blocks.MANGROVE_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.MANGROVE))
        .put(Blocks.BAMBOO_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.BAMBOO))
        .put(Blocks.CRIMSON_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.CRIMSON))
        .put(Blocks.WARPED_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.WARPED))
        .put(Blocks.OAK_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.OAK))
        .put(Blocks.SPRUCE_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.SPRUCE))
        .put(Blocks.BIRCH_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.BIRCH))
        .put(Blocks.ACACIA_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.ACACIA))
        .put(Blocks.CHERRY_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.CHERRY))
        .put(Blocks.JUNGLE_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.JUNGLE))
        .put(Blocks.DARK_OAK_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.DARK_OAK))
        .put(Blocks.PALE_OAK_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.PALE_OAK))
        .put(Blocks.MANGROVE_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.MANGROVE))
        .put(Blocks.BAMBOO_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.BAMBOO))
        .put(Blocks.CRIMSON_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.CRIMSON))
        .put(Blocks.WARPED_WALL_HANGING_SIGN, new HangingSignModelRenderer.Unbaked(WoodType.WARPED))
        .put(Blocks.CONDUIT, new ConduitModelRenderer.Unbaked())
        .put(Blocks.CHEST, new ChestModelRenderer.Unbaked(ChestModelRenderer.NORMAL_ID))
        .put(Blocks.TRAPPED_CHEST, new ChestModelRenderer.Unbaked(ChestModelRenderer.TRAPPED_ID))
        .put(Blocks.ENDER_CHEST, new ChestModelRenderer.Unbaked(ChestModelRenderer.ENDER_ID))
        .put(Blocks.DECORATED_POT, new DecoratedPotModelRenderer.Unbaked())
        .build();
    private static final ChestModelRenderer.Unbaked CHRISTMAS_CHEST = new ChestModelRenderer.Unbaked(ChestModelRenderer.CHRISTMAS_ID);

    public static void bootstrap() {
        ID_MAPPER.put(Identifier.ofVanilla("bed"), BedModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("banner"), BannerModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("conduit"), ConduitModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("chest"), ChestModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("head"), HeadModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("shulker_box"), ShulkerBoxModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("shield"), ShieldModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("trident"), TridentModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("decorated_pot"), DecoratedPotModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("standing_sign"), SignModelRenderer.Unbaked.CODEC);
        ID_MAPPER.put(Identifier.ofVanilla("hanging_sign"), HangingSignModelRenderer.Unbaked.CODEC);
    }

    public static Map<Block, SpecialModelRenderer<?>> buildBlockToModelTypeMap(LoadedEntityModels entityModels) {
        Map<Block, SpecialModelRenderer.Unbaked> map = new HashMap<>(BLOCK_TO_MODEL_TYPE);
        if (ChestBlockEntityRenderer.isAroundChristmas()) {
            map.put(Blocks.CHEST, CHRISTMAS_CHEST);
            map.put(Blocks.TRAPPED_CHEST, CHRISTMAS_CHEST);
        }

        Builder<Block, SpecialModelRenderer<?>> builder = ImmutableMap.builder();
        map.forEach((block, modelType) -> {
            SpecialModelRenderer<?> specialModelRenderer = modelType.bake(entityModels);
            if (specialModelRenderer != null) {
                builder.put(block, specialModelRenderer);
            }
        });
        return builder.build();
    }
}

