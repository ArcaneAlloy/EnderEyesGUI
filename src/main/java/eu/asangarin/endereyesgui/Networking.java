package eu.asangarin.endereyesgui;

import eu.asangarin.endereyesgui.packet.C2SEnderEyesButtonPacket;
import eu.asangarin.endereyesgui.packet.S2CEnderEyesGUIPacket;
import eu.asangarin.endereyesgui.util.EnderEye;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.EnumSet;

public class Networking {
	private static final String PROTOCOL_VERSION = "1";

	public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(EnderEyesGUI.MODID, "schannel"),
			() -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

	public static void init() {
		INSTANCE.registerMessage(0, S2CEnderEyesGUIPacket.class, S2CEnderEyesGUIPacket::encode, S2CEnderEyesGUIPacket::decode, S2CEnderEyesGUIPacket::handle);
		INSTANCE.registerMessage(1, C2SEnderEyesButtonPacket.class, C2SEnderEyesButtonPacket::encode, C2SEnderEyesButtonPacket::decode, C2SEnderEyesButtonPacket::handle);
	}

	public static void openEnderGUI(ServerPlayer player, EnumSet<EnderEye> eyes) {
		INSTANCE.sendTo(new S2CEnderEyesGUIPacket(eyes), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
	}

	public static void requestEnderGUI() {
		INSTANCE.sendToServer(new C2SEnderEyesButtonPacket());
	}
}
