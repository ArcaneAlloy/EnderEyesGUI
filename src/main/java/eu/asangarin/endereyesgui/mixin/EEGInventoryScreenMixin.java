package eu.asangarin.endereyesgui.mixin;

import eu.asangarin.endereyesgui.client.screen.widget.EnderEyeInventoryButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class EEGInventoryScreenMixin extends Screen {
	protected EEGInventoryScreenMixin(Component component) {
		super(component);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void eegInit(CallbackInfo ci) {
		this.addRenderableWidget(new EnderEyeInventoryButton(width, height));
	}
}
