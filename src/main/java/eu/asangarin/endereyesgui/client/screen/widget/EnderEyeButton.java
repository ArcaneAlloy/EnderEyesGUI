package eu.asangarin.endereyesgui.client.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.EnderEyesGUI;
import eu.asangarin.endereyesgui.util.EnderEye;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class EnderEyeButton extends Button {
	private static final ResourceLocation EYE_FRAME_LOCATION = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/eye_frame.png");

	private final boolean unlocked;
	private final ResourceLocation eyeTexture;

	public EnderEyeButton(int x, int y, EnderEye eye, boolean unlocked, OnPress onPress, OnTooltip onTooltip) {
		super(x, y, 27, 27, CommonComponents.EMPTY, onPress, onTooltip);
		this.unlocked = unlocked;
		this.eyeTexture = eye.getIconTexture(unlocked);
	}

	public void renderButton(@NotNull PoseStack stack, int mouseX, int mouseY, float delta) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, EYE_FRAME_LOCATION);
		int v = isHoveredOrFocused() ? 27 : 0;
		int u = unlocked ? 27 : 0;

		RenderSystem.enableDepthTest();
		blit(stack, this.x, this.y, (float)u, (float)v, this.width, this.height, 64, 64);

		RenderSystem.setShaderTexture(0, eyeTexture);
		blit(stack, this.x + 5, this.y + 5, 0, 0, 16, 16, 16, 16);

		if (this.isHovered)
			this.renderToolTip(stack, mouseX, mouseY);
	}
}
