package eu.asangarin.endereyesgui.client.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.EnderEyesGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Botón combinado Explorer + Druid.
 * Muestra un compás (Explorer) como icono y al pulsarlo carga ambas listas.
 */
public class ExplorerDruidButton extends Button {
    private static final ResourceLocation EYE_FRAME_LOCATION =
            new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/eye_frame.png");
    private static final ItemStack ICON_STACK = new ItemStack(Items.COMPASS);
    private final List<Component> tooltip;

    public ExplorerDruidButton(int x, int y, OnPress onPress, List<Component> tooltip) {
        super(x, y, 27, 27, CommonComponents.EMPTY, onPress);
        this.tooltip = tooltip;
    }

    @Override
    public void renderButton(@NotNull PoseStack stack, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, EYE_FRAME_LOCATION);
        RenderSystem.enableDepthTest();
        int v = isHoveredOrFocused() ? 27 : 0;
        blit(stack, this.x, this.y, 0f, (float) v, this.width, this.height, 64, 64);
        Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(ICON_STACK, this.x + 5, this.y + 5);
        if (this.isHovered && Minecraft.getInstance().screen != null)
            Minecraft.getInstance().screen.renderComponentTooltip(stack, tooltip, mouseX, mouseY);
    }
}
