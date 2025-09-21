package eu.asangarin.endereyesgui.packet;

import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.util.EnderEye;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumSet;
import java.util.function.Supplier;

public class C2SEnderEyesButtonPacket {
	public void encode(FriendlyByteBuf buf) {
	}

	public static C2SEnderEyesButtonPacket decode(FriendlyByteBuf buf) {
		return new C2SEnderEyesButtonPacket();
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null) return;
			MinecraftServer server = sender.getServer();
			if (server == null) return;

			EnumSet<EnderEye> eyeSet = EnumSet.noneOf(EnderEye.class);
			for (EnderEye eye : EnderEye.getValues()) {
				Advancement advancement = server.getAdvancements().getAdvancement(eye.getAdvancementLocation());
				if (advancement == null) continue;
				AdvancementProgress progress = sender.getAdvancements().getOrStartProgress(advancement);
				if (progress.isDone()) eyeSet.add(eye);
			}
			Networking.openEnderGUI(sender, eyeSet);
		});
		ctx.get().setPacketHandled(true);
	}
}
