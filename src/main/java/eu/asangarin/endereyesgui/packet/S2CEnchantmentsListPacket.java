package eu.asangarin.endereyesgui.packet;

import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet del servidor al cliente con la lista de WarlockRecipe y
 * los ojos obtenidos por el jugador para mostrar en la pantalla.
 */
public class S2CEnchantmentsListPacket {
	private final List<EnchantmentRecipeData> recipes;
	private final int eyesEarned;
	private final boolean openScreen; // total de ojos del jugador

	public S2CEnchantmentsListPacket(List<EnchantmentRecipeData> recipes, int eyesEarned, boolean openScreen) {
		this.recipes     = recipes;
		this.eyesEarned  = eyesEarned;
		this.openScreen  = openScreen;
	}

	public List<EnchantmentRecipeData> getRecipes()  { return recipes; }
	public int getEyesEarned()                        { return eyesEarned; }
	public boolean shouldOpenScreen()                 { return openScreen; }

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(eyesEarned);
		buf.writeBoolean(openScreen);
		buf.writeVarInt(recipes.size());
		for (EnchantmentRecipeData data : recipes) {
			data.encode(buf);
		}
	}

	public static S2CEnchantmentsListPacket decode(FriendlyByteBuf buf) {
		int eyesEarned = buf.readVarInt();
		boolean openScreen = buf.readBoolean();
		int size = buf.readVarInt();
		List<EnchantmentRecipeData> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(EnchantmentRecipeData.decode(buf));
		}
		return new S2CEnchantmentsListPacket(list, eyesEarned, openScreen);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() ->
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
				eu.asangarin.endereyesgui.client.ClientPackets.handleEnchantmentsPacket(this, ctx)
			)
		);
		ctx.get().setPacketHandled(true);
	}
}
