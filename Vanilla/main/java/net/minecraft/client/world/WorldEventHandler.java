package net.minecraft.client.world;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrushableBlock;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.MultifaceBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.SculkShriekerBlock;
import net.minecraft.block.entity.VaultBlockEntity;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.block.spawner.TrialSpawnerLogic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleUtil;
import net.minecraft.particle.SculkChargeParticleEffect;
import net.minecraft.particle.ShriekParticleEffect;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

@Environment(EnvType.CLIENT)
public class WorldEventHandler {
    private final MinecraftClient client;
    private final World world;
    private final WorldRenderer worldRenderer;
    private final Map<BlockPos, SoundInstance> playingSongs = new HashMap<>();

    public WorldEventHandler(MinecraftClient client, World world, WorldRenderer worldRenderer) {
        this.client = client;
        this.world = world;
        this.worldRenderer = worldRenderer;
    }

    public void processGlobalEvent(int eventId, BlockPos pos, int data) {
        switch (eventId) {
            case 1023:
            case 1028:
            case 1038:
                Camera camera = this.client.gameRenderer.getCamera();
                if (camera.isReady()) {
                    Vec3d vec3d = Vec3d.ofCenter(pos).subtract(camera.getPos()).normalize();
                    Vec3d vec3d2 = camera.getPos().add(vec3d.multiply(2.0));
                    if (eventId == WorldEvents.WITHER_SPAWNS) {
                        this.world.playSound(vec3d2.x, vec3d2.y, vec3d2.z, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.0F, 1.0F, false);
                    } else if (eventId == WorldEvents.END_PORTAL_OPENED) {
                        this.world.playSound(vec3d2.x, vec3d2.y, vec3d2.z, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 1.0F, 1.0F, false);
                    } else {
                        this.world.playSound(vec3d2.x, vec3d2.y, vec3d2.z, SoundEvents.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 5.0F, 1.0F, false);
                    }
                }
        }
    }

    public void processWorldEvent(int eventId, BlockPos pos, int data) {
        Random random = this.world.random;
        switch (eventId) {
            case 1000:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_DISPENSER_DISPENSE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1001:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 1.0F, 1.2F, false);
                break;
            case 1002:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.BLOCKS, 1.0F, 1.2F, false);
                break;
            case 1004:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.NEUTRAL, 1.0F, 1.2F, false);
                break;
            case 1009:
                if (data == 0) {
                    this.world
                        .playSoundAtBlockCenter(
                            pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false
                        );
                } else if (data == 1) {
                    this.world
                        .playSoundAtBlockCenter(
                            pos,
                            SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE,
                            SoundCategory.BLOCKS,
                            0.7F,
                            1.6F + (random.nextFloat() - random.nextFloat()) * 0.4F,
                            false
                        );
                }
                break;
            case 1010:
                this.world.getRegistryManager().getOrThrow(RegistryKeys.JUKEBOX_SONG).getEntry(data).ifPresent(song -> this.playJukeboxSong(song, pos));
                break;
            case 1011:
                this.stopJukeboxSongAndUpdate(pos);
                break;
            case 1015:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ENTITY_GHAST_WARN, SoundCategory.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1016:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1017:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                        SoundCategory.HOSTILE,
                        10.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        false
                    );
                break;
            case 1018:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1019:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                        SoundCategory.HOSTILE,
                        2.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        false
                    );
                break;
            case 1020:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,
                        SoundCategory.HOSTILE,
                        2.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        false
                    );
                break;
            case 1021:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundCategory.HOSTILE,
                        2.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        false
                    );
                break;
            case 1022:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1024:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1025:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ENTITY_BAT_TAKEOFF, SoundCategory.NEUTRAL, 0.05F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1026:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1027:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED,
                        SoundCategory.HOSTILE,
                        2.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        false
                    );
                break;
            case 1029:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_ANVIL_DESTROY, SoundCategory.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1030:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1031:
                this.world
                    .playSoundAtBlockCenter(pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.3F, this.world.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1032:
                this.client.getSoundManager().play(PositionedSoundInstance.ambient(SoundEvents.BLOCK_PORTAL_TRAVEL, random.nextFloat() * 0.4F + 0.8F, 0.25F));
                break;
            case 1033:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1034:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1035:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1039:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ENTITY_PHANTOM_BITE, SoundCategory.HOSTILE, 0.3F, this.world.random.nextFloat() * 0.1F + 0.9F, false
                    );
                break;
            case 1040:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED,
                        SoundCategory.HOSTILE,
                        2.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        false
                    );
                break;
            case 1041:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.ENTITY_HUSK_CONVERTED_TO_ZOMBIE,
                        SoundCategory.HOSTILE,
                        2.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        false
                    );
                break;
            case 1042:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false
                    );
                break;
            case 1043:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false
                    );
                break;
            case 1044:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 1.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false
                    );
                break;
            case 1045:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.BLOCKS, 2.0F, this.world.random.nextFloat() * 0.1F + 0.9F, false
                    );
                break;
            case 1046:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON,
                        SoundCategory.BLOCKS,
                        2.0F,
                        this.world.random.nextFloat() * 0.1F + 0.9F,
                        false
                    );
                break;
            case 1047:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON,
                        SoundCategory.BLOCKS,
                        2.0F,
                        this.world.random.nextFloat() * 0.1F + 0.9F,
                        false
                    );
                break;
            case 1048:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.ENTITY_SKELETON_CONVERTED_TO_STRAY,
                        SoundCategory.HOSTILE,
                        2.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        false
                    );
                break;
            case 1049:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_CRAFTER_CRAFT, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1050:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_CRAFTER_FAIL, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1051:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ENTITY_WIND_CHARGE_THROW, SoundCategory.BLOCKS, 0.5F, 0.4F / (this.world.getRandom().nextFloat() * 0.4F + 0.8F), false
                    );
                break;
            case 1500:
                ComposterBlock.playEffects(this.world, pos, data > 0);
                break;
            case 1501:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false
                    );

                for (int rb = 0; rb < 8; rb++) {
                    this.world
                        .addParticle(
                            ParticleTypes.LARGE_SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.2, pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0
                        );
                }
                break;
            case 1502:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT,
                        SoundCategory.BLOCKS,
                        0.5F,
                        2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F,
                        false
                    );

                for (int sb = 0; sb < 5; sb++) {
                    double tb = pos.getX() + random.nextDouble() * 0.6 + 0.2;
                    double ub = pos.getY() + random.nextDouble() * 0.6 + 0.2;
                    double vb = pos.getZ() + random.nextDouble() * 0.6 + 0.2;
                    this.world.addParticle(ParticleTypes.SMOKE, tb, ub, vb, 0.0, 0.0, 0.0);
                }
                break;
            case 1503:
                this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F, false);

                for (int wb = 0; wb < 16; wb++) {
                    double xb = pos.getX() + (5.0 + random.nextDouble() * 6.0) / 16.0;
                    double yb = pos.getY() + 0.8125;
                    double zb = pos.getZ() + (5.0 + random.nextDouble() * 6.0) / 16.0;
                    this.world.addParticle(ParticleTypes.SMOKE, xb, yb, zb, 0.0, 0.0, 0.0);
                }
                break;
            case 1504:
                PointedDripstoneBlock.createParticle(this.world, pos, this.world.getBlockState(pos));
                break;
            case 1505:
                BoneMealItem.createParticles(this.world, pos, data);
                this.world.playSoundAtBlockCenter(pos, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 2000:
                this.shootParticles(data, pos, random, ParticleTypes.SMOKE);
                break;
            case 2001:
                BlockState blockState = Block.getStateFromRawId(data);
                if (!blockState.isAir()) {
                    BlockSoundGroup blockSoundGroup = blockState.getSoundGroup();
                    this.world
                        .playSoundAtBlockCenter(
                            pos,
                            blockSoundGroup.getBreakSound(),
                            SoundCategory.BLOCKS,
                            (blockSoundGroup.getVolume() + 1.0F) / 2.0F,
                            blockSoundGroup.getPitch() * 0.8F,
                            false
                        );
                }

                this.world.addBlockBreakParticles(pos, blockState);
                break;
            case 2002:
            case 2007:
                Vec3d vec3d = Vec3d.ofBottomCenter(pos);

                for (int j = 0; j < 8; j++) {
                    this.worldRenderer
                        .addParticle(
                            new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)),
                            vec3d.x,
                            vec3d.y,
                            vec3d.z,
                            random.nextGaussian() * 0.15,
                            random.nextDouble() * 0.2,
                            random.nextGaussian() * 0.15
                        );
                }

                float h = (data >> 16 & 0xFF) / 255.0F;
                float k = (data >> 8 & 0xFF) / 255.0F;
                float l = (data >> 0 & 0xFF) / 255.0F;
                ParticleEffect particleEffect = eventId == WorldEvents.INSTANT_SPLASH_POTION_SPLASHED ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

                for (int m = 0; m < 100; m++) {
                    double n = random.nextDouble() * 4.0;
                    double o = random.nextDouble() * Math.PI * 2.0;
                    double p = Math.cos(o) * n;
                    double q = 0.01 + random.nextDouble() * 0.5;
                    double r = Math.sin(o) * n;
                    Particle particle = this.worldRenderer
                        .spawnParticle(
                            particleEffect, particleEffect.getType().shouldAlwaysSpawn(), vec3d.x + p * 0.1, vec3d.y + 0.3, vec3d.z + r * 0.1, p, q, r
                        );
                    if (particle != null) {
                        float s = 0.75F + random.nextFloat() * 0.25F;
                        particle.setColor(h * s, k * s, l * s);
                        particle.move((float)n);
                    }
                }

                this.world
                    .playSoundAtBlockCenter(pos, SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.NEUTRAL, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 2003:
                double d = pos.getX() + 0.5;
                double e = pos.getY();
                double f = pos.getZ() + 0.5;

                for (int i = 0; i < 8; i++) {
                    this.worldRenderer
                        .addParticle(
                            new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)),
                            d,
                            e,
                            f,
                            random.nextGaussian() * 0.15,
                            random.nextDouble() * 0.2,
                            random.nextGaussian() * 0.15
                        );
                }

                for (double g = 0.0; g < Math.PI * 2; g += Math.PI / 20) {
                    this.worldRenderer
                        .addParticle(ParticleTypes.PORTAL, d + Math.cos(g) * 5.0, e - 0.4, f + Math.sin(g) * 5.0, Math.cos(g) * -5.0, 0.0, Math.sin(g) * -5.0);
                    this.worldRenderer
                        .addParticle(ParticleTypes.PORTAL, d + Math.cos(g) * 5.0, e - 0.4, f + Math.sin(g) * 5.0, Math.cos(g) * -7.0, 0.0, Math.sin(g) * -7.0);
                }
                break;
            case 2004:
                for (int t = 0; t < 20; t++) {
                    double u = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
                    double v = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
                    double w = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
                    this.world.addParticle(ParticleTypes.SMOKE, u, v, w, 0.0, 0.0, 0.0);
                    this.world.addParticle(ParticleTypes.FLAME, u, v, w, 0.0, 0.0, 0.0);
                }
                break;
            case 2006:
                for (int ac = 0; ac < 200; ac++) {
                    float bc = random.nextFloat() * 4.0F;
                    float cc = random.nextFloat() * (float) (Math.PI * 2);
                    double dc = MathHelper.cos(cc) * bc;
                    double ec = 0.01 + random.nextDouble() * 0.5;
                    double fc = MathHelper.sin(cc) * bc;
                    Particle particle2 = this.worldRenderer
                        .spawnParticle(ParticleTypes.DRAGON_BREATH, false, pos.getX() + dc * 0.1, pos.getY() + 0.3, pos.getZ() + fc * 0.1, dc, ec, fc);
                    if (particle2 != null) {
                        particle2.move(bc);
                    }
                }

                if (data == 1) {
                    this.world
                        .playSoundAtBlockCenter(
                            pos, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 1.0F, random.nextFloat() * 0.1F + 0.9F, false
                        );
                }
                break;
            case 2008:
                this.world.addParticle(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
                break;
            case 2009:
                for (int gc = 0; gc < 8; gc++) {
                    this.world
                        .addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextDouble(), pos.getY() + 1.2, pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
                }
                break;
            case 2010:
                this.shootParticles(data, pos, random, ParticleTypes.WHITE_SMOKE);
                break;
            case 2011:
                ParticleUtil.spawnParticlesAround(this.world, pos, data, ParticleTypes.HAPPY_VILLAGER);
                break;
            case 2012:
                ParticleUtil.spawnParticlesAround(this.world, pos, data, ParticleTypes.HAPPY_VILLAGER);
                break;
            case 2013:
                ParticleUtil.spawnSmashAttackParticles(this.world, pos, data);
                break;
            case 3000:
                this.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, true, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.BLOCK_END_GATEWAY_SPAWN,
                        SoundCategory.BLOCKS,
                        10.0F,
                        (1.0F + (this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.2F) * 0.7F,
                        false
                    );
                break;
            case 3001:
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 64.0F, 0.8F + this.world.random.nextFloat() * 0.3F, false
                    );
                break;
            case 3002:
                if (data >= 0 && data < Direction.Axis.VALUES.length) {
                    ParticleUtil.spawnParticle(
                        Direction.Axis.VALUES[data], this.world, pos, 0.125, ParticleTypes.ELECTRIC_SPARK, UniformIntProvider.create(10, 19)
                    );
                } else {
                    ParticleUtil.spawnParticle(this.world, pos, ParticleTypes.ELECTRIC_SPARK, UniformIntProvider.create(3, 5));
                }
                break;
            case 3003:
                ParticleUtil.spawnParticle(this.world, pos, ParticleTypes.WAX_ON, UniformIntProvider.create(3, 5));
                this.world.playSoundAtBlockCenter(pos, SoundEvents.ITEM_HONEYCOMB_WAX_ON, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 3004:
                ParticleUtil.spawnParticle(this.world, pos, ParticleTypes.WAX_OFF, UniformIntProvider.create(3, 5));
                break;
            case 3005:
                ParticleUtil.spawnParticle(this.world, pos, ParticleTypes.SCRAPE, UniformIntProvider.create(3, 5));
                break;
            case 3006:
                int bb = data >> 6;
                if (bb > 0) {
                    if (random.nextFloat() < 0.3F + bb * 0.1F) {
                        float cb = 0.15F + 0.02F * bb * bb * random.nextFloat();
                        float db = 0.4F + 0.3F * bb * random.nextFloat();
                        this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_SCULK_CHARGE, SoundCategory.BLOCKS, cb, db, false);
                    }

                    byte b = (byte)(data & 63);
                    IntProvider intProvider = UniformIntProvider.create(0, bb);
                    float eb = 0.005F;
                    Supplier<Vec3d> supplier = () -> new Vec3d(
                        MathHelper.nextDouble(random, -0.005F, 0.005F),
                        MathHelper.nextDouble(random, -0.005F, 0.005F),
                        MathHelper.nextDouble(random, -0.005F, 0.005F)
                    );
                    if (b == 0) {
                        for (Direction direction : Direction.values()) {
                            float fb = direction == Direction.DOWN ? (float) Math.PI : 0.0F;
                            double gb = direction.getAxis() == Direction.Axis.Y ? 0.65 : 0.57;
                            ParticleUtil.spawnParticles(this.world, pos, new SculkChargeParticleEffect(fb), intProvider, direction, supplier, gb);
                        }
                    } else {
                        for (Direction direction2 : MultifaceBlock.flagToDirections(b)) {
                            float hb = direction2 == Direction.UP ? (float) Math.PI : 0.0F;
                            double ib = 0.35;
                            ParticleUtil.spawnParticles(this.world, pos, new SculkChargeParticleEffect(hb), intProvider, direction2, supplier, 0.35);
                        }
                    }
                } else {
                    this.world.playSoundAtBlockCenter(pos, SoundEvents.BLOCK_SCULK_CHARGE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                    boolean bl = this.world.getBlockState(pos).isFullCube(this.world, pos);
                    int jb = bl ? 40 : 20;
                    float kb = bl ? 0.45F : 0.25F;
                    float lb = 0.07F;

                    for (int mb = 0; mb < jb; mb++) {
                        float nb = 2.0F * random.nextFloat() - 1.0F;
                        float ob = 2.0F * random.nextFloat() - 1.0F;
                        float pb = 2.0F * random.nextFloat() - 1.0F;
                        this.world
                            .addParticle(
                                ParticleTypes.SCULK_CHARGE_POP,
                                pos.getX() + 0.5 + nb * kb,
                                pos.getY() + 0.5 + ob * kb,
                                pos.getZ() + 0.5 + pb * kb,
                                nb * 0.07F,
                                ob * 0.07F,
                                pb * 0.07F
                            );
                    }
                }
                break;
            case 3007:
                for (int qb = 0; qb < 10; qb++) {
                    this.world
                        .addParticle(new ShriekParticleEffect(qb * 5), pos.getX() + 0.5, pos.getY() + SculkShriekerBlock.TOP, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
                }

                BlockState blockState3 = this.world.getBlockState(pos);
                boolean bl2 = blockState3.contains(Properties.WATERLOGGED) && blockState3.get(Properties.WATERLOGGED);
                if (!bl2) {
                    this.world
                        .playSound(
                            pos.getX() + 0.5,
                            pos.getY() + SculkShriekerBlock.TOP,
                            pos.getZ() + 0.5,
                            SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK,
                            SoundCategory.BLOCKS,
                            2.0F,
                            0.6F + this.world.random.nextFloat() * 0.4F,
                            false
                        );
                }
                break;
            case 3008:
                BlockState blockState2 = Block.getStateFromRawId(data);
                if (blockState2.getBlock() instanceof BrushableBlock brushableBlock) {
                    this.world.playSoundAtBlockCenter(pos, brushableBlock.getBrushingCompleteSound(), SoundCategory.PLAYERS, 1.0F, 1.0F, false);
                }

                this.world.addBlockBreakParticles(pos, blockState2);
                break;
            case 3009:
                ParticleUtil.spawnParticle(this.world, pos, ParticleTypes.EGG_CRACK, UniformIntProvider.create(3, 6));
                break;
            case 3011:
                TrialSpawnerLogic.addMobSpawnParticles(this.world, pos, random, TrialSpawnerLogic.Type.fromIndex(data).particle);
                break;
            case 3012:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.BLOCK_TRIAL_SPAWNER_SPAWN_MOB,
                        SoundCategory.BLOCKS,
                        1.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        true
                    );
                TrialSpawnerLogic.addMobSpawnParticles(this.world, pos, random, TrialSpawnerLogic.Type.fromIndex(data).particle);
                break;
            case 3013:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER,
                        SoundCategory.BLOCKS,
                        1.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        true
                    );
                TrialSpawnerLogic.addDetectionParticles(this.world, pos, random, data, ParticleTypes.TRIAL_SPAWNER_DETECTION);
                break;
            case 3014:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.BLOCK_TRIAL_SPAWNER_EJECT_ITEM,
                        SoundCategory.BLOCKS,
                        1.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        true
                    );
                TrialSpawnerLogic.addEjectItemParticles(this.world, pos, random);
                break;
            case 3015:
                if (this.world.getBlockEntity(pos) instanceof VaultBlockEntity vaultBlockEntity) {
                    VaultBlockEntity.Client.spawnActivateParticles(
                        this.world,
                        vaultBlockEntity.getPos(),
                        vaultBlockEntity.getCachedState(),
                        vaultBlockEntity.getSharedData(),
                        data == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME
                    );
                    this.world
                        .playSoundAtBlockCenter(
                            pos, SoundEvents.BLOCK_VAULT_ACTIVATE, SoundCategory.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true
                        );
                }
                break;
            case 3016:
                VaultBlockEntity.Client.spawnDeactivateParticles(this.world, pos, data == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME);
                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.BLOCK_VAULT_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true
                    );
                break;
            case 3017:
                TrialSpawnerLogic.addEjectItemParticles(this.world, pos, random);
                break;
            case 3018:
                for (int x = 0; x < 10; x++) {
                    double y = random.nextGaussian() * 0.02;
                    double z = random.nextGaussian() * 0.02;
                    double ab = random.nextGaussian() * 0.02;
                    this.world
                        .addParticle(
                            ParticleTypes.POOF, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), y, z, ab
                        );
                }

                this.world
                    .playSoundAtBlockCenter(
                        pos, SoundEvents.BLOCK_COBWEB_PLACE, SoundCategory.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, true
                    );
                break;
            case 3019:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER,
                        SoundCategory.BLOCKS,
                        1.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        true
                    );
                TrialSpawnerLogic.addDetectionParticles(this.world, pos, random, data, ParticleTypes.TRIAL_SPAWNER_DETECTION_OMINOUS);
                break;
            case 3020:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE,
                        SoundCategory.BLOCKS,
                        data == 0 ? 0.3F : 1.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        true
                    );
                TrialSpawnerLogic.addDetectionParticles(this.world, pos, random, 0, ParticleTypes.TRIAL_SPAWNER_DETECTION_OMINOUS);
                TrialSpawnerLogic.addTrialOmenParticles(this.world, pos, random);
                break;
            case 3021:
                this.world
                    .playSoundAtBlockCenter(
                        pos,
                        SoundEvents.BLOCK_TRIAL_SPAWNER_SPAWN_ITEM,
                        SoundCategory.BLOCKS,
                        1.0F,
                        (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F,
                        true
                    );
                TrialSpawnerLogic.addMobSpawnParticles(this.world, pos, random, TrialSpawnerLogic.Type.fromIndex(data).particle);
        }
    }

    private void shootParticles(int direction, BlockPos pos, Random random, SimpleParticleType particleType) {
        Direction direction2 = Direction.byId(direction);
        int i = direction2.getOffsetX();
        int j = direction2.getOffsetY();
        int k = direction2.getOffsetZ();

        for (int l = 0; l < 10; l++) {
            double d = random.nextDouble() * 0.2 + 0.01;
            double e = pos.getX() + i * 0.6 + 0.5 + i * 0.01 + (random.nextDouble() - 0.5) * k * 0.5;
            double f = pos.getY() + j * 0.6 + 0.5 + j * 0.01 + (random.nextDouble() - 0.5) * j * 0.5;
            double g = pos.getZ() + k * 0.6 + 0.5 + k * 0.01 + (random.nextDouble() - 0.5) * i * 0.5;
            double h = i * d + random.nextGaussian() * 0.01;
            double m = j * d + random.nextGaussian() * 0.01;
            double n = k * d + random.nextGaussian() * 0.01;
            this.worldRenderer.addParticle(particleType, e, f, g, h, m, n);
        }
    }

    private void playJukeboxSong(RegistryEntry<JukeboxSong> song, BlockPos jukeboxPos) {
        this.stopJukeboxSong(jukeboxPos);
        JukeboxSong jukeboxSong = song.value();
        SoundEvent soundEvent = jukeboxSong.soundEvent().value();
        SoundInstance soundInstance = PositionedSoundInstance.record(soundEvent, Vec3d.ofCenter(jukeboxPos));
        this.playingSongs.put(jukeboxPos, soundInstance);
        this.client.getSoundManager().play(soundInstance);
        this.client.inGameHud.setRecordPlayingOverlay(jukeboxSong.description());
        this.updateEntitiesForSong(this.world, jukeboxPos, true);
    }

    private void stopJukeboxSong(BlockPos jukeboxPos) {
        SoundInstance soundInstance = this.playingSongs.remove(jukeboxPos);
        if (soundInstance != null) {
            this.client.getSoundManager().stop(soundInstance);
        }
    }

    private void stopJukeboxSongAndUpdate(BlockPos jukeboxPos) {
        this.stopJukeboxSong(jukeboxPos);
        this.updateEntitiesForSong(this.world, jukeboxPos, false);
    }

    private void updateEntitiesForSong(World world, BlockPos pos, boolean playing) {
        for (LivingEntity livingEntity : world.getNonSpectatingEntities(LivingEntity.class, new Box(pos).expand(3.0))) {
            livingEntity.setNearbySongPlaying(pos, playing);
        }
    }
}

