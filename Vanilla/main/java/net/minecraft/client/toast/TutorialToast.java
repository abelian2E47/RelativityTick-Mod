package net.minecraft.client.toast;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class TutorialToast implements Toast {
    private static final Identifier TEXTURE = Identifier.ofVanilla("toast/tutorial");
    public static final int PROGRESS_BAR_WIDTH = 154;
    public static final int PROGRESS_BAR_HEIGHT = 1;
    public static final int PROGRESS_BAR_X = 3;
    public static final int field_55091 = 4;
    private static final int field_55092 = 7;
    private static final int field_55093 = 3;
    private static final int field_55094 = 11;
    private static final int field_55095 = 30;
    private static final int field_55096 = 126;
    private final TutorialToast.Type type;
    private final List<OrderedText> text;
    private Toast.Visibility visibility = Toast.Visibility.SHOW;
    private long lastTime;
    private float lastProgress;
    private float progress;
    private final boolean hasProgressBar;
    private final int displayDuration;

    public TutorialToast(TextRenderer textRenderer, TutorialToast.Type type, Text title, @Nullable Text description, boolean hasProgressBar, int i) {
        this.type = type;
        this.text = new ArrayList<>(2);
        this.text.addAll(textRenderer.wrapLines(title.copy().withColor(Colors.PURPLE), 126));
        if (description != null) {
            this.text.addAll(textRenderer.wrapLines(description, 126));
        }

        this.hasProgressBar = hasProgressBar;
        this.displayDuration = i;
    }

    public TutorialToast(TextRenderer textRenderer, TutorialToast.Type type, Text title, @Nullable Text description, boolean hasProgressBar) {
        this(textRenderer, type, title, description, hasProgressBar, 0);
    }

    @Override
    public Toast.Visibility getVisibility() {
        return this.visibility;
    }

    @Override
    public void update(ToastManager manager, long time) {
        if (this.displayDuration > 0) {
            this.progress = Math.min((float)time / this.displayDuration, 1.0F);
            this.lastProgress = this.progress;
            this.lastTime = time;
            if (time > this.displayDuration) {
                this.hide();
            }
        } else if (this.hasProgressBar) {
            this.lastProgress = MathHelper.clampedLerp(this.lastProgress, this.progress, (float)(time - this.lastTime) / 100.0F);
            this.lastTime = time;
        }
    }

    @Override
    public int getHeight() {
        return 7 + this.getTextHeight() + 3;
    }

    private int getTextHeight() {
        return Math.max(this.text.size(), 2) * 11;
    }

    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
        int i = this.getHeight();
        context.drawGuiTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, this.getWidth(), i);
        this.type.drawIcon(context, 6, 6);
        int j = this.text.size() * 11;
        int k = 7 + (this.getTextHeight() - j) / 2;

        for (int l = 0; l < this.text.size(); l++) {
            context.drawText(textRenderer, this.text.get(l), 30, k + l * 11, -16777216, false);
        }

        if (this.hasProgressBar) {
            int m = i - 4;
            context.fill(3, m, 157, m + 1, -1);
            int n;
            if (this.progress >= this.lastProgress) {
                n = -16755456;
            } else {
                n = -11206656;
            }

            context.fill(3, m, (int)(3.0F + 154.0F * this.lastProgress), m + 1, n);
        }
    }

    public void hide() {
        this.visibility = Toast.Visibility.HIDE;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    @Environment(EnvType.CLIENT)
    public enum Type {
        MOVEMENT_KEYS(Identifier.ofVanilla("toast/movement_keys")),
        MOUSE(Identifier.ofVanilla("toast/mouse")),
        TREE(Identifier.ofVanilla("toast/tree")),
        RECIPE_BOOK(Identifier.ofVanilla("toast/recipe_book")),
        WOODEN_PLANKS(Identifier.ofVanilla("toast/wooden_planks")),
        SOCIAL_INTERACTIONS(Identifier.ofVanilla("toast/social_interactions")),
        RIGHT_CLICK(Identifier.ofVanilla("toast/right_click"));

        private final Identifier texture;

        Type(final Identifier texture) {
            this.texture = texture;
        }

        public void drawIcon(DrawContext context, int x, int y) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, this.texture, x, y, 20, 20);
        }
    }
}

