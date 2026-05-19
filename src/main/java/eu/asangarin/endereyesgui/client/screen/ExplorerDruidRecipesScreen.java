package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.compat.JeiCompat;
import eu.asangarin.endereyesgui.util.SimpleRecipeData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
 * Pantalla combinada para Explorer Recipes y Druid Recipes en tabs,
 * siguiendo el mismo patrón que BlacksmithRecipesScreen.
 */
public class ExplorerDruidRecipesScreen extends Screen {

    private static final List<String> TABS = List.of("EXPLORER", "DRUID");

    private final List<SimpleRecipeData> explorerRecipes;
    private final List<SimpleRecipeData> druidRecipes;
    private String activeTab = "EXPLORER";
    private SimpleRecipeData selected = null;

    private static final float LIST_FRACTION = 0.50f;
    private static final int   TITLE_H       = 20;
    private static final int   TAB_H         = 18;
    private static final int   BOTTOM_H      = 28;
    private static final int   PAD           = 8;

    private static final int C_UNLOCKED    = 0xFF55FF55;
    private static final int C_LOCKED      = 0xFFFF5555;
    private static final int C_LIST_BG     = 0xAA080018;
    private static final int C_PANEL_BG    = 0xCC100030;
    private static final int C_PANEL_BORD  = 0xFF8800CC;
    private static final int C_DIVIDER     = 0xFF550088;
    private static final int C_SELECTED    = 0x887700BB;
    private static final int C_HOVER       = 0x447700BB;
    private static final int C_TAB_ACTIVE  = 0xCC550099;
    private static final int C_TAB_IDLE    = 0x88330066;
    private static final int C_TAB_BORD    = 0xFF8800CC;
    private static final int C_STAT_LABEL  = 0xFFAAAAAA;
    private static final int C_STAT_VALUE  = 0xFFFFFFFF;
    private static final int C_ING_HEADER  = 0xFFCCCCCC;

    private static final String ICON_OK  = "\u2714 ";
    private static final String ICON_ERR = "\u2718 ";

    private RecipeList recipeList;
    private final List<Button> tabButtons = new ArrayList<>();
    private EditBox searchBox;
    private String searchTerm = "";

    private ItemStack hoveredIngredient = null;
    private int hoveredIngX, hoveredIngY;
    private ItemStack jeiHoveredItem = ItemStack.EMPTY;
    private boolean hoveringResult = false;

    public ExplorerDruidRecipesScreen(List<SimpleRecipeData> explorerRecipes,
                                       List<SimpleRecipeData> druidRecipes) {
        super(Component.empty());
        this.explorerRecipes = new ArrayList<>(explorerRecipes);
        this.druidRecipes    = new ArrayList<>(druidRecipes);
    }

    @Override
    protected void init() {
        super.init();
        tabButtons.clear();
        int listW   = (int) (width * LIST_FRACTION);
        int listTop = TITLE_H + TAB_H + 18;
        int listBot = height - BOTTOM_H;
        int tabW    = listW / TABS.size();
        int tabY    = TITLE_H + 2;

        for (int i = 0; i < TABS.size(); i++) {
            String tab = TABS.get(i);
            int idx = i;
            String tabKey = "EXPLORER".equals(tab)
                    ? "endereyesgui.explorer.title"
                    : "endereyesgui.druid.title";
            Button btn = new Button(idx * tabW, tabY, tabW, TAB_H,
                    Component.translatable(tabKey),
                    b -> { activeTab = tab; selected = null;
                        rebuildList(listW, listTop, listBot); });
            addRenderableWidget(btn);
            tabButtons.add(btn);
        }

        searchBox = new EditBox(font, 2, TITLE_H + TAB_H + 4, listW - 4, 14,
                Component.translatable("endereyesgui.search"));
        searchBox.setMaxLength(50);
        searchBox.setResponder(text -> {
            searchTerm = text.toLowerCase();
            selected = null;
            rebuildList(listW, listTop, listBot);
        });
        addRenderableWidget(searchBox);

        recipeList = new RecipeList(minecraft, listW, listBot - listTop, listTop);
        addWidget(recipeList);

        addRenderableWidget(new Button(
                width / 2 - 40, height - BOTTOM_H + 4, 80, 20,
                Component.translatable("gui.back"),
                btn -> Networking.requestEnderGUI()
        ));
    }

    private void rebuildList(int listW, int listTop, int listBot) {
        children().removeIf(c -> c instanceof RecipeList);
        recipeList = new RecipeList(minecraft, listW, listBot - listTop, listTop);
        addWidget(recipeList);
    }

    private List<SimpleRecipeData> activeRecipes() {
        return "EXPLORER".equals(activeTab) ? explorerRecipes : druidRecipes;
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
        renderBackground(stack);
        hoveredIngredient = null;
        jeiHoveredItem = ItemStack.EMPTY;
        hoveringResult = false;

        // Título centrado
        String titleKey = "EXPLORER".equals(activeTab)
                ? "endereyesgui.explorer.title"
                : "endereyesgui.druid.title";
        drawCenteredString(stack, font,
                Component.translatable(titleKey), width / 2, 6, 0xFFFFFF);

        // Tabs
        int listW = (int) (width * LIST_FRACTION);
        int tabW  = listW / TABS.size();
        int tabY  = TITLE_H + 2;
        for (int i = 0; i < TABS.size(); i++) {
            String tab = TABS.get(i);
            int tx = i * tabW;
            fill(stack, tx, tabY, tx + tabW, tabY + TAB_H,
                    tab.equals(activeTab) ? C_TAB_ACTIVE : C_TAB_IDLE);
            fill(stack, tx, tabY, tx + tabW, tabY + 1, C_TAB_BORD);
            fill(stack, tx, tabY + TAB_H - 1, tx + tabW, tabY + TAB_H, C_TAB_BORD);
            fill(stack, tx, tabY, tx + 1, tabY + TAB_H, C_TAB_BORD);
            fill(stack, tx + tabW - 1, tabY, tx + tabW, tabY + TAB_H, C_TAB_BORD);
        }

        recipeList.render(stack, mx, my, delta);
        renderDetailPanel(stack, mx, my);

        super.render(stack, mx, my, delta);

        if (hoveredIngredient != null) {
            renderTooltip(stack, hoveredIngredient, hoveredIngX, hoveredIngY);
        } else if (hoveringResult && selected != null) {
            renderTooltip(stack, selected.getResult(), mx, my);
        }
    }

    // ── Panel detalle ─────────────────────────────────────────────────────────

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
        hoveringResult = (mx >= px + PAD && mx < px + PAD + 16 && my >= ty && my < ty + 16);
        if (hoveringResult) jeiHoveredItem = selected.getResult();
        String resultName = net.minecraft.ChatFormatting.stripFormatting(
                selected.getResult().getHoverName().getString());
        if (resultName == null) resultName = selected.getResult().getHoverName().getString();
        int nameW = pw - PAD - 16 - 4 - PAD;
        font.drawShadow(stack, font.plainSubstrByWidth(resultName, nameW),
                px + PAD + 16 + 4, ty + (16 - font.lineHeight) / 2, 0xDD00FF);
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

        // ── Descripción del compás (BeyondTheEndCompass) ─────────────────────
        net.minecraft.resources.ResourceLocation compassId =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(selected.getResult().getItem());
        if (compassId != null && "beyondtheendcompass".equals(compassId.getNamespace())) {
            String[] parts = compassId.getPath().split("_");
            for (int i = 1; i < parts.length; i++) {
                String suffix = String.join("_", java.util.Arrays.copyOfRange(parts, i, parts.length));
                Component c1 = Component.translatable("tooltip.beyondtheendcompass." + suffix + ".1");
                Component c2 = Component.translatable("tooltip.beyondtheendcompass." + suffix + ".2");
                if (!c1.getString().equals("tooltip.beyondtheendcompass." + suffix + ".1")) {
                    for (FormattedCharSequence line :
                            font.split(c1.copy().withStyle(ChatFormatting.DARK_GRAY), pw - PAD * 2)) {
                        font.draw(stack, line, cx - font.width(line) / 2, ty, 0xAAAAAA);
                        ty += font.lineHeight + 2;
                    }
                    for (FormattedCharSequence line :
                            font.split(c2.copy().withStyle(ChatFormatting.DARK_AQUA), pw - PAD * 2)) {
                        font.draw(stack, line, cx - font.width(line) / 2, ty, 0x55FFFF);
                        ty += font.lineHeight + 2;
                    }
                    ty += 4;
                    break;
                }
            }
        }

        // ── Ingredientes ──────────────────────────────────────────────────────
        List<ItemStack> ings = selected.getIngredients();
        if (!ings.isEmpty()) {
            Component label = Component.translatable("endereyesgui.blacksmith.ingredients")
                    .withStyle(ChatFormatting.GRAY);
            font.draw(stack, label.getString(), cx - font.width(label) / 2, ty, C_ING_HEADER);
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
            for (SimpleRecipeData d : activeRecipes()) {
                if (searchTerm.isEmpty() || d.getResult().getHoverName().getString()
                        .toLowerCase().contains(searchTerm)) {
                    addEntry(new Entry(d));
                }
            }
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
                selected = data;
                RecipeList.this.setSelected(this);
                return true;
            }

            @Override
            public void render(@NotNull PoseStack stack, int index, int top, int left,
                               int width, int height, int mx, int my, boolean hovered, float delta) {
                boolean sel = RecipeList.this.getSelected() == this;
                if (sel)          fill(stack, left, top, left + width, top + height, C_SELECTED);
                else if (hovered) fill(stack, left, top, left + width, top + height, C_HOVER);

                String icon = data.isUnlocked() ? ICON_OK : ICON_ERR;
                font.drawShadow(stack, icon, left + 2, top + 2,
                        data.isUnlocked() ? C_UNLOCKED : C_LOCKED);
                int nameX = left + 2 + font.width(icon);
                String rawName = net.minecraft.ChatFormatting.stripFormatting(
                        data.getResult().getHoverName().getString());
                if (rawName == null) rawName = data.getResult().getHoverName().getString();
                font.drawShadow(stack, font.plainSubstrByWidth(rawName,
                        width - font.width(icon) - 8), nameX, top + 2,
                        data.isUnlocked() ? 0xEE88FF : 0x996699);
            }
        }
    }
}
