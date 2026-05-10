package eu.asangarin.endereyesgui.packet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.util.BlacksmithRecipeData;
import fr.shoqapik.btemobs.recipe.BlacksmithRecipe;
import fr.shoqapik.btemobs.recipe.BlacksmithUpgradeRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class C2SRequestBlacksmithPacket {

	private static final Logger LOGGER = LogManager.getLogger("EnderEyesGUI/Blacksmith");
	private static Map<String, Integer> ITEM_TIERS = null;

	// ── Ranged damage config ──────────────────────────────────────────────────
	private static Map<String, Float> RANGED_DAMAGE = null;

	private static Map<String, Float> getRangedDamage() {
		if (RANGED_DAMAGE != null) return RANGED_DAMAGE;
		RANGED_DAMAGE = new HashMap<>();
		try {
			InputStream is = C2SRequestBlacksmithPacket.class.getResourceAsStream(
					"/assets/endereyesgui/rangeddamagemodifier.conf");
			if (is == null) return RANGED_DAMAGE;
			java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("\"")) {
					// formato: mod:item=valor
					int eq = line.lastIndexOf('=');
					if (eq < 0) continue;
					String key = line.substring(1, line.indexOf('"', 1));
					float val  = Float.parseFloat(line.substring(eq + 1).trim());
					RANGED_DAMAGE.put(key, val);
				}
			}
		} catch (Exception e) { /* ignorar */ }
		return RANGED_DAMAGE;
	}

	// Daño base de arco en Minecraft: 9 corazones máximo con carga completa
	// El multiplicador del conf se aplica sobre ese base

	// min siempre 1 corazon, max = mult * 4
	private static float[] getRangedDamageForItem(ResourceLocation itemId) {
		Float mult = getRangedDamage().get(itemId.toString());
		if (mult == null) return null;
		return new float[]{ mult * 1.0f, mult * 4.0f };
	}

	private static Map<String, Integer> getItemTiers() {
		if (ITEM_TIERS != null) return ITEM_TIERS;
		try {
			InputStream is = C2SRequestBlacksmithPacket.class.getResourceAsStream(
					"/assets/endereyesgui/item_tiers.json");
			if (is == null) { return ITEM_TIERS = new HashMap<>(); }
			ITEM_TIERS = new Gson().fromJson(new InputStreamReader(is),
					new TypeToken<Map<String, Integer>>(){}.getType());
		} catch (Exception e) { ITEM_TIERS = new HashMap<>(); }
		return ITEM_TIERS;
	}

	private static int getTierForItem(ResourceLocation itemId, int fallback) {
		Integer t = getItemTiers().get(itemId.toString());
		return t != null ? t : fallback;
	}

	private static BlacksmithRecipeData buildData(ResourceLocation id, ItemStack result,
												  String category, int tier, boolean unlocked,
												  List<ItemStack> ingredients, List<ItemStack> baseVariants) {
		var item = result.getItem();
		int durability   = item.getMaxDamage(result);
		int armorPoints  = 0;
		float armorTough = 0f;
		float atkDmg     = 0f;
		float atkSpd     = 0f;
		float atkRange   = 0f;
		int harvestLvl   = 0;
		float mineSpd    = 0f;

		if (item instanceof ArmorItem armor) {
			armorPoints = armor.getDefense();
			armorTough  = armor.getToughness();
		}

		var attrs = item.getDefaultAttributeModifiers(
				net.minecraft.world.entity.EquipmentSlot.MAINHAND);

		var dmgMods = attrs.get(Attributes.ATTACK_DAMAGE);
		if (!dmgMods.isEmpty()) {
			atkDmg = (float)(1.0 + dmgMods.stream()
					.filter(m -> m.getOperation().ordinal() == 0)
					.mapToDouble(m -> m.getAmount()).sum());
		}
		var spdMods = attrs.get(Attributes.ATTACK_SPEED);
		if (!spdMods.isEmpty()) {
			atkSpd = (float)(4.0 + spdMods.stream()
					.filter(m -> m.getOperation().ordinal() == 0)
					.mapToDouble(m -> m.getAmount()).sum());
		}
		try {
			var rangeMods = attrs.get(ForgeMod.ATTACK_RANGE.get());
			atkRange = rangeMods.isEmpty() ? 3.0f
					: (float)(3.0 + rangeMods.stream()
						.filter(m -> m.getOperation().ordinal() == 0)
						.mapToDouble(m -> m.getAmount()).sum());
		} catch (Exception ignored) { atkRange = 3.0f; }

		if (item instanceof TieredItem tiered) {
			harvestLvl = tiered.getTier().getLevel();
			mineSpd    = tiered.getTier().getSpeed();
		}

		// Daño a distancia para bows/crossbows (min=1, max=mult*4)
		ResourceLocation itemId2 = ForgeRegistries.ITEMS.getKey(result.getItem());
		float[] ranged = itemId2 != null ? getRangedDamageForItem(itemId2) : null;
		float rangedMin = ranged != null ? ranged[0] : 0f;
		float rangedMax = ranged != null ? ranged[1] : 0f;

		return new BlacksmithRecipeData(id, result, category, tier, unlocked,
				ingredients, baseVariants, durability, armorPoints, armorTough,
				atkDmg, atkSpd, atkRange, harvestLvl, mineSpd, rangedMin, rangedMax);
	}

	private static List<ItemStack> ingredientsToStacks(net.minecraft.core.NonNullList<Ingredient> ings) {
		List<ItemStack> result = new ArrayList<>();
		for (Ingredient ing : ings) {
			ItemStack[] items = ing.getItems();
			if (items.length > 0) result.add(items[0]);
		}
		return result;
	}

	public void encode(FriendlyByteBuf buf) { }
	public static C2SRequestBlacksmithPacket decode(FriendlyByteBuf buf) {
		return new C2SRequestBlacksmithPacket();
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null) return;

			net.minecraft.stats.ServerRecipeBook recipeBook = sender.getRecipeBook();
			List<BlacksmithRecipeData> dataList = new ArrayList<>();

			try {
				// ── BlacksmithRecipe (sin base) ───────────────────────────────
				for (BlacksmithRecipe bs : sender.getServer().getRecipeManager()
						.getAllRecipesFor(BteMobsRecipeTypes.BLACKSMITH_RECIPE.get())) {
					ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(bs.getResultItem().getItem());
					dataList.add(buildData(
							bs.getId(), bs.getResultItem(),
							bs.getCategory().name(),
							getTierForItem(resultId, bs.getTier()),
							recipeBook.contains(bs.getId()),
							ingredientsToStacks(bs.getIngredients()),
							List.of()   // sin variantes de base
					));
				}

				// ── BlacksmithUpgradeRecipe (con base) ────────────────────────
				// Agrupar por: resultado + ingredientes comunes (sin base)
				// La clave es resultId + ingredientes para detectar variantes
				List<BlacksmithUpgradeRecipe> allUpgrade = sender.getServer().getRecipeManager()
						.getAllRecipesFor(BteMobsRecipeTypes.BLACKSMITH_UPGRADE_RECIPE.get());

				// Mapa: clave de agrupación → lista de recetas
				// getIngredients() incluye el base como último elemento
				Map<String, List<BlacksmithUpgradeRecipe>> grouped = new LinkedHashMap<>();
				for (BlacksmithUpgradeRecipe bu : allUpgrade) {
					// Clave: resultId + ingredientes sin el último (base)
					var ings = bu.getIngredients();
					// El base está al final; los ingredientes "reales" son todos menos el último
					String key = ForgeRegistries.ITEMS.getKey(bu.getResultItem().getItem()) + "|"
							+ ings.subList(0, Math.max(0, ings.size() - 1)).stream()
								.map(ing -> {
									ItemStack[] items = ing.getItems();
									return items.length > 0
											? ForgeRegistries.ITEMS.getKey(items[0].getItem()).toString()
											: "empty";
								})
								.collect(Collectors.joining(","));
					grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(bu);
				}

				for (List<BlacksmithUpgradeRecipe> variants : grouped.values()) {
					BlacksmithUpgradeRecipe first = variants.get(0);
					ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(first.getResultItem().getItem());

					// Ingredientes comunes = todos menos el último (base)
					var allIngs = first.getIngredients();
					// Sublista sin el último elemento (base) -> ingredientes comunes
					net.minecraft.core.NonNullList<Ingredient> commonIngList =
							net.minecraft.core.NonNullList.create();
					for (int i = 0; i < allIngs.size() - 1; i++) commonIngList.add(allIngs.get(i));
					List<ItemStack> commonIngs = ingredientsToStacks(commonIngList);

					// Bases: último ingrediente de cada variante
					List<ItemStack> bases = new ArrayList<>();
					for (BlacksmithUpgradeRecipe bu : variants) {
						var buIngs = bu.getIngredients();
						if (!buIngs.isEmpty()) {
							ItemStack[] baseItems = buIngs.get(buIngs.size() - 1).getItems();
							if (baseItems.length > 0) bases.add(baseItems[0]);
						}
					}

					// Desbloqueada si cualquiera de las variantes está desbloqueada
					boolean unlocked = variants.stream()
							.anyMatch(bu -> recipeBook.contains(bu.getId()));

					dataList.add(buildData(
							first.getId(), first.getResultItem(),
							first.getCategory().name(),
							getTierForItem(resultId, 0),
							unlocked, commonIngs, bases
					));
				}

			} catch (Exception e) {
				LOGGER.warn("[Blacksmith] Error: {}", e.toString());
			}

			LOGGER.info("[Blacksmith] Total: {}, unlocked: {}",
					dataList.size(),
					dataList.stream().filter(BlacksmithRecipeData::isUnlocked).count());

			dataList.sort(Comparator
					.comparing(BlacksmithRecipeData::getCategory)
					.thenComparingInt(BlacksmithRecipeData::getTier)
					.thenComparing(d -> d.getResult().getHoverName().getString().toLowerCase()));

			Networking.sendBlacksmithList(sender, dataList);
		});
		ctx.get().setPacketHandled(true);
	}
}
