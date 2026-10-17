package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;

/**
 * Scroll vertical del panel de detalle de las recetas.
 *
 * Cuando el contenido (estado, quest que la desbloquea, stats, bases, ingredientes...) no cabe en el panel, se recorta
 * al panel y se puede desplazar con la rueda del ratón; a la derecha sale una barra que indica la posición.
 * Al cambiar de receta vuelve arriba.
 */
final class PanelScroll {
    private static final int STEP = 12;
    private static final int BOTTOM_MARGIN = 6;

    private int scroll = 0;
    private int maxScroll = 0;
    private Object lastKey = null;
    private boolean active = false;
    private int x0, y0, x1, y1;

    /** Empieza a dibujar el contenido (ya con el fondo del panel). Devuelve cuánto hay que subirlo. */
    int begin(int px, int py, int pw, int ph, Object key) {
        if (key != lastKey) {
            lastKey = key;
            scroll = 0;
        }
        x0 = px;
        y0 = py;
        x1 = px + pw;
        y1 = py + ph;
        active = true;
        ListClip.begin(x0 + 1, y0 + 1, x1 - 1, y1 - 1);
        return scroll;
    }

    /** y donde ha acabado el contenido (ya desplazado): sirve para saber cuánto se puede bajar. */
    void contentEnd(int ty) {
        maxScroll = Math.max(0, ty + scroll - (y1 - BOTTOM_MARGIN));
        if (scroll > maxScroll) scroll = maxScroll;
    }

    /** Quita el recorte y dibuja la barra de scroll si hace falta. Llamar siempre después del panel. false si este frame no se ha usado. */
    boolean finish(PoseStack stack) {
        if (!active) return false;
        active = false;
        ListClip.end();
        if (maxScroll <= 0) return true;
        int trackX = x1 - 4, trackTop = y0 + 3, trackBottom = y1 - 3;
        int track = trackBottom - trackTop;
        int visible = y1 - y0;
        int thumb = Math.max(12, track * visible / (visible + maxScroll));
        int thumbY = trackTop + (track - thumb) * scroll / maxScroll;
        GuiComponent.fill(stack, trackX, trackTop, trackX + 2, trackBottom, 0x55000000);
        GuiComponent.fill(stack, trackX, thumbY, trackX + 2, thumbY + thumb, 0xFFAA55DD);
        return true;
    }

    /** ¿Está el ratón dentro del panel? (para no hacer caso a lo que queda recortado fuera) */
    boolean contains(double mx, double my) {
        return mx >= x0 && mx < x1 && my >= y0 && my < y1;
    }

    /** Rueda del ratón sobre el panel. */
    boolean scroll(double mx, double my, double delta) {
        if (maxScroll <= 0 || !contains(mx, my)) return false;
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(delta * STEP)));
        return true;
    }
}
