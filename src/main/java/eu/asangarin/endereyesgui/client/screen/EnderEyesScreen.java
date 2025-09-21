package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.client.screen.widget.EnderEyeButton;
import eu.asangarin.endereyesgui.util.EnderEye;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class EnderEyesScreen extends Screen {
	private final EnumSet<EnderEye> eyeSet;

	public EnderEyesScreen(EnumSet<EnderEye> eyeSet) {
		super(Component.empty());
		this.eyeSet = eyeSet;
	}

	@Override
	protected void init() {
		super.init();
		int scaledWidth = width / 2;
		int scaledHeight = height / 2;

		for(EnderEye eye : EnderEye.getValues()) {
			boolean unlocked = eyeSet.contains(eye);
			List<Component> tooltip = Collections.singletonList(Component.translatable(eye.getTranslationKey())
				.withStyle(unlocked ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE));
			int x = 13 * eye.getX();
			if(eye.getX() < 0) x -= 1;
			int y = 13 * eye.getY();
			addRenderableWidget(new EnderEyeButton(scaledWidth + x - 13, scaledHeight + y - 13, eye, unlocked, (button) -> pressEye(eye, unlocked),
				(button, stack, mouseX, mouseY) -> this.renderComponentTooltip(stack, tooltip, mouseX, mouseY)));
		}
	}

	private void pressEye(EnderEye eye, boolean unlocked) {
		if(minecraft != null)
			minecraft.setScreen(new EnderEyeInspectScreen(eye, unlocked));
	}

	@Override
	public void render(@NotNull PoseStack stack, int x, int y, float delta) {
		this.renderBackground(stack);
		super.render(stack, x, y, delta);
	}
}
