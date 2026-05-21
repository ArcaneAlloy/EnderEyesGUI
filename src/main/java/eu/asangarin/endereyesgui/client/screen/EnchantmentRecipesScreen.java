package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.compat.JeiCompat;
import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import eu.asangarin.endereyesgui.util.WarlockPotionRecipeData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnchantmentRecipesScreen extends Screen {

	private final List<EnchantmentRecipeData> recipes;
	private final List<WarlockPotionRecipeData> potions;
	private final int eyesEarned;
	private final boolean openPotions;

	private static final float LIST_FRACTION = 0.50f;
	private static final int   TITLE_H       = 20;
	private static final int   TAB_H         = 16;
	private static final int   BOTTOM_H      = 28;
	private static final int   PAD           = 8;

	private static final int C_UNLOCKED   = 0xFF55FF55;
	private static final int C_LOCKED     = 0xFFFF5555;
	private static final int C_LIST_BG    = 0xAA080018;
	private static final int C_PANEL_BG   = 0xCC100030;
	private static final int C_PANEL_BORD = 0xFF8800CC;
	private static final int C_DIVIDER    = 0xFF550088;
	private static final int C_SELECTED   = 0x887700BB;
	private static final int C_HOVER      = 0x447700BB;
	private static final int C_TAB_ACTIVE = 0xBB6600CC;
	private static final int C_TAB_IDLE   = 0x66330066;
	private static final int C_TAB_BORD   = 0xFF8800CC;

	private static final String ICON_OK  = "\u2714 ";
	private static final String ICON_ERR = "\u2718 ";

	private static final String[] ENCH_TABS = {
		"all", "helmet", "chestplate", "leggings", "boots", "melee", "tools", "ranged", "other"
	};
	private static final String[] ENCH_TAB_KEYS = {
		"endereyesgui.enchantments.tab.all",
		"endereyesgui.enchantments.tab.helmet",
		"endereyesgui.enchantments.tab.chestplate",
		"endereyesgui.enchantments.tab.leggings",
		"endereyesgui.enchantments.tab.boots",
		"endereyesgui.enchantments.tab.melee",
		"endereyesgui.enchantments.tab.tools",
		"endereyesgui.enchantments.tab.ranged",
		"endereyesgui.enchantments.tab.other"
	};
	private static final int TABS_PER_ROW = 5;

	private String activeEnchTab = "all";
	private RecipeList recipeList;
	private EditBox searchBox;
	private String searchTerm = "";
	private ItemStack jeiHoveredItem = ItemStack.EMPTY;
	private boolean hoveringResult = false;
	private int potionPanelScroll = 0;
	private int potionPanelContentHeight = 0;

	private EnchantmentRecipeData selectedEnch = null;
	private WarlockPotionRecipeData selectedPotion = null;
	private ItemStack hoveredIngredient = null;
	private int hoveredIngX, hoveredIngY;

	public EnchantmentRecipesScreen(List<EnchantmentRecipeData> recipes,
	                                 List<WarlockPotionRecipeData> potions, int eyesEarned) {
		this(recipes, potions, eyesEarned, "enchantments");
	}

	public EnchantmentRecipesScreen(List<EnchantmentRecipeData> recipes,
	                                 List<WarlockPotionRecipeData> potions, int eyesEarned, String initialTab) {
		super(Component.empty());
		this.recipes     = new ArrayList<>(recipes);
		this.potions     = new ArrayList<>(potions);
		this.eyesEarned  = eyesEarned;
		this.openPotions = "potions".equals(initialTab);
	}

	// ── Categorización ────────────────────────────────────────────────────────

	/**
	 * Mapa de override manual para encantamientos de mods externos que usan
	 * EnchantmentCategory.ARMOR genérico (Dungeons Gear, Dragon Enchants, etc.)
	 * y cuyos EquipmentSlot[] son privados o simplemente incorrectos.
	 * Prioridad máxima: si un encantamiento está aquí, se usa este mapa.
	 */
	private static final java.util.Map<String, String[]> SLOT_OVERRIDES;
	static {
		SLOT_OVERRIDES = new java.util.HashMap<>();
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
		// ── Revised Phantoms ──────────────────────────────────────────────────
		SLOT_OVERRIDES.put("revised_phantoms:suspending", new String[]{ "other" });
		// ── Step ─────────────────────────────────────────────────────────────
		SLOT_OVERRIDES.put("step:stepping", new String[]{ "boots" });
		// ── Supplementaries ───────────────────────────────────────────────────
		SLOT_OVERRIDES.put("supplementaries:stasis", new String[]{ "other" });
		// ── Minecraft vanilla ───────────────────────────────────────────────────
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
				// ── Twilight Forest ───────────────────────────────────────────────────
		SLOT_OVERRIDES.put("twilightforest:chill_aura",  new String[]{ "chestplate" });
		SLOT_OVERRIDES.put("twilightforest:destruction", new String[]{ "melee" });
		SLOT_OVERRIDES.put("twilightforest:fire_react",  new String[]{ "chestplate" });
	}

	/**
	 * Lee el campo EquipmentSlot[] privado de Enchantment via reflection.
	 * Devuelve null si no es accesible (fallback a canEnchant).
	 */
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

	/**
	 * Devuelve todas las pestañas a las que pertenece un encantamiento.
	 * Orden de prioridad:
	 * 1. SLOT_OVERRIDES (mapa manual para mods con categorías genéricas)
	 * 2. EquipmentSlot[] via reflection (vanilla + mods bien definidos)
	 * 3. canEnchant() con items concretos (fallback para armas/herramientas)
	 */
	private Set<String> getEnchCategories(EnchantmentRecipeData data) {
		Set<String> cats = new HashSet<>();
		String enchId = data.getEnchantmentId().toString();

		// ── 1. Override manual ───────────────────────────────────────────────
		String[] override = SLOT_OVERRIDES.get(enchId);
		if (override != null) {
			for (String s : override) cats.add(s);
			return cats;
		}

		Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(data.getEnchantmentId());
		if (ench == null) { cats.add("other"); return cats; }

		// ── 2. Slots via reflection ──────────────────────────────────────────
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

		// ── 3. Armas / herramientas por canEnchant ───────────────────────────
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
			// Armor: solo si reflection no encontró slots específicos de armor
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

	private boolean enchMatchesTab(EnchantmentRecipeData data, String tab) {
		if ("all".equals(tab)) return true;
		return getEnchCategories(data).contains(tab);
	}

	// ── Layout helpers ────────────────────────────────────────────────────────

	private int tabRowsHeight() { return TAB_H * 2; }

	private int listTop() {
		return openPotions ? TITLE_H + 18 : TITLE_H + tabRowsHeight() + 18;
	}

	@Override
	protected void init() {
		super.init();
		int listW   = (int) (width * LIST_FRACTION);
		int lt      = listTop();
		int listBot = height - BOTTOM_H;

		int searchY = openPotions ? TITLE_H + 2 : TITLE_H + tabRowsHeight() + 2;
		searchBox = new EditBox(font, 2, searchY, listW - 4, 14,
				Component.translatable("endereyesgui.search"));
		searchBox.setMaxLength(50);
		searchBox.setResponder(text -> {
			searchTerm = text.toLowerCase();
			rebuildList(listW, lt, listBot);
		});
		addRenderableWidget(searchBox);

		recipeList = new RecipeList(minecraft, listW, listBot - lt, lt);
		addWidget(recipeList);

		addRenderableWidget(new Button(
				width / 2 - 40, height - BOTTOM_H + 4, 80, 20,
				Component.translatable("gui.back"),
				btn -> Networking.requestEnderGUI()
		));
	}

	private void rebuildList(int listW, int lt, int listBot) {
		children().removeIf(c -> c instanceof RecipeList);
		selectedEnch   = null;
		selectedPotion = null;
		recipeList = new RecipeList(minecraft, listW, listBot - lt, lt);
		addWidget(recipeList);
	}

	@Override
	public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
		renderBackground(stack);
		hoveredIngredient = null;
		jeiHoveredItem    = ItemStack.EMPTY;
		hoveringResult    = false;

		String titleKey = openPotions ? "endereyesgui.warlock.potions.title" : "endereyesgui.enchantments.title";
		drawCenteredString(stack, font, Component.translatable(titleKey), width / 2, 6, 0xFFFFFF);

		int listW = (int) (width * LIST_FRACTION);
		if (!openPotions) renderEnchantmentTabs(stack, mx, my, listW);

		recipeList.render(stack, mx, my, delta);

		if (!openPotions) renderEnchantmentPanel(stack, mx, my);
		else              renderPotionPanel(stack, mx, my);

		super.render(stack, mx, my, delta);

		if (hoveredIngredient != null)
			renderTooltip(stack, hoveredIngredient, hoveredIngX, hoveredIngY);
		else if (hoveringResult) {
			if (!openPotions && selectedEnch != null)
				renderTooltip(stack, buildBookStack(selectedEnch), mx, my);
			else if (openPotions && selectedPotion != null)
				renderTooltip(stack, selectedPotion.getBasePotion(), mx, my);
		}
	}

	private void renderEnchantmentTabs(PoseStack stack, int mx, int my, int listW) {
		int totalTabs = ENCH_TABS.length;
		int tabY      = TITLE_H + 2;
		for (int row = 0; row < 2; row++) {
			int startIdx   = row * TABS_PER_ROW;
			int endIdx     = Math.min(startIdx + TABS_PER_ROW, totalTabs);
			int countInRow = endIdx - startIdx;
			int tabW       = listW / countInRow;
			for (int i = startIdx; i < endIdx; i++) {
				int col    = i - startIdx;
				String tab = ENCH_TABS[i];
				boolean active = tab.equals(activeEnchTab);
				int tx = col * tabW, ty = tabY + row * TAB_H;
				fill(stack, tx, ty, tx + tabW, ty + TAB_H, active ? C_TAB_ACTIVE : C_TAB_IDLE);
				fill(stack, tx,          ty,           tx + tabW, ty + 1,       C_TAB_BORD);
				fill(stack, tx,          ty + TAB_H-1, tx + tabW, ty + TAB_H,   C_TAB_BORD);
				fill(stack, tx,          ty,           tx + 1,    ty + TAB_H,   C_TAB_BORD);
				fill(stack, tx + tabW-1, ty,           tx + tabW, ty + TAB_H,   C_TAB_BORD);
				drawCenteredString(stack, font, Component.translatable(ENCH_TAB_KEYS[i]),
						tx + tabW / 2, ty + (TAB_H - font.lineHeight) / 2, 0xFFFFFF);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int btn) {
		if (!openPotions) {
			int listW     = (int) (width * LIST_FRACTION);
			int totalTabs = ENCH_TABS.length;
			int tabY      = TITLE_H + 2;
			for (int row = 0; row < 2; row++) {
				int startIdx   = row * TABS_PER_ROW;
				int endIdx     = Math.min(startIdx + TABS_PER_ROW, totalTabs);
				int countInRow = endIdx - startIdx;
				int tabW       = listW / countInRow;
				int ty         = tabY + row * TAB_H;
				if (my >= ty && my < ty + TAB_H) {
					for (int i = startIdx; i < endIdx; i++) {
						int tx = (i - startIdx) * tabW;
						if (mx >= tx && mx < tx + tabW) {
							String newTab = ENCH_TABS[i];
							if (!newTab.equals(activeEnchTab)) {
								activeEnchTab = newTab;
								searchTerm    = "";
								if (searchBox != null) searchBox.setValue("");
								rebuildList(listW, listTop(), height - BOTTOM_H);
							}
							return true;
						}
					}
				}
			}
		}
		return super.mouseClicked(mx, my, btn);
	}

	// ── Panel encantamientos ──────────────────────────────────────────────────

	private void renderEnchantmentPanel(PoseStack stack, int mx, int my) {
		int listW = (int) (width * LIST_FRACTION);
		int px = listW + PAD, py = TITLE_H + 2;
		int pw = width - listW - PAD * 2 - PAD;
		int ph = height - BOTTOM_H - py;
		int cx = px + pw / 2;

		fill(stack, px, py, px + pw, py + ph, C_PANEL_BG);
		fill(stack, px,          py,          px + pw,     py + 1,      C_PANEL_BORD);
		fill(stack, px,          py + ph - 1, px + pw,     py + ph,     C_PANEL_BORD);
		fill(stack, px,          py,          px + 1,      py + ph,     C_PANEL_BORD);
		fill(stack, px + pw - 1, py,          px + pw,     py + ph,     C_PANEL_BORD);

		if (selectedEnch == null) {
			drawHint(stack, cx, py, ph, Component.translatable("endereyesgui.enchantments.detail.hint"));
			return;
		}
		int ty = py + PAD;
		ItemStack book = buildBookStack(selectedEnch);
		minecraft.getItemRenderer().renderAndDecorateFakeItem(book, px + PAD, ty);
		hoveringResult = (mx >= px + PAD && mx < px + PAD + 16 && my >= ty && my < ty + 16);
		if (hoveringResult) jeiHoveredItem = book;
		font.draw(stack, getEnchantmentName(selectedEnch).getString(),
				px + PAD + 16 + 4, ty + (16 - font.lineHeight) / 2, 0xDD00FF);
		ty += 22;
		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 6;

		boolean canAfford = eyesEarned >= selectedEnch.getNeedEyes();
		if (selectedEnch.isUnlocked()) {
			drawCenteredString(stack, font,
					Component.literal(ICON_OK).withStyle(ChatFormatting.GREEN)
					.append(Component.translatable("endereyesgui.enchantments.detail.unlocked").withStyle(ChatFormatting.GREEN)),
					cx, ty, 0xFFFFFF);
		} else {
			drawCenteredString(stack, font,
					Component.translatable("endereyesgui.enchantments.detail.eyes_obtained", eyesEarned)
					.withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.YELLOW), cx, ty, 0xFFFFFF);
			ty += font.lineHeight + 3;
			drawCenteredString(stack, font,
					Component.translatable("endereyesgui.enchantments.detail.eyes_cost", selectedEnch.getNeedEyes())
					.withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.RED), cx, ty, 0xFFFFFF);
		}
		ty += font.lineHeight + 8;
		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 6;

		if (selectedEnch.getExperience() > 0) {
			drawCenteredString(stack, font,
					Component.translatable("endereyesgui.enchantments.detail.experience", selectedEnch.getExperience())
					.withStyle(ChatFormatting.GREEN), cx, ty, 0xFFFFFF);
			ty += font.lineHeight + 4;
		}
		List<ItemStack> ings = selectedEnch.getIngredients();
		if (!ings.isEmpty()) {
			drawCenteredString(stack, font,
					Component.translatable("endereyesgui.blacksmith.ingredients").withStyle(ChatFormatting.GRAY),
					cx, ty, 0xCCCCCC);
			ty += font.lineHeight + 3;
			int ix = px + PAD;
			for (ItemStack ing : ings) {
				if (ix + 18 > px + pw - PAD) { ix = px + PAD; ty += 18; }
				minecraft.getItemRenderer().renderAndDecorateFakeItem(ing, ix, ty);
				minecraft.getItemRenderer().renderGuiItemDecorations(font, ing, ix, ty);
				if (mx >= ix && mx < ix + 16 && my >= ty && my < ty + 16) {
					hoveredIngredient = ing; hoveredIngX = mx; hoveredIngY = my; jeiHoveredItem = ing;
				}
				ix += 18;
			}
			ty += 22;
		}
		if (ty < py + ph - PAD) { fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 6; }
		var ench = ForgeRegistries.ENCHANTMENTS.getValue(selectedEnch.getEnchantmentId());
		if (ench != null) {
			var descKey = "enchantment.description." + selectedEnch.getEnchantmentId().getNamespace()
					+ "." + selectedEnch.getEnchantmentId().getPath();
			var desc = Component.translatable(descKey);
			if (!desc.getString().equals(descKey)) {
				for (FormattedCharSequence line : font.split(desc.copy().withStyle(ChatFormatting.GRAY), pw - PAD * 2)) {
					font.draw(stack, line, cx - font.width(line) / 2, ty, 0xAAAAAA);
					ty += font.lineHeight + 2;
				}
			}
		}
	}

	// ── Panel pociones ────────────────────────────────────────────────────────

	private void renderPotionPanel(PoseStack stack, int mx, int my) {
		int listW = (int) (width * LIST_FRACTION);
		int px = listW + PAD, py = TITLE_H + 2;
		int pw = width - listW - PAD * 2 - PAD;
		int ph = height - BOTTOM_H - py;
		int cx = px + pw / 2;

		fill(stack, px, py, px + pw, py + ph, C_PANEL_BG);
		fill(stack, px,          py,          px + pw,     py + 1,      C_PANEL_BORD);
		fill(stack, px,          py + ph - 1, px + pw,     py + ph,     C_PANEL_BORD);
		fill(stack, px,          py,          px + 1,      py + ph,     C_PANEL_BORD);
		fill(stack, px + pw - 1, py,          px + pw,     py + ph,     C_PANEL_BORD);

		if (selectedPotion == null) {
			drawHint(stack, cx, py, ph, Component.translatable("endereyesgui.warlock.potions.hint"));
			return;
		}
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		double guiScale = mc.getWindow().getGuiScale();
		int screenH = mc.getWindow().getHeight();
		com.mojang.blaze3d.systems.RenderSystem.enableScissor(
				(int)(px * guiScale), (int)(screenH - (py + ph) * guiScale),
				(int)(pw * guiScale), (int)(ph * guiScale));

		int ty = py + PAD - potionPanelScroll;
		ItemStack base = selectedPotion.getBasePotion();
		if (base.isEmpty()) {
			net.minecraft.resources.ResourceLocation tryId =
					net.minecraft.resources.ResourceLocation.tryParse(selectedPotion.getEffectId());
			if (tryId != null) {
				net.minecraft.world.item.alchemy.Potion p2 =
						net.minecraftforge.registries.ForgeRegistries.POTIONS.getValue(tryId);
				if (p2 != null && !p2.getEffects().isEmpty())
					base = net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), p2);
			}
			if (base.isEmpty()) base = new ItemStack(Items.POTION);
		}
		if (!base.isEmpty()) {
			minecraft.getItemRenderer().renderAndDecorateFakeItem(base, px + PAD, ty);
			hoveringResult = (mx >= px + PAD && mx < px + PAD + 16 && my >= ty && my < ty + 16);
			if (hoveringResult) jeiHoveredItem = base;
		}
		String effectId = selectedPotion.getEffectId();
		String[] words = effectId.substring(effectId.lastIndexOf(':') + 1).split("_");
		StringBuilder sb = new StringBuilder();
		for (String w : words) { if (!w.isEmpty()) { if (sb.length() > 0) sb.append(" "); sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)); } }
		font.draw(stack, sb.toString(), px + PAD + 20, ty + (16 - font.lineHeight) / 2, 0xDD00FF);
		ty += 22;
		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 6;
		if (selectedPotion.isUnlocked()) {
			drawCenteredString(stack, font, Component.literal(ICON_OK).withStyle(ChatFormatting.GREEN)
					.append(Component.translatable("endereyesgui.enchantments.detail.unlocked").withStyle(ChatFormatting.GREEN)), cx, ty, 0xFFFFFF);
		} else {
			drawCenteredString(stack, font, Component.literal(ICON_ERR).withStyle(ChatFormatting.RED)
					.append(Component.translatable("endereyesgui.enchantments.detail.locked").withStyle(ChatFormatting.RED)), cx, ty, 0xFFFFFF);
		}
		ty += font.lineHeight + 8;
		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 6;

		net.minecraft.world.item.alchemy.Potion pot = net.minecraft.world.item.alchemy.PotionUtils.getPotion(base);
		ItemStack splashPotion    = net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), pot);
		ItemStack lingeringPotion = net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), pot);
		ItemStack baseIng = selectedPotion.getIngredient();
		ItemStack gunpowder = new ItemStack(Items.GUNPOWDER), dragonBreath = new ItemStack(Items.DRAGON_BREATH), glowstone = new ItemStack(Items.GLOWSTONE_DUST);
		String eid = selectedPotion.getEffectId();
		net.minecraft.resources.ResourceLocation strongId = new net.minecraft.resources.ResourceLocation(
				eid.substring(0, eid.indexOf(':') + 1) + "strong_" + eid.substring(eid.indexOf(':') + 1));
		net.minecraft.world.item.alchemy.Potion strongPot = net.minecraftforge.registries.ForgeRegistries.POTIONS.getValue(strongId);
		if (strongPot != null && strongPot.getEffects().isEmpty()) strongPot = null;
		ItemStack potionII    = strongPot != null ? net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), strongPot) : ItemStack.EMPTY;
		ItemStack splashII    = strongPot != null ? net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), strongPot) : ItemStack.EMPTY;
		ItemStack lingeringII = strongPot != null ? net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), strongPot) : ItemStack.EMPTY;
		boolean isSwiftness = "minecraft:swiftness".equals(eid);
		ItemStack gazelleHorn = ItemStack.EMPTY;
		if (isSwiftness) { net.minecraft.world.item.Item gh = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("alexsmobs", "gazelle_horn")); if (gh != null) gazelleHorn = new ItemStack(gh); }

		ty = renderVariantRow(stack, mx, my, px, ty, pw, base, "endereyesgui.warlock.potion.normal", new ItemStack[]{baseIng}); ty += 4;
		ty = renderVariantRow(stack, mx, my, px, ty, pw, splashPotion, "endereyesgui.warlock.potion.splash", new ItemStack[]{baseIng, gunpowder}); ty += 4;
		ty = renderVariantRow(stack, mx, my, px, ty, pw, lingeringPotion, "endereyesgui.warlock.potion.lingering", new ItemStack[]{baseIng, dragonBreath});
		if (!potionII.isEmpty()) {
			ty += 6; fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 4;
			ty = renderVariantRow(stack, mx, my, px, ty, pw, potionII, "endereyesgui.warlock.potion.normal_ii", new ItemStack[]{baseIng, glowstone}); ty += 4;
			ty = renderVariantRow(stack, mx, my, px, ty, pw, splashII, "endereyesgui.warlock.potion.splash_ii", new ItemStack[]{baseIng, glowstone, gunpowder}); ty += 4;
			renderVariantRow(stack, mx, my, px, ty, pw, lingeringII, "endereyesgui.warlock.potion.lingering_ii", new ItemStack[]{baseIng, glowstone, dragonBreath}); ty += 18;
		}
		if (isSwiftness && !gazelleHorn.isEmpty()) {
			ty += 6; fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 4;
			ItemStack swiftIII = !potionII.isEmpty() ? potionII : net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraftforge.registries.ForgeRegistries.POTIONS.getValue(strongId) != null ? net.minecraftforge.registries.ForgeRegistries.POTIONS.getValue(strongId) : pot);
			ty = renderVariantRow(stack, mx, my, px, ty, pw, swiftIII, "endereyesgui.warlock.potion.normal_iii", new ItemStack[]{gazelleHorn, glowstone}); ty += 4;
			ItemStack splashIII    = net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION),    net.minecraft.world.item.alchemy.PotionUtils.getPotion(swiftIII));
			ItemStack lingeringIII = net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), net.minecraft.world.item.alchemy.PotionUtils.getPotion(swiftIII));
			ty = renderVariantRow(stack, mx, my, px, ty, pw, splashIII, "endereyesgui.warlock.potion.splash_iii", new ItemStack[]{gazelleHorn, glowstone, gunpowder}); ty += 4;
			renderVariantRow(stack, mx, my, px, ty, pw, lingeringIII, "endereyesgui.warlock.potion.lingering_iii", new ItemStack[]{gazelleHorn, glowstone, dragonBreath});
		}
		potionPanelContentHeight = (ty + potionPanelScroll) - py;
		com.mojang.blaze3d.systems.RenderSystem.disableScissor();
		if (potionPanelContentHeight > ph) {
			int trackX = px + pw - 6, trackY = py + 4, trackH = ph - 8;
			fill(stack, trackX, trackY, trackX + 3, trackY + trackH, 0x33FFFFFF);
			int thumbH = Math.max(10, trackH * ph / potionPanelContentHeight);
			int thumbY = trackY + (potionPanelScroll * (trackH - thumbH) / Math.max(1, potionPanelContentHeight - ph));
			fill(stack, trackX, thumbY, trackX + 3, thumbY + thumbH, 0xBB8800CC);
		}
	}

	private int renderVariantRow(PoseStack stack, int mx, int my, int px, int ty, int pw, ItemStack potion, String labelKey, ItemStack[] ingredients) {
		if (!potion.isEmpty()) {
			minecraft.getItemRenderer().renderAndDecorateFakeItem(potion, px + PAD, ty);
			minecraft.getItemRenderer().renderGuiItemDecorations(font, potion, px + PAD, ty);
			if (mx >= px + PAD && mx < px + PAD + 16 && my >= ty && my < ty + 16) { hoveredIngredient = potion; hoveredIngX = mx; hoveredIngY = my; jeiHoveredItem = potion; }
		}
		font.draw(stack, Component.translatable(labelKey).getString(), px + PAD + 20, ty + (16 - font.lineHeight) / 2, 0xCCCCCC);
		int ix = px + pw - PAD - 16 * ingredients.length - 6 * (ingredients.length - 1);
		for (int i = 0; i < ingredients.length; i++) {
			if (i > 0) ix += 2;
			ItemStack ing = ingredients[i];
			if (!ing.isEmpty()) {
				minecraft.getItemRenderer().renderAndDecorateFakeItem(ing, ix, ty);
				minecraft.getItemRenderer().renderGuiItemDecorations(font, ing, ix, ty);
				if (mx >= ix && mx < ix + 16 && my >= ty && my < ty + 16) { hoveredIngredient = ing; hoveredIngX = mx; hoveredIngY = my; jeiHoveredItem = ing; }
				ix += 18;
			}
		}
		return ty + 18;
	}

	private void drawHint(PoseStack stack, int cx, int py, int ph, Component hint) {
		List<FormattedCharSequence> lines = font.split(hint.copy().withStyle(ChatFormatting.GRAY), (int)(width * LIST_FRACTION));
		int sy = py + ph / 2 - (lines.size() * (font.lineHeight + 2)) / 2;
		for (int i = 0; i < lines.size(); i++) { int lx = cx - font.width(lines.get(i)) / 2; font.draw(stack, lines.get(i), lx, sy + i * (font.lineHeight + 2), 0x888888); }
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (JeiCompat.isAvailable() && !jeiHoveredItem.isEmpty()) {
			var key = com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode);
			if (JeiCompat.isShowRecipesKey(key)) { JeiCompat.showRecipes(jeiHoveredItem); return true; }
			if (JeiCompat.isShowUsagesKey(key))  { JeiCompat.showUsages(jeiHoveredItem);  return true; }
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		if (openPotions && mx > (int)(width * LIST_FRACTION) && selectedPotion != null) {
			int ph = height - BOTTOM_H - (TITLE_H + 2);
			potionPanelScroll = Math.max(0, Math.min((int)(potionPanelScroll - delta * 10), potionPanelContentHeight - ph + PAD * 2));
			return true;
		}
		return super.mouseScrolled(mx, my, delta);
	}

	@Override public boolean isPauseScreen() { return false; }

	private Component getEnchantmentName(EnchantmentRecipeData data) {
		Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(data.getEnchantmentId());
		return ench != null ? ench.getFullname(data.getLevel()) : Component.literal(data.getEnchantmentId().getPath());
	}

	private ItemStack buildBookStack(EnchantmentRecipeData data) {
		Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(data.getEnchantmentId());
		if (ench == null) return new ItemStack(Items.BOOK);
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(ench, data.getLevel()));
		return book;
	}

	private class RecipeList extends ObjectSelectionList<RecipeList.BaseEntry> {
		RecipeList(Minecraft mc, int width, int height, int top) {
			super(mc, width, height, top, top + height, 14);
			setRenderHeader(false, 0); setRenderBackground(false); setRenderTopAndBottom(false);
			populate();
		}
		void populate() {
			clearEntries();
			if (!openPotions) {
				for (EnchantmentRecipeData d : recipes) {
					if (!enchMatchesTab(d, activeEnchTab)) continue;
					if (searchTerm.isEmpty() || getEnchantmentName(d).getString().toLowerCase().contains(searchTerm))
						addEntry(new EnchEntry(d));
				}
			} else {
				for (WarlockPotionRecipeData d : potions) {
					String name = d.getEffectId().substring(d.getEffectId().lastIndexOf(':') + 1).replace("_", " ");
					if (searchTerm.isEmpty() || name.contains(searchTerm)) addEntry(new PotionEntry(d));
				}
			}
		}
		@Override public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
			fill(stack, 0, y0, getScrollbarPosition() + 6, y1, C_LIST_BG);
			super.render(stack, mx, my, delta);
		}
		@Override protected int getScrollbarPosition() { return this.width - 6; }
		@Override public boolean isFocused() { return false; }
		abstract class BaseEntry extends ObjectSelectionList.Entry<BaseEntry> {}

		class EnchEntry extends BaseEntry {
			private final EnchantmentRecipeData data;
			EnchEntry(EnchantmentRecipeData data) { this.data = data; }
			@Override public @NotNull Component getNarration() { return getEnchantmentName(data); }
			@Override public boolean mouseClicked(double mx, double my, int btn) { selectedEnch = data; RecipeList.this.setSelected(this); return true; }
			@Override public void render(@NotNull PoseStack stack, int index, int top, int left, int width, int height, int mx, int my, boolean hovered, float delta) {
				boolean sel = RecipeList.this.getSelected() == this;
				if (sel)          fill(stack, left, top, left + width, top + height, C_SELECTED);
				else if (hovered) fill(stack, left, top, left + width, top + height, C_HOVER);
				String icon = data.isUnlocked() ? ICON_OK : ICON_ERR;
				font.draw(stack, icon, left + 2, top + 2, data.isUnlocked() ? C_UNLOCKED : C_LOCKED);
				font.draw(stack, font.plainSubstrByWidth(getEnchantmentName(data).getString(), width - font.width(icon) - 8),
						left + 2 + font.width(icon), top + 2, data.isUnlocked() ? 0xEE88FF : 0x996699);
			}
		}

		class PotionEntry extends BaseEntry {
			private final WarlockPotionRecipeData data;
			PotionEntry(WarlockPotionRecipeData data) { this.data = data; }
			@Override public @NotNull Component getNarration() { return Component.literal(data.getEffectId()); }
			@Override public boolean mouseClicked(double mx, double my, int btn) { selectedPotion = data; potionPanelScroll = 0; RecipeList.this.setSelected(this); return true; }
			@Override public void render(@NotNull PoseStack stack, int index, int top, int left, int width, int height, int mx, int my, boolean hovered, float delta) {
				boolean sel = RecipeList.this.getSelected() == this;
				if (sel)          fill(stack, left, top, left + width, top + height, C_SELECTED);
				else if (hovered) fill(stack, left, top, left + width, top + height, C_HOVER);
				String icon = data.isUnlocked() ? ICON_OK : ICON_ERR;
				font.draw(stack, icon, left + 2, top + 2, data.isUnlocked() ? C_UNLOCKED : C_LOCKED);
				String eid = data.getEffectId();
				String[] wds = eid.substring(eid.lastIndexOf(':') + 1).split("_");
				StringBuilder sb = new StringBuilder();
				for (String w : wds) { if (!w.isEmpty()) { if (sb.length() > 0) sb.append(" "); sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)); } }
				font.draw(stack, font.plainSubstrByWidth(sb.toString(), width - font.width(icon) - 8),
						left + 2 + font.width(icon), top + 2, data.isUnlocked() ? 0xEE88FF : 0x996699);
			}
		}
	}
}
