package eu.asangarin.endereyesgui.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector4f;
import eu.asangarin.endereyesgui.client.screen.RecipeLockState;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Árbol de evoluciones del Blacksmith en JEI, al estilo del árbol de Digimon: el objeto arriba a la izquierda (con sus
 * bases delante) y todas sus mejoras posibles hacia la derecha, con flechas en rama.
 * <p>
 * JEI 1.19.2 no tiene scroll dentro de una receta y sus casillas son fijas, así que el árbol lo dibuja esta clase
 * entera dentro de un recuadro con scroll:
 * <ul>
 *   <li>Rueda: arriba/abajo. Mayús + rueda: izquierda/derecha. Arrastrar con el ratón: mover el árbol.</li>
 *   <li>Clic izquierdo (o R) en un objeto: su árbol. Clic derecho (o U): en qué se puede mejorar.</li>
 *   <li>Candado: tooltip con la quest que lo desbloquea (censurada si aún no se puede ver); clic para abrirla.</li>
 * </ul>
 * El objeto raíz va como salida invisible (R sobre un objeto abre su árbol) y sus bases como entradas invisibles
 * (U sobre un objeto muestra los árboles de lo que se hace con él).
 */
public class EvolutionCategory implements IRecipeCategory<EvolutionTree.Recipe> {

	public static final RecipeType<EvolutionTree.Recipe> TYPE =
			RecipeType.create("endereyesgui", "blacksmith_evolutions", EvolutionTree.Recipe.class);

	private static final int WIDTH = 176;
	private static final int HEIGHT = 152;
	/** Recuadro con scroll (dentro del borde de 1 px). */
	private static final int VIEW_X = 1, VIEW_Y = 1, VIEW_W = WIDTH - 2, VIEW_H = HEIGHT - 2;
	private static final int SCROLL_STEP = 24;

	private static final int C_VIEW_BG = 0x30000000;
	private static final int C_VIEW_BORDER = 0xFF555555;
	private static final int C_TRUNK = 0xFF8866AA;
	private static final int C_NONE = 0xFF999999;
	private static final int C_HOVER = 0x80FFFFFF;
	private static final int C_SCROLLBAR = 0xAA8866AA;

	private final IDrawable background;
	private final IDrawable icon;
	private final IDrawableStatic slot;
	private final EvolutionTree tree;

	public EvolutionCategory(IGuiHelper gui, EvolutionTree tree) {
		this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
		this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.ANVIL));
		this.slot = gui.getSlotDrawable();
		this.tree = tree;
		Input.register();
	}

	@Override public @NotNull RecipeType<EvolutionTree.Recipe> getRecipeType() { return TYPE; }
	@Override public @NotNull Component getTitle() { return Component.translatable("endereyesgui.jei.evolutions"); }
	@Override public @NotNull IDrawable getBackground() { return background; }
	@Override public @NotNull IDrawable getIcon() { return icon; }

	@Override
	public void setRecipe(@NotNull IRecipeLayoutBuilder builder, EvolutionTree.Recipe recipe, @NotNull IFocusGroup focuses) {
		builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(new ItemStack(recipe.root()));
		List<ItemStack> bases = new ArrayList<>();
		for (Item base : recipe.bases()) bases.add(new ItemStack(base));
		if (!bases.isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(bases);
	}

	// ── Estado ────────────────────────────────────────────────────────────────

	@Nullable
	private RecipeLockState state(Item item) {
		return RecipeLockState.ofRecipes(tree.producers(item));
	}

	private int stateColor(Item item) {
		RecipeLockState state = state(item);
		return state == null ? C_NONE : state.color();
	}

	// ── Zonas (coordenadas del árbol, sin scroll) ─────────────────────────────

	private static boolean inView(double mx, double my) {
		return mx >= VIEW_X && mx < VIEW_X + VIEW_W && my >= VIEW_Y && my < VIEW_Y + VIEW_H;
	}

	/** Lo que hay bajo el ratón: casilla, candado o marca de "repetido". */
	private record Hit(EvolutionTree.Node node, Part part) {}
	private enum Part { SLOT, LOCK, REPEATED }

	@Nullable
	private Hit hit(EvolutionTree.Recipe recipe, double mx, double my) {
		if (!inView(mx, my)) return null;
		double tx = mx - VIEW_X + recipe.scrollX, ty = my - VIEW_Y + recipe.scrollY;
		for (EvolutionTree.Node n : recipe.layout().nodes()) {
			if (tx >= n.x() - 1 && tx < n.x() + 17 && ty >= n.y() - 1 && ty < n.y() + 17) return new Hit(n, Part.SLOT);
			if (tx >= n.x() - 12 && tx < n.x() - 2 && ty >= n.y() + 3 && ty < n.y() + 14
					&& state(n.item()) == RecipeLockState.LOCKED) return new Hit(n, Part.LOCK);
			if (n.repeated() && tx >= n.x() + 18 && tx < n.x() + 26 && ty >= n.y() + 4 && ty < n.y() + 14) return new Hit(n, Part.REPEATED);
		}
		return null;
	}

	// ── Dibujo ────────────────────────────────────────────────────────────────

	@Override
	public void draw(EvolutionTree.Recipe recipe, @NotNull IRecipeSlotsView view, @NotNull PoseStack stack, double mouseX, double mouseY) {
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		EvolutionTree.Layout layout = recipe.layout();
		clamp(recipe);

		GuiComponent.fill(stack, 0, 0, WIDTH, HEIGHT, C_VIEW_BORDER);
		GuiComponent.fill(stack, VIEW_X, VIEW_Y, VIEW_X + VIEW_W, VIEW_Y + VIEW_H, 0xFF000000);
		GuiComponent.fill(stack, VIEW_X, VIEW_Y, VIEW_X + VIEW_W, VIEW_Y + VIEW_H, C_VIEW_BG);

		// Posición real en pantalla del recuadro: para recortar y para la rueda / el arrastre
		Vector4f origin = new Vector4f(VIEW_X, VIEW_Y, 0, 1);
		origin.transform(stack.last().pose());
		Input.drawn(recipe, this, origin.x() - VIEW_X, origin.y() - VIEW_Y);
		Window window = mc.getWindow();
		double scale = window.getGuiScale();
		RenderSystem.enableScissor((int) Math.floor(origin.x() * scale),
				(int) Math.floor(window.getHeight() - (origin.y() + VIEW_H) * scale),
				(int) Math.ceil(VIEW_W * scale), (int) Math.ceil(VIEW_H * scale));
		try {
			stack.pushPose();
			stack.translate(VIEW_X - recipe.scrollX, VIEW_Y - recipe.scrollY, 0);
			drawTree(stack, font, recipe, layout, hit(recipe, mouseX, mouseY));
			drawItems(stack, mc, layout);
			stack.popPose();
		} finally {
			RenderSystem.disableScissor();
		}
		drawScrollbars(stack, recipe, layout);
	}

	private void drawTree(PoseStack stack, Font font, EvolutionTree.Recipe recipe, EvolutionTree.Layout layout, @Nullable Hit hover) {
		List<EvolutionTree.Node> nodes = layout.nodes();
		EvolutionTree.Node root = nodes.get(layout.root());

		// Bases → raíz: tronco a la izquierda de la raíz
		int baseTrunk = root.x() - 20;
		int bMin = root.y() + 9, bMax = root.y() + 9;
		boolean anyBase = false;
		// Raíz y cada objeto → sus mejoras: tronco a la derecha del objeto
		for (int i = 0; i < nodes.size(); i++) {
			EvolutionTree.Node from = nodes.get(i);
			if (from.base()) {
				int y = from.y() + 9;
				GuiComponent.fill(stack, from.x() + 18, y, baseTrunk + 1, y + 1, C_TRUNK);
				bMin = Math.min(bMin, y); bMax = Math.max(bMax, y);
				anyBase = true;
				continue;
			}
			int trunk = from.x() + 24;
			int y0 = from.y() + 9, y1 = y0;
			boolean any = false;
			for (EvolutionTree.Edge e : layout.edges()) {
				if (e.from() != i) continue;
				EvolutionTree.Node to = nodes.get(e.to());
				int y = to.y() + 9;
				y1 = Math.max(y1, y);
				arrow(stack, trunk, to.x() - 13, y, stateColor(to.item()));
				any = true;
			}
			if (any) {
				GuiComponent.fill(stack, from.x() + 18, y0, trunk + 1, y0 + 1, C_TRUNK);
				GuiComponent.fill(stack, trunk, y0, trunk + 1, y1 + 1, C_TRUNK);
			}
		}
		if (anyBase) {
			GuiComponent.fill(stack, baseTrunk, bMin, baseTrunk + 1, bMax + 1, C_TRUNK);
			arrow(stack, baseTrunk, root.x() - 13, root.y() + 9, stateColor(root.item()));
		}

		// Casillas, iconos de estado y marca de "repetido"
		for (EvolutionTree.Node n : nodes) {
			slot.draw(stack, n.x() - 1, n.y() - 1);
			if (hover != null && hover.node() == n && hover.part() == Part.SLOT) {
				GuiComponent.fill(stack, n.x(), n.y(), n.x() + 16, n.y() + 16, C_HOVER);
			}
			RecipeLockState state = state(n.item());
			if (state != null) state.drawListIcon(stack, font, n.x() - 13, n.y() + 3, true);
			if (n.repeated()) font.drawShadow(stack, "…", n.x() + 19, n.y() + 4, 0xFFAAAAAA);
		}
		// La raíz, resaltada
		GuiComponent.fill(stack, root.x() - 1, root.y() - 1, root.x() + 17, root.y(), 0xFFDD88FF);
		GuiComponent.fill(stack, root.x() - 1, root.y() + 16, root.x() + 17, root.y() + 17, 0xFFDD88FF);
		GuiComponent.fill(stack, root.x() - 1, root.y(), root.x(), root.y() + 16, 0xFFDD88FF);
		GuiComponent.fill(stack, root.x() + 16, root.y(), root.x() + 17, root.y() + 16, 0xFFDD88FF);
	}

	/** Los objetos con la misma transformación que el resto (scroll incluido) y dentro del recorte. */
	private static void drawItems(PoseStack stack, Minecraft mc, EvolutionTree.Layout layout) {
		PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.pushPose();
		modelView.mulPoseMatrix(stack.last().pose());
		RenderSystem.applyModelViewMatrix();
		try {
			for (EvolutionTree.Node n : layout.nodes()) {
				mc.getItemRenderer().renderAndDecorateFakeItem(n.stack(), n.x(), n.y());
			}
		} finally {
			modelView.popPose();
			RenderSystem.applyModelViewMatrix();
		}
	}

	private static void drawScrollbars(PoseStack stack, EvolutionTree.Recipe recipe, EvolutionTree.Layout layout) {
		if (layout.height() > VIEW_H) {
			int len = Math.max(10, VIEW_H * VIEW_H / layout.height());
			int pos = (int) ((VIEW_H - len) * (recipe.scrollY / (layout.height() - VIEW_H)));
			GuiComponent.fill(stack, VIEW_X + VIEW_W - 2, VIEW_Y + pos, VIEW_X + VIEW_W, VIEW_Y + pos + len, C_SCROLLBAR);
		}
		if (layout.width() > VIEW_W) {
			int len = Math.max(10, VIEW_W * VIEW_W / layout.width());
			int pos = (int) ((VIEW_W - len) * (recipe.scrollX / (layout.width() - VIEW_W)));
			GuiComponent.fill(stack, VIEW_X + pos, VIEW_Y + VIEW_H - 2, VIEW_X + pos + len, VIEW_Y + VIEW_H, C_SCROLLBAR);
		}
	}

	/** Flecha horizontal de x0 a la punta x1 (incluida) en la altura y. */
	private static void arrow(PoseStack stack, int x0, int x1, int y, int color) {
		GuiComponent.fill(stack, x0, y, x1 - 1, y + 1, color);
		GuiComponent.fill(stack, x1 - 3, y - 2, x1 - 2, y + 3, color);
		GuiComponent.fill(stack, x1 - 2, y - 1, x1 - 1, y + 2, color);
		GuiComponent.fill(stack, x1 - 1, y, x1, y + 1, color);
	}

	private static void clamp(EvolutionTree.Recipe recipe) {
		EvolutionTree.Layout layout = recipe.layout();
		recipe.scrollX = Math.max(0, Math.min(recipe.scrollX, layout.width() - VIEW_W));
		recipe.scrollY = Math.max(0, Math.min(recipe.scrollY, layout.height() - VIEW_H));
	}

	// ── Tooltips ──────────────────────────────────────────────────────────────

	@Override
	public @NotNull List<Component> getTooltipStrings(EvolutionTree.Recipe recipe, @NotNull IRecipeSlotsView view, double mouseX, double mouseY) {
		Hit hit = hit(recipe, mouseX, mouseY);
		if (hit == null) return List.of();
		Item item = hit.node().item();
		switch (hit.part()) {
			case LOCK -> {
				String quest = RecipeLockState.questFor(tree.producers(item));
				if (quest == null) return List.of(RecipeLockState.LOCKED.tooltipLine());
				List<Component> lines = new ArrayList<>(RecipeLockState.questTooltip(quest));
				return lines.isEmpty() ? List.of(RecipeLockState.LOCKED.tooltipLine()) : lines;
			}
			case REPEATED -> {
				return List.of(Component.translatable("endereyesgui.jei.evolutions.repeated").withStyle(ChatFormatting.GRAY));
			}
			default -> {
				Minecraft mc = Minecraft.getInstance();
				List<Component> lines = new ArrayList<>(hit.node().stack().getTooltipLines(mc.player,
						mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL));
				RecipeLockState state = state(item);
				if (state != null) lines.add(state.tooltipLine());
				if (hit.node().repeated()) lines.add(Component.translatable("endereyesgui.jei.evolutions.repeated").withStyle(ChatFormatting.GRAY));
				if (item != recipe.root()) lines.add(Component.translatable("endereyesgui.jei.evolutions.click").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
				return lines;
			}
		}
	}

	// ── Teclas (R / U sobre un objeto) ────────────────────────────────────────

	@Override
	public boolean handleInput(EvolutionTree.Recipe recipe, double mouseX, double mouseY, InputConstants.Key key) {
		if (key.getType() == InputConstants.Type.MOUSE) return false;
		Hit hit = hit(recipe, mouseX, mouseY);
		if (hit == null || hit.part() != Part.SLOT) return false;
		if (JeiCompat.isShowRecipesKey(key)) { JeiCompat.showRecipes(hit.node().stack()); return true; }
		if (JeiCompat.isShowUsagesKey(key)) { JeiCompat.showUsages(hit.node().stack()); return true; }
		return false;
	}

	/** Clic (sin arrastrar) dentro del recuadro, en coordenadas de la receta. */
	private void click(EvolutionTree.Recipe recipe, double mx, double my, int button, Screen screen) {
		Hit hit = hit(recipe, mx, my);
		if (hit == null) return;
		if (hit.part() == Part.LOCK) {
			String quest = RecipeLockState.questFor(tree.producers(hit.node().item()));
			if (quest != null) RecipeLockState.openQuest(quest, screen);
			return;
		}
		if (hit.node().item() == recipe.root()) return;
		if (button == InputConstants.MOUSE_BUTTON_LEFT) JeiCompat.showRecipes(hit.node().stack());
		else if (button == InputConstants.MOUSE_BUTTON_RIGHT) JeiCompat.showUsages(hit.node().stack());
	}

	// ── Ratón: rueda, arrastre y clic ─────────────────────────────────────────

	/**
	 * JEI no pasa la rueda ni el arrastre a las recetas: se cogen los eventos de la pantalla cuando el ratón está sobre
	 * un árbol dibujado en el último frame y se cancelan para que JEI no cambie de página.
	 */
	public static final class Input {
		private static boolean registered = false;

		/** Árbol dibujado: dónde está la receta en pantalla y cuándo se dibujó. */
		private record Drawn(EvolutionTree.Recipe recipe, EvolutionCategory category, Screen screen, double x, double y, long time) {
			boolean contains(double mx, double my) {
				return inView(mx - x, my - y);
			}
		}

		private static final List<Drawn> DRAWN = new ArrayList<>();
		@Nullable private static Drawn pressed = null;
		private static int pressedButton;
		private static double dragged;

		static void register() {
			if (registered) return;
			registered = true;
			MinecraftForge.EVENT_BUS.register(Input.class);
		}

		static void drawn(EvolutionTree.Recipe recipe, EvolutionCategory category, double x, double y) {
			long now = System.currentTimeMillis();
			DRAWN.removeIf(d -> now - d.time() > 250 || d.recipe() == recipe);
			DRAWN.add(new Drawn(recipe, category, Minecraft.getInstance().screen, x, y, now));
		}

		@Nullable
		private static Drawn at(Screen screen, double mx, double my) {
			long now = System.currentTimeMillis();
			for (Drawn d : DRAWN) {
				if (d.screen() == screen && now - d.time() <= 250 && d.contains(mx, my)) return d;
			}
			return null;
		}

		@SubscribeEvent
		public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
			Drawn d = at(event.getScreen(), event.getMouseX(), event.getMouseY());
			if (d == null) return;
			double amount = event.getScrollDelta() * SCROLL_STEP;
			if (Screen.hasShiftDown()) d.recipe().scrollX -= amount;
			else d.recipe().scrollY -= amount;
			clamp(d.recipe());
			event.setCanceled(true);
		}

		@SubscribeEvent
		public static void onPress(ScreenEvent.MouseButtonPressed.Pre event) {
			Drawn d = at(event.getScreen(), event.getMouseX(), event.getMouseY());
			if (d == null) return;
			pressed = d;
			pressedButton = event.getButton();
			dragged = 0;
			event.setCanceled(true);
		}

		@SubscribeEvent
		public static void onDrag(ScreenEvent.MouseDragged.Pre event) {
			if (pressed == null || event.getScreen() != pressed.screen()) return;
			pressed.recipe().scrollX -= event.getDragX();
			pressed.recipe().scrollY -= event.getDragY();
			clamp(pressed.recipe());
			dragged += Math.abs(event.getDragX()) + Math.abs(event.getDragY());
			event.setCanceled(true);
		}

		@SubscribeEvent
		public static void onRelease(ScreenEvent.MouseButtonReleased.Pre event) {
			Drawn d = pressed;
			if (d == null) return;
			pressed = null;
			event.setCanceled(true);
			if (dragged > 3 || event.getButton() != pressedButton || event.getScreen() != d.screen()) return;
			d.category().click(d.recipe(), event.getMouseX() - d.x(), event.getMouseY() - d.y(), pressedButton, d.screen());
		}
	}
}
