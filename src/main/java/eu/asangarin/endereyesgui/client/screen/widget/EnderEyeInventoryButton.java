package eu.asangarin.endereyesgui.client.screen.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Botón de los Ender Eyes en el inventario: primero de la columna de botones a la izquierda del inventario
 * (Ender Eyes, Quests, Mochila, Almacén, Forgotten Realm). Sigue al inventario si se abre el libro de recetas.
 */
public class EnderEyeInventoryButton extends ImageButton {
	private static final ResourceLocation ENDER_EYES_BUTTON_LOCATION = new ResourceLocation("endereyesgui", "textures/gui/inventory_button.png");

	/** Columna de botones: x = izquierda del inventario - 24 (botones de 20), primera fila y = +4. El botón mide 18, se centra. */
	private static final int OFFSET_X = -23;
	private static final int OFFSET_Y = 5;

	private final InventoryScreen screen;

	public EnderEyeInventoryButton(InventoryScreen screen) {
		super(screen.getGuiLeft() + OFFSET_X, screen.getGuiTop() + OFFSET_Y, 18, 18, 0, 0, 18, ENDER_EYES_BUTTON_LOCATION,
				48, 48, (button) -> Networking.requestEnderGUI(),
				(button, stack, mouseX, mouseY) -> {
					List<Component> tooltip = List.of(
						Component.translatable("endereyesgui.inventory_button.tooltip")
					);
					net.minecraft.client.Minecraft.getInstance().screen.renderComponentTooltip(stack, tooltip, mouseX, mouseY);
				},
				Component.translatable("endereyesgui.inventory_button.tooltip"));
		this.screen = screen;
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
		this.x = screen.getGuiLeft() + OFFSET_X;
		this.y = screen.getGuiTop() + OFFSET_Y;
		super.renderButton(poseStack, mouseX, mouseY, partialTick);
	}
}
