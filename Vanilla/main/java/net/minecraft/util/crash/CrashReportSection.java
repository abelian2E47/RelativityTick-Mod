package net.minecraft.util.crash;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;

public class CrashReportSection {
    private final String title;
    private final List<CrashReportSection.Element> elements = Lists.newArrayList();
    private StackTraceElement[] stackTrace = new StackTraceElement[0];

    public CrashReportSection(String title) {
        this.title = title;
    }

    public static String createPositionString(HeightLimitView world, double x, double y, double z) {
        return String.format(Locale.ROOT, "%.2f,%.2f,%.2f - %s", x, y, z, createPositionString(world, BlockPos.ofFloored(x, y, z)));
    }

    public static String createPositionString(HeightLimitView world, BlockPos pos) {
        return createPositionString(world, pos.getX(), pos.getY(), pos.getZ());
    }

    public static String createPositionString(HeightLimitView world, int x, int y, int z) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            stringBuilder.append(String.format(Locale.ROOT, "World: (%d,%d,%d)", x, y, z));
        } catch (Throwable throwable) {
            stringBuilder.append("(Error finding world loc)");
        }

        stringBuilder.append(", ");

        try {
            int i = ChunkSectionPos.getSectionCoord(x);
            int j = ChunkSectionPos.getSectionCoord(y);
            int k = ChunkSectionPos.getSectionCoord(z);
            int l = x & 15;
            int m = y & 15;
            int n = z & 15;
            int o = ChunkSectionPos.getBlockCoord(i);
            int p = world.getBottomY();
            int q = ChunkSectionPos.getBlockCoord(k);
            int r = ChunkSectionPos.getBlockCoord(i + 1) - 1;
            int s = world.getTopYInclusive();
            int t = ChunkSectionPos.getBlockCoord(k + 1) - 1;
            stringBuilder.append(
                String.format(Locale.ROOT, "Section: (at %d,%d,%d in %d,%d,%d; chunk contains blocks %d,%d,%d to %d,%d,%d)", l, m, n, i, j, k, o, p, q, r, s, t)
            );
        } catch (Throwable throwable2) {
            stringBuilder.append("(Error finding chunk loc)");
        }

        stringBuilder.append(", ");

        try {
            int u = x >> 9;
            int v = z >> 9;
            int w = u << 5;
            int ab = v << 5;
            int bb = (u + 1 << 5) - 1;
            int cb = (v + 1 << 5) - 1;
            int db = u << 9;
            int eb = world.getBottomY();
            int fb = v << 9;
            int gb = (u + 1 << 9) - 1;
            int hb = world.getTopYInclusive();
            int ib = (v + 1 << 9) - 1;
            stringBuilder.append(
                String.format(
                    Locale.ROOT, "Region: (%d,%d; contains chunks %d,%d to %d,%d, blocks %d,%d,%d to %d,%d,%d)", u, v, w, ab, bb, cb, db, eb, fb, gb, hb, ib
                )
            );
        } catch (Throwable throwable3) {
            stringBuilder.append("(Error finding world loc)");
        }

        return stringBuilder.toString();
    }

    public CrashReportSection add(String name, CrashCallable<String> callable) {
        try {
            this.add(name, callable.call());
        } catch (Throwable throwable) {
            this.add(name, throwable);
        }

        return this;
    }

    public CrashReportSection add(String name, Object detail) {
        this.elements.add(new CrashReportSection.Element(name, detail));
        return this;
    }

    public void add(String name, Throwable throwable) {
        this.add(name, (Object)throwable);
    }

    public int initStackTrace(int ignoredCallCount) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (stackTraceElements.length <= 0) {
            return 0;
        }

        this.stackTrace = new StackTraceElement[stackTraceElements.length - 3 - ignoredCallCount];
        System.arraycopy(stackTraceElements, 3 + ignoredCallCount, this.stackTrace, 0, this.stackTrace.length);
        return this.stackTrace.length;
    }

    public boolean shouldGenerateStackTrace(StackTraceElement prev, StackTraceElement next) {
        if (this.stackTrace.length != 0 && prev != null) {
            StackTraceElement stackTraceElement = this.stackTrace[0];
            if (stackTraceElement.isNativeMethod() == prev.isNativeMethod()
                && stackTraceElement.getClassName().equals(prev.getClassName())
                && stackTraceElement.getFileName().equals(prev.getFileName())
                && stackTraceElement.getMethodName().equals(prev.getMethodName())) {
                if (next != null != this.stackTrace.length > 1) {
                    return false;
                }

                if (next != null && !this.stackTrace[1].equals(next)) {
                    return false;
                }

                this.stackTrace[0] = prev;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void trimStackTraceEnd(int callCount) {
        StackTraceElement[] stackTraceElements = new StackTraceElement[this.stackTrace.length - callCount];
        System.arraycopy(this.stackTrace, 0, stackTraceElements, 0, stackTraceElements.length);
        this.stackTrace = stackTraceElements;
    }

    public void addStackTrace(StringBuilder crashReportBuilder) {
        crashReportBuilder.append("-- ").append(this.title).append(" --\n");
        crashReportBuilder.append("Details:");

        for (CrashReportSection.Element element : this.elements) {
            crashReportBuilder.append("\n\t");
            crashReportBuilder.append(element.getName());
            crashReportBuilder.append(": ");
            crashReportBuilder.append(element.getDetail());
        }

        if (this.stackTrace != null && this.stackTrace.length > 0) {
            crashReportBuilder.append("\nStacktrace:");

            for (StackTraceElement stackTraceElement : this.stackTrace) {
                crashReportBuilder.append("\n\tat ");
                crashReportBuilder.append(stackTraceElement);
            }
        }
    }

    public StackTraceElement[] getStackTrace() {
        return this.stackTrace;
    }

    public static void addBlockInfo(CrashReportSection element, HeightLimitView world, BlockPos pos, @Nullable BlockState state) {
        if (state != null) {
            element.add("Block", state::toString);
        }

        element.add("Block location", () -> createPositionString(world, pos));
    }

    static class Element {
        private final String name;
        private final String detail;

        public Element(String name, @Nullable Object detail) {
            this.name = name;
            if (detail == null) {
                this.detail = "~~NULL~~";
            } else if (detail instanceof Throwable throwable) {
                this.detail = "~~ERROR~~ " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
            } else {
                this.detail = detail.toString();
            }
        }

        public String getName() {
            return this.name;
        }

        public String getDetail() {
            return this.detail;
        }
    }
}

