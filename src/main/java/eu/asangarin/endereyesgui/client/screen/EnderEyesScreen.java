package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.api.IBottom;
import eu.asangarin.endereyesgui.client.screen.widget.EnderEyeButton;
import eu.asangarin.endereyesgui.client.screen.widget.EnchantmentBookButton;
import eu.asangarin.endereyesgui.client.screen.widget.PotionButton;
import eu.asangarin.endereyesgui.client.screen.widget.BlacksmithButton;
import eu.asangarin.endereyesgui.client.screen.widget.NatureButton;
import eu.asangarin.endereyesgui.client.screen.widget.MatrixStorageButton;
import eu.asangarin.endereyesgui.client.screen.MatrixStorageScreen;
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
		// ── 5 botones en fila y=7: Nature(-8) Enchantments(-4) Blacksmith(0) Potions(4) Matrix(8) ──

		// Botón Nature (Explorer + Druid): x=-8, y=7
		List<Component> natureTooltip = List.of(
				Component.translatable("endereyesgui.nature.button.tooltip")
		);
		addRenderableWidget(new NatureButton(
				scaledWidth + (-8) * 13 - 13, scaledHeight + 7 * 13 - 13,
				btn -> Networking.requestNatureList(),
				natureTooltip
		));

		// Botón Encantamientos: x=-4, y=7
		List<Component> enchTooltip = List.of(
				Component.translatable("endereyesgui.enchantments.button.tooltip")
		);
		addRenderableWidget(new EnchantmentBookButton(
				scaledWidth + (-4) * 13 - 13, scaledHeight + 7 * 13 - 13,
				btn -> Networking.requestEnchantmentsList(),
				enchTooltip
		));

		// Botón Blacksmith: x=0, y=7
		List<Component> smithTooltip = List.of(
				Component.translatable("endereyesgui.blacksmith.button.tooltip")
		);
		addRenderableWidget(new BlacksmithButton(
				scaledWidth + 0 * 13 - 13, scaledHeight + 7 * 13 - 13,
				btn -> Networking.requestBlacksmithList(),
				smithTooltip
		));

		// Botón Pociones: x=4, y=7
		List<Component> potionTooltip = List.of(
				Component.translatable("endereyesgui.potions.button.tooltip")
		);
		addRenderableWidget(new PotionButton(
				scaledWidth + 4 * 13 - 13, scaledHeight + 7 * 13 - 13,
				btn -> Networking.requestPotionsList(),
				potionTooltip
		));

		// Botón Matrix Storage: x=8, y=7
		List<Component> matrixTooltip = List.of(
				Component.translatable("endereyesgui.matrix.button.tooltip")
		);
		PortalPlayer portalPlayer2 = PortalPlayer.get(minecraft.player).orElse(null);
		int eyesEarned2 = portalPlayer2 != null ? portalPlayer2.getEyesEarn() : 0;
		int finalEyesEarned = eyesEarned2;
		addRenderableWidget(new MatrixStorageButton(
				scaledWidth + 8 * 13 - 13, scaledHeight + 7 * 13 - 13,
				btn -> minecraft.setScreen(new MatrixStorageScreen(finalEyesEarned)),
				matrixTooltip
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
