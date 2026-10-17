package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Texto de pestañas y botones que nunca se sale de su recuadro: si cabe, va centrado; si no, se desplaza lentamente
 * de izquierda a derecha y vuelta (con una pausa en cada extremo), recortado al interior del recuadro.
 * Es lo mismo que hacen los botones de las quests de los NPCs de Beyond The End Mobs.
 */
final class ScrollingText {
    private ScrollingText() {}

    /** Margen a cada lado (el borde del recuadro). */
    static final int PADDING = 3;

    /** Dibuja el texto centrado entre x0 y x1 (o desplazándose si no cabe), con su línea de base en y. */
    static void drawCentered(PoseStack stack, Font font, Component text, int x0, int x1, int y, int color) {
        int left = x0 + PADDING, right = x1 - PADDING;
        int inner = right - left;
        int textWidth = font.width(text);
        if (textWidth <= inner) {
            GuiComponent.drawCenteredString(stack, font, text, (x0 + x1) / 2, y, color);
            return;
        }
        int overflow = textWidth - inner;
        double seconds = Util.getMillis() / 1000.0D;
        double period = Math.max(overflow * 0.5D, 3.0D);
        // 0 -> 1 -> 0 suavizado: se para un momento en cada extremo
        double t = Math.sin(Math.PI / 2.0D * Math.cos(Math.PI * 2.0D * seconds / period)) / 2.0D + 0.5D;
        int offset = (int) Math.round(t * overflow);

        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int sx = (int) Math.floor(left * scale);
        int sy = (int) Math.floor(window.getHeight() - (y + font.lineHeight + 2) * scale);
        int sw = (int) Math.ceil(inner * scale);
        int sh = (int) Math.ceil((font.lineHeight + 4) * scale);
        RenderSystem.enableScissor(Math.max(0, sx), Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
        try {
            GuiComponent.drawString(stack, font, text, left - offset, y, color);
        } finally {
            RenderSystem.disableScissor();
        }
    }

    /** Botón de Minecraft cuyo texto se desplaza si no cabe (pestañas de categorías). */
    static Button button(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        return new Button(x, y, width, height, message, onPress) {
            @Override
            public void renderButton(PoseStack stack, int mouseX, int mouseY, float partialTick) {
                // El fondo lo dibuja Minecraft sin texto; el texto, aquí
                Component text = getMessage();
                setMessage(Component.empty());
                super.renderButton(stack, mouseX, mouseY, partialTick);
                setMessage(text);
                int color = getFGColor() | (int) Math.ceil(alpha * 255.0F) << 24;
                drawCentered(stack, Minecraft.getInstance().font, text, this.x, this.x + this.width,
                        this.y + (this.height - 8) / 2, color);
            }
        };
    }
}
