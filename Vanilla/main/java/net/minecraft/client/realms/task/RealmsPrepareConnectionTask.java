package net.minecraft.client.realms.task;

import com.mojang.logging.LogUtils;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.PopupScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.RealmsServerAddress;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.exception.RetryCallException;
import net.minecraft.client.realms.gui.RealmsPopups;
import net.minecraft.client.realms.gui.screen.RealmsBrokenWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsGenericErrorScreen;
import net.minecraft.client.realms.gui.screen.RealmsLongRunningMcoTaskScreen;
import net.minecraft.client.realms.gui.screen.RealmsLongRunningTickableTaskScreen;
import net.minecraft.client.realms.gui.screen.RealmsTermsScreen;
import net.minecraft.client.resource.server.ServerResourcePackLoader;
import net.minecraft.text.Text;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsPrepareConnectionTask extends LongRunningTask {
    private static final Text APPLYING_PACK_TEXT = Text.translatable("multiplayer.applyingPack");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Text TITLE = Text.translatable("mco.connect.connecting");
    private final RealmsServer server;
    private final Screen lastScreen;

    public RealmsPrepareConnectionTask(Screen lastScreen, RealmsServer server) {
        this.lastScreen = lastScreen;
        this.server = server;
    }

    @Override
    public void run() {
        RealmsServerAddress realmsServerAddress;
        try {
            realmsServerAddress = this.join();
        } catch (CancellationException cancellationException) {
            LOGGER.info("User aborted connecting to realms");
            return;
        } catch (RealmsServiceException realmsServiceException) {
            switch (realmsServiceException.error.getErrorCode()) {
                case 6002:
                    setScreen(new RealmsTermsScreen(this.lastScreen, this.server));
                    return;
                case 6006:
                    boolean bl = MinecraftClient.getInstance().uuidEquals(this.server.ownerUUID);
                    setScreen(
                        bl
                            ? new RealmsBrokenWorldScreen(this.lastScreen, this.server.id, this.server.isMinigame())
                            : new RealmsGenericErrorScreen(
                                Text.translatable("mco.brokenworld.nonowner.title"), Text.translatable("mco.brokenworld.nonowner.error"), this.lastScreen
                            )
                    );
                    return;
                default:
                    this.error(realmsServiceException);
                    LOGGER.error("Couldn't connect to world", realmsServiceException);
                    return;
            }
        } catch (TimeoutException timeoutException) {
            this.error(Text.translatable("mco.errorMessage.connectionFailure"));
            return;
        } catch (Exception exception) {
            LOGGER.error("Couldn't connect to world", exception);
            this.error(exception);
            return;
        }

        if (realmsServerAddress.address == null) {
            this.error(Text.translatable("mco.errorMessage.connectionFailure"));
        } else {
            boolean bl2 = realmsServerAddress.resourcePackUrl != null && realmsServerAddress.resourcePackHash != null;
            Screen screen = bl2
                ? this.createResourcePackConfirmationScreen(realmsServerAddress, getResourcePackId(this.server), this::createConnectingScreen)
                : this.createConnectingScreen(realmsServerAddress);
            setScreen(screen);
        }
    }

    private static UUID getResourcePackId(RealmsServer server) {
        return server.minigameName != null
            ? UUID.nameUUIDFromBytes(("minigame:" + server.minigameName).getBytes(StandardCharsets.UTF_8))
            : UUID.nameUUIDFromBytes(("realms:" + Objects.requireNonNullElse(server.name, "") + ":" + server.activeSlot).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Text getTitle() {
        return TITLE;
    }

    private RealmsServerAddress join() throws RealmsServiceException, TimeoutException, CancellationException {
        RealmsClient realmsClient = RealmsClient.create();

        for (int i = 0; i < 40; i++) {
            if (this.aborted()) {
                throw new CancellationException();
            }

            try {
                return realmsClient.join(this.server.id);
            } catch (RetryCallException retryCallException) {
                pause(retryCallException.delaySeconds);
            }
        }

        throw new TimeoutException();
    }

    public RealmsLongRunningMcoTaskScreen createConnectingScreen(RealmsServerAddress address) {
        return new RealmsLongRunningTickableTaskScreen(this.lastScreen, new RealmsConnectTask(this.lastScreen, this.server, address));
    }

    private PopupScreen createResourcePackConfirmationScreen(
        RealmsServerAddress address, UUID id, Function<RealmsServerAddress, Screen> connectingScreenCreator
    ) {
        Text text = Text.translatable("mco.configure.world.resourcepack.question");
        return RealmsPopups.createInfoPopup(this.lastScreen, text, popup -> {
            setScreen(new MessageScreen(APPLYING_PACK_TEXT));
            this.downloadResourcePack(address, id).thenRun(() -> setScreen(connectingScreenCreator.apply(address))).exceptionally(throwable -> {
                MinecraftClient.getInstance().getServerResourcePackProvider().clear();
                LOGGER.error("Failed to download resource pack from {}", address, throwable);
                setScreen(new RealmsGenericErrorScreen(Text.translatable("mco.download.resourcePack.fail"), this.lastScreen));
                return null;
            });
        });
    }

    private CompletableFuture<?> downloadResourcePack(RealmsServerAddress address, UUID id) {
        try {
            if (address.resourcePackUrl == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("resourcePackUrl was null"));
            }

            if (address.resourcePackHash == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("resourcePackHash was null"));
            }

            ServerResourcePackLoader serverResourcePackLoader = MinecraftClient.getInstance().getServerResourcePackProvider();
            CompletableFuture<Void> completableFuture = serverResourcePackLoader.getPackLoadFuture(id);
            serverResourcePackLoader.acceptAll();
            serverResourcePackLoader.addResourcePack(id, new URL(address.resourcePackUrl), address.resourcePackHash);
            return completableFuture;
        } catch (Exception exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}

