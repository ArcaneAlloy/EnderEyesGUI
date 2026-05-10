package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.compat.JeiCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla que muestra el tier actual del Occultism Matrix Storage
 * basado en los Ender Eyes obtenidos por el jugador.
 */
public class MatrixStorageScreen extends Screen {

    // Thresholds de ojos para cada tier
    private static final int[] TIER_THRESHOLDS = {3, 7, 13, 18};
    private static final String[] TIER_ITEMS = {
            "occultism:storage_stabilizer_tier1",
            "occultism:storage_stabilizer_tier2",
            "occultism:storage_stabilizer_tier3",
            "occultism:storage_stabilizer_tier4"
    };

    private static final int C_PANEL_BG   = 0xCC100030;
    private static final int C_PANEL_BORD = 0xFF8800CC;
    private static final int C_DIVIDER    = 0xFF550088;
    private static final int PAD          = 12;

    private final int eyesEarned;

    public MatrixStorageScreen(int eyesEarned) {
        super(Component.empty());
        this.eyesEarned = eyesEarned;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new Button(
                width / 2 - 40, height - 28 + 4, 80, 20,
                Component.translatable("gui.back"),
                btn -> Networking.requestEnderGUI()
        ));
    }

    @Override
    public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
        renderBackground(stack);

        drawCenteredString(stack, font,
                Component.translatable("endereyesgui.matrix.title"),
                width / 2, 6, 0xFFFFFF);

        renderPanel(stack, mx, my);

        super.render(stack, mx, my, delta);
    }

    private void renderPanel(PoseStack stack, int mx, int my) {
        int pw = Math.min(300, width - PAD * 4);
        int px = width / 2 - pw / 2;
        int py = 24;
        int ph = height - 28 - py - 4;
        int cx = width / 2;

        // Fondo y bordes
        fill(stack, px, py, px + pw, py + ph, C_PANEL_BG);
        fill(stack, px,          py,          px + pw,     py + 1,      C_PANEL_BORD);
        fill(stack, px,          py + ph - 1, px + pw,     py + ph,     C_PANEL_BORD);
        fill(stack, px,          py,          px + 1,      py + ph,     C_PANEL_BORD);
        fill(stack, px + pw - 1, py,          px + pw,     py + ph,     C_PANEL_BORD);

        int ty = py + PAD;

        // ── Ojos obtenidos ───────────────────────────────────────────────────
        Component eyesLine = Component.translatable("endereyesgui.matrix.eyes_obtained", eyesEarned)
                .withStyle(ChatFormatting.YELLOW);
        drawCenteredString(stack, font, eyesLine, cx, ty, 0xFFFFFF);
        ty += font.lineHeight + 8;

        // ── Divisor ───────────────────────────────────────────────────────────
        fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
        ty += 8;

        // ── Tier actual ───────────────────────────────────────────────────────
        int currentTier = getCurrentTier();
        if (currentTier >= 0) {
            Component tierLabel = Component.translatable("endereyesgui.matrix.current_tier")
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
            drawCenteredString(stack, font, tierLabel, cx, ty, 0xFFFFFF);
            ty += font.lineHeight + 6;

            // Icono del Storage Stabilizer actual centrado
            ItemStack tierItem = getTierItem(currentTier);
            if (!tierItem.isEmpty()) {
                int iconX = cx - 8;
                int iconY = ty;
                minecraft.getItemRenderer().renderAndDecorateFakeItem(tierItem, iconX, iconY);
                ty += 16 + 4;
                // Nombre del item
                String name = tierItem.getHoverName().getString();
                drawCenteredString(stack, font, Component.literal(name)
                        .withStyle(ChatFormatting.GOLD), cx, ty, 0xFFFFFF);
                ty += font.lineHeight + 8;
            }
        } else {
            Component noTier = Component.translatable("endereyesgui.matrix.no_tier")
                    .withStyle(ChatFormatting.GRAY);
            drawCenteredString(stack, font, noTier, cx, ty, 0xFFFFFF);
            ty += font.lineHeight + 8;
        }

        // ── Divisor ───────────────────────────────────────────────────────────
        fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
        ty += 8;

        // ── Progreso hacia los tiers ──────────────────────────────────────────
        Component progressLabel = Component.translatable("endereyesgui.matrix.progress")
                .withStyle(ChatFormatting.GRAY);
        drawCenteredString(stack, font, progressLabel, cx, ty, 0xAAAAAA);
        ty += font.lineHeight + 6;

        for (int i = 0; i < TIER_THRESHOLDS.length; i++) {
            int threshold = TIER_THRESHOLDS[i];
            ItemStack item = getTierItem(i);
            boolean unlocked = eyesEarned >= threshold;

            // Icono a la izquierda
            int rowX = px + PAD + 4;
            int rowY = ty;
            minecraft.getItemRenderer().renderAndDecorateFakeItem(item, rowX, rowY);

            // Nombre del tier
            String tierName = item.isEmpty() ? "Tier " + (i + 1) : item.getHoverName().getString();
            int nameColor = unlocked ? 0x55FF55 : 0x996699;
            font.draw(stack, tierName, rowX + 20, rowY + (16 - font.lineHeight) / 2, nameColor);

            // Estado a la derecha
            if (unlocked) {
                font.draw(stack, "\u2714", px + pw - PAD - font.width("\u2714") - 4,
                        rowY + (16 - font.lineHeight) / 2, 0x55FF55);
            } else {
                String needed = eyesEarned + "/" + threshold;
                font.draw(stack, needed, px + pw - PAD - font.width(needed) - 4,
                        rowY + (16 - font.lineHeight) / 2, 0xFF5555);
            }

            ty += 20;
        }
    }

    private int getCurrentTier() {
        int tier = -1;
        for (int i = 0; i < TIER_THRESHOLDS.length; i++) {
            if (eyesEarned >= TIER_THRESHOLDS[i]) tier = i;
        }
        return tier;
    }

    private ItemStack getTierItem(int tierIndex) {
        if (tierIndex < 0 || tierIndex >= TIER_ITEMS.length) return ItemStack.EMPTY;
        var item = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(TIER_ITEMS[tierIndex]));
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
