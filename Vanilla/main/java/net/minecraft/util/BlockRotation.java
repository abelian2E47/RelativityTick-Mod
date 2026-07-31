package net.minecraft.util;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.DirectionTransformation;
import net.minecraft.util.math.random.Random;

public enum BlockRotation implements StringIdentifiable {
    NONE("none", DirectionTransformation.IDENTITY),
    CLOCKWISE_90("clockwise_90", DirectionTransformation.ROT_90_Y_NEG),
    CLOCKWISE_180("180", DirectionTransformation.ROT_180_FACE_XZ),
    COUNTERCLOCKWISE_90("counterclockwise_90", DirectionTransformation.ROT_90_Y_POS);

    public static final Codec<BlockRotation> CODEC = StringIdentifiable.createCodec(BlockRotation::values);
    private final String id;
    private final DirectionTransformation directionTransformation;

    BlockRotation(final String id, final DirectionTransformation directionTransformation) {
        this.id = id;
        this.directionTransformation = directionTransformation;
    }

    public BlockRotation rotate(BlockRotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> {
                switch (this) {
                    case NONE:
                        yield CLOCKWISE_90;
                    case CLOCKWISE_90:
                        yield CLOCKWISE_180;
                    case CLOCKWISE_180:
                        yield COUNTERCLOCKWISE_90;
                    case COUNTERCLOCKWISE_90:
                        yield NONE;
                    default:
                        throw new MatchException(null, null);
                }
            }
            case CLOCKWISE_180 -> {
                switch (this) {
                    case NONE:
                        yield CLOCKWISE_180;
                    case CLOCKWISE_90:
                        yield COUNTERCLOCKWISE_90;
                    case CLOCKWISE_180:
                        yield NONE;
                    case COUNTERCLOCKWISE_90:
                        yield CLOCKWISE_90;
                    default:
                        throw new MatchException(null, null);
                }
            }
            case COUNTERCLOCKWISE_90 -> {
                switch (this) {
                    case NONE:
                        yield COUNTERCLOCKWISE_90;
                    case CLOCKWISE_90:
                        yield NONE;
                    case CLOCKWISE_180:
                        yield CLOCKWISE_90;
                    case COUNTERCLOCKWISE_90:
                        yield CLOCKWISE_180;
                    default:
                        throw new MatchException(null, null);
                }
            }
            default -> this;
        };
    }

    public DirectionTransformation getDirectionTransformation() {
        return this.directionTransformation;
    }

    public Direction rotate(Direction direction) {
        if (direction.getAxis() == Direction.Axis.Y) {
            return direction;
        }

        return switch (this) {
            case CLOCKWISE_90 -> direction.rotateYClockwise();
            case CLOCKWISE_180 -> direction.getOpposite();
            case COUNTERCLOCKWISE_90 -> direction.rotateYCounterclockwise();
            default -> direction;
        };
    }

    public int rotate(int rotation, int fullTurn) {
        return switch (this) {
            case CLOCKWISE_90 -> (rotation + fullTurn / 4) % fullTurn;
            case CLOCKWISE_180 -> (rotation + fullTurn / 2) % fullTurn;
            case COUNTERCLOCKWISE_90 -> (rotation + fullTurn * 3 / 4) % fullTurn;
            default -> rotation;
        };
    }

    public static BlockRotation random(Random random) {
        return Util.getRandom(values(), random);
    }

    public static List<BlockRotation> randomRotationOrder(Random random) {
        return Util.copyShuffled(values(), random);
    }

    @Override
    public String asString() {
        return this.id;
    }
}

