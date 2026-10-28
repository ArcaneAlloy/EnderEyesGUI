package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.api.IBottom;
import eu.asangarin.endereyesgui.client.screen.widget.EnderEyeIcon;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class EnderEyeInspectScreen extends Screen {
	private final boolean unlocked;
	private final IBottom eye;

	private int scaledWidth, scaledHeight;
	/** Botón "Volver": se recoloca debajo de la quest y sus requisitos, que ocupan un alto variable. */
	private Button backButton;

	/** Quest de BTE Mobs que abre el portal del Twilight Forest. */
	private static final String TWILIGHT_QUEST = "bte_mobs:noah_infested_temple";

	/**
	 * Quest de BTE Mobs que lleva a cada ojo (o al portal del Twilight Forest) y la cabecera que la presenta.
	 * Se muestra como enlace en lugar de la descripción fija, con sus requisitos; las quests previas que ya se
	 * pueden ver también son enlaces.
	 */
	private static final java.util.Map<IBottom, String[]> QUESTS = java.util.Map.of(
			eu.asangarin.endereyesgui.util.DimensionBottom.TWILIGHT_FOREST, new String[]{TWILIGHT_QUEST, "endereyesgui.twilight.quest_lock"},
			eu.asangarin.endereyesgui.util.EnderEye.CURSED, new String[]{"bte_mobs:noah_cursed_eye", "endereyesgui.eye.cursed.quest_lock"},
			eu.asangarin.endereyesgui.util.EnderEye.EVIL, new String[]{"bte_mobs:antonio_save_village", "endereyesgui.eye.evil.quest_lock"},
			eu.asangarin.endereyesgui.util.EnderEye.SCULK, new String[]{"bte_mobs:noah_warden", "endereyesgui.eye.sculk.quest_lock"});

	/** Quest de este ojo / dimensión y su cabecera, o null si no tiene. */
	@org.jetbrains.annotations.Nullable
	private String[] quest() {
		return QUESTS.get(eye);
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
		// La quest y sus requisitos ocupan más que la descripción: el botón va más abajo
		int backY = quest() != null ? Math.min(height - 24, scaledHeight + 100) : scaledHeight + 80;
		backButton = addRenderableWidget(new Button(scaledWidth - 30, backY, 60, 20, Component.translatable("gui.back"), (button) -> Networking.requestEnderGUI()));
	}

	@Override
	public void render(@NotNull PoseStack stack, int x, int y, float delta) {
		this.renderBackground(stack);
		super.render(stack, x, y, delta);
		renderDifficulty(stack);
		String[] quest = quest();
		if (quest != null) {
			// Quest que lleva al ojo / abre el portal, como enlace (o censurada si aún no se puede ver) y sus requisitos.
			// Los ojos tienen debajo del icono la dificultad en grande: la quest empieza debajo de ella.
			QuestLockInfo.reset();
			int startY = eye.getDifficult() != null ? scaledHeight + 36 : scaledHeight + 4;
			int endY = QuestLockInfo.renderQuest(stack, this.font, quest[0],
					Component.translatable(quest[1]), scaledWidth, startY,
					Math.min(width - 40, 320), x, y);
			if (endY != startY) {
				// El botón "Volver" justo debajo de lo dibujado, sin salirse de la pantalla
				if (backButton != null) backButton.y = Math.max(scaledHeight + 80, Math.min(height - 24, endY + 6));
				return;
			}
		}
		// Descripción: cada línea solo se parte si no cabe en el ancho de la pantalla (si no, se cortaba por los lados);
		// si hay sitio se queda en una línea, como antes
		int maxWidth = width - 40;
		int lineY = scaledHeight + 40;
		for (int i = 1; i <= 2; i++) {
			for (net.minecraft.util.FormattedCharSequence line : this.font.split(Component.translatable(eye.getDescriptionKey(i)), maxWidth)) {
				this.font.drawShadow(stack, line, scaledWidth - this.font.width(line) / 2.0F, lineY, 0xFFFFFF);
				lineY += this.font.lineHeight + 3;
			}
		}
		// El botón "Volver" debajo de la descripción, sin salirse de la pantalla
		if (backButton != null) backButton.y = Math.max(scaledHeight + 80, Math.min(height - 24, lineY + 8));
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (quest() != null && button == 0 && QuestLockInfo.click(mx, my, this)) return true;
		return super.mouseClicked(mx, my, button);
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
		stack.translate(scaledWidth, scaledHeight + 9, 400);
		stack.scale(scale, scale, 1.0F);
		this.font.drawShadow(stack, text, -this.font.width(text) / 2.0F, 0, 0xFFFFFF);
		stack.popPose();
		com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
	}
}
