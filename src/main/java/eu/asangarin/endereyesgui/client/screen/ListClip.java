package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

/**
 * Recorta lo que se dibuja a un rectángulo de la pantalla (coordenadas de la GUI).
 *
 * Las listas de recetas no dibujan las franjas de arriba y abajo de Minecraft (que tapaban las filas que se salen),
 * así que sin recorte la última fila a medio mostrar se dibujaba por debajo del recuadro de la lista.
 */
final class ListClip {
    private ListClip() {}

    static void begin(int x0, int y0, int x1, int y1) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int x = (int) Math.floor(x0 * scale);
        int y = (int) Math.floor(window.getHeight() - y1 * scale);
        int w = Math.max(0, (int) Math.ceil((x1 - x0) * scale));
        int h = Math.max(0, (int) Math.ceil((y1 - y0) * scale));
        RenderSystem.enableScissor(x, Math.max(0, y), w, h);
    }

    static void end() {
        RenderSystem.disableScissor();
    }
}
