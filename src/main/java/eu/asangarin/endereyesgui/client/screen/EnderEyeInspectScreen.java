package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.api.IBottom;
import eu.asangarin.endereyesgui.client.screen.widget.EnderEyeIcon;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class EnderEyeInspectScreen extends Screen {
	private final boolean unlocked;
	private final IBottom eye;

	private int scaledWidth, scaledHeight;
	/** Botón "Volver": siempre en el mismo sitio; lo que no cabe encima se ve con la barra de scroll. */
	private Button backButton;

	// ---- Zona de texto con scroll (descripción, notas y quests) entre la dificultad y el botón "Volver" ----
	private int areaTop, areaBottom;
	/** Desplazamiento actual y alto del contenido (del último frame). */
	private double scroll = 0;
	private int contentHeight = 0;
	private boolean draggingBar = false;
	private static final int BAR_WIDTH = 4;
	private static final int SCROLL_STEP = 14;
	/**
	 * Cuánto se sube la dificultad (y con ella el texto) para pegarla al icono: el icono acaba en y - 23 y la
	 * dificultad empezaba en y + 9, así que quedaba un hueco de 32 px; con esto queda a 6 px.
	 */
	private static final int DIFFICULTY_UP = 26;

	/** Quest de BTE Mobs que abre el portal del Twilight Forest. */
	private static final String TWILIGHT_QUEST = "bte_mobs:noah_infested_temple";

	/**
	 * Un trozo de la guía de un ojo: una nota (quest == null) o una quest de BTE Mobs con su cabecera. La quest se
	 * dibuja como enlace (o censurada si aún no se puede ver) con sus requisitos; las quests previas que ya se pueden
	 * ver también son enlaces.
	 */
	private record Part(@org.jetbrains.annotations.Nullable String quest, String key) {}

	/** Guía de un ojo: con description = false las partes sustituyen a la descripción fija. */
	private record Guide(boolean description, java.util.List<Part> parts) {}

	private static Part quest(String quest, String header) { return new Part("bte_mobs:" + quest, header); }
	private static Part note(String key) { return new Part(null, key); }
	private static Guide replace(Part... parts) { return new Guide(false, java.util.List.of(parts)); }
	private static Guide extra(Part... parts) { return new Guide(true, java.util.List.of(parts)); }

	private static final String COMPASS = "endereyesgui.eye.header.compass";
	private static final String MAP = "endereyesgui.eye.header.map";
	private static final String NETHER = "endereyesgui.eye.note.nether";
	private static final String END = "endereyesgui.eye.note.end";

	private static final java.util.Map<IBottom, Guide> GUIDES = java.util.Map.ofEntries(
			java.util.Map.entry(eu.asangarin.endereyesgui.util.DimensionBottom.TWILIGHT_FOREST,
					replace(new Part(TWILIGHT_QUEST, "endereyesgui.twilight.quest_lock"))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.CURSED,
					replace(quest("noah_cursed_eye", "endereyesgui.eye.cursed.quest_lock"))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.EVIL,
					replace(quest("antonio_save_village", "endereyesgui.eye.evil.quest_lock"))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.ROGUE,
					extra(quest("antonio_jungle_compass", COMPASS))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.NETHER,
					extra(note(NETHER))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.EXOTIC,
					extra(quest("anna_shears", "endereyesgui.eye.header.shears"))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.COLD,
					extra(quest("antonio_iceologer", MAP))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.CASTLE,
					extra(quest("antonio_wind_essence", "endereyesgui.eye.header.stone"))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.SPECTRAL,
					extra(note(END))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.FORBIDDEN,
					extra(note("endereyesgui.eye.note.forbidden_map"), note(NETHER))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.FIERY,
					extra(note("endereyesgui.eye.note.twilight"), new Part(TWILIGHT_QUEST, "endereyesgui.twilight.quest_lock"))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.DESERT,
					extra(quest("antonio_sandy_compass", COMPASS))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.ABYSS,
					extra(quest("antonio_submersible_compass", COMPASS))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.SCULK,
					extra(quest("antonio_deepness_compass", COMPASS))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.UNDEAD,
					extra(quest("antonio_frosted_compass", COMPASS))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.FLAME,
					extra(note(NETHER), quest("antonio_fiery_compass", COMPASS))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.MECH,
					extra(quest("noah_wither", "endereyesgui.eye.header.wither"), quest("antonio_mechanical_compass", COMPASS))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.VOID,
					extra(note(END), quest("antonio_nameless_compass", COMPASS))),
			java.util.Map.entry(eu.asangarin.endereyesgui.util.EnderEye.PARASITE,
					extra(note(END), quest("oriana_capsid", "endereyesgui.eye.header.parasite"))));

	/** Guía de este ojo / dimensión, o null si no tiene. */
	@org.jetbrains.annotations.Nullable
	private Guide guide() {
		return GUIDES.get(eye);
	}

	/** ¿La guía tiene alguna quest (y por tanto enlaces que se pueden pulsar)? */
	private boolean hasQuests() {
		Guide g = guide();
		return g != null && g.parts().stream().anyMatch(p -> p.quest() != null);
	}

	protected EnderEyeInspectScreen(IBottom eye, boolean unlocked) {
		super(Component.empty());
		this.eye = eye;
		this.unlocked = unlocked;
	}

	@Override
	protected void init() {
		super.init();

		scaledWidth = width / 2;
		scaledHeight = height / 2;
		addRenderableWidget(new EnderEyeIcon(scaledWidth, scaledHeight, eye.getIconTexture(unlocked)));
		// "Volver" fijo: abajo del todo si hay guía (notas y quests); si solo hay descripción, donde estaba siempre
		int backY = guide() != null ? height - 26 : Math.min(height - 26, scaledHeight + 80);
		backButton = addRenderableWidget(new Button(scaledWidth - 30, backY, 60, 20, Component.translatable("gui.back"), (button) -> Networking.requestEnderGUI()));
		// Zona de texto: de debajo de la dificultad (o del icono, sin dificultad) hasta encima del botón
		areaTop = eye.getDifficult() != null ? scaledHeight + 34 - DIFFICULTY_UP : scaledHeight + 2;
		areaBottom = backY - 4;
	}

	@Override
	public void render(@NotNull PoseStack stack, int x, int y, float delta) {
		this.renderBackground(stack);
		super.render(stack, x, y, delta);
		renderDifficulty(stack);
		QuestLockInfo.reset();
		int maxWidth = width - 40;
		Guide guide = guide();

		int maxScroll = maxScroll();
		scroll = Mth.clamp(scroll, 0, maxScroll);
		// Fuera de la zona de texto el ratón no resalta enlaces (están recortados)
		boolean inArea = y >= areaTop && y < areaBottom;
		int my = inArea ? y : -10000;

		// Todo el texto se dibuja recortado a la zona y desplazado por el scroll
		enableScissor(areaTop, areaBottom);
		int start = areaTop - (int) Math.round(scroll);
		int lineY;
		if (guide == null || guide.description()) {
			lineY = renderDescription(stack, start + 6, maxWidth) + 4;
		} else {
			lineY = start + 2; // la guía sustituye a la descripción
		}

		if (guide != null) {
			int partsStart = lineY;
			for (Part part : guide.parts()) {
				if (part.quest() == null) {
					// Nota (portal que hay que abrir, dónde buscar un mapa...)
					for (net.minecraft.util.FormattedCharSequence line : this.font.split(
							Component.translatable(part.key()).withStyle(net.minecraft.ChatFormatting.GOLD), maxWidth)) {
						this.font.drawShadow(stack, line, scaledWidth - this.font.width(line) / 2.0F, lineY, 0xFFFFFF);
						lineY += this.font.lineHeight + 3;
					}
					lineY += 2;
				} else {
					lineY = QuestLockInfo.renderQuest(stack, this.font, part.quest(), Component.translatable(part.key()),
							scaledWidth, lineY, maxWidth, x, my, false);
				}
			}
			// Un solo aviso de clic al final, si hay algún enlace
			if (QuestLockInfo.hasLinks()) lineY = QuestLockInfo.renderClickHint(stack, this.font, scaledWidth, lineY);
			// Sin BTE Mobs (o con una versión sin enlaces a quests) no se dibuja nada de la guía: la descripción fija
			if (!guide.description() && lineY == partsStart) lineY = renderDescription(stack, start + 6, maxWidth);
		}
		RenderSystem.disableScissor();

		contentHeight = lineY - start;
		renderScrollBar(stack, x, y);
	}

	// ---------------------------------------------------------------- scroll

	private int areaHeight() {
		return Math.max(0, areaBottom - areaTop);
	}

	private int maxScroll() {
		return Math.max(0, contentHeight - areaHeight());
	}

	/** Recorte en coordenadas de la ventana (y desde abajo). */
	private void enableScissor(int top, int bottom) {
		com.mojang.blaze3d.platform.Window window = net.minecraft.client.Minecraft.getInstance().getWindow();
		double scale = window.getGuiScale();
		RenderSystem.enableScissor(0, (int) (window.getHeight() - bottom * scale), window.getWidth(),
				(int) (Math.max(0, bottom - top) * scale));
	}

	private int barX() {
		return width - 14;
	}

	/** Alto y posición de la barra que se arrastra; null si todo cabe. */
	private int[] thumb() {
		int max = maxScroll();
		if (max <= 0) return null;
		int track = areaHeight();
		int size = Math.max(12, track * track / Math.max(1, contentHeight));
		int top = areaTop + (int) Math.round((track - size) * (scroll / max));
		return new int[]{top, size};
	}

	private void renderScrollBar(PoseStack stack, int mx, int my) {
		int[] t = thumb();
		if (t == null) return;
		int x0 = barX();
		fill(stack, x0, areaTop, x0 + BAR_WIDTH, areaBottom, 0x60000000);
		boolean hover = draggingBar || (mx >= x0 - 2 && mx < x0 + BAR_WIDTH + 2 && my >= t[0] && my < t[0] + t[1]);
		fill(stack, x0, t[0], x0 + BAR_WIDTH, t[0] + t[1], hover ? 0xFFFFFFFF : 0xFFA0A0A0);
	}

	private void scrollToMouse(double my) {
		int[] t = thumb();
		if (t == null) return;
		double free = areaHeight() - t[1];
		if (free <= 0) return;
		scroll = Mth.clamp((my - areaTop - t[1] / 2.0) / free * maxScroll(), 0, maxScroll());
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		if (maxScroll() > 0) {
			scroll = Mth.clamp(scroll - delta * SCROLL_STEP, 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mx, my, delta);
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (draggingBar) {
			scrollToMouse(my);
			return true;
		}
		return super.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		draggingBar = false;
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		// Barra de scroll: arrastrar (o pulsar en la pista para saltar)
		if (button == 0 && thumb() != null && mx >= barX() - 2 && mx < barX() + BAR_WIDTH + 2 && my >= areaTop && my < areaBottom) {
			draggingBar = true;
			scrollToMouse(my);
			return true;
		}
		// Enlaces a quests: solo la parte visible de la zona de texto
		if (hasQuests() && button == 0 && my >= areaTop && my < areaBottom && QuestLockInfo.click(mx, my, this)) return true;
		return super.mouseClicked(mx, my, button);
	}

	/**
	 * Las dos líneas de descripción, centradas a partir de y. Cada línea solo se parte si no cabe en el ancho de la
	 * pantalla (antes se cortaba por los lados); si hay sitio se queda en una línea. Devuelve la y siguiente.
	 */
	private int renderDescription(PoseStack stack, int y, int maxWidth) {
		for (int i = 1; i <= 2; i++) {
			Component text = Component.translatable(eye.getDescriptionKey(i));
			if (text.getString().isEmpty()) continue; // segunda línea vacía (p. ej. Ojo del Nether, Ojo de Sculk)
			for (net.minecraft.util.FormattedCharSequence line : this.font.split(text, maxWidth)) {
				this.font.drawShadow(stack, line, scaledWidth - this.font.width(line) / 2.0F, y, 0xFFFFFF);
				y += this.font.lineHeight + 3;
			}
		}
		return y;
	}

	/** Dificultad del ojo en grande, con su color, entre el icono y la descripción. */
	private void renderDifficulty(PoseStack stack) {
		eu.asangarin.endereyesgui.util.EnderEyeDifficult difficult = eye.getDifficult();
		if (difficult == null) return;
		Component text = Component.translatable(difficult.getTranslate()).withStyle(difficult.getColorForChat());
		float scale = 2.5F;
		// El icono deja el depth test activo y escribe profundidad; dibujamos por encima de todo
		com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
		stack.pushPose();
		stack.translate(scaledWidth, scaledHeight + 9 - DIFFICULTY_UP, 400);
		stack.scale(scale, scale, 1.0F);
		this.font.drawShadow(stack, text, -this.font.width(text) / 2.0F, 0, 0xFFFFFF);
		stack.popPose();
		com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
	}
}
