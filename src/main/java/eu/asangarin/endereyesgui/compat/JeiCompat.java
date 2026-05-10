package eu.asangarin.endereyesgui.compat;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
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

    @Override
    public ResourceLocation getPluginUid() { return PLUGIN_ID; }

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
}
