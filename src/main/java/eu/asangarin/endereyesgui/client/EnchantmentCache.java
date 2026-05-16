package eu.asangarin.endereyesgui.client;

import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import eu.asangarin.endereyesgui.util.WarlockPotionRecipeData;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cache cliente de recetas de encantamiento.
 * Clave: enchantmentId + level → EnchantmentRecipeData
 */
public class EnchantmentCache {

    private record Key(ResourceLocation id, int level) {}

    private static final Map<Key, EnchantmentRecipeData> CACHE = new HashMap<>();
    private static int cachedEyesEarned = 0;

    private static List<WarlockPotionRecipeData> cachedPotions = new java.util.ArrayList<>();

    public static List<WarlockPotionRecipeData> getPotions() { return cachedPotions; }

    public static void update(List<EnchantmentRecipeData> recipes, List<WarlockPotionRecipeData> potions, int eyesEarned) {
        cachedPotions = potions;
        CACHE.clear();
        for (EnchantmentRecipeData data : recipes) {
            CACHE.put(new Key(data.getEnchantmentId(), data.getLevel()), data);
        }
        cachedEyesEarned = eyesEarned;
    }

    public static EnchantmentRecipeData get(ResourceLocation enchantmentId, int level) {
        return CACHE.get(new Key(enchantmentId, level));
    }

    public static int getEyesEarned() { return cachedEyesEarned; }
    public static boolean hasData()    { return !CACHE.isEmpty(); }
}
