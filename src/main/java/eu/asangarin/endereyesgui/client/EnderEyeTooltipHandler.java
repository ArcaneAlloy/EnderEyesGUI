package eu.asangarin.endereyesgui.client;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import eu.asangarin.endereyesgui.util.EnderEye;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Añade una línea de tooltip a los 24 items de Ender Eye (endrem:*_eye)
 * recordando que hay que hacer clic derecho para desbloquearlos, ya que
 * desde que EnderEyeUnlockHandler cambió el desbloqueo de "recoger" a
 * "clic derecho" esto ya no es obvio para un jugador nuevo.
 */
@Mod.EventBusSubscriber(modid = EnderEyesGUI.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EnderEyeTooltipHandler {

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		if (stack.isEmpty()) return;

		ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
		if (itemId == null) return;

		EnderEye eye = EnderEye.byItemId(itemId);
		if (eye == null) return;

		event.getToolTip().add(
				Component.translatable("endereyesgui.tooltip.right_click_unlock")
						.withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC)
		);
	}
}
