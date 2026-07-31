package net.minecraft.client.gui.screen.advancement;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class AdvancementWidget {
    private static final Identifier TITLE_BOX_TEXTURE = Identifier.ofVanilla("advancements/title_box");
    private static final int field_32286 = 26;
    private static final int field_32287 = 0;
    private static final int field_32288 = 200;
    private static final int field_32289 = 26;
    private static final int ICON_OFFSET_X = 8;
    private static final int ICON_OFFSET_Y = 5;
    private static final int ICON_SIZE = 26;
    private static final int field_32293 = 3;
    private static final int field_32294 = 5;
    private static final int TITLE_OFFSET_X = 32;
    private static final int TITLE_OFFSET_Y = 9;
    private static final int field_55103 = 8;
    private static final int TITLE_MAX_WIDTH = 163;
    private static final int field_55104 = 80;
    private static final int[] SPLIT_OFFSET_CANDIDATES = new int[]{0, 10, -10, 25, -25};
    private final AdvancementTab tab;
    private final PlacedAdvancement advancement;
    private final AdvancementDisplay display;
    private final List<OrderedText> title;
    private final int width;
    private final List<OrderedText> description;
    private final MinecraftClient client;
    @Nullable
    private AdvancementWidget parent;
    private final List<AdvancementWidget> children = Lists.newArrayList();
    @Nullable
    private AdvancementProgress progress;
    private final int x;
    private final int y;

    public AdvancementWidget(AdvancementTab tab, MinecraftClient client, PlacedAdvancement advancement, AdvancementDisplay display) {
        this.tab = tab;
        this.advancement = advancement;
        this.display = display;
        this.client = client;
        this.title = client.textRenderer.wrapLines(display.getTitle(), 163);
        this.x = MathHelper.floor(display.getX() * 28.0F);
        this.y = MathHelper.floor(display.getY() * 27.0F);
        int i = Math.max(this.title.stream().mapToInt(client.textRenderer::getWidth).max().orElse(0), 80);
        int j = this.getProgressWidth();
        int k = 29 + i + j;
        this.description = Language.getInstance()
            .reorder(
                this.wrapDescription(Texts.setStyleIfAbsent(display.getDescription().copy(), Style.EMPTY.withColor(display.getFrame().getTitleFormat())), k)
            );

        for (OrderedText orderedText : this.description) {
            k = Math.max(k, client.textRenderer.getWidth(orderedText));
        }

        this.width = k + 3 + 5;
    }

    private int getProgressWidth() {
        int i = this.advancement.getAdvancement().requirements().getLength();
        if (i <= 1) {
            return 0;
        }

        int j = 8;
        Text text = Text.translatable("advancements.progress", i, i);
        return this.client.textRenderer.getWidth(text) + 8;
    }

    private static float getMaxWidth(TextHandler textHandler, List<StringVisitable> lines) {
        return (float)lines.stream().mapToDouble(textHandler::getWidth).max().orElse(0.0);
    }

    private List<StringVisitable> wrapDescription(Text text, int width) {
        TextHandler textHandler = this.client.textRenderer.getTextHandler();
        List<StringVisitable> list = null;
        float f = Float.MAX_VALUE;

        for (int i : SPLIT_OFFSET_CANDIDATES) {
            List<StringVisitable> list2 = textHandler.wrapLines(text, width - i, Style.EMPTY);
            float g = Math.abs(getMaxWidth(textHandler, list2) - width);
            if (g <= 10.0F) {
                return list2;
            }

            if (g < f) {
                f = g;
                list = list2;
            }
        }

        return list;
    }

    @Nullable
    private AdvancementWidget getParent(PlacedAdvancement advancement) {
        do {
            advancement = advancement.getParent();
        } while (advancement != null && advancement.getAdvancement().display().isEmpty());

        return advancement != null && !advancement.getAdvancement().display().isEmpty() ? this.tab.getWidget(advancement.getAdvancementEntry()) : null;
    }

    public void renderLines(DrawContext context, int x, int y, boolean border) {
        if (this.parent != null) {
            int i = x + this.parent.x + 13;
            int j = x + this.parent.x + 26 + 4;
            int k = y + this.parent.y + 13;
            int l = x + this.x + 13;
            int m = y + this.y + 13;
            int n = border ? Colors.BLACK : Colors.WHITE;
            if (border) {
                context.drawHorizontalLine(j, i, k - 1, n);
                context.drawHorizontalLine(j + 1, i, k, n);
                context.drawHorizontalLine(j, i, k + 1, n);
                context.drawHorizontalLine(l, j - 1, m - 1, n);
                context.drawHorizontalLine(l, j - 1, m, n);
                context.drawHorizontalLine(l, j - 1, m + 1, n);
                context.drawVerticalLine(j - 1, m, k, n);
                context.drawVerticalLine(j + 1, m, k, n);
            } else {
                context.drawHorizontalLine(j, i, k, n);
                context.drawHorizontalLine(l, j, m, n);
                context.drawVerticalLine(j, m, k, n);
            }
        }

        for (AdvancementWidget advancementWidget : this.children) {
            advancementWidget.renderLines(context, x, y, border);
        }
    }

    public void renderWidgets(DrawContext context, int x, int y) {
        if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
            float f = this.progress == null ? 0.0F : this.progress.getProgressBarPercentage();
            AdvancementObtainedStatus advancementObtainedStatus;
            if (f >= 1.0F) {
                advancementObtainedStatus = AdvancementObtainedStatus.OBTAINED;
            } else {
                advancementObtainedStatus = AdvancementObtainedStatus.UNOBTAINED;
            }

            context.drawGuiTexture(
                RenderLayer::getGuiTextured, advancementObtainedStatus.getFrameTexture(this.display.getFrame()), x + this.x + 3, y + this.y, 26, 26
            );
            context.drawItemWithoutEntity(this.display.getIcon(), x + this.x + 8, y + this.y + 5);
        }

        for (AdvancementWidget advancementWidget : this.children) {
            advancementWidget.renderWidgets(context, x, y);
        }
    }

    public int getWidth() {
        return this.width;
    }

    public void setProgress(AdvancementProgress progress) {
        this.progress = progress;
    }

    public void addChild(AdvancementWidget widget) {
        this.children.add(widget);
    }

    public void drawTooltip(DrawContext context, int originX, int originY, float alpha, int x, int y) {
        TextRenderer textRenderer = this.client.textRenderer;
        int i = 9 * this.title.size() + 9 + 8;
        int j = originY + this.y + (26 - i) / 2;
        int k = j + i;
        int l = this.description.size() * 9;
        int m = 6 + l;
        boolean bl = x + originX + this.x + this.width + 26 >= this.tab.getScreen().width;
        Text text = this.progress == null ? null : this.progress.getProgressBarFraction();
        int n = text == null ? 0 : textRenderer.getWidth(text);
        boolean bl2 = k + m >= 113;
        float f = this.progress == null ? 0.0F : this.progress.getProgressBarPercentage();
        int o = MathHelper.floor(f * this.width);
        AdvancementObtainedStatus advancementObtainedStatus;
        AdvancementObtainedStatus advancementObtainedStatus2;
        AdvancementObtainedStatus advancementObtainedStatus3;
        if (f >= 1.0F) {
            o = this.width / 2;
            advancementObtainedStatus = AdvancementObtainedStatus.OBTAINED;
            advancementObtainedStatus2 = AdvancementObtainedStatus.OBTAINED;
            advancementObtainedStatus3 = AdvancementObtainedStatus.OBTAINED;
        } else if (o < 2) {
            o = this.width / 2;
            advancementObtainedStatus = AdvancementObtainedStatus.UNOBTAINED;
            advancementObtainedStatus2 = AdvancementObtainedStatus.UNOBTAINED;
            advancementObtainedStatus3 = AdvancementObtainedStatus.UNOBTAINED;
        } else if (o > this.width - 2) {
            o = this.width / 2;
            advancementObtainedStatus = AdvancementObtainedStatus.OBTAINED;
            advancementObtainedStatus2 = AdvancementObtainedStatus.OBTAINED;
            advancementObtainedStatus3 = AdvancementObtainedStatus.UNOBTAINED;
        } else {
            advancementObtainedStatus = AdvancementObtainedStatus.OBTAINED;
            advancementObtainedStatus2 = AdvancementObtainedStatus.UNOBTAINED;
            advancementObtainedStatus3 = AdvancementObtainedStatus.UNOBTAINED;
        }

        int p = this.width - o;
        int q;
        if (bl) {
            q = originX + this.x - this.width + 26 + 6;
        } else {
            q = originX + this.x;
        }

        int s = i + m;
        if (!this.description.isEmpty()) {
            if (bl2) {
                context.drawGuiTexture(RenderLayer::getGuiTextured, TITLE_BOX_TEXTURE, q, k - s, this.width, s);
            } else {
                context.drawGuiTexture(RenderLayer::getGuiTextured, TITLE_BOX_TEXTURE, q, j, this.width, s);
            }
        }

        if (advancementObtainedStatus != advancementObtainedStatus2) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, advancementObtainedStatus.getBoxTexture(), 200, i, 0, 0, q, j, o, i);
            context.drawGuiTexture(RenderLayer::getGuiTextured, advancementObtainedStatus2.getBoxTexture(), 200, i, 200 - p, 0, q + o, j, p, i);
        } else {
            context.drawGuiTexture(RenderLayer::getGuiTextured, advancementObtainedStatus.getBoxTexture(), q, j, this.width, i);
        }

        context.drawGuiTexture(
            RenderLayer::getGuiTextured, advancementObtainedStatus3.getFrameTexture(this.display.getFrame()), originX + this.x + 3, originY + this.y, 26, 26
        );
        int t = q + 5;
        if (bl) {
            this.drawText(context, this.title, t, j + 9, -1);
            if (text != null) {
                context.drawTextWithShadow(textRenderer, text, originX + this.x - n, j + 9, Colors.WHITE);
            }
        } else {
            this.drawText(context, this.title, originX + this.x + 32, j + 9, -1);
            if (text != null) {
                context.drawTextWithShadow(textRenderer, text, originX + this.x + this.width - n - 5, j + 9, Colors.WHITE);
            }
        }

        if (bl2) {
            this.drawText(context, this.description, t, j - l + 1, -16711936);
        } else {
            this.drawText(context, this.description, t, k, -16711936);
        }

        context.drawItemWithoutEntity(this.display.getIcon(), originX + this.x + 8, originY + this.y + 5);
    }

    private void drawText(DrawContext context, List<OrderedText> text, int x, int y, int color) {
        TextRenderer textRenderer = this.client.textRenderer;

        for (int i = 0; i < text.size(); i++) {
            context.drawTextWithShadow(textRenderer, text.get(i), x, y + i * 9, color);
        }
    }

    public boolean shouldRender(int originX, int originY, int mouseX, int mouseY) {
        if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
            int i = originX + this.x;
            int j = i + 26;
            int k = originY + this.y;
            int l = k + 26;
            return mouseX >= i && mouseX <= j && mouseY >= k && mouseY <= l;
        } else {
            return false;
        }
    }

    public void addToTree() {
        if (this.parent == null && this.advancement.getParent() != null) {
            this.parent = this.getParent(this.advancement);
            if (this.parent != null) {
                this.parent.addChild(this);
            }
        }
    }

    public int getY() {
        return this.y;
    }

    public int getX() {
        return this.x;
    }
}

