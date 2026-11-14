package eu.asangarin.endereyesgui.compat;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Árbol de evoluciones del Blacksmith en JEI, al estilo del árbol de Digimon:
 * <pre>
 *   bases ──► OBJETO ──┬─► evolución ──┬─► evolución
 *                      │               └─► evolución
 *                      └─► evolución
 * </pre>
 * Cada objeto lleva el tick, la X amarilla o el candado de la GUI, y su flecha va en ese color.
 * <ul>
 *   <li>El objeto del centro es la salida: R (o clic izquierdo en cualquier objeto) abre su árbol.</li>
 *   <li>Las bases son entradas: U sobre un objeto muestra los árboles de lo que se puede hacer con él.</li>
 *   <li>Las evoluciones solo se dibujan (no ensucian las búsquedas de R/U), pero se pueden pulsar igual.</li>
 * </ul>
 */
public class EvolutionCategory implements IRecipeCategory<EvolutionTree.Page> {

	public static final RecipeType<EvolutionTree.Page> TYPE =
			RecipeType.create("endereyesgui", "blacksmith_evolutions", EvolutionTree.Page.class);

	private static final int ROW_H = 20;
	private static final int TOP = 1;
	/** Columnas (x de la casilla): bases, objeto, evoluciones y sus evoluciones. A la izquierda de cada una, el icono. */
	private static final int COL_PARENT = 15;
	private static final int COL_FOCUS = 63;
	private static final int COL_CHILD = 111;
	private static final int COL_GRAND = 159;
	private static final int WIDTH = COL_GRAND + 18 + 1;
	private static final int HEIGHT = TOP + EvolutionTree.ROWS * ROW_H + 9;

	private static final int C_TRUNK = 0xFF8866AA;
	private static final int C_NONE = 0xFF999999;

	private final IDrawable background;
	private final IDrawable icon;
	private final IDrawableStatic slot;
	private final EvolutionTree tree;

	public EvolutionCategory(IGuiHelper gui, EvolutionTree tree) {
		this.background = gui.createBlankDrawable(WIDTH, HEIGHT);
		this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.ANVIL));
		this.slot = gui.getSlotDrawable();
		this.tree = tree;
	}

	@Override public @NotNull RecipeType<EvolutionTree.Page> getRecipeType() { return TYPE; }
	@Override public @NotNull Component getTitle() { return Component.translatable("endereyesgui.jei.evolutions"); }
	@Override public @NotNull IDrawable getBackground() { return background; }
	@Override public @NotNull IDrawable getIcon() { return icon; }

	// ── Posiciones ────────────────────────────────────────────────────────────

	/** Filas que ocupa la página (para centrar en vertical el objeto y sus bases). */
	private static int usedRows(EvolutionTree.Page page) {
		return Math.max(1, Math.max(page.rows(), Math.min(page.parents().size(), EvolutionTree.MAX_PARENTS)));
	}

	/** y de la casilla de un bloque de n filas que empieza en la fila first. */
	private static int blockY(int first, int n) {
		return TOP + first * ROW_H + (n - 1) * ROW_H / 2;
	}

	private static int focusY(EvolutionTree.Page page) {
		return blockY(0, usedRows(page));
	}

	private static int parentY(EvolutionTree.Page page, int i) {
		int shown = Math.min(page.parents().size(), EvolutionTree.MAX_PARENTS);
		int offset = (usedRows(page) - shown) * ROW_H / 2;
		return TOP + offset + i * ROW_H;
	}

	// ── Casillas ──────────────────────────────────────────────────────────────

	@Override
	public void setRecipe(@NotNull IRecipeLayoutBuilder builder, EvolutionTree.Page page, @NotNull IFocusGroup focuses) {
		List<Item> parents = page.parents();
		int shown = Math.min(parents.size(), EvolutionTree.MAX_PARENTS);
		for (int i = 0; i < shown; i++) {
			// La última casilla rota entre las bases que no caben
			List<Item> items = (i == shown - 1 && parents.size() > shown) ? parents.subList(i, parents.size()) : List.of(parents.get(i));
			var slotBuilder = builder.addSlot(RecipeIngredientRole.INPUT, COL_PARENT, parentY(page, i));
			for (Item item : items) slotBuilder.addItemStack(new ItemStack(item));
			slotBuilder.addTooltipCallback((view, tooltip) -> addStateTooltip(view.getDisplayedIngredient(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY), tooltip, true));
		}

		builder.addSlot(RecipeIngredientRole.OUTPUT, COL_FOCUS, focusY(page))
				.addItemStack(new ItemStack(page.focus()))
				.addTooltipCallback((view, tooltip) -> addStateTooltip(new ItemStack(page.focus()), tooltip, false));

		int row = 0;
		for (EvolutionTree.Branch branch : page.branches()) {
			ItemStack child = new ItemStack(branch.child());
			builder.addSlot(RecipeIngredientRole.RENDER_ONLY, COL_CHILD, blockY(row, branch.rows()))
					.addItemStack(child)
					.addTooltipCallback((view, tooltip) -> addStateTooltip(child, tooltip, true));
			for (int g = 0; g < branch.grandchildren().size(); g++) {
				ItemStack grand = new ItemStack(branch.grandchildren().get(g));
				builder.addSlot(RecipeIngredientRole.RENDER_ONLY, COL_GRAND, TOP + (row + g) * ROW_H)
						.addItemStack(grand)
						.addTooltipCallback((view, tooltip) -> addStateTooltip(grand, tooltip, true));
			}
			row += branch.rows();
		}
	}

	private void addStateTooltip(ItemStack stack, List<Component> tooltip, boolean clickHint) {
		if (stack.isEmpty()) return;
		RecipeLockState state = RecipeLockState.ofRecipes(tree.producers(stack.getItem()));
		if (state != null) tooltip.add(state.tooltipLine());
		if (clickHint) tooltip.add(Component.translatable("endereyesgui.jei.evolutions.click").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
	}

	// ── Dibujo: marcos, flechas en rama, iconos de estado ─────────────────────

	@Override
	public void draw(EvolutionTree.Page page, @NotNull IRecipeSlotsView view, @NotNull PoseStack stack, double mouseX, double mouseY) {
		Font font = Minecraft.getInstance().font;
		int fy = focusY(page);

		// Bases → objeto
		List<Item> parents = page.parents();
		int shown = Math.min(parents.size(), EvolutionTree.MAX_PARENTS);
		if (shown > 0) {
			int bx = COL_FOCUS - 11 - 9;
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
			for (int i = 0; i < shown; i++) {
				int py = parentY(page, i) + 9;
				minY = Math.min(minY, py); maxY = Math.max(maxY, py);
				GuiComponent.fill(stack, COL_PARENT + 18, py, bx + 1, py + 1, C_TRUNK);
			}
			GuiComponent.fill(stack, bx, Math.min(minY, fy + 9), bx + 1, Math.max(maxY, fy + 9) + 1, C_TRUNK);
			arrow(stack, bx, COL_FOCUS - 13, fy + 9, stateColor(page.focus()));
		}

		// Objeto → evoluciones → sus evoluciones
		if (!page.branches().isEmpty()) {
			int bx = COL_CHILD - 11 - 9;
			GuiComponent.fill(stack, COL_FOCUS + 18, fy + 9, bx + 1, fy + 10, C_TRUNK);
			int minY = fy + 9, maxY = fy + 9;
			int row = 0;
			for (EvolutionTree.Branch branch : page.branches()) {
				int cy = blockY(row, branch.rows()) + 9;
				minY = Math.min(minY, cy); maxY = Math.max(maxY, cy);
				arrow(stack, bx, COL_CHILD - 13, cy, stateColor(branch.child()));

				if (!branch.grandchildren().isEmpty()) {
					int gx = COL_GRAND - 11 - 9;
					GuiComponent.fill(stack, COL_CHILD + 18, cy, gx + 1, cy + 1, C_TRUNK);
					int gMin = cy, gMax = cy;
					for (int g = 0; g < branch.grandchildren().size(); g++) {
						int gy = TOP + (row + g) * ROW_H + 9;
						gMin = Math.min(gMin, gy); gMax = Math.max(gMax, gy);
						arrow(stack, gx, COL_GRAND - 13, gy, stateColor(branch.grandchildren().get(g)));
					}
					GuiComponent.fill(stack, gx, gMin, gx + 1, gMax + 1, C_TRUNK);
				}
				row += branch.rows();
			}
			GuiComponent.fill(stack, bx, minY, bx + 1, maxY + 1, C_TRUNK);
		}

		// Marcos de las casillas e iconos de estado (a la izquierda de cada casilla)
		for (int i = 0; i < shown; i++) {
			int py = parentY(page, i);
			slot.draw(stack, COL_PARENT - 1, py - 1);
			if (i == shown - 1 && parents.size() > shown) {
				// Cuántas bases rotan en esta casilla, en pequeño donde iría el icono
				String more = "+" + (parents.size() - shown + 1);
				stack.pushPose();
				stack.translate(COL_PARENT - 2 - font.width(more) * 0.75f, py + 6, 0);
				stack.scale(0.75f, 0.75f, 1f);
				font.draw(stack, more, 0, 0, 0xFF666666);
				stack.popPose();
			} else {
				drawState(stack, font, parents.get(i), COL_PARENT, py);
			}
		}
		slot.draw(stack, COL_FOCUS - 1, fy - 1);
		drawState(stack, font, page.focus(), COL_FOCUS, fy);
		int row = 0;
		for (EvolutionTree.Branch branch : page.branches()) {
			int cy = blockY(row, branch.rows());
			slot.draw(stack, COL_CHILD - 1, cy - 1);
			drawState(stack, font, branch.child(), COL_CHILD, cy);
			for (int g = 0; g < branch.grandchildren().size(); g++) {
				int gy = TOP + (row + g) * ROW_H;
				slot.draw(stack, COL_GRAND - 1, gy - 1);
				drawState(stack, font, branch.grandchildren().get(g), COL_GRAND, gy);
			}
			row += branch.rows();
		}

		if (page.pages() > 1) {
			String label = page.page() + "/" + page.pages();
			font.draw(stack, label, WIDTH - font.width(label), HEIGHT - font.lineHeight, 0xFF666666);
		}
	}

	@Nullable
	private RecipeLockState state(Item item) {
		return RecipeLockState.ofRecipes(tree.producers(item));
	}

	private int stateColor(Item item) {
		RecipeLockState state = state(item);
		return state == null ? C_NONE : state.color();
	}

	/** Icono de estado a la izquierda de la casilla (nada si el objeto no sale del Blacksmith). */
	private void drawState(PoseStack stack, Font font, Item item, int slotX, int slotY) {
		RecipeLockState state = state(item);
		if (state != null) state.drawListIcon(stack, font, slotX - 13, slotY + 3, true);
	}

	/** Flecha horizontal de x0 a la punta x1 (incluida) en la altura y. */
	private static void arrow(PoseStack stack, int x0, int x1, int y, int color) {
		GuiComponent.fill(stack, x0, y, x1 - 1, y + 1, color);
		GuiComponent.fill(stack, x1 - 3, y - 2, x1 - 2, y + 3, color);
		GuiComponent.fill(stack, x1 - 2, y - 1, x1 - 1, y + 2, color);
		GuiComponent.fill(stack, x1 - 1, y, x1, y + 1, color);
	}
}
