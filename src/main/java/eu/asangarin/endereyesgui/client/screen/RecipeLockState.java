package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Estado de una receta en las listas de la GUI, con el mismo icono en la lista, en el panel de detalle y en el filtro:
 * <ul>
 *   <li>UNLOCKED: ya desbloqueada (tick verde).</li>
 *   <li>UNLOCKABLE: aún no, pero el jugador puede desbloquearla ya (X amarilla).</li>
 *   <li>LOCKED: le falta algo antes (una quest, Ender Eyes...): candado.</li>
 * </ul>
 * El filtro es comun a todas las pantallas de recetas y se mantiene mientras dure la partida.
 */
public enum RecipeLockState {
	UNLOCKED("✔ ", 0xFF55FF55, ChatFormatting.GREEN, "endereyesgui.enchantments.detail.unlocked", "endereyesgui.filter.unlocked"),
	UNLOCKABLE("✘ ", 0xFFFFDD33, ChatFormatting.YELLOW, "endereyesgui.enchantments.detail.unlockable", "endereyesgui.filter.unlockable"),
	LOCKED("✘ ", 0xFFFF5555, ChatFormatting.RED, "endereyesgui.enchantments.detail.locked", "endereyesgui.filter.locked");

	/** Texto del icono (el candado ocupa el mismo ancho para que los nombres queden alineados). */
	private final String icon;
	private final int color;
	private final ChatFormatting format;
	private final String detailKey;
	private final String filterKey;

	RecipeLockState(String icon, int color, ChatFormatting format, String detailKey, String filterKey) {
		this.icon = icon;
		this.color = color;
		this.format = format;
		this.detailKey = detailKey;
		this.filterKey = filterKey;
	}

	public static RecipeLockState of(boolean unlocked, boolean locked) {
		return unlocked ? UNLOCKED : locked ? LOCKED : UNLOCKABLE;
	}

	// ── Filtro ────────────────────────────────────────────────────────────────

	private static final EnumSet<RecipeLockState> SHOWN = EnumSet.allOf(RecipeLockState.class);

	public boolean isShown() { return SHOWN.contains(this); }

	// ── Lista ─────────────────────────────────────────────────────────────────

	/** Ancho reservado para el icono delante del nombre. */
	public static int iconWidth(Font font) { return font.width(UNLOCKED.icon); }

	/** Icono de la fila de la lista: tick, X o candado. */
	public void drawListIcon(PoseStack stack, Font font, int left, int top, boolean shadow) {
		if (this == LOCKED) {
			QuestLockInfo.drawLock(stack, left + 2, top + 1, font.lineHeight);
		} else if (shadow) {
			font.drawShadow(stack, icon, left + 2, top + 2, color);
		} else {
			font.draw(stack, icon, left + 2, top + 2, color);
		}
	}

	/** Color del nombre en la lista (atenuado si no esta desbloqueada). */
	public int nameColor() { return this == UNLOCKED ? 0xEE88FF : 0x996699; }

	// ── Panel de detalle ──────────────────────────────────────────────────────

	/** "✔ RECETA DESBLOQUEADA", "✘ RECETA DESBLOQUEABLE" o "[candado] RECETA BLOQUEADA", centrado en cx. */
	public void drawDetailStatus(PoseStack stack, Font font, int cx, int y) {
		MutableComponent text = Component.translatable(detailKey).withStyle(format);
		if (this == LOCKED) {
			int size = font.lineHeight;
			int gap = font.width(" ");
			int total = size + gap + font.width(text);
			int x = cx - total / 2;
			QuestLockInfo.drawLock(stack, x, y - 1, size);
			font.drawShadow(stack, text, x + size + gap, y, 0xFFFFFF);
		} else {
			GuiComponent.drawCenteredString(stack, font, Component.literal(icon).withStyle(format).append(text), cx, y, 0xFFFFFF);
		}
	}

	// ── Botones del filtro ────────────────────────────────────────────────────

	public static final int BUTTON_W = 16;
	public static final int BUTTON_GAP = 2;
	/** Ancho total que ocupan los tres botones (con su separacion a la izquierda). */
	public static final int BUTTONS_W = values().length * (BUTTON_W + BUTTON_GAP);

	/**
	 * Crea los tres botones del filtro, uno al lado del otro empezando en x. Cada uno muestra u oculta las recetas de
	 * su estado; onChange se llama al pulsar para reconstruir la lista. La pantalla los añade con addRenderableWidget.
	 */
	public static List<FilterButton> createButtons(Font font, int x, int y, int h, Runnable onChange) {
		List<FilterButton> list = new ArrayList<>();
		for (RecipeLockState state : values()) {
			list.add(new FilterButton(font, state, x, y, h, onChange));
			x += BUTTON_W + BUTTON_GAP;
		}
		return list;
	}

	/** Tooltip del boton bajo el raton; llamar al final del render de la pantalla. */
	public static void renderTooltips(Screen screen, PoseStack stack, List<FilterButton> buttons, int mx, int my) {
		for (FilterButton b : buttons) {
			if (b.visible && b.isHoveredOrFocused() && b.isMouseOver(mx, my)) {
				screen.renderTooltip(stack, b.tooltip(), mx, my);
				return;
			}
		}
	}

	public static class FilterButton extends AbstractButton {
		private final Font font;
		private final RecipeLockState state;
		private final Runnable onChange;

		FilterButton(Font font, RecipeLockState state, int x, int y, int h, Runnable onChange) {
			super(x, y, BUTTON_W, h, Component.translatable(state.filterKey));
			this.font = font;
			this.state = state;
			this.onChange = onChange;
		}

		Component tooltip() {
			return Component.translatable(state.filterKey).withStyle(state.format)
					.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
					.append(Component.translatable(state.isShown() ? "endereyesgui.filter.shown" : "endereyesgui.filter.hidden")
							.withStyle(ChatFormatting.GRAY));
		}

		@Override
		public void onPress() {
			if (!SHOWN.remove(state)) SHOWN.add(state);
			onChange.run();
		}

		@Override
		public void renderButton(@NotNull PoseStack stack, int mx, int my, float delta) {
			boolean on = state.isShown();
			int bg = on ? 0xCC550099 : 0x88220044;
			int border = isHoveredOrFocused() ? 0xFFDD88FF : on ? 0xFF8800CC : 0xFF442255;
			GuiComponent.fill(stack, x, y, x + width, y + height, bg);
			GuiComponent.fill(stack, x, y, x + width, y + 1, border);
			GuiComponent.fill(stack, x, y + height - 1, x + width, y + height, border);
			GuiComponent.fill(stack, x, y, x + 1, y + height, border);
			GuiComponent.fill(stack, x + width - 1, y, x + width, y + height, border);

			int iy = y + (height - font.lineHeight) / 2;
			if (state == LOCKED) {
				int size = font.lineHeight;
				int lx = x + (width - size) / 2;
				QuestLockInfo.drawLock(stack, lx, iy, size);
				// Apagado: oscurecido como los otros iconos (drawLock fija el color del shader a blanco)
				if (!on) GuiComponent.fill(stack, lx, iy, lx + size, iy + size, 0xAA220044);
			} else {
				String glyph = state.icon.trim();
				int color = on ? state.color : 0xFF555555;
				font.drawShadow(stack, glyph, x + (width - font.width(glyph)) / 2f, iy + 1, color);
			}
		}

		@Override
		public void updateNarration(@NotNull NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, tooltip());
		}
	}
}
