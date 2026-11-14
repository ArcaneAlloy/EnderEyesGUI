package eu.asangarin.endereyesgui.compat;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

@JeiPlugin
public class JeiCompat implements IModPlugin {

    private static final Logger LOGGER = LogManager.getLogger("EnderEyesGUI/JEI");
    private static final ResourceLocation PLUGIN_ID =
            new ResourceLocation("endereyesgui", "jei_plugin");

    @Nullable private static IJeiRuntime jeiRuntime = null;
    @Nullable private static KeyMapping showRecipesKey = null;
    @Nullable private static KeyMapping showUsesKey    = null;
    private static boolean keysMapped = false;

    /** Árbol de evoluciones del Blacksmith; se construye con las recetas del cliente al cargar JEI. */
    @Nullable private static EvolutionTree evolutionTree = null;
    @Nullable private static EvolutionCategory evolutionCategory = null;
    /** Árbol de cada objeto, para abrirlo directamente desde la GUI del Blacksmith. */
    private static final java.util.Map<net.minecraft.world.item.Item, EvolutionTree.Recipe> EVOLUTION_TREES = new java.util.HashMap<>();

    @Override
    public ResourceLocation getPluginUid() { return PLUGIN_ID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        evolutionTree = null;
        evolutionCategory = null;
        EVOLUTION_TREES.clear();
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            evolutionTree = EvolutionTree.build(level.getRecipeManager());
            evolutionCategory = new EvolutionCategory(registration.getJeiHelpers().getGuiHelper(), evolutionTree);
            registration.addRecipeCategories(evolutionCategory);
        } catch (Throwable t) {
            evolutionTree = null;
            evolutionCategory = null;
            LOGGER.warn("[JEI] Blacksmith evolutions not available: {}", t.toString());
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (evolutionTree == null) return;
        var trees = evolutionTree.recipes();
        for (EvolutionTree.Recipe tree : trees) EVOLUTION_TREES.put(tree.root(), tree);
        registration.addRecipes(EvolutionCategory.TYPE, trees);
        LOGGER.info("[JEI] Blacksmith evolutions: {} trees", trees.size());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        keysMapped = false;
        LOGGER.info("[JEI] Runtime available");
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
        showRecipesKey = null;
        showUsesKey    = null;
        keysMapped     = false;
    }

    private static void ensureKeysMapped() {
        if (keysMapped) return;
        keysMapped = true;
        for (KeyMapping km : Minecraft.getInstance().options.keyMappings) {
            String name = km.getName();
            // Loguear todas las teclas de JEI para diagnóstico
            if (name.contains("jei")) {
                LOGGER.info("[JEI] Found key: {}", name);
            }
            // Tomar solo la tecla primaria (sin sufijo 2)
            if (showRecipesKey == null && "key.jei.showRecipe".equals(name)) {
                showRecipesKey = km;
                LOGGER.info("[JEI] Mapped showRecipes -> {}", name);
            }
            if (showUsesKey == null && "key.jei.showUses".equals(name)) {
                showUsesKey = km;
                LOGGER.info("[JEI] Mapped showUses -> {}", name);
            }
        }
    }

    public static boolean isAvailable() {
        return jeiRuntime != null && ModList.get().isLoaded("jei");
    }

    public static boolean isShowRecipesKey(InputConstants.Key key) {
        if (!isAvailable()) return false;
        ensureKeysMapped();
        return showRecipesKey != null && showRecipesKey.isActiveAndMatches(key);
    }

    public static boolean isShowUsagesKey(InputConstants.Key key) {
        if (!isAvailable()) return false;
        ensureKeysMapped();
        return showUsesKey != null && showUsesKey.isActiveAndMatches(key);
    }

    public static void showRecipes(ItemStack stack) {
        if (!isAvailable() || stack.isEmpty()) return;
        try {
            var focus = jeiRuntime.getJeiHelpers().getFocusFactory()
                    .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
            jeiRuntime.getRecipesGui().show(focus);
        } catch (Exception e) {
            LOGGER.warn("[JEI] showRecipes error: {}", e.toString());
        }
    }

    public static void showUsages(ItemStack stack) {
        if (!isAvailable() || stack.isEmpty()) return;
        try {
            var focus = jeiRuntime.getJeiHelpers().getFocusFactory()
                    .createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, stack);
            jeiRuntime.getRecipesGui().show(focus);
        } catch (Exception e) {
            LOGGER.warn("[JEI] showUsages error: {}", e.toString());
        }
    }

    // ── Árbol de evoluciones desde la GUI del Blacksmith ──────────────────────

    /** ¿Tiene el objeto árbol de evoluciones (forma parte de alguna mejora del Blacksmith)? */
    public static boolean hasEvolutionTree(ItemStack stack) {
        return isAvailable() && !stack.isEmpty() && EVOLUTION_TREES.containsKey(stack.getItem());
    }

    /**
     * Abre JEI directamente en el árbol del objeto. IRecipesGui.showRecipes no está en todas las versiones de JEI 11,
     * así que se busca por reflexión; si no está, se abre JEI con el objeto (el árbol sale entre sus pestañas).
     */
    public static void showEvolutionTree(ItemStack stack) {
        if (!hasEvolutionTree(stack)) return;
        EvolutionTree.Recipe tree = EVOLUTION_TREES.get(stack.getItem());
        try {
            var focus = jeiRuntime.getJeiHelpers().getFocusFactory()
                    .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
            var gui = jeiRuntime.getRecipesGui();
            try {
                java.lang.reflect.Method showRecipes = mezz.jei.api.runtime.IRecipesGui.class.getMethod("showRecipes",
                        mezz.jei.api.recipe.category.IRecipeCategory.class, java.util.List.class, java.util.List.class);
                showRecipes.invoke(gui, evolutionCategory, java.util.List.of(tree), java.util.List.of(focus));
            } catch (NoSuchMethodException e) {
                gui.show(focus);
            }
        } catch (Exception e) {
            LOGGER.warn("[JEI] showEvolutionTree error: {}", e.toString());
        }
    }
}
