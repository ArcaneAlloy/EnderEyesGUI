package eu.asangarin.endereyesgui.client.screen.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.EnderEyesGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Botón de acceso a la pantalla de recetas del Blacksmith.
 * Mismo frame 27×27 que EnchantmentBookButton, con un yunque como icono.
 */
public class BlacksmithButton extends Button {

	private static final ResourceLocation EYE_FRAME_LOCATION =
			new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/eye_frame.png");

	private static final ItemStack ANVIL_STACK = new ItemStack(Items.ANVIL);

	private final List<Component> tooltip;

	public BlacksmithButton(int x, int y, OnPress onPress, List<Component> tooltip) {
		super(x, y, 27, 27, CommonComponents.EMPTY, onPress);
		this.tooltip = tooltip;
	}

	@Override
	public void renderButton(@NotNull PoseStack stack, int mouseX, int mouseY, float delta) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, EYE_FRAME_LOCATION);
		RenderSystem.enableDepthTest();

		int v = isHoveredOrFocused() ? 27 : 0;
		blit(stack, this.x, this.y, 0f, (float) v, this.width, this.height, 64, 64);

		Minecraft.getInstance().getItemRenderer()
				.renderAndDecorateFakeItem(ANVIL_STACK, this.x + 5, this.y + 5);

		if (this.isHovered) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.screen != null) {
				mc.screen.renderComponentTooltip(stack, tooltip, mouseX, mouseY);
			}
		}
	}
}
