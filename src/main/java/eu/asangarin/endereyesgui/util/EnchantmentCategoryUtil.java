package eu.asangarin.endereyesgui.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Clasificación de encantamientos en pestañas (melee/ranged/tools/helmet/
 * chestplate/leggings/boots/other). Es la MISMA lógica y el mismo mapa de
 * overrides que usa {@code EnchantmentRecipesScreen} en el cliente, pero
 * extraída a una clase sin dependencias de cliente para poder usarla
 * también en el servidor (p. ej. al construir el resumen de la Fase 3
 * del overlay de desbloqueo de Ender Eyes).
 * <p>
 * IMPORTANTE: si en el futuro se edita {@code SLOT_OVERRIDES} en
 * {@code EnchantmentRecipesScreen}, hay que replicar el cambio aquí (o,
 * mejor, migrar la screen para que use esta clase en vez de su copia
 * propia).
 */
public class EnchantmentCategoryUtil {

	public enum Bucket { COMBAT, DEFENSIVE, TOOLS, OTHER }

	private static final Map<String, String[]> SLOT_OVERRIDES;
	static {
		SLOT_OVERRIDES = new HashMap<>();
		// ── Air Hop ─────────────────────────────────────────────────────────
		SLOT_OVERRIDES.put("airhop:air_hop", new String[]{ "leggings" });
		// ── Alex's Mobs ──────────────────────────────────────────────────────
		SLOT_OVERRIDES.put("alexsmobs:board_return",   new String[]{ "other" });
		SLOT_OVERRIDES.put("alexsmobs:lavawax",        new String[]{ "other" });
		SLOT_OVERRIDES.put("alexsmobs:serpentfriend",  new String[]{ "other" });
		SLOT_OVERRIDES.put("alexsmobs:straddle_jump",  new String[]{ "other" });
		// ── Backpacked ───────────────────────────────────────────────────────
		SLOT_OVERRIDES.put("backpacked:funnelling",  new String[]{ "other" });
		SLOT_OVERRIDES.put("backpacked:imbued_hide", new String[]{ "other" });
		SLOT_OVERRIDES.put("backpacked:looted",      new String[]{ "other" });
		SLOT_OVERRIDES.put("backpacked:marksman",    new String[]{ "other" });
		SLOT_OVERRIDES.put("backpacked:repairman",   new String[]{ "other" });
		// ── Combat Roll ──────────────────────────────────────────────────────
		SLOT_OVERRIDES.put("combatroll:longfooter", new String[]{ "boots" });
		// ── Dash ─────────────────────────────────────────────────────────────
		SLOT_OVERRIDES.put("dash:dashing", new String[]{ "boots" });
		// ── Dragon Enchants ──────────────────────────────────────────────────
		SLOT_OVERRIDES.put("dragonenchants:aftershock",      new String[]{ "melee" });
		SLOT_OVERRIDES.put("dragonenchants:berserk",         new String[]{ "melee" });
		SLOT_OVERRIDES.put("dragonenchants:critical_sunder", new String[]{ "melee" });
		SLOT_OVERRIDES.put("dragonenchants:detonate",        new String[]{ "melee" });
		SLOT_OVERRIDES.put("dragonenchants:devour",          new String[]{ "melee" });
		SLOT_OVERRIDES.put("dragonenchants:end_step",        new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dragonenchants:end_walker",      new String[]{ "boots" });
		SLOT_OVERRIDES.put("dragonenchants:enderference",    new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dragonenchants:eternal",         new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dragonenchants:frost_heart",     new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dragonenchants:homing",          new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dragonenchants:obliterate",      new String[]{ "melee" });
		SLOT_OVERRIDES.put("dragonenchants:true_edge",       new String[]{ "melee" });
		SLOT_OVERRIDES.put("dragonenchants:venomous",        new String[]{ "melee" });
		SLOT_OVERRIDES.put("dragonenchants:water_aspect",    new String[]{ "melee", "ranged" });
		SLOT_OVERRIDES.put("dragonenchants:wind_step",       new String[]{ "leggings" });
		// ── Dungeons Gear ─────────────────────────────────────────────────────
		SLOT_OVERRIDES.put("dungeons_gear:accelerate",       new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:acrobat",          new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:altruistic",       new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:anima_conduit",    new String[]{ "melee", "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:arrow_hoarder",    new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:artifact_synergy", new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:bag_of_souls",     new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:beast_boss",       new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:beast_burst",      new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dungeons_gear:beast_surge",      new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dungeons_gear:beehive",          new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:burning",          new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:busy_bee",         new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:chain_reaction",   new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:chains",           new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:chilling",         new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:committed",        new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:cooldown",         new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dungeons_gear:cooldown_shot",    new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:cowardice",        new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:critical_hit",     new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:death_barter",     new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:deflect",          new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:dodge",            new String[]{ "boots" });
		SLOT_OVERRIDES.put("dungeons_gear:dynamo",           new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:echo",             new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:electrified",      new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:enigma_resonator", new String[]{ "melee", "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:exploding",        new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:exploding_shot",   new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:explorer",         new String[]{ "boots" });
		SLOT_OVERRIDES.put("dungeons_gear:final_shout",      new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:fire_focus",       new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dungeons_gear:fire_trail",       new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:food_reserves",    new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dungeons_gear:fortune_of_the_sea", new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:freezing",         new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:freezing_shot",    new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:frenzied",         new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:fuse_shot",        new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:gale_shot",        new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:gravity",          new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:gravity_pulse",    new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:gravity_shot",     new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:growing",          new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:guarding_strike",  new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:harpoon_shot",     new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:health_synergy",   new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:illagers_bane",    new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:leeching",         new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:life_boost",       new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:life_steal_aura",  new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:lightning_focus",  new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dungeons_gear:master_call",      new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:melee_aura",       new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:multi_roll",       new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:opulent_shield",   new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:overcharge",       new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:pain_cycle",       new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:poison_cloud",     new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:poison_focus",     new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dungeons_gear:potion_aura",      new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:potion_barrier",   new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dungeons_gear:prospector",       new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:radiance",         new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:radiance_shot",    new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:rampaging",        new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:reckless",         new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:recycler",         new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("dungeons_gear:refreshment",      new String[]{ "ranged", "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:replenish",        new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:ricochet",         new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:roll_charge",      new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:rush",             new String[]{ "boots" });
		SLOT_OVERRIDES.put("dungeons_gear:rushdown",         new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:shock_web",        new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:shockwave",        new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:snowball",         new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:soul_focus",       new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dungeons_gear:soul_siphon",      new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:speed_aura",       new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:speed_synergy",    new String[]{ "boots" });
		SLOT_OVERRIDES.put("dungeons_gear:stunning",         new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:supercharge",      new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:surprise_gift",    new String[]{ "helmet" });
		SLOT_OVERRIDES.put("dungeons_gear:swiftfooted",      new String[]{ "boots" });
		SLOT_OVERRIDES.put("dungeons_gear:swirling",         new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:tempo_theft",      new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:thundering",       new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:tumblebee",        new String[]{ "leggings" });
		SLOT_OVERRIDES.put("dungeons_gear:velocity",         new String[]{ "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:void_dodge",       new String[]{ "boots" });
		SLOT_OVERRIDES.put("dungeons_gear:weakening",        new String[]{ "melee" });
		SLOT_OVERRIDES.put("dungeons_gear:wild_rage",        new String[]{ "helmet", "chestplate", "leggings", "boots", "melee", "ranged" });
		SLOT_OVERRIDES.put("dungeons_gear:levitation",       new String[]{ "melee" });
		// ── Illager Revolution ────────────────────────────────────────────────
		SLOT_OVERRIDES.put("illagerrevolutionmod:insight",       new String[]{ "helmet" });
		SLOT_OVERRIDES.put("illagerrevolutionmod:serrated_edge", new String[]{ "melee" });
		SLOT_OVERRIDES.put("illagerrevolutionmod:soul_slash",    new String[]{ "melee" });
		SLOT_OVERRIDES.put("illagerrevolutionmod:wary_lenses",   new String[]{ "helmet" });
		// ── Leap ─────────────────────────────────────────────────────────────
		SLOT_OVERRIDES.put("leap:leaping", new String[]{ "boots" });
		// ── Revised Phantoms ─────────────────────────────────────────────────
		SLOT_OVERRIDES.put("revised_phantoms:suspending", new String[]{ "other" });
		// ── Step ─────────────────────────────────────────────────────────────
		SLOT_OVERRIDES.put("step:stepping", new String[]{ "boots" });
		// ── Supplementaries ──────────────────────────────────────────────────
		SLOT_OVERRIDES.put("supplementaries:stasis", new String[]{ "other" });
		// ── Minecraft vanilla ─────────────────────────────────────────────────
		SLOT_OVERRIDES.put("minecraft:aqua_affinity", new String[]{ "helmet" });
		SLOT_OVERRIDES.put("minecraft:bane_of_arthropods", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:blast_protection", new String[]{ "helmet", "chestplate", "leggings", "boots" });
		SLOT_OVERRIDES.put("minecraft:channeling", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:depth_strider", new String[]{ "boots" });
		SLOT_OVERRIDES.put("minecraft:efficiency", new String[]{ "tools" });
		SLOT_OVERRIDES.put("minecraft:feather_falling", new String[]{ "boots" });
		SLOT_OVERRIDES.put("minecraft:fire_aspect", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:fire_protection", new String[]{ "helmet", "chestplate", "leggings", "boots" });
		SLOT_OVERRIDES.put("minecraft:flame", new String[]{ "ranged" });
		SLOT_OVERRIDES.put("minecraft:fortune", new String[]{ "tools" });
		SLOT_OVERRIDES.put("minecraft:frost_walker", new String[]{ "boots" });
		SLOT_OVERRIDES.put("minecraft:impaling", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:infinity", new String[]{ "ranged" });
		SLOT_OVERRIDES.put("minecraft:knockback", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:looting", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:loyalty", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:luck_of_the_sea", new String[]{ "tools" });
		SLOT_OVERRIDES.put("minecraft:lure", new String[]{ "tools" });
		SLOT_OVERRIDES.put("minecraft:mending", new String[]{ "helmet", "chestplate", "leggings", "boots", "melee", "tools", "ranged" });
		SLOT_OVERRIDES.put("minecraft:multishot", new String[]{ "ranged" });
		SLOT_OVERRIDES.put("minecraft:piercing", new String[]{ "ranged" });
		SLOT_OVERRIDES.put("minecraft:power", new String[]{ "ranged" });
		SLOT_OVERRIDES.put("minecraft:projectile_protection", new String[]{ "helmet", "chestplate", "leggings", "boots" });
		SLOT_OVERRIDES.put("minecraft:protection", new String[]{ "helmet", "chestplate", "leggings", "boots" });
		SLOT_OVERRIDES.put("minecraft:punch", new String[]{ "ranged" });
		SLOT_OVERRIDES.put("minecraft:quick_charge", new String[]{ "ranged" });
		SLOT_OVERRIDES.put("minecraft:respiration", new String[]{ "helmet" });
		SLOT_OVERRIDES.put("minecraft:riptide", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:sharpness", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:silk_touch", new String[]{ "tools" });
		SLOT_OVERRIDES.put("minecraft:smite", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:soul_speed", new String[]{ "boots" });
		SLOT_OVERRIDES.put("minecraft:sweeping_edge", new String[]{ "melee" });
		SLOT_OVERRIDES.put("minecraft:swift_sneak", new String[]{ "leggings" });
		SLOT_OVERRIDES.put("minecraft:thorns", new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("minecraft:unbreaking", new String[]{ "helmet", "chestplate", "leggings", "boots", "melee", "tools", "ranged" });
		// ── Twilight Forest ──────────────────────────────────────────────────
		SLOT_OVERRIDES.put("twilightforest:chill_aura",  new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("twilightforest:destruction", new String[]{ "melee" });
		SLOT_OVERRIDES.put("twilightforest:fire_react",  new String[]{ "chestplate" });
	}

	@SuppressWarnings("unchecked")
	private static EquipmentSlot[] getEnchantmentSlots(Enchantment ench) {
		try {
			for (java.lang.reflect.Field f : Enchantment.class.getDeclaredFields()) {
				if (f.getType() == EquipmentSlot[].class) {
					f.setAccessible(true);
					return (EquipmentSlot[]) f.get(ench);
				}
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static Set<String> getEnchCategories(ResourceLocation enchantmentId) {
		Set<String> cats = new HashSet<>();
		String enchId = enchantmentId.toString();

		String[] override = SLOT_OVERRIDES.get(enchId);
		if (override != null) {
			for (String s : override) cats.add(s);
			return cats;
		}

		Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
		if (ench == null) { cats.add("other"); return cats; }

		EquipmentSlot[] slots = getEnchantmentSlots(ench);
		boolean hasArmorSlot = false;
		if (slots != null && slots.length > 0) {
			for (EquipmentSlot slot : slots) {
				switch (slot) {
					case HEAD:  cats.add("helmet");     hasArmorSlot = true; break;
					case CHEST: cats.add("chestplate"); hasArmorSlot = true; break;
					case LEGS:  cats.add("leggings");   hasArmorSlot = true; break;
					case FEET:  cats.add("boots");      hasArmorSlot = true; break;
					default:    break;
				}
			}
		}

		EnchantmentCategory cat = ench.category;
		if (cat != null) {
			if (cat.canEnchant(Items.IRON_SWORD))   cats.add("melee");
			if (cat.canEnchant(Items.IRON_AXE))     cats.add("melee");
			if (cat.canEnchant(Items.TRIDENT))      cats.add("melee");
			if (cat.canEnchant(Items.IRON_PICKAXE)) cats.add("tools");
			if (cat.canEnchant(Items.IRON_SHOVEL))  cats.add("tools");
			if (cat.canEnchant(Items.IRON_HOE))     cats.add("tools");
			if (cat.canEnchant(Items.BOW))          cats.add("ranged");
			if (cat.canEnchant(Items.CROSSBOW))     cats.add("ranged");
			if (!hasArmorSlot) {
				if (cat.canEnchant(Items.IRON_HELMET))     cats.add("helmet");
				if (cat.canEnchant(Items.IRON_CHESTPLATE)) cats.add("chestplate");
				if (cat.canEnchant(Items.IRON_LEGGINGS))   cats.add("leggings");
				if (cat.canEnchant(Items.IRON_BOOTS))      cats.add("boots");
			}
		}

		if (cats.isEmpty()) cats.add("other");
		return cats;
	}

	/**
	 * Agrupa las pestañas finas (melee/ranged/tools/helmet/chestplate/...)
	 * en los 3 grandes bloques que pide el overlay de desbloqueo:
	 * combate (melee+ranged), defensivo (armadura) y herramientas.
	 * Un encantamiento puede caer en más de un bloque (p. ej. mending).
	 */
	public static Set<Bucket> getBuckets(ResourceLocation enchantmentId) {
		Set<String> cats = getEnchCategories(enchantmentId);
		Set<Bucket> buckets = new HashSet<>();
		for (String c : cats) {
			switch (c) {
				case "melee":
				case "ranged":
					buckets.add(Bucket.COMBAT);
					break;
				case "helmet":
				case "chestplate":
				case "leggings":
				case "boots":
					buckets.add(Bucket.DEFENSIVE);
					break;
				case "tools":
					buckets.add(Bucket.TOOLS);
					break;
				default:
					break;
			}
		}
		if (buckets.isEmpty()) buckets.add(Bucket.OTHER);
		return buckets;
	}
}
