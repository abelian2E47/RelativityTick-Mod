package net.minecraft.client.gui.widget;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public abstract class EntryListWidget<E extends EntryListWidget.Entry<E>> extends ContainerWidget {
    private static final Identifier MENU_LIST_BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/menu_list_background.png");
    private static final Identifier INWORLD_MENU_LIST_BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/inworld_menu_list_background.png");
    protected final MinecraftClient client;
    protected final int itemHeight;
    private final List<E> children = new EntryListWidget.Entries();
    protected boolean centerListVertically = true;
    private boolean renderHeader;
    protected int headerHeight;
    @Nullable
    private E selected;
    @Nullable
    private E hoveredEntry;

    public EntryListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
        super(0, y, width, height, ScreenTexts.EMPTY);
        this.client = client;
        this.itemHeight = itemHeight;
    }

    public EntryListWidget(MinecraftClient client, int width, int height, int y, int itemHeight, int headerHeight) {
        this(client, width, height, y, itemHeight);
        this.renderHeader = true;
        this.headerHeight = headerHeight;
    }

    /**
     * {@return the selected entry of this entry list, or {@code null} if there is none}
     */
    @Nullable
    public E getSelectedOrNull() {
        return this.selected;
    }

    public void setSelected(int index) {
        if (index == -1) {
            this.setSelected(null);
        } else if (this.getEntryCount() != 0) {
            this.setSelected(this.getEntry(index));
        }
    }

    public void setSelected(@Nullable E entry) {
        this.selected = entry;
    }

    public E getFirst() {
        return this.children.get(0);
    }

    @Nullable
    public E getFocused() {
        return (E)super.getFocused();
    }

    @Override
    public final List<E> children() {
        return this.children;
    }

    protected void clearEntries() {
        this.children.clear();
        this.selected = null;
    }

    public void replaceEntries(Collection<E> newEntries) {
        this.clearEntries();
        this.children.addAll(newEntries);
    }

    protected E getEntry(int index) {
        return this.children().get(index);
    }

    protected int addEntry(E entry) {
        this.children.add(entry);
        return this.children.size() - 1;
    }

    protected void addEntryToTop(E entry) {
        double d = this.getMaxScrollY() - this.getScrollY();
        this.children.add(0, entry);
        this.setScrollY(this.getMaxScrollY() - d);
    }

    protected boolean removeEntryWithoutScrolling(E entry) {
        double d = this.getMaxScrollY() - this.getScrollY();
        boolean bl = this.removeEntry(entry);
        this.setScrollY(this.getMaxScrollY() - d);
        return bl;
    }

    protected int getEntryCount() {
        return this.children().size();
    }

    protected boolean isSelectedEntry(int index) {
        return Objects.equals(this.getSelectedOrNull(), this.children().get(index));
    }

    @Nullable
    protected final E getEntryAtPosition(double x, double y) {
        int i = this.getRowWidth() / 2;
        int j = this.getX() + this.width / 2;
        int k = j - i;
        int l = j + i;
        int m = MathHelper.floor(y - this.getY()) - this.headerHeight + (int)this.getScrollY() - 4;
        int n = m / this.itemHeight;
        return x >= k && x <= l && n >= 0 && m >= 0 && n < this.getEntryCount() ? this.children().get(n) : null;
    }

    public void position(int width, ThreePartsLayoutWidget layout) {
        this.position(width, layout.getContentHeight(), layout.getHeaderHeight());
    }

    public void position(int width, int height, int y) {
        this.setDimensions(width, height);
        this.setPosition(0, y);
        this.refreshScroll();
    }

    @Override
    protected int getContentsHeightWithPadding() {
        return this.getEntryCount() * this.itemHeight + this.headerHeight + 4;
    }

    protected void renderHeader(DrawContext context, int x, int y) {
    }

    protected void renderDecorations(DrawContext context, int mouseX, int mouseY) {
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        this.hoveredEntry = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;
        this.drawMenuListBackground(context);
        this.enableScissor(context);
        if (this.renderHeader) {
            int i = this.getRowLeft();
            int j = this.getY() + 4 - (int)this.getScrollY();
            this.renderHeader(context, i, j);
        }

        this.renderList(context, mouseX, mouseY, delta);
        context.disableScissor();
        this.drawHeaderAndFooterSeparators(context);
        this.drawScrollbar(context);
        this.renderDecorations(context, mouseX, mouseY);
    }

    protected void drawHeaderAndFooterSeparators(DrawContext context) {
        Identifier identifier = this.client.world == null ? Screen.HEADER_SEPARATOR_TEXTURE : Screen.INWORLD_HEADER_SEPARATOR_TEXTURE;
        Identifier identifier2 = this.client.world == null ? Screen.FOOTER_SEPARATOR_TEXTURE : Screen.INWORLD_FOOTER_SEPARATOR_TEXTURE;
        context.drawTexture(RenderLayer::getGuiTextured, identifier, this.getX(), this.getY() - 2, 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
        context.drawTexture(RenderLayer::getGuiTextured, identifier2, this.getX(), this.getBottom(), 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
    }

    protected void drawMenuListBackground(DrawContext context) {
        Identifier identifier = this.client.world == null ? MENU_LIST_BACKGROUND_TEXTURE : INWORLD_MENU_LIST_BACKGROUND_TEXTURE;
        context.drawTexture(
            RenderLayer::getGuiTextured,
            identifier,
            this.getX(),
            this.getY(),
            this.getRight(),
            this.getBottom() + (int)this.getScrollY(),
            this.getWidth(),
            this.getHeight(),
            32,
            32
        );
    }

    protected void enableScissor(DrawContext context) {
        context.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
    }

    protected void centerScrollOn(E entry) {
        this.setScrollY(this.children().indexOf(entry) * this.itemHeight + this.itemHeight / 2 - this.height / 2);
    }

    protected void ensureVisible(E entry) {
        int i = this.getRowTop(this.children().indexOf(entry));
        int j = i - this.getY() - 4 - this.itemHeight;
        if (j < 0) {
            this.scroll(j);
        }

        int k = this.getBottom() - i - this.itemHeight - this.itemHeight;
        if (k < 0) {
            this.scroll(-k);
        }
    }

    private void scroll(int amount) {
        this.setScrollY(this.getScrollY() + amount);
    }

    @Override
    protected double getDeltaYPerScroll() {
        return this.itemHeight / 2.0;
    }

    @Override
    protected int getScrollbarX() {
        return this.getRowRight() + 6 + 2;
    }

    @Override
    public Optional<Element> hoveredElement(double mouseX, double mouseY) {
        return Optional.ofNullable(this.getEntryAtPosition(mouseX, mouseY));
    }

    @Override
    public void setFocused(@Nullable Element focused) {
        E entry = this.getFocused();
        if (entry != focused && entry instanceof ParentElement parentElement) {
            parentElement.setFocused(null);
        }

        super.setFocused(focused);
        int i = this.children.indexOf(focused);
        if (i >= 0) {
            E entry2 = this.children.get(i);
            this.setSelected(entry2);
            if (this.client.getNavigationType().isKeyboard()) {
                this.ensureVisible(entry2);
            }
        }
    }

    @Nullable
    protected E getNeighboringEntry(NavigationDirection direction) {
        return this.getNeighboringEntry(direction, entry -> true);
    }

    @Nullable
    protected E getNeighboringEntry(NavigationDirection direction, Predicate<E> predicate) {
        return this.getNeighboringEntry(direction, predicate, this.getSelectedOrNull());
    }

    @Nullable
    protected E getNeighboringEntry(NavigationDirection direction, Predicate<E> predicate, @Nullable E selected) {
        int i = switch (direction) {
            case RIGHT, LEFT -> 0;
            case UP -> -1;
            case DOWN -> 1;
        };
        if (!this.children().isEmpty() && i != 0) {
            int j;
            if (selected == null) {
                j = i > 0 ? 0 : this.children().size() - 1;
            } else {
                j = this.children().indexOf(selected) + i;
            }

            for (int l = j; l >= 0 && l < this.children.size(); l += i) {
                E entry = this.children().get(l);
                if (predicate.test(entry)) {
                    return entry;
                }
            }
        }

        return null;
    }

    protected void renderList(DrawContext context, int mouseX, int mouseY, float delta) {
        int i = this.getRowLeft();
        int j = this.getRowWidth();
        int k = this.itemHeight - 4;
        int l = this.getEntryCount();

        for (int m = 0; m < l; m++) {
            int n = this.getRowTop(m);
            int o = this.getRowBottom(m);
            if (o >= this.getY() && n <= this.getBottom()) {
                this.renderEntry(context, mouseX, mouseY, delta, m, i, n, j, k);
            }
        }
    }

    protected void renderEntry(DrawContext context, int mouseX, int mouseY, float delta, int index, int x, int y, int entryWidth, int entryHeight) {
        E entry = this.getEntry(index);
        entry.drawBorder(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, Objects.equals(this.hoveredEntry, entry), delta);
        if (this.isSelectedEntry(index)) {
            int i = this.isFocused() ? -1 : -8355712;
            this.drawSelectionHighlight(context, y, entryWidth, entryHeight, i, -16777216);
        }

        entry.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, Objects.equals(this.hoveredEntry, entry), delta);
    }

    protected void drawSelectionHighlight(DrawContext context, int y, int entryWidth, int entryHeight, int borderColor, int fillColor) {
        int i = this.getX() + (this.width - entryWidth) / 2;
        int j = this.getX() + (this.width + entryWidth) / 2;
        context.fill(i, y - 2, j, y + entryHeight + 2, borderColor);
        context.fill(i + 1, y - 1, j - 1, y + entryHeight + 1, fillColor);
    }

    public int getRowLeft() {
        return this.getX() + this.width / 2 - this.getRowWidth() / 2 + 2;
    }

    public int getRowRight() {
        return this.getRowLeft() + this.getRowWidth();
    }

    public int getRowTop(int index) {
        return this.getY() + 4 - (int)this.getScrollY() + index * this.itemHeight + this.headerHeight;
    }

    public int getRowBottom(int index) {
        return this.getRowTop(index) + this.itemHeight;
    }

    public int getRowWidth() {
        return 220;
    }

    @Override
    public Selectable.SelectionType getType() {
        if (this.isFocused()) {
            return Selectable.SelectionType.FOCUSED;
        } else {
            return this.hoveredEntry != null ? Selectable.SelectionType.HOVERED : Selectable.SelectionType.NONE;
        }
    }

    @Nullable
    protected E remove(int index) {
        E entry = this.children.get(index);
        return this.removeEntry(this.children.get(index)) ? entry : null;
    }

    protected boolean removeEntry(E entry) {
        boolean bl = this.children.remove(entry);
        if (bl && entry == this.getSelectedOrNull()) {
            this.setSelected(null);
        }

        return bl;
    }

    @Nullable
    protected E getHoveredEntry() {
        return this.hoveredEntry;
    }

    void setEntryParentList(EntryListWidget.Entry<E> entry) {
        entry.parentList = this;
    }

    protected void appendNarrations(NarrationMessageBuilder builder, E entry) {
        List<E> list = this.children();
        if (list.size() > 1) {
            int i = list.indexOf(entry);
            if (i != -1) {
                builder.put(NarrationPart.POSITION, Text.translatable("narrator.position.list", i + 1, list.size()));
            }
        }
    }

    @Environment(EnvType.CLIENT)
    class Entries extends AbstractList<E> {
        private final List<E> entries = Lists.newArrayList();

        public E get(int i) {
            return this.entries.get(i);
        }

        @Override
        public int size() {
            return this.entries.size();
        }

        public E set(int i, E entry) {
            E entry2 = this.entries.set(i, entry);
            EntryListWidget.this.setEntryParentList(entry);
            return entry2;
        }

        public void add(int i, E entry) {
            this.entries.add(i, entry);
            EntryListWidget.this.setEntryParentList(entry);
        }

        public E remove(int i) {
            return this.entries.remove(i);
        }
    }

    @Environment(EnvType.CLIENT)
    protected abstract static class Entry<E extends EntryListWidget.Entry<E>> implements Element {
        @Deprecated
        EntryListWidget<E> parentList;

        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return this.parentList.getFocused() == this;
        }

        /**
         * Renders an entry in a list.
         * 
         * @param index the index of the entry
         * @param y the Y coordinate of the entry
         * @param x the X coordinate of the entry
         * @param entryWidth the width of the entry
         * @param entryHeight the height of the entry
         * @param mouseX the X coordinate of the mouse
         * @param mouseY the Y coordinate of the mouse
         * @param hovered whether the mouse is hovering over the entry
         */
        public abstract void render(
            DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
        );

        public void drawBorder(
            DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
        ) {
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return Objects.equals(this.parentList.getEntryAtPosition(mouseX, mouseY), this);
        }
    }
}

