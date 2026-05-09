package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.api.IBottom;
import eu.asangarin.endereyesgui.client.screen.widget.EnderEyeButton;
import eu.asangarin.endereyesgui.client.screen.widget.EnchantmentBookButton;
import eu.asangarin.endereyesgui.util.DimensionBottom;
import eu.asangarin.endereyesgui.util.EnderEye;
import mc.duzo.ender_journey.capabilities.PortalPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

		for(DimensionBottom eye : DimensionBottom.getValues()) {
			PortalPlayer portalPlayer = PortalPlayer.get(minecraft.player).orElse(null);
			boolean unlocked = portalPlayer.getEyesEarn()>=eye.getEyes();
			if(eye.isNeedAdvanced()){
				unlocked = portalPlayer.visitTwilightForest();
			}
			List<Component> tooltip = new ArrayList<>();
			Component nameComponent;
			if (unlocked) {
				nameComponent = Component.translatable(eye.getTranslationKey()).withStyle(ChatFormatting.LIGHT_PURPLE);
			} else if (eye.isNeedAdvanced() || eye.getEyes() == 0) {
				nameComponent = Component.translatable(eye.getTranslationKey()).withStyle(ChatFormatting.DARK_PURPLE);
			} else {
				int eyesNeeded = Math.max(eye.getEyes() - portalPlayer.getEyesEarn(), 0);
				nameComponent = Component.translatable(eye.getTranslationKey())
					.append(Component.literal(" "))
					.append(Component.translatable(eye.getTranslationRemaining()))
					.append(Component.literal(" : " + eyesNeeded))
					.withStyle(ChatFormatting.DARK_PURPLE);
			}
			tooltip.add(nameComponent);

			int x = 13 * eye.getX();
			if(eye.getX() < 0) x -= 1;
			int y = 13 * eye.getY();
			boolean finalUnlocked = unlocked;
			addRenderableWidget(new EnderEyeButton(scaledWidth + x - 13, scaledHeight + y - 13, eye, unlocked,
					(button) -> pressEye(eye, finalUnlocked),
					(button, stack, mouseX, mouseY) -> this.renderComponentTooltip(stack, tooltip, mouseX, mouseY)));
		}

		for(EnderEye eye : EnderEye.getValues()) {
			boolean unlocked = eyeSet.contains(eye);
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(Component.translatable(eye.getTranslationKey()).withStyle(unlocked ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE));
			tooltip.add(Component.translatable(eye.getDifficult().getTranslate()).withStyle(eye.getDifficult().getColorForChat()));

			int x = 13 * eye.getX();
			if(eye.getX() < 0) x -= 1;
			int y = 13 * eye.getY();
			addRenderableWidget(new EnderEyeButton(scaledWidth + x - 13, scaledHeight + y - 13, eye, unlocked,
					(button) -> pressEye(eye, unlocked),
					(button, stack, mouseX, mouseY) -> this.renderComponentTooltip(stack, tooltip, mouseX, mouseY)));
		}

		// ── Botón de encantamientos ───────────────────────────────────────────
		// Botón de encantamientos: centrado entre FIERY(-2,4) y UNDEAD(2,4),
		// una fila más abajo (y=5 → 5*13=65px). x=0 → centrado exactamente.
		// Usa el mismo frame 27x27 que los demás botones con libro encantado de icono.
		List<Component> enchTooltip = List.of(
				Component.translatable("endereyesgui.enchantments.button.tooltip")
		);
		// Hueco natural del grid en x=0, y=-2 (entre GUARDIAN y LOST)
		// Misma fórmula que el resto: scaledWidth + x*13 - 13, scaledHeight + y*13 - 13
		addRenderableWidget(new EnchantmentBookButton(
				scaledWidth + 0 * 13 - 13, scaledHeight + 7 * 13 - 13,
				btn -> Networking.requestEnchantmentsList(),
				enchTooltip
		));
	}

	private void pressEye(IBottom eye, boolean unlocked) {
		if(minecraft != null)
			minecraft.setScreen(new EnderEyeInspectScreen(eye, unlocked));
	}

	@Override
	public void render(@NotNull PoseStack stack, int x, int y, float delta) {
		this.renderBackground(stack);
		super.render(stack, x, y, delta);
	}
}
