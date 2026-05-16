package eu.asangarin.endereyesgui.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Datos de una WarlockPotionRecipe transmitidos al cliente.
 * Las 3 variantes (normal/splash/lingering) se construyen en cliente.
 */
public class WarlockPotionRecipeData {
    private final ResourceLocation recipeId;
    private final String effectId;
    private final int tier;
    private final boolean unlocked;
    private final ItemStack ingredient;  // ingrediente primario (spider_eye, etc.)
    // Item del tipo de la poción base (para mostrar arriba)
    private final ItemStack basePotion;

    public WarlockPotionRecipeData(ResourceLocation recipeId, String effectId, int tier,
                                   boolean unlocked, ItemStack ingredient, ItemStack basePotion) {
        this.recipeId   = recipeId;
        this.effectId   = effectId;
        this.tier       = tier;
        this.unlocked   = unlocked;
        this.ingredient = ingredient;
        this.basePotion = basePotion;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(recipeId);
        buf.writeUtf(effectId);
        buf.writeVarInt(tier);
        buf.writeBoolean(unlocked);
        buf.writeItem(ingredient);
        buf.writeItem(basePotion);
    }

    public static WarlockPotionRecipeData decode(FriendlyByteBuf buf) {
        ResourceLocation recipeId = buf.readResourceLocation();
        String effectId           = buf.readUtf();
        int tier                  = buf.readVarInt();
        boolean unlocked          = buf.readBoolean();
        ItemStack ingredient      = buf.readItem();
        ItemStack basePotion      = buf.readItem();
        return new WarlockPotionRecipeData(recipeId, effectId, tier, unlocked, ingredient, basePotion);
    }

    public ResourceLocation getRecipeId()  { return recipeId; }
    public String getEffectId()            { return effectId; }
    public int getTier()                   { return tier; }
    public boolean isUnlocked()            { return unlocked; }
    public ItemStack getIngredient()       { return ingredient; }
    public ItemStack getBasePotion()       { return basePotion; }
}
