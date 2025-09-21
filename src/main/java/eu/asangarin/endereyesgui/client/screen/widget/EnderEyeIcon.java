package eu.asangarin.endereyesgui.client.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class EnderEyeIcon extends AbstractWidget {
	private final ResourceLocation eyeTexture;

	public EnderEyeIcon(int width, int height, ResourceLocation eyeTexture) {
		super(width, height, 16, 16, CommonComponents.EMPTY);
		this.eyeTexture = eyeTexture;
	}

	@Override
	public void updateNarration(@NotNull NarrationElementOutput output) {}

	@Override
	public void render(@NotNull PoseStack stack, int mouseX, int mouseY, float delta) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, eyeTexture);

		RenderSystem.enableDepthTest();
		stack.pushPose();
		stack.translate(-8d, -8d, 0d);
		stack.scale(5.0f, 5.0f, 5.0f);
		stack.translate(((double) x / 5) - 7d, ((double) y / 5) - 19d, 1.0d);
		blit(stack, 0, 0, 0, 0, 16, 16, 16, 16);
		stack.popPose();
	}
}
