package eu.asangarin.endereyesgui.event;

import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Comprueba, al subir el número de Ender Eyes de un jugador, si alguna
 * WarlockRecipe (aún no aprendida) acaba de volverse asequible y se lo
 * avisa por chat.
 */
public class NewRecipesNotifier {

	/**
	 * @param player   jugador que acaba de desbloquear un Ender Eye
	 * @param prevEyes número de ojos ANTES del desbloqueo actual
	 * @param newEyes  número de ojos DESPUÉS del desbloqueo actual
	 */
	public static void notifyIfNewRecipesAvailable(ServerPlayer player, int prevEyes, int newEyes) {
		if (newEyes <= prevEyes) return;

		List<WarlockRecipe> allRecipes;
		try {
			allRecipes = player.getServer().getRecipeManager()
					.getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_RECIPE.get());
		} catch (Exception e) {
			return;
		}

		Set<ResourceLocation> unlockedIds = new HashSet<>();
		try {
			for (WarlockRecipe r : BteMobsMod.getListRecipe(BteMobsRecipeTypes.WARLOCK_RECIPE.get(), player)) {
				unlockedIds.add(r.getId());
			}
		} catch (Exception e) {
			// Si falla, seguimos sin filtrar por ya-aprendidas.
		}

		List<String> newlyAvailable = new ArrayList<>();
		for (WarlockRecipe recipe : allRecipes) {
			int needEyes = recipe.getNeedEyes();
			// Solo las que acaban de cruzar el umbral con este desbloqueo, y que
			// el jugador todavía no ha aprendido.
			if (needEyes <= prevEyes || needEyes > newEyes) continue;
			if (unlockedIds.contains(recipe.getId())) continue;

			Enchantment ench = recipe.getEnchantment();
			if (ench == null) continue;
			newlyAvailable.add(ench.getFullname(recipe.getLevel()).getString());
		}

		if (newlyAvailable.isEmpty()) return;

		newlyAvailable.sort(String.CASE_INSENSITIVE_ORDER);

		player.sendSystemMessage(
				Component.translatable("endereyesgui.eye.new_recipes_header")
						.withStyle(ChatFormatting.LIGHT_PURPLE)
		);

		// Aprovechamos el ancho del chat: varios encantamientos por línea en vez de uno solo.
		final int perLine = 3;
		final int columnWidth = 18;
		for (int i = 0; i < newlyAvailable.size(); i += perLine) {
			MutableComponent line = Component.empty();
			for (int j = i; j < Math.min(i + perLine, newlyAvailable.size()); j++) {
				String name = newlyAvailable.get(j);
				String padded = (j < i + perLine - 1 && j < newlyAvailable.size() - 1)
						? padRight(name, columnWidth)
						: name;
				line.append(Component.literal("• ").withStyle(ChatFormatting.GRAY))
						.append(Component.literal(padded).withStyle(ChatFormatting.GREEN));
			}
			player.sendSystemMessage(line);
		}
	}

	private static String padRight(String text, int width) {
		if (text.length() >= width) return text + " ";
		StringBuilder sb = new StringBuilder(text);
		while (sb.length() < width) sb.append(' ');
		return sb.toString();
	}
}
