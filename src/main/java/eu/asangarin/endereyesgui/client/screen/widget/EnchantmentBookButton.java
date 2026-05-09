package eu.asangarin.endereyesgui.client.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Botón de acceso a la pantalla de recetas de encantamiento.
 * Usa el mismo frame 27×27 que EnderEyeButton (eye_frame.png)
 * y renderiza un libro encantado como icono central.
 */
public class EnchantmentBookButton extends Button {

	private static final ResourceLocation EYE_FRAME_LOCATION =
			new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/eye_frame.png");

	// ItemStack estático: libro encantado sin encantamientos específicos
	private static final ItemStack BOOK_STACK = new ItemStack(Items.ENCHANTED_BOOK);

	private final List<Component> tooltip;

	public EnchantmentBookButton(int x, int y, OnPress onPress, List<Component> tooltip) {
		super(x, y, 27, 27, CommonComponents.EMPTY, onPress);
		this.tooltip = tooltip;
	}

	@Override
	public void renderButton(@NotNull PoseStack stack, int mouseX, int mouseY, float delta) {
		// ── Frame (mismo atlas que EnderEyeButton) ────────────────────────────
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, EYE_FRAME_LOCATION);
		RenderSystem.enableDepthTest();

		// u=0 (estilo "bloqueado"/neutro), v=27 si hover
		int v = isHoveredOrFocused() ? 27 : 0;
		blit(stack, this.x, this.y, 0f, (float) v, this.width, this.height, 64, 64);

		// ── Libro encantado centrado en el frame ──────────────────────────────
		// El frame tiene 5px de padding (igual que EnderEyeButton usa x+5, y+5)
		Minecraft.getInstance().getItemRenderer()
				.renderAndDecorateFakeItem(BOOK_STACK, this.x + 5, this.y + 5);

		// ── Tooltip al hacer hover ─────────────────────────────────────────────
		if (this.isHovered) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.screen != null) {
				mc.screen.renderComponentTooltip(stack, tooltip, mouseX, mouseY);
			}
		}
	}
}
