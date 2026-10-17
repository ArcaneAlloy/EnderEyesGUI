package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Debajo de "RECETA BLOQUEADA": la quest de Beyond The End Mobs que desbloquea la receta, como enlace que abre esa
 * quest en el libro de quests, y lo que le falta a la quest (portal del Nether o del End, otra quest...).
 *
 * Todo por reflexión con fr.shoqapik.btemobs.api.QuestRecipeLinks: si el mod de los NPC es de una versión sin esa
 * API, simplemente no se muestra nada más (queda solo "RECETA BLOQUEADA").
 */
final class QuestLockInfo {
    private QuestLockInfo() {}

    private static final String API = "fr.shoqapik.btemobs.api.QuestRecipeLinks";
    private static boolean looked = false;
    private static Method questForRecipe, questTitle, questRequirements, openQuest;

    /** Enlace dibujado en el último frame (para el clic). */
    private static int linkX0, linkY0, linkX1, linkY1;
    @Nullable
    private static String linkQuest;

    private static boolean lookup() {
        if (!looked) {
            looked = true;
            try {
                Class<?> api = Class.forName(API);
                questForRecipe = api.getMethod("questForRecipe", ResourceLocation.class);
                questTitle = api.getMethod("questTitle", String.class);
                questRequirements = api.getMethod("questRequirements", String.class);
                openQuest = api.getMethod("openQuest", String.class, Screen.class);
            } catch (Throwable t) {
                questForRecipe = null;
            }
        }
        return questForRecipe != null;
    }

    /** Se llama al empezar a dibujar el panel: sin receta bloqueada no hay enlace. */
    static void reset() {
        linkQuest = null;
    }

    /** Candado de las quests bloqueadas de Beyond The End Mobs (20x20). */
    private static final ResourceLocation LOCK = new ResourceLocation("bte_mobs", "textures/gui/buttons/explorer/candado.png");

    /** Quest de cada receta (vacío si ninguna); se vacía al abrir cada pantalla de recetas. */
    private static final java.util.Map<ResourceLocation, java.util.Optional<String>> QUEST_CACHE = new java.util.HashMap<>();

    static void clearCache() {
        QUEST_CACHE.clear();
    }

    @Nullable
    private static String questFor(ResourceLocation recipeId) {
        if (recipeId == null || !lookup()) return null;
        return QUEST_CACHE.computeIfAbsent(recipeId, id -> {
            try {
                return java.util.Optional.ofNullable((String) questForRecipe.invoke(null, id));
            } catch (Throwable t) {
                return java.util.Optional.empty();
            }
        }).orElse(null);
    }

    /** ¿La receta solo se desbloquea con una quest (que puede pedir además abrir el portal del Nether / End...)? */
    static boolean isQuestLocked(ResourceLocation recipeId) {
        return questFor(recipeId) != null;
    }

    /** Dibuja el candado (size x size) en lugar del ✘ de la lista. */
    static void drawLock(PoseStack stack, int x, int y, int size) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, LOCK);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        GuiComponent.blit(stack, x, y, size, size, 0, 0, 20, 20, 20, 20);
    }

    /**
     * Dibuja centrado en cx, a partir de y, la quest que desbloquea la receta. Devuelve la y siguiente
     * (la misma si la receta no depende de ninguna quest).
     */
    @SuppressWarnings("unchecked")
    static int render(PoseStack stack, Font font, ResourceLocation recipeId, int cx, int y, int maxWidth, int mx, int my) {
        String quest = questFor(recipeId);
        if (quest == null) return y;
        Component title;
        List<Component> requirements;
        try {
            title = (Component) questTitle.invoke(null, quest);
            requirements = (List<Component>) questRequirements.invoke(null, quest);
        } catch (Throwable t) {
            return y;
        }
        if (title == null) return y;

        y += 2;
        GuiComponent.drawCenteredString(stack, font, Component.translatable("endereyesgui.recipe.quest_lock")
                .withStyle(ChatFormatting.GRAY), cx, y, 0xFFFFFF);
        y += font.lineHeight + 3;

        // Enlace: nombre de la quest (y su NPC); se resalta al pasar el ratón
        Component link = Component.literal("▶ ").append(title);
        String text = font.plainSubstrByWidth(link.getString(), maxWidth);
        int w = font.width(text);
        int x0 = cx - w / 2;
        boolean hover = mx >= x0 - 2 && mx < x0 + w + 2 && my >= y - 2 && my < y + font.lineHeight + 2;
        GuiComponent.fill(stack, x0 - 3, y - 2, x0 + w + 3, y + font.lineHeight + 1, hover ? 0x667700BB : 0x33220044);
        Component shown = Component.literal(text).withStyle(hover ? ChatFormatting.YELLOW : ChatFormatting.AQUA, ChatFormatting.UNDERLINE);
        font.drawShadow(stack, shown, x0, y, 0xFFFFFF);
        linkQuest = quest;
        linkX0 = x0 - 3;
        linkY0 = y - 2;
        linkX1 = x0 + w + 3;
        linkY1 = y + font.lineHeight + 1;
        y += font.lineHeight + 4;

        // Lo que le falta a la quest para estar disponible
        if (requirements != null && !requirements.isEmpty()) {
            GuiComponent.drawCenteredString(stack, font, Component.translatable("endereyesgui.recipe.quest_requires")
                    .withStyle(ChatFormatting.GOLD), cx, y, 0xFFFFFF);
            y += font.lineHeight + 2;
            for (Component req : requirements) {
                for (FormattedCharSequence line : font.split(req, maxWidth)) {
                    font.drawShadow(stack, line, cx - font.width(line) / 2.0F, y, 0xFFFFFF);
                    y += font.lineHeight + 1;
                }
            }
            y += 2;
        }

        GuiComponent.drawCenteredString(stack, font, Component.translatable("endereyesgui.recipe.quest_click")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), cx, y, 0xFFFFFF);
        y += font.lineHeight + 4;
        return y;
    }

    /** Clic en el enlace: abre la quest (al cerrarla se vuelve a esta pantalla). */
    static boolean click(double mx, double my, Screen parent) {
        if (linkQuest == null || mx < linkX0 || mx >= linkX1 || my < linkY0 || my >= linkY1) return false;
        try {
            boolean opened = (Boolean) openQuest.invoke(null, linkQuest, parent);
            if (opened) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            }
            return opened;
        } catch (Throwable t) {
            return false;
        }
    }
}
