package eu.asangarin.endereyesgui.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos de una ExplorerRecipe o DruidRecipe transmitidos al cliente.
 * Sin categorías ni stats — solo resultado, tier, ingredientes y estado.
 */
public class SimpleRecipeData {
    private final ResourceLocation recipeId;
    private final ItemStack result;
    private final int tier;
    private final boolean unlocked;
    private final List<ItemStack> ingredients;

    public SimpleRecipeData(ResourceLocation recipeId, ItemStack result,
                            int tier, boolean unlocked, List<ItemStack> ingredients) {
        this.recipeId    = recipeId;
        this.result      = result;
        this.tier        = tier;
        this.unlocked    = unlocked;
        this.ingredients = ingredients;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(recipeId);
        buf.writeItem(result);
        buf.writeVarInt(tier);
        buf.writeBoolean(unlocked);
        buf.writeVarInt(ingredients.size());
        for (ItemStack s : ingredients) buf.writeItem(s);
    }

    public static SimpleRecipeData decode(FriendlyByteBuf buf) {
        ResourceLocation recipeId = buf.readResourceLocation();
        ItemStack result          = buf.readItem();
        int tier                  = buf.readVarInt();
        boolean unlocked          = buf.readBoolean();
        int count = buf.readVarInt();
        List<ItemStack> ingredients = new ArrayList<>(count);
        for (int i = 0; i < count; i++) ingredients.add(buf.readItem());
        return new SimpleRecipeData(recipeId, result, tier, unlocked, ingredients);
    }

    public ResourceLocation getRecipeId()    { return recipeId; }
    public ItemStack getResult()             { return result; }
    public int getTier()                     { return tier; }
    public boolean isUnlocked()              { return unlocked; }
    public List<ItemStack> getIngredients()  { return ingredients; }
}
