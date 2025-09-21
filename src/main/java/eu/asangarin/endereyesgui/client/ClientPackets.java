package eu.asangarin.endereyesgui.client;

import eu.asangarin.endereyesgui.client.screen.EnderEyesScreen;
import eu.asangarin.endereyesgui.packet.S2CEnderEyesGUIPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientPackets {
	public static void handlePacket(S2CEnderEyesGUIPacket msg, Supplier<NetworkEvent.Context> ctx) {
		Minecraft.getInstance().setScreen(new EnderEyesScreen(msg.getEyeSet()));
	}
}
