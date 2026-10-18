package com.abelian.client;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.gui.PanelButton;
import com.abelian.client.gui.RegionPanelDraw;
import com.abelian.client.gui.RegionPanelScreen;
import com.abelian.client.gui.RegionPanelStyle;
import com.abelian.client.gui.RegionPanelText;
import com.abelian.network.RegionStatusPayload;
import com.abelian.regionTick.RegionTickManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** 实时监测：状态徽章 + 六项指标 + 两段历史曲线 + mspt 占用；只认服务端广播的快照，过期宁可画"—"。 */
public class RegionMonitorScreen extends RegionPanelScreen {
    private static final int STATUS_INTERVAL_TICKS = 4;
    private static final long FRESH_MILLIS = 1200L;
    private static final int COLUMN_GAP = 20;
    private static final int ROW_HEIGHT = 22;
    /** 两列各三行：左列吞吐与规模，右列时间轴与积压。 */
    private static final int ROWS = 3;

    /** 历史曲线的窗口长度：40 个采样点，按 4 tick 一拍约 8 秒。 */
    private static final int HISTORY_SAMPLES = 40;
    private static final int SPARK_MIN_HEIGHT = 12;
    /** 曲线左边的标签宽度下限（"目标 TPS: 100" 比指标名长）。 */
    private static final int SPARK_LABEL_WIDTH = 52;
    /** 量程档位：取第一个装得下峰值的档。 */
    private static final double[] CEILING_STEPS = {10.0, 20.0, 50.0, 100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0};
    /** TPS 量程下限：低于原版 20 会一直贴着顶。 */
    private static final double TPS_FLOOR = 20.0;
    /** mspt 量程下限：跟默认的 10ms 上限对齐，超没超一眼能看出来。 */
    private static final double MSPT_FLOOR = 10.0;

    private final String regionId;
    private final Screen parent;

    private Layout layout = Layout.EMPTY;
    private int statusCountdown;

    /** 环形历史缓冲：旧 → 新。容量固定，写满之后整段左移一格。 */
    private final double[] tpsHistory = new double[HISTORY_SAMPLES];
    private final double[] durationHistory = new double[HISTORY_SAMPLES];
    private int historyCount;
    private int historyCountdown;

    public RegionMonitorScreen(String regionId, Screen parent) {
        super(Text.translatable("screen.relativitytick.monitor.title"));
        this.regionId = regionId;
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout = computeLayout();
        statusCountdown = STATUS_INTERVAL_TICKS;
        ClientRegionManager.requestLiveStatus(regionId);
        PanelButton panelConfig = new PanelButton(layout.innerX(), layout.footerY(), panelConfigWidth(), WIDGET_HEIGHT,
                Text.translatable("screen.relativitytick.panel_config"), button -> openPanelConfig(), PanelButton.Kind.SECONDARY);
        panelConfig.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.panel_config.tooltip")));
        addDrawableChild(panelConfig);

        PanelButton close = new PanelButton(panelX() + panelWidth() - RegionPanelDraw.PADDING - FOOTER_BUTTON_WIDTH,
                layout.footerY(), FOOTER_BUTTON_WIDTH, WIDGET_HEIGHT,
                Text.translatable("screen.relativitytick.close"), button -> close(), PanelButton.Kind.SECONDARY);
        close.setTooltip(Tooltip.of(Text.translatable("screen.relativitytick.close.tooltip")));
        addDrawableChild(close);
    }

    @Override
    public void tick() {
        if (--statusCountdown <= 0) {
            statusCountdown = STATUS_INTERVAL_TICKS;
            ClientRegionManager.requestLiveStatus(regionId);
        }
        sampleHistory();
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void openPanelConfig() {
        if (client == null) return;
        client.setScreen(new PanelConfigScreen(this));
    }

    private int panelConfigWidth() {
        return Math.min(96, RegionPanelDraw.PADDING + textRenderer.getWidth(Text.translatable("screen.relativitytick.panel_config")) + 24);
    }

    // ---------------------------------------------------------------- 历史采样

    /** 每 4 tick 记一拍（与请求快照同节奏），只记实时且采样有效的快照。 */
    private void sampleHistory() {
        if (--historyCountdown > 0) return;
        historyCountdown = STATUS_INTERVAL_TICKS;

        RegionStatusPayload status = liveStatus();
        if (status == null || !status.tpsSampleReady()) return;

        if (historyCount == HISTORY_SAMPLES) {
            System.arraycopy(tpsHistory, 1, tpsHistory, 0, HISTORY_SAMPLES - 1);
            System.arraycopy(durationHistory, 1, durationHistory, 0, HISTORY_SAMPLES - 1);
            historyCount = HISTORY_SAMPLES - 1;
        }
        tpsHistory[historyCount] = status.tps();
        durationHistory[historyCount] = status.tickWindowMs();
        historyCount++;
    }

    // ---------------------------------------------------------------- 布局

    private record Layout(int x, int y, int width, int height, int badgeY, int dividerY,
                          int gridTop, int rowHeight, int rowGap, int sparkGap, int sparkHeight,
                          int loadDividerY, int loadLabelY, int footerY, boolean dense, boolean showHistory) {
        static final Layout EMPTY = new Layout(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false);

        int innerX() { return x + RegionPanelDraw.PADDING; }
        int innerRight() { return x + width - RegionPanelDraw.PADDING; }
        int innerWidth() { return width - RegionPanelDraw.PADDING * 2; }
        int leftColumnRight() { return innerX() + (innerWidth() - COLUMN_GAP) / 2; }
        int rightColumnLeft() { return innerX() + (innerWidth() + COLUMN_GAP) / 2; }
        int gridHeight() { return rowHeight * ROWS; }
        /** 第一段曲线的顶。 */
        int sparkTop() { return gridTop + gridHeight() + rowGap + sparkGap; }
        int sparkStep() { return sparkHeight + sparkGap; }
    }

    // 纵向预算：按常规间距排一遍，放不下就压间距和曲线高度，再放不下就不画历史段
    private Layout computeLayout() {
        int panelWidth = panelWidth();
        boolean dense = height < 240;
        int rowHeight = dense ? 18 : ROW_HEIGHT;
        boolean showHistory = height >= 170;

        int badgeY = RegionPanelDraw.HEADER_HEIGHT + 8;
        int dividerY = badgeY + 22;
        int gridTop = dividerY + 12;

        int addGap = 12;
        int rowGap = 15;
        int sparkGap = 16;
        int sparkHeight = 40;
        int loadGap = 8;

        int available = height - 24 - (gridTop + rowHeight * ROWS);
        if (!dense) {
            int needed = 12 + 8 + 2 + RegionPanelDraw.LOAD_HEIGHT + 12 + WIDGET_HEIGHT + RegionPanelDraw.PADDING;
            available = height - 24 - gridTop - rowHeight * ROWS - needed;
        }

        if (available < 60) {
            addGap = 6;
            rowGap = 6;
            loadGap = 4;
            sparkGap = 4;
            int free = available - 10;
            sparkHeight = free >= SPARK_MIN_HEIGHT * 4 ? free / 4 : SPARK_MIN_HEIGHT;
            if (free < SPARK_MIN_HEIGHT * 2 + 8) {
                showHistory = false;
                sparkHeight = 0;
                sparkGap = 0;
                rowGap = Math.max(2, rowGap);
                addGap = 4;
            }
        }

        int content = rowHeight * ROWS + (showHistory ? rowGap + sparkGap * 2 + sparkHeight * 2 : 0);
        int loadDividerY = gridTop + content + loadGap;
        int loadLabelY = loadDividerY + loadGap + 2;
        int footerY = loadLabelY + RegionPanelDraw.LOAD_HEIGHT + addGap;
        int panelHeight = footerY + WIDGET_HEIGHT + RegionPanelDraw.PADDING;
        int panelY = Math.max(4, (height - panelHeight) / 2);

        return new Layout(panelX(), panelY, panelWidth, panelHeight,
                panelY + badgeY, panelY + dividerY, panelY + gridTop, rowHeight, rowGap,
                sparkGap, sparkHeight, panelY + loadDividerY, panelY + loadLabelY,
                panelY + footerY, dense, showHistory);
    }

    // ---------------------------------------------------------------- 绘制

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        RegionPanelDraw.card(context, layout.x(), layout.y(), layout.width(), layout.height());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        RegionPanelDraw.header(context, textRenderer, layout.x(), layout.y(), layout.width(),
                title, Text.literal(textRenderer.trimToWidth(regionId, layout.innerWidth() / 2)));

        RegionStatusPayload status = liveStatus();
        ClientRegion region = ClientRegionManager.getRegion(regionId);
        if (region == null && status == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.relativitytick.no_region"),
                    layout.x() + layout.width() / 2, layout.badgeY() + 4, RegionPanelStyle.current().warn());
            return;
        }

        drawStatus(context, status, region);
        RegionPanelDraw.divider(context, layout.innerX(), layout.dividerY(), layout.innerWidth());

        List<Metric> left = new ArrayList<>(ROWS);
        List<Metric> right = new ArrayList<>(ROWS);
        collectMetrics(status, region, left, right);
        drawColumn(context, left, layout.innerX(), layout.leftColumnRight());
        drawColumn(context, right, layout.rightColumnLeft(), layout.innerRight());

        if (layout.showHistory()) {
            drawHistory(context, status, region);
        }

        RegionPanelDraw.divider(context, layout.innerX(), layout.loadDividerY(), layout.innerWidth());
        drawLoad(context, status);
    }

    /** 两段历史曲线（TPS / mspt），标签写参照值（目标速率、mspt 上限），量程按峰值取固定档位。 */
    private void drawHistory(DrawContext context, RegionStatusPayload status, ClientRegion region) {
        RegionPanelStyle style = RegionPanelStyle.current();
        TextRenderer renderer = textRenderer;
        int x = layout.innerX();
        int right = layout.innerRight();

        double rate = status != null ? status.rate() : region.getRate();
        double tpsMax = ceiling(tpsHistory, historyCount, Math.max(TPS_FLOOR, rate));
        drawSpark(context, renderer, x, right, layout.sparkTop(),
                Text.translatable("screen.relativitytick.spark.target_tps", RegionPanelText.number(rate)),
                tpsHistory, historyCount, 0.0, tpsMax, rate, style.accent());

        double limit = status != null ? status.tickWindowLimitMs() : 0.0;
        double msptMax = ceiling(durationHistory, historyCount, Math.max(MSPT_FLOOR, limit));
        drawSpark(context, renderer, x, right, layout.sparkTop() + layout.sparkStep(),
                Text.translatable("screen.relativitytick.spark.mspt_limit",
                        limit > 0.0 ? RegionPanelText.number(limit) : unknown()),
                durationHistory, historyCount, 0.0, msptMax, limit, style.info());
    }

    /** 取装得下样本峰值的量程档；量化到固定档位，避免曲线每拍抖动。 */
    private static double ceiling(double[] samples, int count, double floor) {
        double peak = floor;
        for (int i = 0; i < count; i++) {
            if (samples[i] > peak) peak = samples[i];
        }
        for (double step : CEILING_STEPS) {
            if (peak <= step) return step;
        }
        return CEILING_STEPS[CEILING_STEPS.length - 1];
    }

    private void drawSpark(DrawContext context, TextRenderer renderer, int x, int right, int y,
                           Text label, double[] samples, int count, double min, double max, double reference, int color) {
        RegionPanelStyle style = RegionPanelStyle.current();
        int labelWidth = renderer.getWidth(label);
        Text reading = count > 0
                ? Text.literal(RegionPanelText.number(samples[count - 1]))
                : Text.translatable("screen.relativitytick.unknown");
        int readingWidth = renderer.getWidth(reading);

        context.drawTextWithShadow(renderer, label, x, y + 1, style.textMuted());
        int sparkX = x + Math.max(SPARK_LABEL_WIDTH, labelWidth + 6);
        //读数占的宽度不能全算在曲线头上，窄窗口里给个下限，曲线才不会被读数压成竖线
        int sparkRight = right - Math.max(48, readingWidth + 6);
        if (sparkRight - sparkX > 1) {
            RegionPanelDraw.sparkline(context, renderer, sparkX, y, sparkRight - sparkX, layout.sparkHeight(),
                    samples, count, min, max, reference, color,
                    Text.translatable("screen.relativitytick.spark.waiting"));
        }
        context.drawTextWithShadow(renderer, reading, right - readingWidth, y + 1,
                count > 0 ? style.text() : style.textDim());
    }

    /** mspt 占用：只用实时快照，上限不在常态广播里。 */
    private void drawLoad(DrawContext context, RegionStatusPayload status) {
        double used = status != null ? status.tickWindowMs() : 0.0;
        double limit = status != null ? status.tickWindowLimitMs() : 0.0;
        RegionPanelDraw.load(context, textRenderer, layout.innerX(), layout.innerRight(),
                layout.loadLabelY(), used, limit, status != null && limit > 0.0);
    }

    /** label 与 value 分开着色：标签是灰的，数值按状态亮起来。 */
    private record Metric(String labelKey, Text value, int valueColor, int labelColor) {
        Metric(String labelKey, Text value, int valueColor) {
            this(labelKey, value, valueColor, RegionPanelStyle.current().textMuted());
        }
    }

    private void drawColumn(DrawContext context, List<Metric> metrics, int left, int right) {
        for (int row = 0; row < metrics.size(); row++) {
            Metric metric = metrics.get(row);
            int y = layout.gridTop() + row * layout.rowHeight();
            drawRow(context, left, y, Text.translatable(metric.labelKey()), metric.value(),
                    metric.valueColor(), metric.labelColor(), right - left);
        }
    }

    /** 左对齐标签、右对齐数值：一列数字右对齐才读得出大小，两列各自对齐各自的边界。 */
    private void drawRow(DrawContext context, int x, int y, Text label, Text value,
                         int valueColor, int labelColor, int columnWidth) {
        TextRenderer renderer = textRenderer;
        int valueWidth = renderer.getWidth(value);
        int labelRoom = columnWidth - valueWidth - 8;
        context.drawTextWithShadow(renderer,
                Text.literal(renderer.trimToWidth(label.getString(), Math.max(0, labelRoom))),
                x, y, labelColor);
        context.drawTextWithShadow(renderer, value, x + columnWidth - valueWidth, y, valueColor);
    }

    private void collectMetrics(RegionStatusPayload status, ClientRegion region,
                                List<Metric> left, List<Metric> right) {
        RegionPanelStyle style = RegionPanelStyle.current();
        boolean fresh = status != null;
        boolean sampled = fresh && status.tpsSampleReady();

        double tps = fresh ? status.tps() : region.getRegionTPS();
        double msprt = fresh ? status.msprtMs() : region.getMsprtMs();
        long virtualTime = fresh ? status.virtualTime() : region.getVirtualTime();
        int chunks = fresh ? status.chunkCount() : region.getChunkCount();
        int pending = fresh ? status.pendingWorkSteps() : 0;
        long sprintLeft = fresh ? status.sprintRemainingGt() : 0;

        //左列吞吐与规模，右列时间轴与积压；每列三行，两列的数值边缘各自对齐
        left.add(new Metric("screen.relativitytick.metric.tps",
                sampled ? Text.literal(RegionPanelText.integer(Math.round(tps))) : unknown(),
                !sampled ? style.textDim() : style.good()));
        left.add(new Metric("screen.relativitytick.metric.msprt",
                Text.literal(RegionPanelText.number(msprt) + " ms"),
                fresh ? style.text() : style.textDim()));
        left.add(new Metric("screen.relativitytick.metric.chunks",
                Text.translatable("screen.relativitytick.unit.chunks", RegionPanelText.integer(chunks)),
                style.text()));
        right.add(new Metric("screen.relativitytick.metric.virtual_time",
                Text.translatable("screen.relativitytick.unit.gt", RegionPanelText.integer(virtualTime)),
                style.text()));
        right.add(new Metric("screen.relativitytick.metric.pending",
                fresh ? Text.translatable("screen.relativitytick.unit.steps", RegionPanelText.integer(pending)) : unknown(),
                !fresh ? style.textDim() : pending > 0 ? style.warn() : style.text()));
        right.add(new Metric("screen.relativitytick.metric.sprint",
                fresh ? Text.translatable("screen.relativitytick.unit.gt", RegionPanelText.integer(sprintLeft)) : unknown(),
                !fresh ? style.textDim() : sprintLeft > 0 ? style.info() : style.text()));
    }

    private static Text unknown() {
        return Text.translatable("screen.relativitytick.unknown");
    }

    private void drawStatus(DrawContext context, RegionStatusPayload status, ClientRegion region) {
        RegionPanelStyle style = RegionPanelStyle.current();
        RegionTickManager.RegionState state = status != null ? status.state() : region.getRegionState();
        Text badge = Text.translatable(RegionPanelStyle.stateKey(state));
        int width = RegionPanelDraw.badge(context, textRenderer, badge, layout.innerX(), layout.badgeY(),
                style.stateColor(state), false);

        long age = ClientRegionManager.getLiveStatusAgeMillis(regionId);
        Text freshness;
        int color;
        if (status == null || age < 0) {
            freshness = Text.translatable("screen.relativitytick.status.pending");
            color = style.textDim();
        } else if (age <= FRESH_MILLIS) {
            freshness = Text.translatable("screen.relativitytick.status.live");
            color = style.good();
        } else {
            freshness = Text.translatable("screen.relativitytick.status.age", RegionPanelText.number(age / 1000.0));
            color = style.warn();
        }
        context.drawTextWithShadow(textRenderer, freshness, layout.innerX() + width + 12,
                layout.badgeY() + 2, color);
    }

    private RegionStatusPayload liveStatus() {
        return ClientRegionManager.getLiveStatus(regionId);
    }
}
