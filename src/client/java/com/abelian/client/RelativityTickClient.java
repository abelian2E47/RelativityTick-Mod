package com.abelian.client;

import com.abelian.client.clientRegionTick.ClientRegion;
import com.abelian.client.clientRegionTick.ClientRegionManager;
import com.abelian.client.clientRegionTick.ClientScheduledTickManager;
import com.abelian.client.clientRegionTick.ClientRegionTicker;
import com.abelian.client.config.RelativityTickClientConfig;
import com.abelian.regionTick.RegionTickManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;


public class RelativityTickClient implements ClientModInitializer {
    public enum SelectionState {
        OFF,
        SELECTING_CHUNKS
    }

	public static SelectionState currentState = SelectionState.OFF;
	private static KeyBinding startSelectingKeyBinding;
    private static KeyBinding regionPanelKeyBinding;
    private static KeyBinding stepKeyBinding;
	public static Set<Long> selectChunks = new HashSet<>();


	@Override
	public void onInitializeClient() {

		RelativityTickClientConfig.initialize();
		ClientRegionManager.register();
		ClientRegionTicker.register();
		ClientScheduledTickManager.register();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearClientState());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> clearClientState());
        CommandRegistrationCallback.EVENT.register(ClientCommand::register);
        ClientCommandRegistrationCallback.EVENT.register(ClientCommand::registerClientConfigCommands);

		startSelectingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.relativitytick.start_selecting",
				InputUtil.Type.KEYSYM,
				InputUtil.UNKNOWN_KEY.getCode(),
				"category.relativitytick"));

		regionPanelKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.relativitytick.region_panel",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_BRACKET,
				"category.relativitytick"));

		stepKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.relativitytick.step",
				InputUtil.Type.KEYSYM,
				InputUtil.UNKNOWN_KEY.getCode(),
				"category.relativitytick"));
		applyStepKey();



		ClientTickEvents.END_CLIENT_TICK.register(client -> {

			KeyBinding attackKey = client.options.attackKey;
			KeyBinding useKey = client.options.useKey;

			//左键
			if (attackKey.isPressed() && currentState.equals(SelectionState.SELECTING_CHUNKS)) {
                if (client.player == null) return;
                BlockPos blockPos = client.player.getBlockPos();
				ChunkPos pos = new ChunkPos(blockPos);
				if (!selectChunks.contains(pos.toLong())){
					selectChunks.add(pos.toLong());
					client.player.sendMessage(Text.translatable("relativitytick.selection.chunk_added", pos.x, pos.z), false);
				}
			}

			//右键
			if (useKey.isPressed() && currentState.equals(SelectionState.SELECTING_CHUNKS)) {
				if (client.player == null) return;
				BlockPos blockPos = client.player.getBlockPos();
				ChunkPos pos = new ChunkPos(blockPos);

				if (selectChunks.contains(pos.toLong())){
					selectChunks.remove(pos.toLong());
					client.player.sendMessage(Text.translatable("relativitytick.selection.chunk_removed", pos.x, pos.z), false);
				} else {
					client.player.sendMessage(Text.translatable("relativitytick.selection.chunk_not_selected", pos.x, pos.z), false);
				}
			}

            while (startSelectingKeyBinding.wasPressed()) {
                selectChunksMode();
            }
            while (regionPanelKeyBinding.wasPressed()) {
                toggleRegionPanel(client);
            }
            while (stepKeyBinding.wasPressed()) {
                stepControlledRegions(client);
            }
        });


	}

    /**
     * 快捷键行为：没有界面时打开执行面板，已经打开执行面板时关闭它。
     * 按键判定走客户端 tick，因此必须自己判断"当前有没有别的界面"——
     * 否则在配置页的输入框里敲到同一个字符会把面板顶掉。
     */
    private static void toggleRegionPanel(MinecraftClient client) {
        if (client.currentScreen instanceof RegionManagementScreen) {
            client.currentScreen.close();
            return;
        }
        if (client.currentScreen != null) return;
        if (client.player == null || client.world == null) return;
        client.setScreen(new RegionManagementScreen());
    }

    public static void selectChunksMode() {        if (currentState == SelectionState.OFF) {
            currentState = SelectionState.SELECTING_CHUNKS;
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.translatable("relativitytick.selection.mode_activated").formatted(Formatting.GREEN), false);
            }
        } else {
            currentState = SelectionState.OFF;
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        Text.translatable("relativitytick.selection.mode_deactivated").formatted(Formatting.YELLOW), false);
            }
        }
    }

    public static void clearClientState() {
		selectChunks.clear();
		currentState = SelectionState.OFF;
		ClientRegionManager.clear();
		ClientRegionTicker.clear();
		ClientScheduledTickManager.clear();
	}

    /** step 快捷键的当前绑定，可能为 null（配置里没绑键）。 */
    public static KeyBinding getStepKeyBinding() {
        return stepKeyBinding;
    }

    /**
     * 把配置里存的按键标识套到 step 快捷键上。
     * 键位由面板设置改写，所以这里必须能在运行时重绑，而不能只在注册时定死。
     */
    public static void applyStepKey() {
        if (stepKeyBinding == null) return;
        InputUtil.Key key = InputUtil.fromTranslationKey(RelativityTickClientConfig.getStepKey().isEmpty()
                ? InputUtil.UNKNOWN_KEY.getTranslationKey()
                : RelativityTickClientConfig.getStepKey());
        stepKeyBinding.setBoundKey(key);
        KeyBinding.updateKeysByCode();
    }

    /**
     * step 快捷键的行为：给每个"受控且已冻结"的区域排队推进配置里的步数。
     *
     * <p>判定和 step 按钮、服务端命令保持一致——运行中/冲刺中的区域服务端会拒绝，
     * 所以客户端的快捷键直接不问，免得每按一次都刷一条错误。
     */
    private static void stepControlledRegions(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        int steps = RelativityTickClientConfig.getStepSize();
        for (ClientRegion region : ClientRegionManager.getRegions()) {
            RegionTickManager.RegionState state = region.getRegionState();
            if (state != RegionTickManager.RegionState.FROZEN) continue;
            client.player.networkHandler.sendChatCommand(
                    "regionTick step \"" + region.getId().replace("\\", "\\\\").replace("\"", "\\\"") + "\" " + steps);
        }
    }


}