package com.abelian.client;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.config.RelativityTickClientConfig;
import com.abelian.client.gui.IconButtonWidget;
import com.abelian.client.gui.PanelButton;
import com.abelian.client.gui.PanelTextField;
import com.abelian.client.gui.RegionPanelDraw;
import com.abelian.client.gui.RegionPanelScreen;
import com.abelian.client.gui.RegionPanelStyle;
import com.abelian.client.gui.RegionPanelText;
import com.abelian.regionTick.RegionTickManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// 区域执行面板：一行一个区域，列序为区域名（含区块数/速率）→ 状态徽章 → 运行/冻结 → 步进 → 冲刺 → 配置 → 监测
public class RegionManagementScreen extends RegionPanelScreen {
    private static final int ROW_HEIGHT = 30;
    private static final int MAX_VISIBLE_ROWS = 8;

    private static final int ACTION_WIDTH = 52;
    private static final int SPRINT_WIDTH = 44;
    private static final int STEP_MIN_WIDTH = 46;
    private static final int GEAR_WIDTH = 22;
    private static final int MONITOR_WIDTH = 24;
    private static final int GAP = 8;

    private static final int BADGE_GAP = 12;
    private static final int MIN_NAME_WIDTH = 40;

    /** 筛选框自己画圆角外框，内层文字只占 8px，所以这里给的是"外框"高度。 */
    private static final int FIELD_HEIGHT = 18;
    private static final int CLEAR_WIDTH = 24;
    private static final int TOOLBAR_GAP = 8;
    /** 筛选框右下角那个小叉的边长；字号量不出图标，只能按行高给一个不会盖住文字的尺寸。 */
    private static final int CLEAR_GLYPH = 11;

    private static final int ROW_TOP;
    private static final int LIST_PADDING = 12;

    static {
        int toolbarTop = RegionPanelDraw.HEADER_HEIGHT + 6;
        ROW_TOP = toolbarTop + FIELD_HEIGHT + 10;
    }

    /** 全部区域，唯一的真值来源；筛选只影响 {@link #view}，批量操作照样作用于全集。 */
    private final List<ClientRegion> regions = new ArrayList<>();
    /** 当前显示的（筛过的）区域。 */
    private final List<ClientRegion> view = new ArrayList<>();

    private final Map<String, ButtonWidget> freezeButtons = new LinkedHashMap<>();
    private final Map<String, ButtonWidget> stepButtons = new LinkedHashMap<>();
    private final Map<String, ButtonWidget> sprintButtons = new LinkedHashMap<>();

    private Panel panel = Panel.EMPTY;
    private PanelTextField filterField;
    private ButtonWidget filterClearButton;
    private ButtonWidget toggleBatchButton;
    private ButtonWidget freezeBatchButton;
    /** 上一次 tick 看到的列表长相；相等就说明不用重建控件。 */
    private int viewSignature;

    private int firstRow;
    private int scroll;
    /** 键盘/点击选中的那一行；批量操作只作用于它，不受筛选影响。 */
    private String selectedId;
    private String filter = "";

    public RegionManagementScreen() {
        super(Text.translatable("screen.relativitytick.region_panel"));
        refreshView();
        viewSignature = signature();
    }

    @Override
    protected void init() {
        panel = layout();
        rebuildRows();
        // 工具栏在行之后重建：控件顺序决定 Tab 焦点顺序，齿轮必须排在最前
        rebuildToolbar();
    }

    @Override
    public void tick() {
        if (signature() != viewSignature) {
            //列表变了要重算行数（行数影响面板高度），顺带把控件全换一遍
            refreshView();
            panel = layout();
            rebuildRows();
            rebuildToolbar();
            viewSignature = signature();
            return;
        }
        //列表没变：区域数据每 tick 都在变，按钮文案要跟着服务端状态走，但不必重建控件
        for (Map.Entry<String, ButtonWidget> entry : freezeButtons.entrySet()) {
            ClientRegion region = find(entry.getKey());
            if (region != null) {
                entry.getValue().setMessage(freezeLabel(region));
                entry.getValue().setTooltip(Tooltip.of(freezeTooltip(region)));
            }
        }
        for (Map.Entry<String, ButtonWidget> entry : sprintButtons.entrySet()) {
            ClientRegion region = find(entry.getKey());
            if (region != null) {
                entry.getValue().setMessage(sprintLabel(region));
                entry.getValue().setTooltip(Tooltip.of(sprintTooltip(region)));
                entry.getValue().active = !region.isReleased();
            }
        }
        for (Map.Entry<String, ButtonWidget> entry : stepButtons.entrySet()) {
            ClientRegion region = find(entry.getKey());
            if (region != null) {
                //文案里的步长是面板设置里的值，改完不重建控件也要跟着变
                entry.getValue().setMessage(stepLabel());
                entry.getValue().setTooltip(Tooltip.of(stepTooltip()));
                entry.getValue().active = canStep(region);
            }
        }
        refreshBatchActive();
    }

    // ---------------------------------------------------------------- 列表数据

    private void refreshView() {
        regions.clear();
        regions.addAll(ClientRegionManager.getRegions());
        regions.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));

        view.clear();
        for (ClientRegion region : regions) {
            if (matchesFilter(region)) view.add(region);
        }
    }

    // 列表长相的指纹：id 顺序 + 滚动位置 + 筛选词
    private int signature() {
        int hash = firstRow * 31 + filter.hashCode();
        for (ClientRegion region : view) {
            hash = hash * 31 + region.getId().hashCode();
        }
        return hash * 31 + view.size();
    }

    private ClientRegion find(String id) {
        if (id == null) return null;
        for (ClientRegion region : regions) {
            if (region.getId().equals(id)) return region;
        }
        return null;
    }

    /** 筛选的命中范围比看到的字宽：输入 "mine" 要能命中 "minecraft:overworld" 这种带命名空间的 id。 */
    private boolean matchesFilter(ClientRegion region) {
        if (filter.isEmpty()) return true;
        String needle = filter.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return true;
        String state = RegionPanelStyle.stateKey(region.getRegionState()).toLowerCase(Locale.ROOT);
        return region.getId().toLowerCase(Locale.ROOT).contains(needle)
                || region.getDimension().toLowerCase(Locale.ROOT).contains(needle)
                || state.contains(needle);
    }

    // ---------------------------------------------------------------- 布局

    private record Panel(int x, int y, int width, int height,
                         int toolbarY, int fieldX, int fieldWidth, int clearX,
                         int rowTop, int rowBottom, int rowCount,
                         int nameX, int nameWidth,
                         int badgeX, int actionX, int stepX, int stepWidth, int sprintX,
                         int gearX, int monitorX) {
        static final Panel EMPTY = new Panel(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private Panel layout() {
        int panelWidth = panelWidth();
        int innerX = panelX() + RegionPanelDraw.PADDING;
        int innerRight = panelX() + panelWidth - RegionPanelDraw.PADDING;

        //从右往左摆：右端两个图标是固定宽度的，先占位才能让左边的按钮贴齐
        int monitorX = innerRight - MONITOR_WIDTH;
        int gearX = monitorX - GAP - GEAR_WIDTH;
        int sprintX = gearX - GAP - SPRINT_WIDTH;
        int stepWidth = Math.max(STEP_MIN_WIDTH, textRenderer.getWidth(stepLabel()));
        int stepX = sprintX - GAP - stepWidth;
        int actionX = stepX - GAP - ACTION_WIDTH;

        int badgeWidth = 0;
        for (RegionTickManager.RegionState state : RegionTickManager.RegionState.values()) {
            badgeWidth = Math.max(badgeWidth, RegionPanelDraw.badgeWidth(textRenderer, Text.translatable(RegionPanelStyle.stateKey(state))));
        }
        int badgeX = actionX - BADGE_GAP - badgeWidth;
        int nameWidth = Math.max(MIN_NAME_WIDTH, badgeX - GAP - innerX);

        // 行数按可用高度给，面板不顶出屏幕（页脚关闭按钮是唯一出口）
        int maxRows = Math.max(1, Math.min(MAX_VISIBLE_ROWS,
                (height - LIST_PADDING * 2 - ROW_TOP - 30) / ROW_HEIGHT));
        int rowCount = regions.isEmpty() ? 1 : MathHelper.clamp(view.size(), 1, maxRows);
        int rowBottom = rowCount * ROW_HEIGHT;
        // 页脚按钮底边距固定，面板高度不能压到它以下
        int panelHeight = Math.max(ROW_TOP + 30, Math.min(height - LIST_PADDING, ROW_TOP + rowBottom + 30));
        int panelY = Math.max(4, (height - panelHeight) / 2);

        int toolbarY = panelY + RegionPanelDraw.HEADER_HEIGHT + 6;
        int clearX = innerRight - CLEAR_WIDTH;
        int fieldWidth = Math.max(80, clearX - TOOLBAR_GAP - innerX);

        return new Panel(panelX(), panelY, panelWidth, panelHeight,
                toolbarY, innerX, fieldWidth, clearX,
                panelY + ROW_TOP, panelY + ROW_TOP + rowBottom, rowCount,
                innerX, nameWidth, badgeX, actionX, stepX, stepWidth, sprintX, gearX, monitorX);
    }

    // ---------------------------------------------------------------- 控件

    private void rebuildRows() {
        clearChildren();
        freezeButtons.clear();
        stepButtons.clear();
        sprintButtons.clear();
        filterField = null;
        filterClearButton = null;
        toggleBatchButton = null;
        freezeBatchButton = null;
        firstRow = MathHelper.clamp(scroll, 0, Math.max(0, view.size() - panel.rowCount()));
        scroll = firstRow;

        for (int index = firstRow; index < view.size() && index < firstRow + panel.rowCount(); index++) {
            ClientRegion region = view.get(index);
            int rowY = panel.rowTop() + (index - firstRow) * ROW_HEIGHT + (ROW_HEIGHT - WIDGET_HEIGHT) / 2;
            addRowWidgets(region, rowY);
        }

        addToggleButton();
        PanelButton close = new PanelButton(panel.x() + panel.width() - RegionPanelDraw.PADDING - FOOTER_BUTTON_WIDTH,
                panel.rowBottom() + 10, FOOTER_BUTTON_WIDTH, WIDGET_HEIGHT,
                Text.translatable("screen.relativitytick.close"), button -> close(), PanelButton.Kind.SECONDARY);
        close.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.close.tooltip")));
        addDrawableChild(close);
    }

    private void addRowWidgets(ClientRegion region, int rowY) {
        String id = region.getId();

        PanelButton freeze = new PanelButton(panel.actionX(), rowY, ACTION_WIDTH, WIDGET_HEIGHT,
                freezeLabel(region), button -> toggleFreeze(id), PanelButton.Kind.PRIMARY);
        freeze.setTooltip(Tooltip.of(freezeTooltip(region)));
        addDrawableChild(freeze);
        freezeButtons.put(id, freeze);

        PanelButton step = new PanelButton(panel.stepX(), rowY, panel.stepWidth(), WIDGET_HEIGHT,
                stepLabel(), button -> step(id), PanelButton.Kind.SECONDARY);
        step.setTooltip(Tooltip.of(stepTooltip()));
        step.active = canStep(region);
        addDrawableChild(step);
        stepButtons.put(id, step);

        PanelButton sprint = new PanelButton(panel.sprintX(), rowY, SPRINT_WIDTH, WIDGET_HEIGHT,
                sprintLabel(region), button -> toggleSprint(id), PanelButton.Kind.SECONDARY);
        sprint.setTooltip(Tooltip.of(sprintTooltip(region)));
        sprint.active = !region.isReleased();
        addDrawableChild(sprint);
        sprintButtons.put(id, sprint);

        IconButtonWidget gear = new IconButtonWidget(panel.gearX(), rowY, GEAR_WIDTH, WIDGET_HEIGHT,
                IconButtonWidget.Icon.GEAR, Text.translatable("screen.relativitytick.configure"), button -> openConfig(id));
        gear.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.configure")));
        addDrawableChild(gear);

        IconButtonWidget monitor = new IconButtonWidget(panel.monitorX(), rowY, MONITOR_WIDTH, WIDGET_HEIGHT,
                IconButtonWidget.Icon.MONITOR, Text.translatable("screen.relativitytick.monitor"), button -> openMonitor(id));
        monitor.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.monitor")));
        addDrawableChild(monitor);
    }

    // 底部的批量按钮与行按钮一起重建，作用面跟着筛选走
    private void addToggleButton() {
        toggleBatchButton = new PanelButton(panel.x() + RegionPanelDraw.PADDING, panel.rowBottom() + 10,
                batchButtonWidth("screen.relativitytick.batch.toggle"), WIDGET_HEIGHT,
                Text.translatable("screen.relativitytick.batch.toggle"), button -> toggleBatchFormula(), PanelButton.Kind.SECONDARY);
        toggleBatchButton.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.batch.toggle.tooltip")));
        addDrawableChild(toggleBatchButton);

        freezeBatchButton = new PanelButton(panel.x() + RegionPanelDraw.PADDING
                + batchButtonWidth("screen.relativitytick.batch.toggle") + GAP, panel.rowBottom() + 10,
                batchButtonWidth("screen.relativitytick.batch.freeze"), WIDGET_HEIGHT,
                Text.translatable("screen.relativitytick.batch.freeze"), button -> freezeBatchFormula(), PanelButton.Kind.PRIMARY);
        freezeBatchButton.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.batch.freeze.tooltip")));
        addDrawableChild(freezeBatchButton);

        refreshBatchActive();
    }

    // 批量按钮的可用状态跟着「筛选后还剩几个能动的」走，每次重建都重算
    private void refreshBatchActive() {
        if (toggleBatchButton != null) toggleBatchButton.active = !frozenForBatch().isEmpty();
        if (freezeBatchButton != null) freezeBatchButton.active = !freezableForBatch().isEmpty();
    }

    private int batchButtonWidth(String key) {
        return Math.max(56, textRenderer.getWidth(Text.translatable(key)) + 16);
    }

    /** 批量按钮只改"看得见的那些区域"：筛选框里的词就是作用域，这样批量也能缩到某一类上。 */
    private void toggleBatchFormula() {
        for (ClientRegion region : frozenForBatch()) {
            sendCommand("regionTick freeze " + quote(region.getId()));
        }
    }

    /** 批量冻结：对这些区域发 freeze，就是把它们交给面板接管（行按钮上写的正是"冻结"）。 */
    private void freezeBatchFormula() {
        for (ClientRegion region : freezableForBatch()) {
            sendCommand("regionTick freeze " + quote(region.getId()));
        }
    }

    /** 批量解冻的对象：冻结中的区域。已释放（RELEASED）的不算——它不是被这个面板停下来的。 */
    private List<ClientRegion> frozenForBatch() {
        List<ClientRegion> result = new ArrayList<>();
        for (ClientRegion region : view) {
            if (region.getRegionState() == RegionTickManager.RegionState.FROZEN) result.add(region);
        }
        return result;
    }

    // 批量冻结的对象：除 FROZEN 与 RELEASED 以外、行按钮会显示「冻结」的区域
    private List<ClientRegion> freezableForBatch() {
        List<ClientRegion> result = new ArrayList<>();
        for (ClientRegion region : view) {
            RegionTickManager.RegionState state = region.getRegionState();
            if (state != RegionTickManager.RegionState.FROZEN && state != RegionTickManager.RegionState.RELEASED) {
                result.add(region);
            }
        }
        return result;
    }

    private void rebuildToolbar() {
        String keep = filterField != null ? filterField.getText() : filter;
        if (filterField != null) remove(filterField);
        if (filterClearButton != null) remove(filterClearButton);

        filter = keep;
        filterField = new PanelTextField(textRenderer, panel.fieldX(), panel.toolbarY(), panel.fieldWidth(),
                FIELD_HEIGHT, Text.translatable("screen.relativitytick.filter"));
        filterField.setText(filter);
        filterField.setMaxLength(64);
        // 空框里的提示语，只在未聚焦且为空时绘制，不改 getMessage()
        filterField.setPlaceholder(Text.translatable("screen.relativitytick.filter.placeholder"));
        filterField.setChangedListener(value -> {
            // 筛选词变了立刻重算显示集合；行控件重建交给下一 tick 的指纹比对
            filter = value;
            refreshView();
            viewSignature = signature();
        });
        addDrawableChild(filterField);

        if (!filter.isEmpty()) {
            Text label = Text.translatable("screen.relativitytick.filter.clear");
            filterClearButton = new PanelButton(panel.clearX(), panel.toolbarY(), CLEAR_WIDTH, FIELD_HEIGHT,
                    label, button -> {
                        filterField.setText("");
                        filter = "";
                    }, PanelButton.Kind.SECONDARY);
            filterClearButton.setTooltip(Tooltip.of(label));
            addDrawableChild(filterClearButton);
        }
    }

    // ---------------------------------------------------------------- 绘制

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        RegionPanelDraw.card(context, panel.x(), panel.y(), panel.width(), panel.height());

        boolean inList = mouseY >= panel.rowTop() && mouseY <= panel.rowBottom();
        context.enableScissor(panel.x() + 1, panel.rowTop(), panel.x() + panel.width() - 1, panel.rowBottom());
        for (int index = firstRow; index < view.size() && index < firstRow + panel.rowCount(); index++) {
            drawRowBackground(context, index, inList && mouseY >= rowTop(index) && mouseY < rowTop(index) + ROW_HEIGHT);
        }
        context.disableScissor();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        RegionPanelDraw.header(context, textRenderer, panel.x(), panel.y(), panel.width(), title, count());

        RegionPanelDraw.divider(context, panel.x() + RegionPanelDraw.PADDING, panel.rowTop() - 8,
                panel.width() - RegionPanelDraw.PADDING * 2);

        context.enableScissor(panel.x() + 1, panel.rowTop(), panel.x() + panel.width() - 1, panel.rowBottom());
        if (view.isEmpty()) {
            drawEmptyState(context);
        } else {
            ClientRegion badgeTarget = hoveredBadge(mouseX, mouseY);
            for (int index = firstRow; index < view.size() && index < firstRow + panel.rowCount(); index++) {
                drawRowContent(context, view.get(index), index - firstRow, badgeTarget);
            }
        }
        context.disableScissor();

        RegionPanelDraw.divider(context, panel.x() + RegionPanelDraw.PADDING, panel.rowBottom() + 4,
                panel.width() - RegionPanelDraw.PADDING * 2);
        RegionPanelDraw.scrollbar(context, panel.x() + panel.width() - 6, panel.rowTop(), panel.rowBottom(),
                firstRow, Math.min(panel.rowCount(), view.size()), view.size());

        if (hoveredBadge(mouseX, mouseY) != null) {
            context.drawTooltip(textRenderer, Text.translatable("screen.relativitytick.monitor"), mouseX, mouseY);
        }
    }

    private void drawRowBackground(DrawContext context, int index, boolean hovered) {
        RegionPanelStyle style = RegionPanelStyle.current();
        boolean selected = isSelected(view.get(index));
        int top = rowTop(index);
        int fill = style.rowBackground(selected, hovered, index % 2 == 1);
        if (fill != 0) {
            context.fill(panel.x() + 1, top, panel.x() + panel.width() - 1, top + ROW_HEIGHT, fill);
        }
        //选中行左边补一道强调色：背景色在深色主题下差得不多，光靠底色看不出选没选中
        if (selected || hovered) {
            RegionPanelDraw.rowAccent(context, panel.x() + 2, top, ROW_HEIGHT);
        }
    }

    private void drawRowContent(DrawContext context, ClientRegion region, int visibleIndex, ClientRegion badgeTarget) {
        RegionPanelStyle style = RegionPanelStyle.current();
        int top = panel.rowTop() + visibleIndex * ROW_HEIGHT;

        Text name = Text.literal(textRenderer.trimToWidth(region.getId(), panel.nameWidth()));
        context.drawTextWithShadow(textRenderer, name, panel.nameX(), top + 5, style.text());

        Text details = Text.translatable("screen.relativitytick.row_details",
                region.getChunkCount(), RegionPanelText.number(region.getRate()));
        context.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(details.getString(), panel.nameWidth())),
                panel.nameX(), top + 16, style.textDim());

        RegionTickManager.RegionState state = region.getRegionState();
        Text badge = Text.translatable(RegionPanelStyle.stateKey(state));
        RegionPanelDraw.badge(context, textRenderer, badge, panel.badgeX(), top + 9,
                style.stateColor(state), region == badgeTarget);
    }

    private ClientRegion hoveredBadge(double mouseX, double mouseY) {
        ClientRegion region = rowAt(mouseX, mouseY);
        if (region == null) return null;
        int badgeWidth = RegionPanelDraw.badgeWidth(textRenderer,
                Text.translatable(RegionPanelStyle.stateKey(region.getRegionState())));
        return mouseX >= panel.badgeX() && mouseX <= panel.badgeX() + badgeWidth ? region : null;
    }

    /** 鼠标落在哪一行；命中区是整个行的横向范围（要排除左右各 1px 的卡片描边）。 */
    private ClientRegion rowAt(double mouseX, double mouseY) {
        if (mouseX < panel.x() + 1 || mouseX > panel.x() + panel.width() - 1) return null;
        if (mouseY < panel.rowTop() || mouseY >= panel.rowBottom()) return null;
        int index = firstRow + (int) ((mouseY - panel.rowTop()) / ROW_HEIGHT);
        return index >= 0 && index < view.size() ? view.get(index) : null;
    }

    private void drawEmptyState(DrawContext context) {
        Text line = Text.translatable(regions.isEmpty()
                ? "screen.relativitytick.no_region"
                : "screen.relativitytick.filter.empty");
        int centerY = (panel.rowTop() + panel.rowBottom()) / 2 - 4;
        context.drawCenteredTextWithShadow(textRenderer, line, panel.x() + panel.width() / 2, centerY,
                RegionPanelStyle.current().textMuted());
    }

    private int rowTop(int index) {
        return panel.rowTop() + (index - firstRow) * ROW_HEIGHT;
    }

    private Text count() {
        if (filter.isEmpty()) {
            return Text.translatable("screen.relativitytick.region_panel.count", regions.size());
        }
        return Text.translatable("screen.relativitytick.region_panel.count.filtered", view.size(), regions.size());
    }

    private boolean isSelected(ClientRegion region) {
        return selectedId != null && selectedId.equals(region.getId());
    }

    // ---------------------------------------------------------------- 交互

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        //徽章一整块都是监测入口，先判它：让控件先吃事件的话，点在徽章上会变成"选中这一行"
        ClientRegion badgeTarget = button == 0 ? hoveredBadge(mouseX, mouseY) : null;
        if (badgeTarget != null) {
            openMonitor(badgeTarget.getId());
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 && button != 1) return false;

        ClientRegion row = rowAt(mouseX, mouseY);
        if (row != null) {
            //再点一次同一行取消选中：选中是为了对某一行做批量操作，选完没法退掉会很别扭
            selectedId = isSelected(row) ? null : row.getId();
            return true;
        }
        if (selectedId != null && mouseY >= panel.rowTop() && mouseY <= panel.rowBottom()) {
            selectedId = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            ClientRegion row = rowAt(mouseX, mouseY);
            if (row != null) selectedId = row.getId();
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < panel.x() || mouseX > panel.x() + panel.width() || mouseY < panel.rowTop() || mouseY > panel.rowBottom()) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        //只改滚动量，控件重建交给 tick 里的指纹比对：一次滚轮事件里重建会把按钮的悬停动画打断
        int maxScroll = Math.max(0, view.size() - panel.rowCount());
        scroll = MathHelper.clamp(firstRow - (int) Math.signum(verticalAmount), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        //筛选框拿着焦点时方向键归它（文本光标移动），这里不能抢
        if (filterField != null && filterField.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            moveSelection(1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            moveSelection(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            //ESC 直接退回游戏：面板是临时界面，多一层"确认"只会碍事
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 上下键选行：焦点不在筛选框里时也要能选，所以不依赖控件的 focused 状态。 */
    private void moveSelection(int delta) {
        if (view.isEmpty()) return;
        int current = -1;
        for (int index = 0; index < view.size(); index++) {
            if (isSelected(view.get(index))) {
                current = index;
                break;
            }
        }
        int next = current < 0 ? (delta > 0 ? 0 : view.size() - 1)
                : MathHelper.clamp(current + delta, 0, view.size() - 1);
        selectedId = view.get(next).getId();
        //选中行滑出视野就把列表跟着滚过去，否则键盘选中的是哪一行看不出来
        if (next < firstRow) {
            scroll = next;
        } else if (next >= firstRow + panel.rowCount()) {
            scroll = next - panel.rowCount() + 1;
        }
    }

    // ---------------------------------------------------------------- 命令

    private void toggleFreeze(String regionId) {
        sendCommand("regionTick freeze " + quote(regionId));
    }

    private void step(String regionId) {
        sendCommand("regionTick step " + quote(regionId) + " " + RelativityTickClientConfig.getStepSize());
    }

    private void toggleSprint(String regionId) {
        sendCommand("regionTick sprint " + quote(regionId));
    }

    private void openConfig(String regionId) {
        if (client == null) return;
        client.setScreen(new RegionConfigScreen(regionId, this));
    }

    private void openMonitor(String regionId) {
        if (client == null) return;
        client.setScreen(new RegionMonitorScreen(regionId, this));
    }

    /** 运行 / 冻结按钮：文案说"点下去会怎样"，而不是"现在是什么"。 */
    private static Text freezeLabel(ClientRegion region) {
        RegionTickManager.RegionState state = region.getRegionState();
        return Text.translatable(state == RegionTickManager.RegionState.FROZEN || state == RegionTickManager.RegionState.RELEASED
                ? "screen.relativitytick.action.run"
                : "screen.relativitytick.action.freeze");
    }

    private static Text freezeTooltip(ClientRegion region) {
        RegionTickManager.RegionState state = region.getRegionState();
        String key = switch (state) {
            case RELEASED -> "screen.relativitytick.action.takeover_run";
            case FROZEN -> "screen.relativitytick.action.run.tooltip";
            case RUNNING, SPRINTING -> "screen.relativitytick.action.freeze.tooltip";
        };
        return Text.translatable(key);
    }

    private static Text sprintLabel(ClientRegion region) {
        return Text.translatable(region.isSprinting()
                ? "screen.relativitytick.action.stop_sprint"
                : "screen.relativitytick.action.sprint");
    }

    private static Text sprintTooltip(ClientRegion region) {
        RegionTickManager.RegionState state = region.getRegionState();
        String key = switch (state) {
            case RELEASED -> "screen.relativitytick.action.sprint.released.tooltip";
            case SPRINTING -> "screen.relativitytick.action.stop_sprint.tooltip";
            case FROZEN, RUNNING -> "screen.relativitytick.action.sprint.tooltip";
        };
        return Text.translatable(key);
    }

    /** 步进按钮的文案带当前步长：点一次推进几刻，按钮上直接写着。 */
    private static Text stepLabel() {
        return Text.translatable("screen.relativitytick.action.step", RelativityTickClientConfig.getStepSize());
    }

    /** 服务端只允许冻结中的区域步进，按钮的可用状态和提示都跟着这条规则走。 */
    private static boolean canStep(ClientRegion region) {
        RegionTickManager.RegionState state = region.getRegionState();
        return state == RegionTickManager.RegionState.FROZEN;
    }

    private static Text stepTooltip() {
        return Text.translatable("screen.relativitytick.action.step.tooltip", RelativityTickClientConfig.getStepSize());
    }
}
