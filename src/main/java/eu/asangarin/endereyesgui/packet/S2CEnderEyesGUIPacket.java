package eu.asangarin.endereyesgui.packet;

import eu.asangarin.endereyesgui.client.ClientPackets;
import eu.asangarin.endereyesgui.util.EnderEye;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.EnumUtils;

import java.util.EnumSet;
import java.util.function.Supplier;

public class S2CEnderEyesGUIPacket {
	private final EnumSet<EnderEye> eyeSet;

	public S2CEnderEyesGUIPacket(EnumSet<EnderEye> eyeSet) {
		this.eyeSet = eyeSet;
	}

	public EnumSet<EnderEye> getEyeSet() {
		return eyeSet;
	}

	public void encode(FriendlyByteBuf buf) {
		long bitVector = EnumUtils.generateBitVector(EnderEye.class, eyeSet);
		buf.writeLong(bitVector);
	}

	public static S2CEnderEyesGUIPacket decode(FriendlyByteBuf buf) {
		long bitVector = buf.readLong();
		EnumSet<EnderEye> eyeSet = EnumUtils.processBitVector(EnderEye.class, bitVector);
		return new S2CEnderEyesGUIPacket(eyeSet);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPackets.handlePacket(this, ctx)));
		ctx.get().setPacketHandled(true);
	}
}
