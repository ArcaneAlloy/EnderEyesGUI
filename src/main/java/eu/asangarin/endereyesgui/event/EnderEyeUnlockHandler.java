package eu.asangarin.endereyesgui.event;

import eu.asangarin.endereyesgui.util.EnderEye;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Sustituye el desbloqueo automático por inventario (antiguo trigger
 * minecraft:inventory_changed, ahora minecraft:impossible en el datapack)
 * por un desbloqueo manual al hacer clic derecho con el Ender Eye en mano.
 * <p>
 * También cancela por completo el uso vanilla del item de End: Remastered
 * (localizar stronghold), ya que en este modpack el clic derecho queda
 * dedicado exclusivamente a registrar el ojo.
 */
public class EnderEyeUnlockHandler {

	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		// El evento se dispara para ambas manos; procesamos solo una vez.
		if (event.getHand() != InteractionHand.MAIN_HAND) return;

		ItemStack stack = event.getItemStack();
		if (stack.isEmpty()) return;

		var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
		if (itemId == null) return;

		EnderEye eye = EnderEye.byItemId(itemId);
		if (eye == null) return;

		// Desactiva la función original del item (localizar stronghold) en todos los casos.
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);

		Player player = event.getEntity();
		if (player.getLevel().isClientSide()) return;

		if (!(player instanceof ServerPlayer serverPlayer)) return;
		MinecraftServer server = serverPlayer.getServer();
		if (server == null) return;

		Advancement advancement = server.getAdvancements().getAdvancement(eye.getAdvancementLocation());
		if (advancement == null) return;

		AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);

		if (progress.isDone()) {
			serverPlayer.displayClientMessage(
					Component.translatable("endereyesgui.eye.already_unlocked"),
					true
			);
			return;
		}

		List<String> remaining = new ArrayList<>();
		for (String criterion : progress.getRemainingCriteria()) {
			remaining.add(criterion);
		}
		for (String criterion : remaining) {
			serverPlayer.getAdvancements().award(advancement, criterion);
		}

		if (!serverPlayer.getAbilities().instabuild) {
			stack.shrink(1);
		}

		serverPlayer.getLevel().playSound(
				null,
				serverPlayer.blockPosition(),
				SoundEvents.PLAYER_LEVELUP,
				SoundSource.PLAYERS,
				0.6f,
				1.4f
		);

		// award() dispara AdvancementEarnEvent de forma síncrona, que enders_journey
		// escucha y usa para publicar EnderEyeMilestoneEvent con los hitos reales
		// (corazón/storage/portal). EnderEyeMilestoneListener recoge ese evento y
		// se encarga de construir y enviar el overlay al jugador.
	}
}
