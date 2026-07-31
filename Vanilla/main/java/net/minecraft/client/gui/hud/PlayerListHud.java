package net.minecraft.client.gui.hud;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nullables;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

/**
 * Responsible for rendering the player list while the {@linkplain
 * net.minecraft.client.option.GameOptions#playerListKey player list
 * key} is pressed.
 * 
 * <p>The current instance used by the client can be obtained by {@code
 * MinecraftClient.getInstance().inGameHud.getPlayerListHud()}.
 */
@Environment(EnvType.CLIENT)
public class PlayerListHud {
    private static final Identifier PING_UNKNOWN_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_unknown");
    private static final Identifier PING_1_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_1");
    private static final Identifier PING_2_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_2");
    private static final Identifier PING_3_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_3");
    private static final Identifier PING_4_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_4");
    private static final Identifier PING_5_ICON_TEXTURE = Identifier.ofVanilla("icon/ping_5");
    private static final Identifier CONTAINER_HEART_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/container_blinking");
    private static final Identifier CONTAINER_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier FULL_HEART_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/full_blinking");
    private static final Identifier HALF_HEART_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/half_blinking");
    private static final Identifier ABSORBING_FULL_HEART_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_full_blinking");
    private static final Identifier FULL_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier ABSORBING_HALF_HEART_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_half_blinking");
    private static final Identifier HALF_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/half");
    private static final Comparator<PlayerListEntry> ENTRY_ORDERING = Comparator.<PlayerListEntry>comparingInt(entry -> -entry.getListOrder())
        .thenComparingInt(entry -> entry.getGameMode() == GameMode.SPECTATOR ? 1 : 0)
        .thenComparing(entry -> Nullables.mapOrElse(entry.getScoreboardTeam(), Team::getName, ""))
        .thenComparing(entry -> entry.getProfile().getName(), String::compareToIgnoreCase);
    public static final int MAX_ROWS = 20;
    private final MinecraftClient client;
    private final InGameHud inGameHud;
    @Nullable
    private Text footer;
    @Nullable
    private Text header;
    private boolean visible;
    private final Map<UUID, PlayerListHud.Heart> hearts = new Object2ObjectOpenHashMap<>();

    public PlayerListHud(MinecraftClient client, InGameHud inGameHud) {
        this.client = client;
        this.inGameHud = inGameHud;
    }

    /**
     * {@return the player name rendered by this HUD}
     */
    public Text getPlayerName(PlayerListEntry entry) {
        return entry.getDisplayName() != null
            ? this.applyGameModeFormatting(entry, entry.getDisplayName().copy())
            : this.applyGameModeFormatting(entry, Team.decorateName(entry.getScoreboardTeam(), Text.literal(entry.getProfile().getName())));
    }

    /**
     * {@linkplain net.minecraft.util.Formatting#ITALIC Italicizes} the given text if
     * the given player is in {@linkplain net.minecraft.world.GameMode#SPECTATOR spectator mode}.
     */
    private Text applyGameModeFormatting(PlayerListEntry entry, MutableText name) {
        return entry.getGameMode() == GameMode.SPECTATOR ? name.formatted(Formatting.ITALIC) : name;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.hearts.clear();
            this.visible = visible;
            if (visible) {
                Text text = Texts.join(this.collectPlayerEntries(), Text.literal(", "), this::getPlayerName);
                this.client.getNarratorManager().narrate(Text.translatable("multiplayer.player.list.narration", text));
            }
        }
    }

    private List<PlayerListEntry> collectPlayerEntries() {
        return this.client.player.networkHandler.getListedPlayerListEntries().stream().sorted(ENTRY_ORDERING).limit(80L).toList();
    }

    public void render(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective) {
        List<PlayerListEntry> list = this.collectPlayerEntries();
        List<PlayerListHud.ScoreDisplayEntry> list2 = new ArrayList<>(list.size());
        int i = this.client.textRenderer.getWidth(" ");
        int j = 0;
        int k = 0;

        for (PlayerListEntry playerListEntry : list) {
            Text text = this.getPlayerName(playerListEntry);
            j = Math.max(j, this.client.textRenderer.getWidth(text));
            int l = 0;
            Text text2 = null;
            int m = 0;
            if (objective != null) {
                ScoreHolder scoreHolder = ScoreHolder.fromProfile(playerListEntry.getProfile());
                ReadableScoreboardScore readableScoreboardScore = scoreboard.getScore(scoreHolder, objective);
                if (readableScoreboardScore != null) {
                    l = readableScoreboardScore.getScore();
                }

                if (objective.getRenderType() != ScoreboardCriterion.RenderType.HEARTS) {
                    NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.YELLOW);
                    text2 = ReadableScoreboardScore.getFormattedScore(readableScoreboardScore, numberFormat);
                    m = this.client.textRenderer.getWidth(text2);
                    k = Math.max(k, m > 0 ? i + m : 0);
                }
            }

            list2.add(new PlayerListHud.ScoreDisplayEntry(text, l, text2, m));
        }

        if (!this.hearts.isEmpty()) {
            Set<UUID> set = list.stream().map(playerEntry -> playerEntry.getProfile().getId()).collect(Collectors.toSet());
            this.hearts.keySet().removeIf(uuid -> !set.contains(uuid));
        }

        int n = list.size();
        int o = n;

        int p;
        for (p = 1; o > 20; o = (n + p - 1) / p) {
            p++;
        }

        boolean bl = this.client.isInSingleplayer() || this.client.getNetworkHandler().getConnection().isEncrypted();
        int q;
        if (objective != null) {
            if (objective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) {
                q = 90;
            } else {
                q = k;
            }
        } else {
            q = 0;
        }

        int t = Math.min(p * ((bl ? 9 : 0) + j + q + 13), scaledWindowWidth - 50) / p;
        int u = scaledWindowWidth / 2 - (t * p + (p - 1) * 5) / 2;
        int v = 10;
        int w = t * p + (p - 1) * 5;
        List<OrderedText> list3 = null;
        if (this.header != null) {
            list3 = this.client.textRenderer.wrapLines(this.header, scaledWindowWidth - 50);

            for (OrderedText orderedText : list3) {
                w = Math.max(w, this.client.textRenderer.getWidth(orderedText));
            }
        }

        List<OrderedText> list4 = null;
        if (this.footer != null) {
            list4 = this.client.textRenderer.wrapLines(this.footer, scaledWindowWidth - 50);

            for (OrderedText orderedText2 : list4) {
                w = Math.max(w, this.client.textRenderer.getWidth(orderedText2));
            }
        }

        if (list3 != null) {
            context.fill(scaledWindowWidth / 2 - w / 2 - 1, v - 1, scaledWindowWidth / 2 + w / 2 + 1, v + list3.size() * 9, Integer.MIN_VALUE);

            for (OrderedText orderedText3 : list3) {
                int x = this.client.textRenderer.getWidth(orderedText3);
                context.drawTextWithShadow(this.client.textRenderer, orderedText3, scaledWindowWidth / 2 - x / 2, v, -1);
                v += 9;
            }

            v++;
        }

        context.fill(scaledWindowWidth / 2 - w / 2 - 1, v - 1, scaledWindowWidth / 2 + w / 2 + 1, v + o * 9, Integer.MIN_VALUE);
        int y = this.client.options.getTextBackgroundColor(553648127);

        for (int z = 0; z < n; z++) {
            int ab = z / o;
            int bb = z % o;
            int cb = u + ab * t + ab * 5;
            int db = v + bb * 9;
            context.fill(cb, db, cb + t, db + 8, y);
            if (z < list.size()) {
                PlayerListEntry playerListEntry2 = list.get(z);
                PlayerListHud.ScoreDisplayEntry scoreDisplayEntry = list2.get(z);
                GameProfile gameProfile = playerListEntry2.getProfile();
                if (bl) {
                    PlayerEntity playerEntity = this.client.world.getPlayerByUuid(gameProfile.getId());
                    boolean bl2 = playerEntity != null && LivingEntityRenderer.shouldFlipUpsideDown(playerEntity);
                    PlayerSkinDrawer.draw(context, playerListEntry2.getSkinTextures().texture(), cb, db, 8, playerListEntry2.shouldShowHat(), bl2, -1);
                    cb += 9;
                }

                context.drawTextWithShadow(
                    this.client.textRenderer, scoreDisplayEntry.name, cb, db, playerListEntry2.getGameMode() == GameMode.SPECTATOR ? -1862270977 : Colors.WHITE
                );
                if (objective != null && playerListEntry2.getGameMode() != GameMode.SPECTATOR) {
                    int eb = cb + j + 1;
                    int fb = eb + q;
                    if (fb - eb > 5) {
                        this.renderScoreboardObjective(objective, db, scoreDisplayEntry, eb, fb, gameProfile.getId(), context);
                    }
                }

                this.renderLatencyIcon(context, t, cb - (bl ? 9 : 0), db, playerListEntry2);
            }
        }

        if (list4 != null) {
            v += o * 9 + 1;
            context.fill(scaledWindowWidth / 2 - w / 2 - 1, v - 1, scaledWindowWidth / 2 + w / 2 + 1, v + list4.size() * 9, Integer.MIN_VALUE);

            for (OrderedText orderedText4 : list4) {
                int gb = this.client.textRenderer.getWidth(orderedText4);
                context.drawTextWithShadow(this.client.textRenderer, orderedText4, scaledWindowWidth / 2 - gb / 2, v, -1);
                v += 9;
            }
        }
    }

    protected void renderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry) {
        Identifier identifier;
        if (entry.getLatency() < 0) {
            identifier = PING_UNKNOWN_ICON_TEXTURE;
        } else if (entry.getLatency() < 150) {
            identifier = PING_5_ICON_TEXTURE;
        } else if (entry.getLatency() < 300) {
            identifier = PING_4_ICON_TEXTURE;
        } else if (entry.getLatency() < 600) {
            identifier = PING_3_ICON_TEXTURE;
        } else if (entry.getLatency() < 1000) {
            identifier = PING_2_ICON_TEXTURE;
        } else {
            identifier = PING_1_ICON_TEXTURE;
        }

        context.getMatrices().push();
        context.getMatrices().translate(0.0F, 0.0F, 100.0F);
        context.drawGuiTexture(RenderLayer::getGuiTextured, identifier, x + width - 11, y, 10, 8);
        context.getMatrices().pop();
    }

    private void renderScoreboardObjective(
        ScoreboardObjective objective, int y, PlayerListHud.ScoreDisplayEntry scoreDisplayEntry, int left, int right, UUID uuid, DrawContext context
    ) {
        if (objective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) {
            this.renderHearts(y, left, right, uuid, context, scoreDisplayEntry.score);
        } else if (scoreDisplayEntry.formattedScore != null) {
            context.drawTextWithShadow(this.client.textRenderer, scoreDisplayEntry.formattedScore, right - scoreDisplayEntry.scoreWidth, y, 16777215);
        }
    }

    private void renderHearts(int y, int left, int right, UUID uuid, DrawContext context, int score) {
        PlayerListHud.Heart heart = this.hearts.computeIfAbsent(uuid, uuid2 -> new PlayerListHud.Heart(score));
        heart.tick(score, this.inGameHud.getTicks());
        int i = MathHelper.ceilDiv(Math.max(score, heart.getPrevScore()), 2);
        int j = Math.max(score, Math.max(heart.getPrevScore(), 20)) / 2;
        boolean bl = heart.useHighlighted(this.inGameHud.getTicks());
        if (i > 0) {
            int k = MathHelper.floor(Math.min((float)(right - left - 4) / j, 9.0F));
            if (k <= 3) {
                float f = MathHelper.clamp(score / 20.0F, 0.0F, 1.0F);
                int l = (int)((1.0F - f) * 255.0F) << 16 | (int)(f * 255.0F) << 8;
                float g = score / 2.0F;
                Text text = Text.translatable("multiplayer.player.list.hp", g);
                Text text2;
                if (right - this.client.textRenderer.getWidth(text) >= left) {
                    text2 = text;
                } else {
                    text2 = Text.literal(Float.toString(g));
                }

                context.drawTextWithShadow(this.client.textRenderer, text2, (right + left - this.client.textRenderer.getWidth(text2)) / 2, y, l);
            } else {
                Identifier identifier = bl ? CONTAINER_HEART_BLINKING_TEXTURE : CONTAINER_HEART_TEXTURE;

                for (int m = i; m < j; m++) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, identifier, left + m * k, y, 9, 9);
                }

                for (int n = 0; n < i; n++) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, identifier, left + n * k, y, 9, 9);
                    if (bl) {
                        if (n * 2 + 1 < heart.getPrevScore()) {
                            context.drawGuiTexture(RenderLayer::getGuiTextured, FULL_HEART_BLINKING_TEXTURE, left + n * k, y, 9, 9);
                        }

                        if (n * 2 + 1 == heart.getPrevScore()) {
                            context.drawGuiTexture(RenderLayer::getGuiTextured, HALF_HEART_BLINKING_TEXTURE, left + n * k, y, 9, 9);
                        }
                    }

                    if (n * 2 + 1 < score) {
                        context.drawGuiTexture(
                            RenderLayer::getGuiTextured, n >= 10 ? ABSORBING_FULL_HEART_BLINKING_TEXTURE : FULL_HEART_TEXTURE, left + n * k, y, 9, 9
                        );
                    }

                    if (n * 2 + 1 == score) {
                        context.drawGuiTexture(
                            RenderLayer::getGuiTextured, n >= 10 ? ABSORBING_HALF_HEART_BLINKING_TEXTURE : HALF_HEART_TEXTURE, left + n * k, y, 9, 9
                        );
                    }
                }
            }
        }
    }

    public void setFooter(@Nullable Text footer) {
        this.footer = footer;
    }

    public void setHeader(@Nullable Text header) {
        this.header = header;
    }

    public void clear() {
        this.header = null;
        this.footer = null;
    }

    @Environment(EnvType.CLIENT)
    static class Heart {
        private static final long COOLDOWN_TICKS = 20L;
        private static final long SCORE_DECREASE_HIGHLIGHT_TICKS = 20L;
        private static final long SCORE_INCREASE_HIGHLIGHT_TICKS = 10L;
        private int score;
        private int prevScore;
        private long lastScoreChangeTick;
        private long highlightEndTick;

        public Heart(int score) {
            this.prevScore = score;
            this.score = score;
        }

        public void tick(int score, long currentTick) {
            if (score != this.score) {
                long l = score < this.score ? 20L : 10L;
                this.highlightEndTick = currentTick + l;
                this.score = score;
                this.lastScoreChangeTick = currentTick;
            }

            if (currentTick - this.lastScoreChangeTick > 20L) {
                this.prevScore = score;
            }
        }

        public int getPrevScore() {
            return this.prevScore;
        }

        public boolean useHighlighted(long currentTick) {
            return this.highlightEndTick > currentTick && (this.highlightEndTick - currentTick) % 6L >= 3L;
        }
    }

    @Environment(EnvType.CLIENT)
    record ScoreDisplayEntry(Text name, int score, @Nullable Text formattedScore, int scoreWidth) {
    }
}

