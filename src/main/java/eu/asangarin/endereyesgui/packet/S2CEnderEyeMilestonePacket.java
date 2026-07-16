package eu.asangarin.endereyesgui.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Datos para el popup/overlay que se muestra al jugador al desbloquear un
 * Ender Eye: qué ojo fue, el progreso total, qué hitos reales ocurrieron
 * (corazón, storage, portal) y cuántos encantamientos nuevos se volvieron
 * asequibles, agrupados por categoría.
 */
public class S2CEnderEyeMilestonePacket {

	private final String eyeId;
	private final int eyesEarned;
	private final int eyesTotal;
	private final int heartsGiven;
	private final boolean storageUpgraded;
	private final String portalOpened; // "none" | "nether" | "end" | "final"
	private final int combatCount;
	private final int defensiveCount;
	private final int toolsCount;
	private final int otherCount;
	private final int chunksAdded;
	private final int chunksTotal;

	public S2CEnderEyeMilestonePacket(String eyeId, int eyesEarned, int eyesTotal, int heartsGiven,
	                                   boolean storageUpgraded, String portalOpened,
	                                   int combatCount, int defensiveCount, int toolsCount, int otherCount,
	                                   int chunksAdded, int chunksTotal) {
		this.eyeId = eyeId;
		this.eyesEarned = eyesEarned;
		this.eyesTotal = eyesTotal;
		this.heartsGiven = heartsGiven;
		this.storageUpgraded = storageUpgraded;
		this.portalOpened = portalOpened;
		this.combatCount = combatCount;
		this.defensiveCount = defensiveCount;
		this.toolsCount = toolsCount;
		this.otherCount = otherCount;
		this.chunksAdded = chunksAdded;
		this.chunksTotal = chunksTotal;
	}

	public String getEyeId() { return eyeId; }
	public int getEyesEarned() { return eyesEarned; }
	public int getEyesTotal() { return eyesTotal; }
	public int getHeartsGiven() { return heartsGiven; }
	public boolean isStorageUpgraded() { return storageUpgraded; }
	public String getPortalOpened() { return portalOpened; }
	public int getCombatCount() { return combatCount; }
	public int getDefensiveCount() { return defensiveCount; }
	public int getToolsCount() { return toolsCount; }
	public int getOtherCount() { return otherCount; }
	public int getChunksAdded() { return chunksAdded; }
	public int getChunksTotal() { return chunksTotal; }

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(eyeId);
		buf.writeVarInt(eyesEarned);
		buf.writeVarInt(eyesTotal);
		buf.writeVarInt(heartsGiven);
		buf.writeBoolean(storageUpgraded);
		buf.writeUtf(portalOpened);
		buf.writeVarInt(combatCount);
		buf.writeVarInt(defensiveCount);
		buf.writeVarInt(toolsCount);
		buf.writeVarInt(otherCount);
		buf.writeVarInt(chunksAdded);
		buf.writeVarInt(chunksTotal);
	}

	public static S2CEnderEyeMilestonePacket decode(FriendlyByteBuf buf) {
		return new S2CEnderEyeMilestonePacket(
				buf.readUtf(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readBoolean(),
				buf.readUtf(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt(),
				buf.readVarInt()
		);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() ->
				DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
						eu.asangarin.endereyesgui.client.ClientPackets.handleEnderEyeMilestonePacket(this)
				)
		);
		ctx.get().setPacketHandled(true);
	}
}
