package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;

public record DyedColorComponent(int rgb, boolean showInTooltip) implements TooltipAppender {
    private static final Codec<DyedColorComponent> BASE_CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                Codec.INT.fieldOf("rgb").forGetter(DyedColorComponent::rgb),
                Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(DyedColorComponent::showInTooltip)
            )
            .apply(instance, DyedColorComponent::new)
    );
    public static final Codec<DyedColorComponent> CODEC = Codec.withAlternative(BASE_CODEC, Codec.INT, rgb -> new DyedColorComponent(rgb, true));
    public static final PacketCodec<ByteBuf, DyedColorComponent> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.INTEGER, DyedColorComponent::rgb, PacketCodecs.BOOLEAN, DyedColorComponent::showInTooltip, DyedColorComponent::new
    );
    public static final int DEFAULT_COLOR = -6265536;

    public static int getColor(ItemStack stack, int defaultColor) {
        DyedColorComponent dyedColorComponent = stack.get(DataComponentTypes.DYED_COLOR);
        return dyedColorComponent != null ? ColorHelper.fullAlpha(dyedColorComponent.rgb()) : defaultColor;
    }

    public static ItemStack setColor(ItemStack stack, List<DyeItem> dyes) {
        if (!stack.isIn(ItemTags.DYEABLE)) {
            return ItemStack.EMPTY;
        }

        ItemStack itemStack = stack.copyWithCount(1);
        int i = 0;
        int j = 0;
        int k = 0;
        int l = 0;
        int m = 0;
        DyedColorComponent dyedColorComponent = itemStack.get(DataComponentTypes.DYED_COLOR);
        if (dyedColorComponent != null) {
            int n = ColorHelper.getRed(dyedColorComponent.rgb());
            int o = ColorHelper.getGreen(dyedColorComponent.rgb());
            int p = ColorHelper.getBlue(dyedColorComponent.rgb());
            l += Math.max(n, Math.max(o, p));
            i += n;
            j += o;
            k += p;
            m++;
        }

        for (DyeItem dyeItem : dyes) {
            int q = dyeItem.getColor().getEntityColor();
            int r = ColorHelper.getRed(q);
            int s = ColorHelper.getGreen(q);
            int t = ColorHelper.getBlue(q);
            l += Math.max(r, Math.max(s, t));
            i += r;
            j += s;
            k += t;
            m++;
        }

        int u = i / m;
        int v = j / m;
        int w = k / m;
        float f = (float)l / m;
        float g = Math.max(u, Math.max(v, w));
        u = (int)(u * f / g);
        v = (int)(v * f / g);
        w = (int)(w * f / g);
        int x = ColorHelper.getArgb(0, u, v, w);
        boolean bl = dyedColorComponent == null || dyedColorComponent.showInTooltip();
        itemStack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(x, bl));
        return itemStack;
    }

    @Override
    public void appendTooltip(Item.TooltipContext context, Consumer<Text> tooltip, TooltipType type) {
        if (this.showInTooltip) {
            if (type.isAdvanced()) {
                tooltip.accept(Text.translatable("item.color", String.format(Locale.ROOT, "#%06X", this.rgb)).formatted(Formatting.GRAY));
            } else {
                tooltip.accept(Text.translatable("item.dyed").formatted(Formatting.GRAY, Formatting.ITALIC));
            }
        }
    }

    public DyedColorComponent withShowInTooltip(boolean showInTooltip) {
        return new DyedColorComponent(this.rgb, showInTooltip);
    }
}

