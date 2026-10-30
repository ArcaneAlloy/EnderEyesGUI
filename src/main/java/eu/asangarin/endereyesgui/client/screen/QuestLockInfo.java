package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
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
    /** Opcional (versiones nuevas de BTE Mobs): false si a la quest aún le falta algún requisito. */
    @Nullable
    private static Method canOpenQuest;
    /** Opcional: ids de las quests previas (PARENT_QUEST) de una quest, en el orden de sus requisitos. */
    @Nullable
    private static Method parentQuests;

    /** Enlace a una quest dibujado en el último frame (zona clicable). */
    private record Link(String quest, int x0, int y0, int x1, int y1) {}

    /** Enlaces dibujados en el último frame (para el clic): la quest y las quests previas visibles. */
    private static final List<Link> LINKS = new java.util.ArrayList<>();

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
            try {
                canOpenQuest = Class.forName(API).getMethod("canOpenQuest", String.class);
            } catch (Throwable t) {
                canOpenQuest = null;
            }
            try {
                parentQuests = Class.forName(API).getMethod("parentQuests", String.class);
            } catch (Throwable t) {
                parentQuests = null;
            }
        }
        return questForRecipe != null;
    }

    /** Se llama al empezar a dibujar el panel: sin receta bloqueada no hay enlace. */
    static void reset() {
        LINKS.clear();
    }

    /** ¿Se ha dibujado algún enlace desde el último reset()? */
    static boolean hasLinks() {
        return !LINKS.isEmpty();
    }

    /** Aviso "Haz clic en la quest para abrirla", centrado en cx. Devuelve la y siguiente. */
    static int renderClickHint(PoseStack stack, Font font, int cx, int y) {
        GuiComponent.drawCenteredString(stack, font, Component.translatable("endereyesgui.recipe.quest_click")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), cx, y, 0xFFFFFF);
        return y + font.lineHeight + 4;
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

    /** ¿Se puede abrir la quest? No mientras siga bloqueada (sería un spoiler). */
    private static boolean canOpen(String quest) {
        if (canOpenQuest == null) return true;
        try {
            return (Boolean) canOpenQuest.invoke(null, quest);
        } catch (Throwable t) {
            return true;
        }
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
    static int render(PoseStack stack, Font font, ResourceLocation recipeId, int cx, int y, int maxWidth, int mx, int my) {
        String quest = questFor(recipeId);
        if (quest == null) return y;
        return renderQuest(stack, font, quest, Component.translatable("endereyesgui.recipe.quest_lock"), cx, y, maxWidth, mx, my);
    }

    /**
     * Dibuja centrado en cx, a partir de y, la cabecera (header), la quest como enlace (o censurada si aún está
     * bloqueada), sus requisitos y el aviso de clic. Devuelve la y siguiente. Para usarlo fuera de las recetas
     * (p. ej. el portal del Twilight Forest).
     */
    @SuppressWarnings("unchecked")
    static int renderQuest(PoseStack stack, Font font, String quest, Component header, int cx, int y, int maxWidth, int mx, int my) {
        return renderQuest(stack, font, quest, header, cx, y, maxWidth, mx, my, true);
    }

    /**
     * Igual, pero con clickHint = false no dibuja el aviso "Haz clic en la quest para abrirla" (para pantallas con
     * varias quests seguidas, que lo dibujan una sola vez al final con renderClickHint).
     */
    static int renderQuest(PoseStack stack, Font font, String quest, Component header, int cx, int y, int maxWidth, int mx, int my,
                           boolean clickHint) {
        if (!lookup()) return y;
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
        for (FormattedCharSequence line : font.split(header.copy().withStyle(ChatFormatting.GRAY), maxWidth)) {
            font.drawShadow(stack, line, cx - font.width(line) / 2.0F, y, 0xFFFFFF);
            y += font.lineHeight + 1;
        }
        y += 2;

        // La quest solo se puede abrir si ya está disponible (no le falta nada). Si aún está bloqueada, su nombre se
        // censura con las runas de la mesa de encantamientos y no es clicable (sería un spoiler).
        boolean openable = (requirements == null || requirements.isEmpty()) && canOpen(quest);
        boolean anyLink = false;
        if (openable) {
            // Enlace: nombre de la quest (y su NPC); se resalta al pasar el ratón
            y = drawLink(stack, font, "▶ " + title.getString(), quest, cx, y, maxWidth, mx, my);
            anyLink = true;
        } else {
            // Quest aún bloqueada: nombre censurado, sin enlace
            for (FormattedCharSequence line : font.split(censor(title.getString(), ChatFormatting.GRAY), maxWidth)) {
                font.drawShadow(stack, line, cx - font.width(line) / 2.0F, y, 0xFFFFFF);
                y += font.lineHeight + 1;
            }
            y += 3;
        }

        // Lo que le falta a la quest para estar disponible
        if (requirements != null && !requirements.isEmpty()) {
            GuiComponent.drawCenteredString(stack, font, Component.translatable("endereyesgui.recipe.quest_requires")
                    .withStyle(ChatFormatting.GOLD), cx, y, 0xFFFFFF);
            y += font.lineHeight + 2;
            java.util.Iterator<String> parents = parentQuestIds(quest).iterator();
            for (Component req : requirements) {
                // Requisito "Completa la quest: X" con X ya visible y aún sin completar: enlace que abre X
                String parent = isQuestRequirement(req) && parents.hasNext() ? parents.next() : null;
                if (parent != null && req.getString().contains("\u2718") && !isQuestHidden(parent) && canOpen(parent)) {
                    y = drawLink(stack, font, req.getString().trim(), parent, cx, y, maxWidth, mx, my);
                    anyLink = true;
                    continue;
                }
                for (FormattedCharSequence line : font.split(censorQuestRequirement(req, parent), maxWidth)) {
                    font.drawShadow(stack, line, cx - font.width(line) / 2.0F, y, 0xFFFFFF);
                    y += font.lineHeight + 1;
                }
            }
            y += 2;
        }

        if (anyLink && clickHint) {
            GuiComponent.drawCenteredString(stack, font, Component.translatable("endereyesgui.recipe.quest_click")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), cx, y, 0xFFFFFF);
            y += font.lineHeight + 4;
        }
        return y;
    }

    /** Fuente de la mesa de encantamientos (alfabeto galáctico estándar). */
    private static final ResourceLocation ALT_FONT = new ResourceLocation("minecraft", "alt");
    /** Clave de BTE Mobs del requisito "Completa la quest: %s". */
    private static final String QUEST_REQUIREMENT_KEY = "gui.bte_mobs.quest.requirement.quest";

    /** Texto censurado: runas de la mesa de encantamientos que van cambiando (ofuscado). */
    private static MutableComponent censor(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(style -> style.withFont(ALT_FONT).applyFormat(ChatFormatting.OBFUSCATED).applyFormat(color));
    }

    /** Ids de las quests previas (vacío con versiones de BTE Mobs sin parentQuests). */
    @SuppressWarnings("unchecked")
    private static List<String> parentQuestIds(String quest) {
        if (parentQuests == null) return List.of();
        try {
            List<String> ids = (List<String>) parentQuests.invoke(null, quest);
            return ids == null ? List.of() : ids;
        } catch (Throwable t) {
            return List.of();
        }
    }

    /** ¿La quest aún está bloqueada (le falta algún requisito), es decir, el jugador no debería verla? */
    @SuppressWarnings("unchecked")
    private static boolean isQuestHidden(String quest) {
        try {
            List<Component> reqs = (List<Component>) questRequirements.invoke(null, quest);
            return reqs != null && !reqs.isEmpty();
        } catch (Throwable t) {
            return true;
        }
    }

    /** Línea de enlace a una quest, centrada en cx, resaltada al pasar el ratón. Devuelve la y siguiente. */
    private static int drawLink(PoseStack stack, Font font, String label, String quest, int cx, int y, int maxWidth, int mx, int my) {
        String text = font.plainSubstrByWidth(label, maxWidth);
        int w = font.width(text);
        int x0 = cx - w / 2;
        boolean hover = mx >= x0 - 2 && mx < x0 + w + 2 && my >= y - 2 && my < y + font.lineHeight + 2;
        GuiComponent.fill(stack, x0 - 3, y - 2, x0 + w + 3, y + font.lineHeight + 1, hover ? 0x667700BB : 0x33220044);
        Component shown = Component.literal(text).withStyle(hover ? ChatFormatting.YELLOW : ChatFormatting.AQUA, ChatFormatting.UNDERLINE);
        font.drawShadow(stack, shown, x0, y, 0xFFFFFF);
        LINKS.add(new Link(quest, x0 - 3, y - 2, x0 + w + 3, y + font.lineHeight + 1));
        return y + font.lineHeight + 4;
    }

    /** ¿Es un requisito "Completa la quest: X"? */
    private static boolean isQuestRequirement(Component req) {
        for (Component sib : req.getSiblings()) {
            if (sib.getContents() instanceof TranslatableContents tc && QUEST_REQUIREMENT_KEY.equals(tc.getKey())) return true;
        }
        return false;
    }

    /**
     * Requisito pendiente de otra quest (" ✘ Completa la quest: X (NPC)"): censura el nombre de esa quest si el
     * jugador aún no la puede ver (está bloqueada). Si no se sabe qué quest es (BTE Mobs antiguo), se censura.
     * Los requisitos ya cumplidos y los demás (portales...) se quedan igual.
     */
    private static Component censorQuestRequirement(Component req, @Nullable String parent) {
        boolean isQuestReq = isQuestRequirement(req);
        if (!isQuestReq || !req.getString().contains("\u2718")) return req;
        if (parent != null && !isQuestHidden(parent)) return req;
        MutableComponent out = MutableComponent.create(req.getContents()).setStyle(req.getStyle());
        for (Component sib : req.getSiblings()) {
            if (sib.getContents() instanceof TranslatableContents tc && QUEST_REQUIREMENT_KEY.equals(tc.getKey())) {
                Object[] args = tc.getArgs().clone();
                for (int i = 0; i < args.length; i++) {
                    String name = args[i] instanceof Component c ? c.getString() : String.valueOf(args[i]);
                    args[i] = censor(name, ChatFormatting.RED);
                }
                MutableComponent censored = MutableComponent.create(new TranslatableContents(tc.getKey(), args)).setStyle(sib.getStyle());
                for (Component inner : sib.getSiblings()) censored.append(inner.copy());
                out.append(censored);
            } else {
                out.append(sib.copy());
            }
        }
        return out;
    }

    /** Clic en el enlace: abre la quest (al cerrarla se vuelve a esta pantalla). */
    static boolean click(double mx, double my, Screen parent) {
        Link hit = null;
        for (Link l : LINKS) {
            if (mx >= l.x0() && mx < l.x1() && my >= l.y0() && my < l.y1()) { hit = l; break; }
        }
        if (hit == null || openQuest == null) return false;
        try {
            boolean opened = (Boolean) openQuest.invoke(null, hit.quest(), parent);
            if (opened) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            }
            return opened;
        } catch (Throwable t) {
            return false;
        }
    }
}
