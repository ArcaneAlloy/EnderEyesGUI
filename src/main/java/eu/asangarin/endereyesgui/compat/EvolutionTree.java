package eu.asangarin.endereyesgui.compat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.shoqapik.btemobs.recipe.BlacksmithRecipe;
import fr.shoqapik.btemobs.recipe.BlacksmithUpgradeRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Árbol de evoluciones del Blacksmith (Anna): cada mejora (blacksmith_upgrade) es una flecha de su base a su resultado.
 * No es un árbol puro (un objeto puede salir de varias bases), así que se muestra por objeto: de dónde viene y dos
 * niveles de evoluciones, repartidos en páginas de JEI.
 */
public final class EvolutionTree {

	/** Filas de evoluciones por página. */
	public static final int ROWS = 6;
	/** Bases que caben en la columna izquierda; si hay más, la última casilla va rotando entre las que sobran. */
	public static final int MAX_PARENTS = ROWS;

	/** Una evolución directa del objeto y las suyas (nietos) que caben en esta página. */
	public record Branch(Item child, List<Item> grandchildren) {
		public int rows() { return Math.max(1, grandchildren.size()); }
	}

	/** Una página de JEI: el objeto, sus bases y las ramas que caben. */
	public record Page(Item focus, List<Item> parents, List<Branch> branches, int page, int pages) {
		public int rows() {
			int r = 0;
			for (Branch b : branches) r += b.rows();
			return r;
		}
	}

	private final Map<Item, Set<Item>> children = new HashMap<>();
	private final Map<Item, Set<Item>> parents = new HashMap<>();
	/** Recetas del Blacksmith que dan cada objeto (mejoras y recetas normales): deciden el tick, la X o el candado. */
	private final Map<Item, List<ResourceLocation>> producers = new HashMap<>();
	private final Map<String, Integer> tiers = loadTiers();

	public static EvolutionTree build(RecipeManager recipes) {
		EvolutionTree tree = new EvolutionTree();
		for (BlacksmithUpgradeRecipe r : recipes.getAllRecipesFor(BteMobsRecipeTypes.BLACKSMITH_UPGRADE_RECIPE.get())) {
			Item result = r.getResultItem().getItem();
			tree.producers.computeIfAbsent(result, k -> new ArrayList<>()).add(r.getId());
			for (ItemStack base : r.base.getItems()) {
				if (base.isEmpty() || base.getItem() == result) continue;
				tree.children.computeIfAbsent(base.getItem(), k -> new LinkedHashSet<>()).add(result);
				tree.parents.computeIfAbsent(result, k -> new LinkedHashSet<>()).add(base.getItem());
			}
		}
		for (BlacksmithRecipe r : recipes.getAllRecipesFor(BteMobsRecipeTypes.BLACKSMITH_RECIPE.get())) {
			tree.producers.computeIfAbsent(r.getResultItem().getItem(), k -> new ArrayList<>()).add(r.getId());
		}
		return tree;
	}

	/** Recetas que dan el objeto (vacío si no sale del Blacksmith, p. ej. una espada de hierro vanilla). */
	public List<ResourceLocation> producers(Item item) {
		return producers.getOrDefault(item, List.of());
	}

	/** Todas las páginas: una o varias por cada objeto que forma parte de alguna mejora. */
	public List<Page> pages() {
		Set<Item> nodes = new LinkedHashSet<>();
		nodes.addAll(children.keySet());
		nodes.addAll(parents.keySet());
		List<Item> sorted = new ArrayList<>(nodes);
		sorted.sort(order());

		List<Page> pages = new ArrayList<>();
		for (Item item : sorted) pages.addAll(pagesFor(item));
		return pages;
	}

	private List<Page> pagesFor(Item item) {
		List<Item> parentList = sortedList(parents.get(item));

		// Ramas completas; luego se reparten en páginas de ROWS filas. Una rama más larga que una página se parte y su
		// evolución directa se repite al principio de la siguiente.
		List<List<Branch>> chunks = new ArrayList<>();
		List<Branch> current = new ArrayList<>();
		int used = 0;
		for (Item child : sortedList(children.get(item))) {
			List<Item> grand = sortedList(children.get(child));
			int idx = 0;
			do {
				if (used == ROWS) { chunks.add(current); current = new ArrayList<>(); used = 0; }
				int take = grand.isEmpty() ? 0 : Math.min(grand.size() - idx, ROWS - used);
				// Que una rama corta no se parta si cabe entera en la página siguiente
				if (idx == 0 && grand.size() > take && grand.size() <= ROWS && used > 0) {
					chunks.add(current); current = new ArrayList<>(); used = 0;
					take = grand.size();
				}
				current.add(new Branch(child, List.copyOf(grand.subList(idx, idx + take))));
				used += Math.max(1, take);
				idx += take;
			} while (idx < grand.size());
		}
		if (!current.isEmpty() || chunks.isEmpty()) chunks.add(current);

		List<Page> result = new ArrayList<>();
		for (int i = 0; i < chunks.size(); i++) {
			result.add(new Page(item, parentList, chunks.get(i), i + 1, chunks.size()));
		}
		return result;
	}

	private List<Item> sortedList(Set<Item> items) {
		if (items == null || items.isEmpty()) return List.of();
		List<Item> list = new ArrayList<>(items);
		list.sort(order());
		return Collections.unmodifiableList(list);
	}

	/** Por rareza (item_tiers.json, igual que en la GUI del Blacksmith) y después por nombre. */
	private Comparator<Item> order() {
		return Comparator.<Item>comparingInt(this::tier)
				.thenComparing(i -> new ItemStack(i).getHoverName().getString().toLowerCase());
	}

	private int tier(Item item) {
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
		Integer t = id == null ? null : tiers.get(id.toString());
		return t == null ? 0 : t;
	}

	private static Map<String, Integer> loadTiers() {
		try (InputStream is = EvolutionTree.class.getResourceAsStream("/assets/endereyesgui/item_tiers.json")) {
			if (is == null) return new HashMap<>();
			Map<String, Integer> map = new Gson().fromJson(new InputStreamReader(is),
					new TypeToken<Map<String, Integer>>(){}.getType());
			return map == null ? new HashMap<>() : map;
		} catch (Exception e) {
			return new HashMap<>();
		}
	}
}
