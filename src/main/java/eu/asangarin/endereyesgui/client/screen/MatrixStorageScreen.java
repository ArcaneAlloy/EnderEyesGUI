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

    // Thresholds de ojos para cada tier (el 0 es el Dimensional Storage Actuator solo, desbloqueado desde el principio)
    private static final int[] TIER_THRESHOLDS = {0, 3, 7, 13, 18};
    private static final String[] TIER_ITEMS = {
            "occultism:storage_controller",
            "occultism:storage_stabilizer_tier1",
            "occultism:storage_stabilizer_tier2",
            "occultism:storage_stabilizer_tier3",
            "occultism:storage_stabilizer_tier4"
    };

    private static final int C_PANEL_BG   = 0xCC100030;
    private static final int C_PANEL_BORD = 0xFF8800CC;
    private static final int C_DIVIDER    = 0xFF550088;
    /** Separador entre tiers: más tenue que el de las secciones. */
    private static final int C_ROW_DIVIDER = 0x88550088;
    private static final int PAD          = 12;

    /**
     * Capacidad (slots) de cada tier, igual que defaultconfigs/occultism-server.toml del modpack:
     * controllerBaseSlots = 64 y stabilizerTier1..4Slots = 128, 256, 512, 1024.
     * Tier 0 = el Actuator solo; tier N = el Actuator con el Storage Stabilizer de ese tier.
     */
    private static final int BASE_SLOTS = 64;
    private static final int[] STABILIZER_SLOTS = {0, 128, 256, 512, 1024};

    private static int tierSlots(int tier) {
        return BASE_SLOTS + STABILIZER_SLOTS[tier];
    }

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

        // Con 5 tiers (y la capacidad de cada uno) el contenido es más alto: espacios algo más justos
        int ty = py + PAD;

        // ── Ojos obtenidos ───────────────────────────────────────────────────
        Component eyesLine = Component.translatable("endereyesgui.matrix.eyes_obtained", eyesEarned)
                .withStyle(ChatFormatting.YELLOW);
        drawCenteredString(stack, font, eyesLine, cx, ty, 0xFFFFFF);
        ty += font.lineHeight + 6;

        fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
        ty += 6;

        // ── Tier actual (el 0 siempre está desbloqueado) ─────────────────────
        int currentTier = Math.max(0, getCurrentTier());
        drawCenteredString(stack, font, Component.translatable("endereyesgui.matrix.current_tier")
                .withStyle(ChatFormatting.LIGHT_PURPLE), cx, ty, 0xFFFFFF);
        ty += font.lineHeight + 4;

        // Icono y nombre en la misma línea, centrados
        ItemStack tierItem = getTierItem(currentTier);
        String currentName = tierName(currentTier, tierItem);
        int nameW = Math.min(font.width(currentName), pw - PAD * 2 - 20);
        currentName = font.plainSubstrByWidth(currentName, nameW);
        int rowW = (tierItem.isEmpty() ? 0 : 20) + font.width(currentName);
        int rx = cx - rowW / 2;
        if (!tierItem.isEmpty()) {
            minecraft.getItemRenderer().renderAndDecorateFakeItem(tierItem, rx, ty);
            rx += 20;
        }
        font.drawShadow(stack, currentName, rx, ty + (16 - font.lineHeight) / 2, 0xFFAA00);
        ty += 16 + 3;

        // Capacidad del tier actual
        drawCenteredString(stack, font, Component.translatable("endereyesgui.matrix.capacity", tierSlots(currentTier))
                .withStyle(ChatFormatting.AQUA), cx, ty, 0xFFFFFF);
        ty += font.lineHeight + 6;

        fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
        ty += 6;

        // ── Progreso hacia los tiers ──────────────────────────────────────────
        drawCenteredString(stack, font, Component.translatable("endereyesgui.matrix.progress")
                .withStyle(ChatFormatting.GRAY), cx, ty, 0xAAAAAA);
        ty += font.lineHeight + 4;

        // Alto de cada fila: el que sobre en el panel repartido entre los tiers (mínimo 20, máximo 30),
        // con una línea separadora entre uno y otro
        int rows = TIER_THRESHOLDS.length;
        int rowH = Math.max(22, Math.min(34, (py + ph - 6 - ty) / rows));
        // Hueco igual encima y debajo del texto (19 = nombre + capacidad, 1 = línea separadora)
        int rowPad = Math.max(1, (rowH - 19 - 1) / 2);

        for (int i = 0; i < TIER_THRESHOLDS.length; i++) {
            if (i > 0) fill(stack, px + PAD + 4, ty, px + pw - PAD - 4, ty + 1, C_ROW_DIVIDER);
            int threshold = TIER_THRESHOLDS[i];
            ItemStack item = getTierItem(i);
            boolean unlocked = eyesEarned >= threshold;

            // Icono a la izquierda
            int rowX = px + PAD + 4;
            int rowY = ty + 1 + rowPad;
            minecraft.getItemRenderer().renderAndDecorateFakeItem(item, rowX, rowY);

            // Estado a la derecha: ✔ o ojos que tiene / ojos que hacen falta
            String state = unlocked ? "\u2714" : eyesEarned + "/" + threshold;
            int stateX = px + pw - PAD - font.width(state) - 4;
            font.draw(stack, state, stateX, rowY + (16 - font.lineHeight) / 2, unlocked ? 0x55FF55 : 0xFF5555);

            // Nombre del tier y, debajo, su capacidad (sin pisar el estado de la derecha)
            int textX = rowX + 20;
            int maxW = stateX - 6 - textX;
            font.draw(stack, font.plainSubstrByWidth(tierName(i, item), maxW), textX, rowY,
                    unlocked ? 0x55FF55 : 0x996699);
            Component slots = Component.translatable("endereyesgui.matrix.capacity", tierSlots(i));
            font.draw(stack, slots, textX, rowY + font.lineHeight + 1, unlocked ? 0x55FFFF : 0x777788);

            ty += rowH;
        }
    }

    /** Nombre de la fila: "Tier 0" + nombre del Actuator, o el nombre del Storage Stabilizer (ya lleva su tier). */
    private String tierName(int tier, ItemStack item) {
        if (tier == 0) {
            String actuator = item.isEmpty() ? "Dimensional Storage Actuator" : item.getHoverName().getString();
            return Component.translatable("endereyesgui.matrix.tier0", actuator).getString();
        }
        return item.isEmpty() ? "Tier " + tier : item.getHoverName().getString();
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
