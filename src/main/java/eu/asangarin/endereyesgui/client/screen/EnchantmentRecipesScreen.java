package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentRecipesScreen extends Screen {

	// ── Datos ─────────────────────────────────────────────────────────────────
	private final List<EnchantmentRecipeData> recipes;
	private final int eyesEarned;
	private EnchantmentRecipeData selected = null;

	// ── Geometría ─────────────────────────────────────────────────────────────
	private static final float LIST_FRACTION = 0.50f;
	private static final int   TITLE_H       = 20;
	private static final int   BOTTOM_H      = 28;
	private static final int   PAD           = 8;

	// ── Colores ───────────────────────────────────────────────────────────────
	private static final int C_UNLOCKED   = 0xFF55FF55;
	private static final int C_LOCKED     = 0xFFFF5555;
	private static final int C_LIST_BG    = 0xAA080018;
	private static final int C_PANEL_BG   = 0xCC100030;
	private static final int C_PANEL_BORD = 0xFF8800CC;
	private static final int C_DIVIDER    = 0xFF550088;
	private static final int C_SELECTED   = 0x887700BB;
	private static final int C_HOVER      = 0x447700BB;

	// ── Iconos de estado (solo en código, NO en las claves de traducción) ─────
	private static final String ICON_OK  = "\u2714 "; // ✔ + espacio
	private static final String ICON_ERR = "\u2718 "; // ✗ + espacio

	// ── Widget ────────────────────────────────────────────────────────────────
	private RecipeList recipeList;

	public EnchantmentRecipesScreen(List<EnchantmentRecipeData> recipes, int eyesEarned) {
		super(Component.empty());
		this.recipes    = new ArrayList<>(recipes);
		this.eyesEarned = eyesEarned;
	}

	@Override
	protected void init() {
		super.init();
		int listW   = (int) (width * LIST_FRACTION);
		int listTop = TITLE_H + 2;
		int listBot = height - BOTTOM_H;

		recipeList = new RecipeList(minecraft, listW, listBot - listTop, listTop);
		addWidget(recipeList);

		addRenderableWidget(new Button(
				width / 2 - 40, height - BOTTOM_H + 4, 80, 20,
				Component.translatable("gui.back"),
				btn -> Networking.requestEnderGUI()
		));
	}

	@Override
	public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
		this.renderBackground(stack);
		drawCenteredString(stack, font,
				Component.translatable("endereyesgui.enchantments.title"),
				width / 2, 6, 0xFFFFFF);
		recipeList.render(stack, mx, my, delta);
		renderDetailPanel(stack);
		super.render(stack, mx, my, delta);
	}

	// ── Panel de detalle ──────────────────────────────────────────────────────

	private void renderDetailPanel(PoseStack stack) {
		int listW = (int) (width * LIST_FRACTION);
		int px    = listW + PAD;
		int py    = TITLE_H + 2;
		int pw    = width - listW - PAD * 2 - PAD; // PAD extra margen derecho
		int ph    = height - BOTTOM_H - py;

		// Fondo y bordes
		fill(stack, px, py, px + pw, py + ph, C_PANEL_BG);
		fill(stack, px,          py,          px + pw,     py + 1,      C_PANEL_BORD);
		fill(stack, px,          py + ph - 1, px + pw,     py + ph,     C_PANEL_BORD);
		fill(stack, px,          py,          px + 1,      py + ph,     C_PANEL_BORD);
		fill(stack, px + pw - 1, py,          px + pw,     py + ph,     C_PANEL_BORD);

		int cx = px + pw / 2;

		if (selected == null) {
			// Hint centrado
			List<FormattedCharSequence> lines = font.split(
					Component.translatable("endereyesgui.enchantments.detail.hint")
							.withStyle(ChatFormatting.GRAY),
					pw - PAD * 2);
			int startY = py + ph / 2 - (lines.size() * (font.lineHeight + 2)) / 2;
			for (int i = 0; i < lines.size(); i++) {
				int lx = cx - font.width(lines.get(i)) / 2;
				font.draw(stack, lines.get(i), lx, startY + i * (font.lineHeight + 2), 0x888888);
			}
			return;
		}

		// ── Fila de título: [libro 16x16] Nombre del encantamiento ───────────
		int titleY  = py + PAD;
		int bookSize = 16;
		int bookX   = px + PAD;
		int bookY   = titleY;

		// Renderizar el libro con coordenadas absolutas (NO PoseStack translate)
		ItemStack bookStack = buildBookStack(selected);
		if (bookStack != null) {
			minecraft.getItemRenderer().renderAndDecorateFakeItem(bookStack, bookX, bookY);
		}

		// Nombre a la derecha del libro, verticalmente centrado
		int nameX = bookX + bookSize + 4;
		int nameW = pw - PAD - bookSize - 4 - PAD; // ancho disponible para el nombre
		Component enchName = getEnchantmentName(selected);
		// Truncar si necesario
		String nameStr = font.plainSubstrByWidth(enchName.getString(), nameW);
		int nameColor  = 0xDD00FF;
		font.draw(stack, nameStr, nameX, bookY + (bookSize - font.lineHeight) / 2, nameColor);

		int ty = titleY + bookSize + 6;

		// ── Divisor ───────────────────────────────────────────────────────────
		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
		ty += 6;

		// ── Estado ────────────────────────────────────────────────────────────
		if (selected.isUnlocked()) {
			// ✔ RECIPE UNLOCKED  (icono en código + texto de traducción sin icono)
			MutableComponent statusLine = Component.literal(ICON_OK)
					.withStyle(ChatFormatting.GREEN)
					.append(Component.translatable("endereyesgui.enchantments.detail.unlocked")
							.withStyle(ChatFormatting.GREEN));
			drawCenteredString(stack, font, statusLine, cx, ty, 0xFFFFFF);
			ty += font.lineHeight + 8;
		} else {
			// ✗ RECIPE LOCKED
			MutableComponent statusLine = Component.literal(ICON_ERR)
					.withStyle(ChatFormatting.RED)
					.append(Component.translatable("endereyesgui.enchantments.detail.locked")
							.withStyle(ChatFormatting.RED));
			drawCenteredString(stack, font, statusLine, cx, ty, 0xFFFFFF);
			ty += font.lineHeight + 6;

			// Ender Eyes obtained: X (verde si puede, rojo si no)
			boolean canAfford = eyesEarned >= selected.getNeedEyes();
			drawCenteredString(stack, font,
					Component.translatable("endereyesgui.enchantments.detail.eyes_obtained", eyesEarned)
							.withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.RED),
					cx, ty, 0xFFFFFF);
			ty += font.lineHeight + 3;

			// Ender Eyes needed: X
			drawCenteredString(stack, font,
					Component.translatable("endereyesgui.enchantments.detail.eyes_cost",
							selected.getNeedEyes())
							.withStyle(ChatFormatting.GOLD),
					cx, ty, 0xFFFFFF);
			ty += font.lineHeight + 8;
		}

		// ── Divisor ───────────────────────────────────────────────────────────
		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
		ty += 6;

		// ── Descripción del encantamiento (EnchantmentDescriptions mod) ───────
		// El mod añade claves con el patrón: "enchantment.NAMESPACE.PATH.desc"
		// Ejemplo: "enchantment.airhop.air_hop.desc"
		//          "enchantment.minecraft.sharpness.desc"
		String descKey = "enchantment." +
				selected.getEnchantmentId().getNamespace() + "." +
				selected.getEnchantmentId().getPath() + ".desc";

		Component descComponent = Component.translatable(descKey);
		// Si la clave no existe, Minecraft devuelve la clave en sí misma.
		// Comprobamos si tiene traducción real comparando con la clave.
		String resolved = descComponent.getString();
		if (!resolved.equals(descKey) && !resolved.isBlank()) {
			List<FormattedCharSequence> descLines = font.split(
					((MutableComponent) descComponent).withStyle(ChatFormatting.GRAY), pw - PAD * 2);
			for (FormattedCharSequence line : descLines) {
				int lx = cx - font.width(line) / 2;
				font.draw(stack, line, lx, ty, 0xAAAAAA);
				ty += font.lineHeight + 2;
			}
		}
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	/** Construye el ItemStack de libro encantado para el icono. */
	private ItemStack buildBookStack(EnchantmentRecipeData data) {
		Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(data.getEnchantmentId());
		if (ench == null) return null;
		return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ench, data.getLevel()));
	}

	/** Nombre completo con nivel y color según estado. */
	private Component getEnchantmentName(EnchantmentRecipeData data) {
		Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(data.getEnchantmentId());
		ChatFormatting color = ChatFormatting.LIGHT_PURPLE;
		if (ench != null) {
			return ench.getFullname(data.getLevel()).copy().withStyle(color);
		}
		return Component.literal(data.getEnchantmentId().getPath()).withStyle(color);
	}

	@Override
	public boolean isPauseScreen() { return false; }

	// ── Lista ─────────────────────────────────────────────────────────────────

	private class RecipeList extends ObjectSelectionList<RecipeList.Entry> {

		RecipeList(Minecraft mc, int width, int height, int top) {
			super(mc, width, height, top, top + height, 14);
			setRenderHeader(false, 0);
			setRenderBackground(false);
			setRenderTopAndBottom(false);
			for (EnchantmentRecipeData data : recipes) {
				addEntry(new Entry(data));
			}
		}

		@Override
		public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
			fill(stack, getRowLeft() - 2, y0, getScrollbarPosition() + 6, y1, C_LIST_BG);
			super.render(stack, mx, my, delta);
		}

		@Override
		protected int getScrollbarPosition() { return this.width - 6; }

		@Override
		public boolean isFocused() { return false; }

		class Entry extends ObjectSelectionList.Entry<Entry> {
			private final EnchantmentRecipeData data;

			Entry(EnchantmentRecipeData data) { this.data = data; }

			@Override
			public @NotNull Component getNarration() { return getEnchantmentName(data); }

			@Override
			public boolean mouseClicked(double mx, double my, int button) {
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

				// ✔ verde / ✗ roja — icono solo aquí en código, sin duplicar
				String icon      = data.isUnlocked() ? ICON_OK : ICON_ERR;
				int    iconColor = data.isUnlocked() ? C_UNLOCKED : C_LOCKED;
				font.draw(stack, icon, left + 2, top + 2, iconColor);

				int nameX  = left + 2 + font.width(icon);
				int maxW   = width - font.width(icon) - 8;
				String nameStr = font.plainSubstrByWidth(getEnchantmentName(data).getString(), maxW);
				font.draw(stack, nameStr, nameX, top + 2,
						data.isUnlocked() ? 0xEE88FF : 0x996699);
			}
		}
	}
}
