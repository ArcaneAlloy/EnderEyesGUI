package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.compat.JeiCompat;
import eu.asangarin.endereyesgui.util.SimpleRecipeData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla genérica para Explorer Recipes y Druid Recipes.
 * Lista simple (sin tabs de categoría) con panel de detalle.
 */
public class SimpleRecipesScreen extends Screen {

    private final List<SimpleRecipeData> recipes;
    private final String titleKey;
    private SimpleRecipeData selected = null;
    private ItemStack jeiHoveredItem = ItemStack.EMPTY;

    private static final float LIST_FRACTION = 0.50f;
    private static final int   TITLE_H       = 20;
    private static final int   BOTTOM_H      = 28;
    private static final int   PAD           = 8;

    private static final int C_UNLOCKED   = 0xFF55FF55;
    private static final int C_LOCKED     = 0xFFFF5555;
    private static final int C_LIST_BG    = 0xAA080018;
    private static final int C_PANEL_BG   = 0xCC100030;
    private static final int C_PANEL_BORD = 0xFF8800CC;
    private static final int C_DIVIDER    = 0xFF550088;
    private static final int C_SELECTED   = 0x887700BB;
    private static final int C_HOVER      = 0x447700BB;

    private static final String ICON_OK  = "\u2714 ";
    private static final String ICON_ERR = "\u2718 ";

    private RecipeList recipeList;

    public SimpleRecipesScreen(List<SimpleRecipeData> recipes, String titleKey) {
        super(Component.empty());
        this.recipes  = new ArrayList<>(recipes);
        this.titleKey = titleKey;
    }

    @Override
    protected void init() {
        super.init();
        int listW   = (int) (width * LIST_FRACTION);
        int listTop = TITLE_H + 2;
        int listBot = height - BOTTOM_H;
        recipeList = new RecipeList(minecraft, listW, listBot - listTop, listTop);
        addWidget(recipeList);
        addRenderableWidget(new Button(
                width / 2 - 40, height - BOTTOM_H + 4, 80, 20,
                Component.translatable("gui.back"),
                btn -> Networking.requestEnderGUI()
        ));
    }

    @Override
    public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
        renderBackground(stack);
        jeiHoveredItem = ItemStack.EMPTY;
        drawCenteredString(stack, font,
                Component.translatable(titleKey), width / 2, 6, 0xFFFFFF);
        recipeList.render(stack, mx, my, delta);
        renderDetailPanel(stack, mx, my);
        super.render(stack, mx, my, delta);
        if (hoveredIngredient != null)
            renderTooltip(stack, hoveredIngredient, hoveredIngX, hoveredIngY);
    }

    private ItemStack hoveredIngredient = null;
    private int hoveredIngX, hoveredIngY;

    private void renderDetailPanel(PoseStack stack, int mx, int my) {
        int listW = (int) (width * LIST_FRACTION);
        int px    = listW + PAD;
        int py    = TITLE_H + 2;
        int pw    = width - listW - PAD * 2 - PAD;
        int ph    = height - BOTTOM_H - py;
        int cx    = px + pw / 2;

        fill(stack, px, py, px + pw, py + ph, C_PANEL_BG);
        fill(stack, px,          py,          px + pw,     py + 1,      C_PANEL_BORD);
        fill(stack, px,          py + ph - 1, px + pw,     py + ph,     C_PANEL_BORD);
        fill(stack, px,          py,          px + 1,      py + ph,     C_PANEL_BORD);
        fill(stack, px + pw - 1, py,          px + pw,     py + ph,     C_PANEL_BORD);

        if (selected == null) {
            List<FormattedCharSequence> lines = font.split(
                    Component.translatable("endereyesgui.enchantments.detail.hint")
                            .withStyle(ChatFormatting.GRAY), pw - PAD * 2);
            int sy = py + ph / 2 - (lines.size() * (font.lineHeight + 2)) / 2;
            for (int i = 0; i < lines.size(); i++) {
                int lx = cx - font.width(lines.get(i)) / 2;
                font.draw(stack, lines.get(i), lx, sy + i * (font.lineHeight + 2), 0x888888);
            }
            return;
        }

        int ty = py + PAD;

        // ── Fila título: item + nombre ────────────────────────────────────────
        minecraft.getItemRenderer().renderAndDecorateFakeItem(selected.getResult(), px + PAD, ty);
        if (mx >= px + PAD && mx < px + PAD + 16 && my >= ty && my < ty + 16)
            jeiHoveredItem = selected.getResult();
        String nameStr = font.plainSubstrByWidth(selected.getResult().getHoverName().getString(),
                pw - PAD - 16 - 4 - PAD);
        font.draw(stack, nameStr, px + PAD + 16 + 4, ty + (16 - font.lineHeight) / 2, 0xDD00FF);
        ty += 16 + 6;

        // ── Divisor ───────────────────────────────────────────────────────────
        fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
        ty += 6;

        // ── Tier ─────────────────────────────────────────────────────────────
        drawCenteredString(stack, font,
                Component.translatable("bte_mobs.tier." + selected.getTier())
                        .withStyle(ChatFormatting.GOLD), cx, ty, 0xFFFFFF);
        ty += font.lineHeight + 4;

        // ── Estado ────────────────────────────────────────────────────────────
        if (selected.isUnlocked()) {
            MutableComponent s = Component.literal(ICON_OK).withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable("endereyesgui.enchantments.detail.unlocked")
                            .withStyle(ChatFormatting.GREEN));
            drawCenteredString(stack, font, s, cx, ty, 0xFFFFFF);
        } else {
            MutableComponent s = Component.literal(ICON_ERR).withStyle(ChatFormatting.RED)
                    .append(Component.translatable("endereyesgui.enchantments.detail.locked")
                            .withStyle(ChatFormatting.RED));
            drawCenteredString(stack, font, s, cx, ty, 0xFFFFFF);
        }
        ty += font.lineHeight + 6;

        // ── Divisor ───────────────────────────────────────────────────────────
        fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER);
        ty += 6;

        // ── Ingredientes ──────────────────────────────────────────────────────
        hoveredIngredient = null;
        List<ItemStack> ings = selected.getIngredients();
        if (!ings.isEmpty()) {
            Component label = Component.translatable("endereyesgui.blacksmith.ingredients")
                    .withStyle(ChatFormatting.GRAY);
            font.draw(stack, label.getString(), cx - font.width(label) / 2, ty, 0xCCCCCC);
            ty += font.lineHeight + 3;

            int itemSize = 18;
            int ix = px + PAD;
            for (ItemStack ing : ings) {
                if (ix + itemSize > px + pw - PAD) { ix = px + PAD; ty += itemSize; }
                minecraft.getItemRenderer().renderAndDecorateFakeItem(ing, ix, ty);
                minecraft.getItemRenderer().renderGuiItemDecorations(font, ing, ix, ty);
                if (mx >= ix && mx < ix + 16 && my >= ty && my < ty + 16) {
                    hoveredIngredient = ing;
                    hoveredIngX = mx;
                    hoveredIngY = my;
                    jeiHoveredItem = ing;
                }
                ix += itemSize;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (JeiCompat.isAvailable() && !jeiHoveredItem.isEmpty()) {
            var key = com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode);
            if (JeiCompat.isShowRecipesKey(key)) { JeiCompat.showRecipes(jeiHoveredItem); return true; }
            if (JeiCompat.isShowUsagesKey(key))  { JeiCompat.showUsages(jeiHoveredItem);  return true; }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ── Lista ─────────────────────────────────────────────────────────────────

    private class RecipeList extends ObjectSelectionList<RecipeList.Entry> {
        RecipeList(Minecraft mc, int width, int height, int top) {
            super(mc, width, height, top, top + height, 14);
            setRenderHeader(false, 0);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            for (SimpleRecipeData d : recipes) addEntry(new Entry(d));
        }

        @Override
        public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
            fill(stack, 0, y0, getScrollbarPosition() + 6, y1, C_LIST_BG);
            super.render(stack, mx, my, delta);
        }

        @Override protected int getScrollbarPosition() { return this.width - 6; }
        @Override public boolean isFocused() { return false; }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final SimpleRecipeData data;
            Entry(SimpleRecipeData data) { this.data = data; }

            @Override public @NotNull Component getNarration() { return data.getResult().getHoverName(); }

            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                selected = data; RecipeList.this.setSelected(this); return true;
            }

            @Override
            public void render(@NotNull PoseStack stack, int index, int top, int left,
                               int width, int height, int mx, int my, boolean hovered, float delta) {
                boolean sel = RecipeList.this.getSelected() == this;
                if (sel)          fill(stack, left, top, left + width, top + height, C_SELECTED);
                else if (hovered) fill(stack, left, top, left + width, top + height, C_HOVER);

                String icon = data.isUnlocked() ? ICON_OK : ICON_ERR;
                font.draw(stack, icon, left + 2, top + 2, data.isUnlocked() ? C_UNLOCKED : C_LOCKED);
                int nameX = left + 2 + font.width(icon);
                font.draw(stack, font.plainSubstrByWidth(data.getResult().getHoverName().getString(),
                        width - font.width(icon) - 8), nameX, top + 2,
                        data.isUnlocked() ? 0xEE88FF : 0x996699);
            }
        }
    }
}
