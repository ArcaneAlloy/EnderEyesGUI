package eu.asangarin.endereyesgui.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.compat.JeiCompat;
import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import eu.asangarin.endereyesgui.util.WarlockPotionRecipeData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentRecipesScreen extends Screen {

	private final List<EnchantmentRecipeData> recipes;
	private final List<WarlockPotionRecipeData> potions;
	private final int eyesEarned;

	private static final float LIST_FRACTION = 0.50f;
	private static final int   TITLE_H       = 20;
	private static final int   TAB_H         = 16;
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
	private static final int C_TAB_ACTIVE = 0xBB6600CC;
	private static final int C_TAB_IDLE   = 0x66330066;
	private static final int C_TAB_BORD   = 0xFF8800CC;

	private static final String ICON_OK  = "\u2714 ";
	private static final String ICON_ERR = "\u2718 ";

	private static final String TAB_ENCHANTS = "enchantments";
	private static final String TAB_POTIONS  = "potions";
	private String activeTab = TAB_ENCHANTS;

	private RecipeList recipeList;
	private EditBox searchBox;
	private String searchTerm = "";
	private ItemStack jeiHoveredItem = ItemStack.EMPTY;
	private boolean hoveringResult = false;
	private int potionPanelScroll = 0;
	private int potionPanelContentHeight = 0;

	// Para el panel de detalle
	private EnchantmentRecipeData selectedEnch = null;
	private WarlockPotionRecipeData selectedPotion = null;
	private ItemStack hoveredIngredient = null;
	private int hoveredIngX, hoveredIngY;

	public EnchantmentRecipesScreen(List<EnchantmentRecipeData> recipes,
	                                 List<WarlockPotionRecipeData> potions, int eyesEarned) {
		this(recipes, potions, eyesEarned, "enchantments");
	}

	public EnchantmentRecipesScreen(List<EnchantmentRecipeData> recipes,
	                                 List<WarlockPotionRecipeData> potions, int eyesEarned, String initialTab) {
		super(Component.empty());
		this.recipes    = new ArrayList<>(recipes);
		this.potions    = new ArrayList<>(potions);
		this.eyesEarned = eyesEarned;
		this.activeTab  = initialTab;
	}

	@Override
	protected void init() {
		super.init();
		int listW   = (int) (width * LIST_FRACTION);
		int listTop = TITLE_H + TAB_H + 18;
		int listBot = height - BOTTOM_H;

		searchBox = new EditBox(font, 2, TITLE_H + TAB_H + 2, listW - 4, 14,
				Component.translatable("endereyesgui.search"));
		searchBox.setMaxLength(50);
		searchBox.setResponder(text -> {
			searchTerm = text.toLowerCase();
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
		selectedEnch   = null;
		selectedPotion = null;
		recipeList = new RecipeList(minecraft, listW, listBot - listTop, listTop);
		addWidget(recipeList);
	}

	@Override
	public void render(@NotNull PoseStack stack, int mx, int my, float delta) {
		renderBackground(stack);
		hoveredIngredient = null;
		jeiHoveredItem    = ItemStack.EMPTY;
		hoveringResult    = false;

		drawCenteredString(stack, font,
				Component.translatable("endereyesgui.enchantments.title"), width / 2, 6, 0xFFFFFF);

		// ── Tabs ──────────────────────────────────────────────────────────────
		int listW = (int) (width * LIST_FRACTION);
		int tabW  = listW / 2;
		int tabY  = TITLE_H + 2;
		for (int i = 0; i < 2; i++) {
			String tab = i == 0 ? TAB_ENCHANTS : TAB_POTIONS;
			String labelKey = i == 0 ? "endereyesgui.warlock.tab.enchantments" : "endereyesgui.warlock.tab.potions";
			int tx = i * tabW;
			boolean active = tab.equals(activeTab);
			fill(stack, tx, tabY, tx + tabW, tabY + TAB_H, active ? C_TAB_ACTIVE : C_TAB_IDLE);
			fill(stack, tx, tabY, tx + tabW, tabY + 1, C_TAB_BORD);
			fill(stack, tx, tabY + TAB_H - 1, tx + tabW, tabY + TAB_H, C_TAB_BORD);
			fill(stack, tx, tabY, tx + 1, tabY + TAB_H, C_TAB_BORD);
			fill(stack, tx + tabW - 1, tabY, tx + tabW, tabY + TAB_H, C_TAB_BORD);
			drawCenteredString(stack, font,
					Component.translatable(labelKey), tx + tabW / 2, tabY + (TAB_H - font.lineHeight) / 2, 0xFFFFFF);

			// Click en tab
			if (mx >= tx && mx < tx + tabW && my >= tabY && my < tabY + TAB_H) {
				if (net.minecraft.client.Minecraft.getInstance().mouseHandler.isLeftPressed()) {
					// handled in mouseClicked
				}
			}
		}

		recipeList.render(stack, mx, my, delta);

		if (activeTab.equals(TAB_ENCHANTS)) {
			renderEnchantmentPanel(stack, mx, my);
		} else {
			renderPotionPanel(stack, mx, my);
		}

		super.render(stack, mx, my, delta);

		if (hoveredIngredient != null)
			renderTooltip(stack, hoveredIngredient, hoveredIngX, hoveredIngY);
		else if (hoveringResult) {
			if (activeTab.equals(TAB_ENCHANTS) && selectedEnch != null)
				renderTooltip(stack, buildBookStack(selectedEnch), mx, my);
			else if (activeTab.equals(TAB_POTIONS) && selectedPotion != null)
				renderTooltip(stack, selectedPotion.getBasePotion(), mx, my);
		}
	}

	@Override
	public boolean mouseClicked(double mx, double my, int btn) {
		int listW = (int) (width * LIST_FRACTION);
		int tabW  = listW / 2;
		int tabY  = TITLE_H + 2;
		if (my >= tabY && my < tabY + TAB_H) {
			String newTab = mx < tabW ? TAB_ENCHANTS : TAB_POTIONS;
			if (!newTab.equals(activeTab)) {
				activeTab = newTab;
				searchTerm = "";
				if (searchBox != null) searchBox.setValue("");
				int listTop = TITLE_H + TAB_H + 18;
				int listBot = height - BOTTOM_H;
				rebuildList(listW, listTop, listBot);
			}
			return true;
		}
		return super.mouseClicked(mx, my, btn);
	}

	// ── Panel de encantamientos ───────────────────────────────────────────────

	private void renderEnchantmentPanel(PoseStack stack, int mx, int my) {
		int listW = (int) (width * LIST_FRACTION);
		int px = listW + PAD;
		int py = TITLE_H + 2;
		int pw = width - listW - PAD * 2 - PAD;
		int ph = height - BOTTOM_H - py;
		int cx = px + pw / 2;

		fill(stack, px, py, px + pw, py + ph, C_PANEL_BG);
		fill(stack, px,          py,          px + pw,     py + 1,      C_PANEL_BORD);
		fill(stack, px,          py + ph - 1, px + pw,     py + ph,     C_PANEL_BORD);
		fill(stack, px,          py,          px + 1,      py + ph,     C_PANEL_BORD);
		fill(stack, px + pw - 1, py,          px + pw,     py + ph,     C_PANEL_BORD);

		if (selectedEnch == null) {
			drawHint(stack, cx, py, ph, Component.translatable("endereyesgui.enchantments.detail.hint"));
			return;
		}

		int ty = py + PAD;

		// Item
		ItemStack book = buildBookStack(selectedEnch);
		minecraft.getItemRenderer().renderAndDecorateFakeItem(book, px + PAD, ty);
		hoveringResult = (mx >= px + PAD && mx < px + PAD + 16 && my >= ty && my < ty + 16);
		if (hoveringResult) jeiHoveredItem = book;
		font.draw(stack, getEnchantmentName(selectedEnch).getString(),
				px + PAD + 16 + 4, ty + (16 - font.lineHeight) / 2, 0xDD00FF);
		ty += 16 + 6;

		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 6;

		// Tier / estado
		boolean canAfford = eyesEarned >= selectedEnch.getNeedEyes();
		if (selectedEnch.isUnlocked()) {
			drawCenteredString(stack, font,
					Component.literal(ICON_OK).withStyle(ChatFormatting.GREEN)
					.append(Component.translatable("endereyesgui.enchantments.detail.unlocked").withStyle(ChatFormatting.GREEN)),
					cx, ty, 0xFFFFFF);
		} else {
			drawCenteredString(stack, font,
					Component.translatable("endereyesgui.enchantments.detail.eyes_obtained", eyesEarned)
					.withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.YELLOW), cx, ty, 0xFFFFFF);
			ty += font.lineHeight + 3;
			drawCenteredString(stack, font,
					Component.translatable("endereyesgui.enchantments.detail.eyes_cost", selectedEnch.getNeedEyes())
					.withStyle(canAfford ? ChatFormatting.GREEN : ChatFormatting.RED), cx, ty, 0xFFFFFF);
		}
		ty += font.lineHeight + 8;
		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 6;

		// XP + ingredientes
		if (selectedEnch.getExperience() > 0) {
			drawCenteredString(stack, font,
					Component.translatable("endereyesgui.enchantments.detail.experience", selectedEnch.getExperience())
					.withStyle(ChatFormatting.GREEN), cx, ty, 0xFFFFFF);
			ty += font.lineHeight + 4;
		}
		List<ItemStack> ings = selectedEnch.getIngredients();
		if (!ings.isEmpty()) {
			drawCenteredString(stack, font,
					Component.translatable("endereyesgui.blacksmith.ingredients").withStyle(ChatFormatting.GRAY),
					cx, ty, 0xCCCCCC);
			ty += font.lineHeight + 3;
			int ix = px + PAD;
			for (ItemStack ing : ings) {
				if (ix + 18 > px + pw - PAD) { ix = px + PAD; ty += 18; }
				minecraft.getItemRenderer().renderAndDecorateFakeItem(ing, ix, ty);
				minecraft.getItemRenderer().renderGuiItemDecorations(font, ing, ix, ty);
				if (mx >= ix && mx < ix + 16 && my >= ty && my < ty + 16) {
					hoveredIngredient = ing; hoveredIngX = mx; hoveredIngY = my;
					jeiHoveredItem = ing;
				}
				ix += 18;
			}
			ty += 22;
		}

		// Descripción EnchantmentDescriptions
		if (ty < py + ph - PAD) {
			fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 6;
		}
		var ench = ForgeRegistries.ENCHANTMENTS.getValue(selectedEnch.getEnchantmentId());
		if (ench != null) {
			var descKey = "enchantment.description." + selectedEnch.getEnchantmentId().getNamespace()
					+ "." + selectedEnch.getEnchantmentId().getPath();
			var desc = Component.translatable(descKey);
			if (!desc.getString().equals(descKey)) {
				for (FormattedCharSequence line : font.split(desc.copy().withStyle(ChatFormatting.GRAY), pw - PAD * 2)) {
					int lx = cx - font.width(line) / 2;
					font.draw(stack, line, lx, ty, 0xAAAAAA);
					ty += font.lineHeight + 2;
				}
			}
		}
	}

	// ── Panel de pociones ─────────────────────────────────────────────────────

	private void renderPotionPanel(PoseStack stack, int mx, int my) {
		int listW = (int) (width * LIST_FRACTION);
		int px = listW + PAD;
		int py = TITLE_H + 2;
		int pw = width - listW - PAD * 2 - PAD;
		int ph = height - BOTTOM_H - py;
		int cx = px + pw / 2;

		fill(stack, px, py, px + pw, py + ph, C_PANEL_BG);
		fill(stack, px,          py,          px + pw,     py + 1,      C_PANEL_BORD);
		fill(stack, px,          py + ph - 1, px + pw,     py + ph,     C_PANEL_BORD);
		fill(stack, px,          py,          px + 1,      py + ph,     C_PANEL_BORD);
		fill(stack, px + pw - 1, py,          px + pw,     py + ph,     C_PANEL_BORD);

		if (selectedPotion == null) {
			drawHint(stack, cx, py, ph, Component.translatable("endereyesgui.warlock.potions.hint"));
			return;
		}

		// ── Scissor: limitar el renderizado al área del panel ─────────────────
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		double guiScale = mc.getWindow().getGuiScale();
		int screenH = mc.getWindow().getHeight();
		// Convertir coordenadas GUI a píxeles de pantalla (Y invertida en OpenGL)
		int scissorX = (int)(px * guiScale);
		int scissorY = (int)(screenH - (py + ph) * guiScale);
		int scissorW = (int)(pw * guiScale);
		int scissorH = (int)(ph * guiScale);
		com.mojang.blaze3d.systems.RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

		int ty = py + PAD - potionPanelScroll;

		// ── Título: icono poción base + nombre ────────────────────────────────
		ItemStack base = selectedPotion.getBasePotion();
		// Si la poción base no se encontró en el servidor, intentar en cliente
		if (base.isEmpty()) {
			net.minecraft.resources.ResourceLocation tryId =
					net.minecraft.resources.ResourceLocation.tryParse(selectedPotion.getEffectId());
			if (tryId != null) {
				net.minecraft.world.item.alchemy.Potion p2 =
						net.minecraftforge.registries.ForgeRegistries.POTIONS.getValue(tryId);
				if (p2 != null && !p2.getEffects().isEmpty()) {
					base = net.minecraft.world.item.alchemy.PotionUtils.setPotion(
							new ItemStack(net.minecraft.world.item.Items.POTION), p2);
				}
			}
			// Fallback: poción genérica para que haya icono
			if (base.isEmpty()) {
				base = new ItemStack(net.minecraft.world.item.Items.POTION);
			}
		}
		if (!base.isEmpty()) {
			minecraft.getItemRenderer().renderAndDecorateFakeItem(base, px + PAD, ty);
			hoveringResult = (mx >= px + PAD && mx < px + PAD + 16 && my >= ty && my < ty + 16);
			if (hoveringResult) jeiHoveredItem = base;
		}
		String effectId = selectedPotion.getEffectId();
		// Title Case: "bug_pheromones" -> "Bug Pheromones"
		String[] words = effectId.substring(effectId.lastIndexOf(':') + 1).split("_");
		StringBuilder sb = new StringBuilder();
		for (String w : words) {
			if (!w.isEmpty()) {
				if (sb.length() > 0) sb.append(" ");
				sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
			}
		}
		String effectName = sb.toString();
		font.draw(stack, effectName, px + PAD + 20, ty + (16 - font.lineHeight) / 2, 0xDD00FF);
		ty += 22;

		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 6;

		// ── Estado ────────────────────────────────────────────────────────────
		if (selectedPotion.isUnlocked()) {
			drawCenteredString(stack, font,
					Component.literal(ICON_OK).withStyle(ChatFormatting.GREEN)
					.append(Component.translatable("endereyesgui.enchantments.detail.unlocked").withStyle(ChatFormatting.GREEN)),
					cx, ty, 0xFFFFFF);
		} else {
			drawCenteredString(stack, font,
					Component.literal(ICON_ERR).withStyle(ChatFormatting.RED)
					.append(Component.translatable("endereyesgui.enchantments.detail.locked").withStyle(ChatFormatting.RED)),
					cx, ty, 0xFFFFFF);
		}
		ty += font.lineHeight + 8;

		fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 6;

		// ── Variantes con ingredientes ────────────────────────────────────────
		// Construir splash y lingering en cliente a partir de la base
		net.minecraft.world.item.alchemy.Potion pot = net.minecraft.world.item.alchemy.PotionUtils.getPotion(base);
		ItemStack splashPotion    = net.minecraft.world.item.alchemy.PotionUtils.setPotion(
				new ItemStack(net.minecraft.world.item.Items.SPLASH_POTION), pot);
		ItemStack lingeringPotion = net.minecraft.world.item.alchemy.PotionUtils.setPotion(
				new ItemStack(net.minecraft.world.item.Items.LINGERING_POTION), pot);

		ItemStack baseIng      = selectedPotion.getIngredient();
		ItemStack gunpowder    = new ItemStack(net.minecraft.world.item.Items.GUNPOWDER);
		ItemStack dragonBreath = new ItemStack(net.minecraft.world.item.Items.DRAGON_BREATH);
		ItemStack glowstone    = new ItemStack(net.minecraft.world.item.Items.GLOWSTONE_DUST);

		// Buscar variante strong (Potion II) en el registro de Forge
		String eid = selectedPotion.getEffectId();
		net.minecraft.resources.ResourceLocation strongId = new net.minecraft.resources.ResourceLocation(
				eid.substring(0, eid.indexOf(':') + 1) + "strong_" + eid.substring(eid.indexOf(':') + 1));
		net.minecraft.world.item.alchemy.Potion strongPot =
				net.minecraftforge.registries.ForgeRegistries.POTIONS.getValue(strongId);
		// Validar que la poción strong existe Y tiene efectos (no es uncraftable/empty)
		if (strongPot != null && strongPot.getEffects().isEmpty()) strongPot = null;
		ItemStack potionII    = strongPot != null ? net.minecraft.world.item.alchemy.PotionUtils.setPotion(
				new ItemStack(net.minecraft.world.item.Items.POTION), strongPot) : ItemStack.EMPTY;
		ItemStack splashII    = strongPot != null ? net.minecraft.world.item.alchemy.PotionUtils.setPotion(
				new ItemStack(net.minecraft.world.item.Items.SPLASH_POTION), strongPot) : ItemStack.EMPTY;
		ItemStack lingeringII = strongPot != null ? net.minecraft.world.item.alchemy.PotionUtils.setPotion(
				new ItemStack(net.minecraft.world.item.Items.LINGERING_POTION), strongPot) : ItemStack.EMPTY;

		// Sin casos especiales activos por ahora
		boolean isSpecialNoVariants = false;

		// ── Caso especial: swiftness -> variante III con gazelle_horn ──────────
		boolean isSwiftness = "minecraft:swiftness".equals(eid);
		ItemStack gazelleHorn = ItemStack.EMPTY;
		if (isSwiftness) {
			net.minecraft.world.item.Item gh = net.minecraftforge.registries.ForgeRegistries.ITEMS
					.getValue(new net.minecraft.resources.ResourceLocation("alexsmobs", "gazelle_horn"));
			if (gh != null) gazelleHorn = new ItemStack(gh);
		}

		if (isSpecialNoVariants) {
			// Mostrar el item con su ingrediente
			// Para poison_bottle usamos el label generico, para poison_resistance tambien
			String specialLabel = "alexsmobs:poison_bottle".equals(eid)
					? "endereyesgui.warlock.potion.essence"
					: "endereyesgui.warlock.potion.normal";
			renderVariantRow(stack, mx, my, px, ty, pw,
					base, specialLabel, new ItemStack[]{baseIng});
		} else {
			// Normal: base ingredient
			ty = renderVariantRow(stack, mx, my, px, ty, pw,
					base, "endereyesgui.warlock.potion.normal", new ItemStack[]{baseIng});
			ty += 4;
			// Splash: base ingredient + Gunpowder
			ty = renderVariantRow(stack, mx, my, px, ty, pw,
					splashPotion, "endereyesgui.warlock.potion.splash", new ItemStack[]{baseIng, gunpowder});
			ty += 4;
			// Lingering: base ingredient + Dragon Breath
			ty = renderVariantRow(stack, mx, my, px, ty, pw,
					lingeringPotion, "endereyesgui.warlock.potion.lingering", new ItemStack[]{baseIng, dragonBreath});

			// Si existe variante II (strong), mostrarla
			if (!potionII.isEmpty()) {
				ty += 6;
				fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 4;
				ty = renderVariantRow(stack, mx, my, px, ty, pw,
						potionII, "endereyesgui.warlock.potion.normal_ii", new ItemStack[]{baseIng, glowstone});
				ty += 4;
				ty = renderVariantRow(stack, mx, my, px, ty, pw,
						splashII, "endereyesgui.warlock.potion.splash_ii", new ItemStack[]{baseIng, glowstone, gunpowder});
				ty += 4;
				renderVariantRow(stack, mx, my, px, ty, pw,
						lingeringII, "endereyesgui.warlock.potion.lingering_ii", new ItemStack[]{baseIng, glowstone, dragonBreath});
				ty += 18;
			}

			// Caso especial swiftness: variante III con gazelle_horn
			if (isSwiftness && !gazelleHorn.isEmpty()) {
				// Buscar la poción swift III (swift_iii no existe en Forge, usamos strong_swiftness II como base visual)
				// El item resultado es la poción swiftness III visualmente (usamos la II con glowstone extra)
				ty += 6;
				fill(stack, px + PAD, ty, px + pw - PAD, ty + 1, C_DIVIDER); ty += 4;
				ItemStack swiftIII = !potionII.isEmpty() ? potionII :
						net.minecraft.world.item.alchemy.PotionUtils.setPotion(
								new ItemStack(net.minecraft.world.item.Items.POTION),
								net.minecraftforge.registries.ForgeRegistries.POTIONS.getValue(strongId) != null
								? net.minecraftforge.registries.ForgeRegistries.POTIONS.getValue(strongId)
								: net.minecraft.world.item.alchemy.PotionUtils.getPotion(base));
				ty = renderVariantRow(stack, mx, my, px, ty, pw,
						swiftIII, "endereyesgui.warlock.potion.normal_iii", new ItemStack[]{gazelleHorn, glowstone});
				ty += 4;
				ItemStack splashIII = net.minecraft.world.item.alchemy.PotionUtils.setPotion(
						new ItemStack(net.minecraft.world.item.Items.SPLASH_POTION),
						net.minecraft.world.item.alchemy.PotionUtils.getPotion(swiftIII));
				ItemStack lingeringIII = net.minecraft.world.item.alchemy.PotionUtils.setPotion(
						new ItemStack(net.minecraft.world.item.Items.LINGERING_POTION),
						net.minecraft.world.item.alchemy.PotionUtils.getPotion(swiftIII));
				ty = renderVariantRow(stack, mx, my, px, ty, pw,
						splashIII, "endereyesgui.warlock.potion.splash_iii", new ItemStack[]{gazelleHorn, glowstone, gunpowder});
				ty += 4;
				renderVariantRow(stack, mx, my, px, ty, pw,
						lingeringIII, "endereyesgui.warlock.potion.lingering_iii", new ItemStack[]{gazelleHorn, glowstone, dragonBreath});
			}
		}

		// Guardar altura real del contenido para limitar scroll
		potionPanelContentHeight = (ty + potionPanelScroll) - py;

		com.mojang.blaze3d.systems.RenderSystem.disableScissor();

		// ── Barra de scroll visual (dentro del panel) ────────────────────────
		int contentHeight = potionPanelContentHeight;
		if (contentHeight > ph) {
			int trackX = px + pw - 6;   // 6px desde el borde derecho, bien dentro
			int trackY = py + 4;         // 4px desde el borde superior
			int trackH = ph - 8;         // 4px de margen arriba y abajo
			fill(stack, trackX, trackY, trackX + 3, trackY + trackH, 0x33FFFFFF);
			int thumbH = Math.max(10, trackH * ph / contentHeight);
			int maxScroll = Math.max(1, contentHeight - ph);
			int thumbY = trackY + (potionPanelScroll * (trackH - thumbH) / maxScroll);
			fill(stack, trackX, thumbY, trackX + 3, thumbY + thumbH, 0xBB8800CC);
		}
	}

	private int renderVariantRow(PoseStack stack, int mx, int my,
	                              int px, int ty, int pw,
	                              ItemStack potion, String labelKey, ItemStack[] ingredients) {
		// Icono poción
		if (!potion.isEmpty()) {
			minecraft.getItemRenderer().renderAndDecorateFakeItem(potion, px + PAD, ty);
			minecraft.getItemRenderer().renderGuiItemDecorations(font, potion, px + PAD, ty);
			if (mx >= px + PAD && mx < px + PAD + 16 && my >= ty && my < ty + 16) {
				hoveredIngredient = potion; hoveredIngX = mx; hoveredIngY = my;
				jeiHoveredItem = potion;
			}
		}
		// Etiqueta
		font.draw(stack, Component.translatable(labelKey).getString(),
				px + PAD + 20, ty + (16 - font.lineHeight) / 2, 0xCCCCCC);
		// Ingredientes a la derecha con "+" entre ellos
		int ix = px + pw - PAD - 16 * ingredients.length - 6 * (ingredients.length - 1);
		for (int i = 0; i < ingredients.length; i++) {
			if (i > 0) {
				ix += 2;
			}
			ItemStack ing = ingredients[i];
			if (!ing.isEmpty()) {
				minecraft.getItemRenderer().renderAndDecorateFakeItem(ing, ix, ty);
				minecraft.getItemRenderer().renderGuiItemDecorations(font, ing, ix, ty);
				if (mx >= ix && mx < ix + 16 && my >= ty && my < ty + 16) {
					hoveredIngredient = ing; hoveredIngX = mx; hoveredIngY = my;
					jeiHoveredItem = ing;
				}
				ix += 18;
			}
		}
		return ty + 18;
	}

	private void drawHint(PoseStack stack, int cx, int py, int ph, Component hint) {
		List<FormattedCharSequence> lines = font.split(hint.copy().withStyle(ChatFormatting.GRAY),
				(int)(width * LIST_FRACTION));
		int sy = py + ph / 2 - (lines.size() * (font.lineHeight + 2)) / 2;
		for (int i = 0; i < lines.size(); i++) {
			int lx = cx - font.width(lines.get(i)) / 2;
			font.draw(stack, lines.get(i), lx, sy + i * (font.lineHeight + 2), 0x888888);
		}
	}

	// ── JEI ──────────────────────────────────────────────────────────────────

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
	public boolean mouseScrolled(double mx, double my, double delta) {
		int listW = (int) (width * LIST_FRACTION);
		if (mx > listW && selectedPotion != null && activeTab.equals(TAB_POTIONS)) {
			int ph = height - BOTTOM_H - (TITLE_H + 2);
			potionPanelScroll = Math.max(0, Math.min(
					(int)(potionPanelScroll - delta * 10),
					potionPanelContentHeight - ph + PAD * 2));
			return true;
		}
		return super.mouseScrolled(mx, my, delta);
	}

	@Override public boolean isPauseScreen() { return false; }

	// ── Helpers ───────────────────────────────────────────────────────────────

	private Component getEnchantmentName(EnchantmentRecipeData data) {
		Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(data.getEnchantmentId());
		return ench != null ? ench.getFullname(data.getLevel()) : Component.literal(data.getEnchantmentId().getPath());
	}

	private ItemStack buildBookStack(EnchantmentRecipeData data) {
		Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(data.getEnchantmentId());
		if (ench == null) return new ItemStack(net.minecraft.world.item.Items.BOOK);
		ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
		EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(ench, data.getLevel()));
		return book;
	}

	// ── Lista ─────────────────────────────────────────────────────────────────

	private class RecipeList extends ObjectSelectionList<RecipeList.BaseEntry> {
		RecipeList(Minecraft mc, int width, int height, int top) {
			super(mc, width, height, top, top + height, 14);
			setRenderHeader(false, 0);
			setRenderBackground(false);
			setRenderTopAndBottom(false);
			populate();
		}

		void populate() {
			clearEntries();
			if (activeTab.equals(TAB_ENCHANTS)) {
				for (EnchantmentRecipeData d : recipes) {
					if (searchTerm.isEmpty() || getEnchantmentName(d).getString().toLowerCase().contains(searchTerm))
						addEntry(new EnchEntry(d));
				}
			} else {
				for (WarlockPotionRecipeData d : potions) {
					String name = d.getEffectId().substring(d.getEffectId().lastIndexOf(':') + 1).replace("_", " ");
					if (searchTerm.isEmpty() || name.contains(searchTerm))
						addEntry(new PotionEntry(d));
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

		abstract class BaseEntry extends ObjectSelectionList.Entry<BaseEntry> {}

		class EnchEntry extends BaseEntry {
			private final EnchantmentRecipeData data;
			EnchEntry(EnchantmentRecipeData data) { this.data = data; }

			@Override public @NotNull Component getNarration() { return getEnchantmentName(data); }

			@Override
			public boolean mouseClicked(double mx, double my, int btn) {
				selectedEnch = data; RecipeList.this.setSelected(this); return true;
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
				font.draw(stack, font.plainSubstrByWidth(getEnchantmentName(data).getString(),
						width - font.width(icon) - 8), nameX, top + 2,
						data.isUnlocked() ? 0xEE88FF : 0x996699);
			}
		}

		class PotionEntry extends BaseEntry {
			private final WarlockPotionRecipeData data;
			PotionEntry(WarlockPotionRecipeData data) { this.data = data; }

			@Override public @NotNull Component getNarration() {
				return Component.literal(data.getEffectId());
			}

			@Override
			public boolean mouseClicked(double mx, double my, int btn) {
				selectedPotion = data; potionPanelScroll = 0; RecipeList.this.setSelected(this); return true;
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
				// Nombre legible del efecto
				String effectId2 = data.getEffectId();
				String[] wds = effectId2.substring(effectId2.lastIndexOf(':') + 1).split("_");
				StringBuilder sb2 = new StringBuilder();
				for (String w : wds) {
					if (!w.isEmpty()) {
						if (sb2.length() > 0) sb2.append(" ");
						sb2.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
					}
				}
				String name = sb2.toString();
				font.draw(stack, font.plainSubstrByWidth(name, width - font.width(icon) - 8),
						nameX, top + 2, data.isUnlocked() ? 0xEE88FF : 0x996699);
			}
		}
	}
}
