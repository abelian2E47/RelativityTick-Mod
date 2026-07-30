package net.minecraft.client.realms.task;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.exception.RetryCallException;
import net.minecraft.client.realms.gui.screen.RealmsConfigureWorldScreen;
import net.minecraft.text.Text;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class CloseServerTask extends LongRunningTask {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Text TITLE = Text.translatable("mco.configure.world.closing");
    private final RealmsServer serverData;
    private final RealmsConfigureWorldScreen configureScreen;

    public CloseServerTask(RealmsServer realmsServer, RealmsConfigureWorldScreen configureWorldScreen) {
        this.serverData = realmsServer;
        this.configureScreen = configureWorldScreen;
    }

    @Override
    public void run() {
        RealmsClient realmsClient = RealmsClient.create();

        for (int i = 0; i < 25; i++) {
            if (this.aborted()) {
                return;
            }

            try {
                boolean bl = realmsClient.close(this.serverData.id);
                if (bl) {
                    this.configureScreen.stateChanged();
                    this.serverData.state = RealmsServer.State.CLOSED;
                    setScreen(this.configureScreen);
                    break;
                }
            } catch (RetryCallException retryCallException) {
                if (this.aborted()) {
                    return;
                }

                pause(retryCallException.delaySeconds);
            } catch (Exception exception) {
                if (this.aborted()) {
                    return;
                }

                LOGGER.error("Failed to close server", exception);
                this.error(exception);
            }
        }
    }

    @Override
    public Text getTitle() {
        return TITLE;
    }
}

