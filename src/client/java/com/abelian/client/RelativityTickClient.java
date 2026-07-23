package com.abelian.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.HashSet;
import java.util.Set;


public class RelativityTickClient implements ClientModInitializer {
	public enum SelectionState {
		OFF,
		SELECTING_CHUNKS,
		AWAITING_CONFIRM
	}

	public static SelectionState currentState = SelectionState.OFF;
	private static KeyBinding startSelectingKeyBinding;
	public static Set<Long> selectChunks = new HashSet<>();


	@Override
	public void onInitializeClient() {

		ClientRegionManager.register();
		RegionTickDeltaManager.register();
		ClientRegionTicker.register();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clearClientState());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> clearClientState());
        CommandRegistrationCallback.EVENT.register(ClientCommand::register);

		startSelectingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.relativitytick.start_selecting",
				InputUtil.Type.KEYSYM,
				InputUtil.GLFW_KEY_R,
				"category.relativitytick"));


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

			//选区键
			while (startSelectingKeyBinding.wasPressed()) {
				if (currentState.equals(SelectionState.OFF) || currentState.equals(SelectionState.AWAITING_CONFIRM)) {
					currentState = SelectionState.SELECTING_CHUNKS;
					if (client.player != null) {
						client.player.sendMessage(Text.translatable("relativitytick.selection.mode_activated").formatted(Formatting.GREEN), false);
					}
				} else if (currentState.equals(SelectionState.SELECTING_CHUNKS)) {
					currentState = SelectionState.AWAITING_CONFIRM;
					if (client.player != null) {
						client.player.sendMessage(Text.translatable("relativitytick.selection.completed").formatted(Formatting.YELLOW), false);
					}
				}
			}

		});

	}

	public static void clearClientState() {
		selectChunks.clear();
		currentState = SelectionState.OFF;
		ClientRegionManager.clear();
		ClientRegionTicker.clear();
		RegionTickDeltaManager.clear();
	}


}