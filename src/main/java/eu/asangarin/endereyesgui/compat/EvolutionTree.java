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
import java.util.ArrayDeque;
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
 * Para cada objeto se arma su árbol completo hacia delante (todas las mejoras posibles), más sus bases a la izquierda.
 * <p>
 * No es un árbol puro (un objeto puede salir de varias bases): cada objeto se despliega una sola vez, bajo su aparición
 * menos profunda; si vuelve a aparecer en otra rama, sale como hoja "repetida" (sus mejoras están en la otra rama).
 */
public final class EvolutionTree {

	/** Separación entre columnas (casilla, icono de estado y flecha) y entre filas. */
	public static final int COL_W = 44;
	public static final int ROW_H = 20;
	/** Margen: a la izquierda va el icono de estado de la primera columna. */
	public static final int PAD_X = 14;
	public static final int PAD_Y = 3;

	/**
	 * Casilla del árbol. repeated: el objeto ya está desplegado en otra rama de este árbol y aquí no se repiten sus
	 * mejoras. base: es una de las bases del objeto raíz (columna de la izquierda).
	 */
	public record Node(Item item, int x, int y, boolean repeated, boolean base) {
		public ItemStack stack() { return new ItemStack(item); }
	}

	/** Flecha de la casilla from a la casilla to (índices en nodes). */
	public record Edge(int from, int to) {}

	/** Árbol ya colocado de un objeto. */
	public record Layout(List<Node> nodes, List<Edge> edges, int root, int width, int height) {}

	/** "Receta" de JEI: el árbol de un objeto, con su posición de scroll. */
	public static final class Recipe {
		private final EvolutionTree tree;
		private final Item root;
		private Layout layout;
		public double scrollX, scrollY;

		Recipe(EvolutionTree tree, Item root) {
			this.tree = tree;
			this.root = root;
		}

		public Item root() { return root; }
		public List<Item> bases() { return tree.sortedList(tree.parents.get(root)); }

		/** Se coloca la primera vez que se dibuja (hay cientos de objetos y la mayoría nunca se abren). */
		public Layout layout() {
			if (layout == null) layout = tree.layout(root);
			return layout;
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

	/** Un árbol por cada objeto que forma parte de alguna mejora. */
	public List<Recipe> recipes() {
		Set<Item> nodes = new LinkedHashSet<>();
		nodes.addAll(children.keySet());
		nodes.addAll(parents.keySet());
		List<Item> sorted = new ArrayList<>(nodes);
		sorted.sort(order());
		List<Recipe> list = new ArrayList<>(sorted.size());
		for (Item item : sorted) list.add(new Recipe(this, item));
		return list;
	}

	// ── Colocación ────────────────────────────────────────────────────────────

	private Layout layout(Item root) {
		// Cada objeto se despliega bajo su primera aparición en anchura (la menos profunda)
		Map<Item, Item> home = new HashMap<>();
		home.put(root, null);
		ArrayDeque<Item> queue = new ArrayDeque<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			Item n = queue.poll();
			for (Item c : sortedList(children.get(n))) {
				if (!home.containsKey(c)) { home.put(c, n); queue.add(c); }
			}
		}

		List<Node> nodes = new ArrayList<>();
		List<Edge> edges = new ArrayList<>();
		List<Item> bases = sortedList(parents.get(root));
		int rootCol = bases.isEmpty() ? 0 : 1;

		int[] row = {0};
		int[] maxCol = {rootCol};
		int rootIdx = place(root, rootCol, false, home, nodes, edges, row, maxCol);

		for (int i = 0; i < bases.size(); i++) {
			nodes.add(new Node(bases.get(i), PAD_X, PAD_Y + i * ROW_H, false, true));
			edges.add(new Edge(nodes.size() - 1, rootIdx));
		}

		int rows = Math.max(row[0], bases.size());
		int width = PAD_X + maxCol[0] * COL_W + 18 + 4;
		int height = PAD_Y + rows * ROW_H + 2;
		return new Layout(Collections.unmodifiableList(nodes), Collections.unmodifiableList(edges), rootIdx, width, height);
	}

	/**
	 * Coloca el objeto en la fila actual y sus mejoras a la derecha, empezando en la misma fila (el árbol queda alineado
	 * arriba, con la raíz arriba a la izquierda). Devuelve su índice.
	 */
	private int place(Item item, int col, boolean repeated, Map<Item, Item> home,
					  List<Node> nodes, List<Edge> edges, int[] row, int[] maxCol) {
		maxCol[0] = Math.max(maxCol[0], col);
		List<Item> kids = repeated ? List.of() : sortedList(children.get(item));
		int idx = nodes.size();
		nodes.add(new Node(item, PAD_X + col * COL_W, PAD_Y + row[0] * ROW_H,
				repeated && !sortedList(children.get(item)).isEmpty(), false));
		if (kids.isEmpty()) {
			row[0]++;
			return idx;
		}
		for (Item kid : kids) {
			boolean expandHere = home.get(kid) == item;
			int k = place(kid, col + 1, !expandHere, home, nodes, edges, row, maxCol);
			edges.add(new Edge(idx, k));
		}
		return idx;
	}

	// ── Orden ─────────────────────────────────────────────────────────────────

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
