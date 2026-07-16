package eu.asangarin.endereyesgui.event;

import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.packet.S2CEnderEyeMilestonePacket;
import eu.asangarin.endereyesgui.util.EnchantmentCategoryUtil;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import mc.duzo.ender_journey.event.EnderEyeMilestoneEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Escucha {@link EnderEyeMilestoneEvent} (disparado por enders_journey al
 * completar uno de los 24 advancements de Ender Eye) y construye el resumen
 * completo para el overlay del jugador: progreso de ojos, hitos reales
 * (corazón/storage/portal) y cuántos encantamientos nuevos se han vuelto
 * asequibles, agrupados en combate/defensivos/herramientas/otros.
 */
public class EnderEyeMilestoneListener {

	private static final int EYES_TOTAL = 24;

	@SubscribeEvent
	public static void onEnderEyeMilestone(EnderEyeMilestoneEvent event) {
		ServerPlayer player = event.getPlayer();
		int newEyes = event.getEyesEarned();
		int prevEyes = newEyes - 1; // este handler solo procesa un ojo a la vez

		String eyeId = eyeIdFromAdvancement(event.getAdvancementId());

		int combat = 0, defensive = 0, tools = 0, other = 0;

		try {
			List<WarlockRecipe> allRecipes = player.getServer().getRecipeManager()
					.getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_RECIPE.get());

			Set<ResourceLocation> unlockedIds = new HashSet<>();
			try {
				for (WarlockRecipe r : BteMobsMod.getListRecipe(BteMobsRecipeTypes.WARLOCK_RECIPE.get(), player)) {
					unlockedIds.add(r.getId());
				}
			} catch (Exception ignored) {
				// Seguimos sin filtrar por ya-aprendidas si esto falla.
			}

			for (WarlockRecipe recipe : allRecipes) {
				int needEyes = recipe.getNeedEyes();
				// Solo las que acaban de cruzar el umbral con este desbloqueo concreto.
				if (needEyes <= prevEyes || needEyes > newEyes) continue;
				if (unlockedIds.contains(recipe.getId())) continue;

				Enchantment ench = recipe.getEnchantment();
				if (ench == null) continue;
				ResourceLocation enchId = ForgeRegistries.ENCHANTMENTS.getKey(ench);
				if (enchId == null) continue;

				for (EnchantmentCategoryUtil.Bucket bucket : EnchantmentCategoryUtil.getBuckets(enchId)) {
					switch (bucket) {
						case COMBAT:    combat++;    break;
						case DEFENSIVE: defensive++; break;
						case TOOLS:     tools++;     break;
						case OTHER:     other++;     break;
					}
				}
			}
		} catch (Exception ignored) {
			// Si BeyondTheEndMobs no está presente o falla, el overlay sigue
			// mostrando el resto de información sin la parte de recetas.
		}

		String portal = event.getPortalOpened().name().toLowerCase();

		S2CEnderEyeMilestonePacket packet = new S2CEnderEyeMilestonePacket(
				eyeId, newEyes, EYES_TOTAL,
				event.getHeartsGiven(), event.isStorageUpgraded(), portal,
				combat, defensive, tools, other,
				event.getChunksAdded(), event.getChunksTotal()
		);

		Networking.sendEnderEyeMilestone(player, packet);
	}

	/** "endrem:main/void_eye" → "void" */
	private static String eyeIdFromAdvancement(ResourceLocation advancementId) {
		String path = advancementId.getPath();
		String last = path.substring(path.lastIndexOf('/') + 1);
		return last.endsWith("_eye") ? last.substring(0, last.length() - 4) : last;
	}
}
