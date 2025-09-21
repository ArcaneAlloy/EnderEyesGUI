package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.client.screen.widget.EnderEyeIcon;
import eu.asangarin.endereyesgui.util.EnderEye;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class EnderEyeInspectScreen extends Screen {
	private final boolean unlocked;
	private final EnderEye eye;

	private int scaledWidth, scaledHeight;

	protected EnderEyeInspectScreen(EnderEye eye, boolean unlocked) {
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
		addRenderableWidget(new Button(scaledWidth - 30, scaledHeight + 80, 60, 20, Component.translatable("gui.back"), (button) -> Networking.requestEnderGUI()));
	}

	@Override
	public void render(@NotNull PoseStack stack, int x, int y, float delta) {
		this.renderBackground(stack);
		super.render(stack, x, y, delta);
		drawCenteredString(stack, this.font, Component.translatable(eye.getDescriptionKey(1)), scaledWidth, scaledHeight + 40, 0xFFFFFF);
		drawCenteredString(stack, this.font, Component.translatable(eye.getDescriptionKey(2)), scaledWidth, scaledHeight + 52, 0xFFFFFF);
	}
}
