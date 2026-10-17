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

	/** Quest de BTE Mobs que abre el portal del Twilight Forest. */
	private static final String TWILIGHT_QUEST = "bte_mobs:noah_infested_temple";

	private boolean isTwilight() {
		return eye == eu.asangarin.endereyesgui.util.DimensionBottom.TWILIGHT_FOREST;
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
		// En el Twilight Forest la quest y sus requisitos ocupan más: el botón va más abajo
		int backY = isTwilight() ? Math.min(height - 24, scaledHeight + 100) : scaledHeight + 80;
		addRenderableWidget(new Button(scaledWidth - 30, backY, 60, 20, Component.translatable("gui.back"), (button) -> Networking.requestEnderGUI()));
	}

	@Override
	public void render(@NotNull PoseStack stack, int x, int y, float delta) {
		this.renderBackground(stack);
		super.render(stack, x, y, delta);
		renderDifficulty(stack);
		if (isTwilight()) {
			// Quest que abre el portal, como enlace (o censurada si aún no se puede ver) y sus requisitos
			QuestLockInfo.reset();
			int startY = scaledHeight + 4;
			int endY = QuestLockInfo.renderQuest(stack, this.font, TWILIGHT_QUEST,
					Component.translatable("endereyesgui.twilight.quest_lock"), scaledWidth, startY,
					Math.min(width - 40, 320), x, y);
			if (endY != startY) return;
		}
		drawCenteredString(stack, this.font, Component.translatable(eye.getDescriptionKey(1)), scaledWidth, scaledHeight + 40, 0xFFFFFF);
		drawCenteredString(stack, this.font, Component.translatable(eye.getDescriptionKey(2)), scaledWidth, scaledHeight + 52, 0xFFFFFF);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (isTwilight() && button == 0 && QuestLockInfo.click(mx, my, this)) return true;
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
