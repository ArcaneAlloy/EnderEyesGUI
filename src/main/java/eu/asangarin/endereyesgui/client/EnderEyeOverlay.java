package eu.asangarin.endereyesgui.client;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.packet.S2CEnderEyeMilestonePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Overlay "Fase 1" (versión simple, sin animación todavía): muestra de
 * golpe la información del desbloqueo de un Ender Eye en una caja
 * centrada durante unos segundos y luego desaparece.
 * <p>
 * Nivel emocional + progreso del feedback: nombre del ojo, X/24, hitos
 * reales (corazón/storage/portal), y qué categorías de encantamientos se
 * ampliaron, sin cifras exactas para no saturar el momento.
 * <p>
 * La animación por fases (oscurecer mundo, icono grande, transiciones)
 * se añadirá en una segunda pasada una vez validados los datos.
 */
public class EnderEyeOverlay {

	private static final long DURATION_MS = 8000;

	private static S2CEnderEyeMilestonePacket current;
	private static long showUntil = 0L;

	public static void show(S2CEnderEyeMilestonePacket packet) {
		current = packet;
		showUntil = System.currentTimeMillis() + DURATION_MS;
	}

	public static final IGuiOverlay HUD = (gui, poseStack, partialTick, width, height) -> {
		if (current == null || System.currentTimeMillis() > showUntil) return;

		Font font = gui.getFont();
		int centerX = width / 2;
		int top = height * 2 / 5;

		List<Component> lines = buildLines(current);

		int lineHeight = 11;
		int boxWidth = 280;
		int boxHeight = lines.size() * lineHeight + 12;
		GuiComponent.fill(poseStack, centerX - boxWidth / 2, top - 6, centerX + boxWidth / 2, top - 6 + boxHeight, 0x90000000);

		int ty = top;
		for (Component line : lines) {
			int lw = font.width(line);
			font.drawShadow(poseStack, line, centerX - lw / 2f, ty, 0xFFFFFF);
			ty += lineHeight;
		}
	};

	/** Nivel emocional + progreso: nombre del ojo, X/24, hitos reales, y qué categorías se ampliaron (sin cifras). */
	private static List<Component> buildLines(S2CEnderEyeMilestonePacket p) {
		List<Component> lines = new ArrayList<>();

		String eyeName = Component.translatable("endereyes." + p.getEyeId() + ".name").getString().toUpperCase();
		lines.add(Component.translatable("endereyesgui.overlay.eye_obtained", eyeName)
				.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

		lines.add(Component.translatable("endereyesgui.overlay.progress", p.getEyesEarned(), p.getEyesTotal())
				.withStyle(ChatFormatting.GREEN));

		if (p.getHeartsGiven() > 0) {
			lines.add(Component.translatable("endereyesgui.overlay.heart", p.getHeartsGiven())
					.withStyle(ChatFormatting.RED));
		}
		if (p.isStorageUpgraded()) {
			lines.add(Component.translatable("endereyesgui.overlay.storage")
					.withStyle(ChatFormatting.AQUA));
		}
		if (!"none".equals(p.getPortalOpened())) {
			lines.add(Component.translatable("endereyesgui.overlay.portal." + p.getPortalOpened())
					.withStyle(ChatFormatting.GOLD));
		}
		if (p.getCombatCount() > 0) {
			lines.add(Component.translatable("endereyesgui.overlay.combat_expanded")
					.withStyle(ChatFormatting.YELLOW));
		}
		if (p.getDefensiveCount() > 0) {
			lines.add(Component.translatable("endereyesgui.overlay.defensive_expanded")
					.withStyle(ChatFormatting.BLUE));
		}
		if (p.getToolsCount() > 0) {
			lines.add(Component.translatable("endereyesgui.overlay.tools_expanded")
					.withStyle(ChatFormatting.GRAY));
		}
		if (p.getOtherCount() > 0) {
			lines.add(Component.translatable("endereyesgui.overlay.other_expanded")
					.withStyle(ChatFormatting.WHITE));
		}

		return lines;
	}
}
