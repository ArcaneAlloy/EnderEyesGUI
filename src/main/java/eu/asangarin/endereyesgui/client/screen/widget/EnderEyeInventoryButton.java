package eu.asangarin.endereyesgui.client.screen.widget;

import eu.asangarin.endereyesgui.Networking;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;

public class EnderEyeInventoryButton extends ImageButton {
	private static final ResourceLocation ENDER_EYES_BUTTON_LOCATION = new ResourceLocation("endereyesgui", "textures/gui/inventory_button.png");

	public EnderEyeInventoryButton(int width, int height) {
		super(width / 2 + 7, height / 2 - 22, 18, 18, 0, 0, 18, ENDER_EYES_BUTTON_LOCATION,
				48, 48, (button) -> Networking.requestEnderGUI());
	}
}
