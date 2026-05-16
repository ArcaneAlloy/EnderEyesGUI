package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.compat.JeiCompat;
import eu.asangarin.endereyesgui.util.BlacksmithRecipeData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class BlacksmithRecipesScreen extends Screen {

	private static final List<String> CATEGORY_ORDER = List.of("WEAPONS", "ARMORS", "TOOLS", "ARTIFACTS", "OTHERS");

	private final List<BlacksmithRecipeData> allRecipes;
	private String activeCategory = "WEAPONS";
	private BlacksmithRecipeData selected = null;

	private static final float LIST_FRACTION = 0.50f;
	private static final int   TITLE_H       = 20;
	private static final int   TAB_H         = 18;
	private static final int   BOTTOM_H      = 28;
	private static final int   PAD           = 8;

	private static final int C_UNLOCKED    = 0xFF55FF55;
	private static final int C_LOCKED      = 0xFFFF5555;
	private static final int C_LIST_BG     = 0xAA080018;
	private static final int C_PANEL_BG    = 0xCC100030;
	private static final int C_PANEL_BORD  = 0xFF8800CC;
	private static final int C_DIVIDER     = 0xFF550088;
	private static final int C_SELECTED    = 0x887700BB;
	private static final int C_HOVER       = 0x447700BB;
	private static final int C_TAB_ACTIVE  = 0xCC550099;
	private static final int C_TAB_IDLE    = 0x88330066;
	private static final int C_TAB_BORD    = 0xFF8800CC;
	private static final int C_TIER_HEADER = 0xFF9900CC;
	private static final int C_STAT_LABEL  = 0xFFAAAAAA;
	private static final int C_STAT_VALUE  = 0xFFFFFFFF;
	private static final int C_ING_HEADER  = 0xFFCCCCCC;

	private static final String ICON_OK  = "\u2714 ";
	private static final String ICON_ERR = "\u2718 ";

	private RecipeList recipeList;
	private final List<Button> tabButtons = new ArrayList<>();
	private EditBox searchBox;
	private String searchTerm = "";

	// Item bajo el cursor (para integración JEI)
	private ItemStack hoveredIngredient = null;
	private int hoveredIngX, hoveredIngY;
	private ItemStack jeiHoveredItem = ItemStack.EMPTY;
	private boolean hoveringResult = false;

	public BlacksmithRecipesScreen(List<BlacksmithRecipeData> recipes) {
		super(Component.empty());
		this.allRecipes = recipes;
	}

	@Override
	protected void init() {
		super.init();
		tabButtons.clear();
		int listW   = (int) (width * LIST_FRACTION);
		int listTop = TITLE_H + TAB_H + 18;
		int listBot = height - BOTTOM_H;
		int tabW    = listW / CATEGORY_ORDER.size();
		int tabY    = TITLE_H + 2;

		for (int i = 0; i < CATEGORY_ORDER.size(); i++) {
			String cat = CATEGORY_ORDER.get(i);
			int idx = i;
			Button tab = new Button(idx * tabW, tabY, tabW, TAB_H,
					Component.translatable("endereyesgui.blacksmith.category." + cat.toLowerCase()),
					btn -> { activeCategory = cat; selected = null;
						rebuildList(listW, listTop, listBot); });
			addRenderableWidget(tab);
			tabButtons.add(tab);
		}

		searchBox = new EditBox(font, 2, TITLE_H + TAB_H + 4, listW - 4, 14,
				Component.translatable("endereyesgui.search"));
		searchBox.setMaxLength(50);
		searchBox.setResponder(text -> {
			searchTerm = text.toLowerCase();
			selected = null;
			rebuildList(listW, listTop, listBot);
		});
		addRenderableWidget(searchBox);

		recipeList = new RecipeList(minecraft, listW, listBot - listTop, listTop);
		addWidget(recipeList);

		addRenderableWidget(new Button(
				width / 2 - 40, height - BOTTOM_H + 4, 80, 20,
				Component.translatable("gui.back"),
				btn -> Networking.requestEnderGUI()
		));
	}

	private void rebuildList(int listW, int listTop, int listBot) {
		children().removeIf(c -> c instanceof RecipeList);
		recipeList = new RecipeList(minecraft, listW, listBot - listTop, listTop);
		addWidget(recipeList);
	}

	private List<BlacksmithRecipeData> filteredRecipes() {
		return allRecipes.stream()
				.filter(d -> d.getCategory().equals(activeCategory))
				.collect(Collectors.toList());
	}

	private Map<Integer, List<BlacksmithRecipeData>> recipesByTier() {
		return filteredRecipes().stream()
				.collect(Collectors.groupingBy(BlacksmithRecipeData::getTier, TreeMap::new, Collectors.toList()));
	}

	// ── Render ────────────────────────────────────────────────────────────────

	@Override
	public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
		renderBackground(stack);
		hoveredIngredient = null;
		jeiHoveredItem = ItemStack.EMPTY;
		hoveringResult = false;

		drawCenteredString(stack, font,
				Component.translatable("endereyesgui.blacksmith.title"), width / 2, 6, 0xFFFFFF);

		int listW = (int) (width * LIST_FRACTION);
		int tabW  = listW / CATEGORY_ORDER.size();
		int tabY  = TITLE_H + 2;
		for (int i = 0; i < CATEGORY_ORDER.size(); i++) {
			String cat = CATEGORY_ORDER.get(i);
			int tx = i * tabW;
			fill(stack, tx, tabY, tx + tabW, tabY + TAB_H,
					cat.equals(activeCategory) ? C_TAB_ACTIVE : C_TAB_IDLE);
			fill(stack, tx, tabY, tx + tabW, tabY + 1, C_TAB_BORD);
			fill(stack, tx, tabY + TAB_H - 1, tx + tabW, tabY + TAB_H, C_TAB_BORD);
			fill(stack, tx, tabY, tx + 1, tabY + TAB_H, C_TAB_BORD);
			fill(stack, tx + tabW - 1, tabY, tx + tabW, tabY + TAB_H, C_TAB_BORD);
		}

		recipeList.render(stack, mx, my, delta);
		renderDetailPanel(stack, mx, my);

		super.render(stack, mx, my, delta);

		// Tooltip del ingrediente al final (encima de todo)
		if (hoveredIngredient != null) {
			renderTooltip(stack, hoveredIngredient, hoveredIngX, hoveredIngY);
		} else if (hoveringResult && selected != null) {
			renderTooltip(stack, selected.getResult(), mx, my);
		}
	}

	// ── Panel detalle ─────────────────────────────────────────────────────────

	private void renderDetailPanel(PoseStack stack, int mx, int my) {
		int listW = (int) (width * LIST_FRACTION);
		int px    = listW + PAD;
		int py    = TITLE_H + 2;
		int pw    = width - listW - PAD * 2 - PAD;
		int ph    = height - BOTTOM_H - py;

		fill(stack, px, py, px + pw, py + ph, C_PANEL_BG);
		fill(stack, px,          py,          px + pw,     py + 1,      C_PANEL_BORD);
		fill(stack, px,          py + ph - 1, px + pw,     py + ph,     C_PANEL_BORD);
		fill(stack, px,          py,          px + 1,      py + ph,     C_PANEL_BORD);
		fill(stack, px + pw - 1, py,          px + pw,     py + ph,     C_PANEL_BORD);

		int cx = px + pw / 2;

		if (selected == null) {
			List<FormattedCharSequence> lines = font.split(
					Component.translatable("endereyesgui.enchantments.detail.hint")
							.withStyle(ChatFormatting.GRAY), pw - PAD * 2);
			int sy = py + ph / 2 - (lines.size() * (font.lineHeight + 2)) / 2;
			for (int i = 0; i < lines.size(); i++) {
				int lx = cx - font.width(lines.get(i)) / 2;
				font.draw(stack, lines.get(i), lx, sy + i * (font.lineHeight + 2), 0x888888);
			}
			return;
		}

		int ty = py + PAD;

		// ── Fila título: item + nombre ────────────────────────────────────────
		minecraft.getItemRenderer().renderAndDecorateFakeItem(selected.getResult(), px + PAD, ty);
		// Hover item resultado para JEI y tooltip
		hoveringResult = (mx >= px + PAD && mx < px + PAD + 16 && my >= ty && my < ty + 16);
		if (hoveringResult) jeiHoveredItem = selected.getResult();
		int nameX = px + PAD + 16 + 4;
		int nameW = pw - PAD - 16 - 4 - PAD;
		font.draw(stack, font.plainSubstrByWidth(selected.getResult().getHoverName().getString(), nameW),
				nameX, ty + (16 - font.lineHeight) / 2, 0xDD00FF);
		ty += 16 + 6;

		// ── Divisor ───────────────────────────────────────────────────────────
		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
		ty += 6;

		// ── Tier ─────────────────────────────────────────────────────────────
		drawCenteredString(stack, font,
				Component.translatable("bte_mobs.tier." + selected.getTier())
						.withStyle(ChatFormatting.GOLD), cx, ty, 0xFFFFFF);
		ty += font.lineHeight + 4;

		// ── Estado ────────────────────────────────────────────────────────────
		if (selected.isUnlocked()) {
			MutableComponent s = Component.literal(ICON_OK).withStyle(ChatFormatting.GREEN)
					.append(Component.translatable("endereyesgui.enchantments.detail.unlocked")
							.withStyle(ChatFormatting.GREEN));
			drawCenteredString(stack, font, s, cx, ty, 0xFFFFFF);
		} else {
			MutableComponent s = Component.literal(ICON_ERR).withStyle(ChatFormatting.RED)
					.append(Component.translatable("endereyesgui.enchantments.detail.locked")
							.withStyle(ChatFormatting.RED));
			drawCenteredString(stack, font, s, cx, ty, 0xFFFFFF);
		}
		ty += font.lineHeight + 6;

		// ── Divisor ───────────────────────────────────────────────────────────
		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
		ty += 6;

		// ── Stats según categoría ─────────────────────────────────────────────
		ty = renderStats(stack, px, ty, pw, cx);

		// ── Divisor ───────────────────────────────────────────────────────────
		if (ty < py + ph - PAD * 3) {
			fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
			ty += 6;
		}

		// ── Bases posibles (solo para upgrade recipes) ───────────────────────
		if (selected.hasBaseVariants()) {
			ty = renderItemRow(stack, px, ty, pw, ph, py, mx, my,
					selected.getBaseVariants(),
					Component.translatable("endereyesgui.blacksmith.bases"));
			if (ty < py + ph - PAD * 3) {
				fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
				ty += 6;
			}
		}

		// ── Ingredientes ──────────────────────────────────────────────────────
		if (!selected.getIngredients().isEmpty()) {
			ty = renderItemRow(stack, px, ty, pw, ph, py, mx, my,
					selected.getIngredients(),
					Component.translatable("endereyesgui.blacksmith.ingredients"));
		}
	}

	private int renderStats(PoseStack stack, int px, int ty, int pw, int cx) {
		String cat = selected.getCategory();

		if ("ARMORS".equals(cat)) {
			// Durabilidad
			ty = renderStat(stack, px, ty, pw,
					Component.translatable("endereyesgui.blacksmith.stat.durability"),
					String.valueOf(selected.getDurability()));
			// Armor points
			ty = renderStat(stack, px, ty, pw,
					Component.translatable("endereyesgui.blacksmith.stat.armor"),
					String.valueOf(selected.getArmorPoints()));
			// Armor toughness
			if (selected.getArmorToughness() > 0) {
				ty = renderStat(stack, px, ty, pw,
						Component.translatable("endereyesgui.blacksmith.stat.toughness"),
						String.format("%.1f", selected.getArmorToughness()));
			}

		} else if ("WEAPONS".equals(cat)) {
			if (selected.getDurability() > 0)
				ty = renderStat(stack, px, ty, pw,
						Component.translatable("endereyesgui.blacksmith.stat.durability"),
						String.valueOf(selected.getDurability()));
			if (selected.getAttackDamage() > 0)
				ty = renderStat(stack, px, ty, pw,
						Component.translatable("endereyesgui.blacksmith.stat.damage"),
						String.format("%.1f", selected.getAttackDamage()));
			if (selected.getAttackSpeed() > 0)
				ty = renderStat(stack, px, ty, pw,
						Component.translatable("endereyesgui.blacksmith.stat.speed"),
						String.format("%.2f", selected.getAttackSpeed()));
			if (selected.getAttackRange() > 0)
				ty = renderStat(stack, px, ty, pw,
						Component.translatable("endereyesgui.blacksmith.stat.range"),
						String.format("%.1f", selected.getAttackRange()));
			// Daño a distancia (bows/crossbows): min - max
			if (selected.getRangedDamageMax() > 0)
				ty = renderStat(stack, px, ty, pw,
						Component.translatable("endereyesgui.blacksmith.stat.ranged_damage"),
						String.format("%.1f - %.1f", selected.getRangedDamageMin(), selected.getRangedDamageMax()));

		} else if ("TOOLS".equals(cat)) {
			if (selected.getDurability() > 0)
				ty = renderStat(stack, px, ty, pw,
						Component.translatable("endereyesgui.blacksmith.stat.durability"),
						String.valueOf(selected.getDurability()));
			ty = renderStat(stack, px, ty, pw,
					Component.translatable("endereyesgui.blacksmith.stat.harvest_level"),
					harvestLevelName(selected.getHarvestLevel()));
			if (selected.getMiningSpeed() > 0)
				ty = renderStat(stack, px, ty, pw,
						Component.translatable("endereyesgui.blacksmith.stat.mining_speed"),
						String.format("%.1f", selected.getMiningSpeed()));

		} else if ("ARTIFACTS".equals(cat)) {
			// Ability: clave "ability.dungeons_gear.ITEM_ID"
			net.minecraft.resources.ResourceLocation itemId =
					net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(selected.getResult().getItem());
			if (itemId != null) {
				String abilityKey = "ability." + itemId.getNamespace() + "." + itemId.getPath();
				Component abilityComp = Component.translatable(abilityKey);
				String resolved = abilityComp.getString();
				if (!resolved.equals(abilityKey) && !resolved.isBlank()) {
					// Wrappear en el ancho del panel
					java.util.List<net.minecraft.util.FormattedCharSequence> lines =
							font.split(abilityComp.copy().withStyle(net.minecraft.ChatFormatting.GREEN), pw - PAD * 2);
					for (net.minecraft.util.FormattedCharSequence line : lines) {
						int lx = cx - font.width(line) / 2;
						font.draw(stack, line, lx, ty, 0x55FF55);
						ty += font.lineHeight + 2;
					}
					ty += 4;
				}
			}
		}

		return ty;
	}

	/** Renderiza una línea "Etiqueta: Valor" alineada */
	private int renderStat(PoseStack stack, int px, int ty, int pw, Component label, String value) {
		String labelStr = label.getString() + ": ";
		font.draw(stack, labelStr, px + PAD, ty, C_STAT_LABEL);
		font.draw(stack, value, px + PAD + font.width(labelStr), ty, C_STAT_VALUE);
		return ty + font.lineHeight + 2;
	}

	private int renderItemRow(PoseStack stack, int px, int ty, int pw, int ph, int py,
							  int mx, int my, List<ItemStack> items, Component label) {
		if (items.isEmpty()) return ty;

		Component styledLabel = label.copy().withStyle(ChatFormatting.GRAY);
		int lx = (px + pw / 2) - font.width(styledLabel) / 2;
		font.draw(stack, styledLabel.getString(), lx, ty, C_ING_HEADER);
		ty += font.lineHeight + 3;

		int itemSize = 18;
		int startX   = px + PAD;
		int ix = startX, iy = ty;

		for (ItemStack item : items) {
			if (ix + itemSize > px + pw - PAD) { ix = startX; iy += itemSize; }
			if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) {
				fill(stack, ix - 1, iy - 1, ix + 17, iy + 17, 0x88FFFFFF);
				hoveredIngredient = item;
				jeiHoveredItem = item;
				hoveredIngX = mx;
				hoveredIngY = my;
			}
			minecraft.getItemRenderer().renderAndDecorateFakeItem(item, ix, iy);
			minecraft.getItemRenderer().renderGuiItemDecorations(font, item, ix, iy);
			ix += itemSize;
		}
		return iy + itemSize + 2;
	}

	private String harvestLevelName(int level) {
		return switch (level) {
			case 0 -> "Wood";
			case 1 -> "Stone";
			case 2 -> "Iron";
			case 3 -> "Diamond";
			case 4 -> "Netherite";
			default -> String.valueOf(level);
		};
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (JeiCompat.isAvailable() && !jeiHoveredItem.isEmpty()) {
			var key = com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode);
			if (JeiCompat.isShowRecipesKey(key)) {
				JeiCompat.showRecipes(jeiHoveredItem);
				return true;
			}
			if (JeiCompat.isShowUsagesKey(key)) {
				JeiCompat.showUsages(jeiHoveredItem);
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	// ── Lista ─────────────────────────────────────────────────────────────────

	private class RecipeList extends ObjectSelectionList<RecipeList.BaseEntry> {

		RecipeList(Minecraft mc, int width, int height, int top) {
			super(mc, width, height, top, top + height, 14);
			setRenderHeader(false, 0);
			setRenderBackground(false);
			setRenderTopAndBottom(false);
			populate();
		}

		private void populate() {
			for (Map.Entry<Integer, List<BlacksmithRecipeData>> e : recipesByTier().entrySet()) {
				addEntry(new TierHeader(e.getKey()));
				for (BlacksmithRecipeData d : e.getValue()) {
				if (searchTerm.isEmpty() || d.getResult().getHoverName().getString().toLowerCase().contains(searchTerm)) {
					addEntry(new RecipeEntry(d));
				}
			}
			}
		}

		@Override
		public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
			fill(stack, 0, y0, getScrollbarPosition() + 6, y1, C_LIST_BG);
			super.render(stack, mx, my, delta);
		}

		@Override protected int getScrollbarPosition() { return this.width - 6; }
		@Override public boolean isFocused() { return false; }

		abstract class BaseEntry extends ObjectSelectionList.Entry<BaseEntry> {}

		class TierHeader extends BaseEntry {
			private final int tier;
			TierHeader(int tier) { this.tier = tier; }

			@Override public @NotNull Component getNarration() {
				return Component.translatable("bte_mobs.tier." + tier);
			}
			@Override public boolean mouseClicked(double mx, double my, int btn) { return false; }

			@Override
			public void render(@NotNull PoseStack stack, int index, int top, int left,
							   int width, int height, int mx, int my, boolean hovered, float delta) {
				String label = Component.translatable("bte_mobs.tier." + tier).getString();
				int labelW = font.width(label);
				int labelX = left + (width - labelW) / 2;
				int labelY = top + 2;
				// Líneas a los lados, dejando hueco para el texto
				int lineY = labelY + font.lineHeight / 2;
				fill(stack, left + 2,          lineY, labelX - 4,            lineY + 1, C_TIER_HEADER);
				fill(stack, labelX + labelW + 4, lineY, left + width - 2, lineY + 1, C_TIER_HEADER);
				// Texto sin línea detrás
				font.draw(stack, label, labelX, labelY, C_TIER_HEADER);
			}
		}

		class RecipeEntry extends BaseEntry {
			private final BlacksmithRecipeData data;
			RecipeEntry(BlacksmithRecipeData data) { this.data = data; }

			@Override public @NotNull Component getNarration() { return data.getResult().getHoverName(); }

			@Override
			public boolean mouseClicked(double mx, double my, int btn) {
				selected = data;
				RecipeList.this.setSelected(this);
				return true;
			}

			@Override
			public void render(@NotNull PoseStack stack, int index, int top, int left,
							   int width, int height, int mx, int my, boolean hovered, float delta) {
				boolean sel = RecipeList.this.getSelected() == this;
				if (sel)          fill(stack, left, top, left + width, top + height, C_SELECTED);
				else if (hovered) fill(stack, left, top, left + width, top + height, C_HOVER);

				String icon = data.isUnlocked() ? ICON_OK : ICON_ERR;
				font.draw(stack, icon, left + 2, top + 2,
						data.isUnlocked() ? C_UNLOCKED : C_LOCKED);
				int nameX = left + 2 + font.width(icon);
				font.draw(stack, font.plainSubstrByWidth(
						data.getResult().getHoverName().getString(), width - font.width(icon) - 8),
						nameX, top + 2, data.isUnlocked() ? 0xEE88FF : 0x996699);
			}
		}
	}
}
