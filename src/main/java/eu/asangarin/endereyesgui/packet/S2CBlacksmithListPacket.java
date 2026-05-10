package eu.asangarin.endereyesgui.packet;

import eu.asangarin.endereyesgui.util.BlacksmithRecipeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CBlacksmithListPacket {

	private static final Logger LOGGER = LogManager.getLogger("EnderEyesGUI/Blacksmith");

	private final List<BlacksmithRecipeData> recipes;

	public S2CBlacksmithListPacket(List<BlacksmithRecipeData> recipes) {
		this.recipes = recipes;
	}

	public List<BlacksmithRecipeData> getRecipes() { return recipes; }

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(recipes.size());
		for (BlacksmithRecipeData d : recipes) d.encode(buf);
	}

	public static S2CBlacksmithListPacket decode(FriendlyByteBuf buf) {
		int size = buf.readVarInt();
		LOGGER.info("[Blacksmith] Decoding S2C packet, recipe count: {}", size);
		List<BlacksmithRecipeData> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) list.add(BlacksmithRecipeData.decode(buf));
		return new S2CBlacksmithListPacket(list);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			LOGGER.info("[Blacksmith] Handling S2C packet on client, recipes: {}", recipes.size());
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
				eu.asangarin.endereyesgui.client.ClientPackets.handleBlacksmithPacket(this, ctx)
			);
		});
		ctx.get().setPacketHandled(true);
	}
}
