package eu.asangarin.endereyesgui.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos de una WarlockRecipe transmitidos del servidor al cliente.
 */
public class EnchantmentRecipeData {
	private final ResourceLocation recipeId;
	private final ResourceLocation enchantmentId;
	private final int level;
	private final int needEyes;
	private final boolean unlocked;
	private final int experience;
	private final List<ItemStack> ingredients;

	public EnchantmentRecipeData(ResourceLocation recipeId, ResourceLocation enchantmentId,
								 int level, int needEyes, boolean unlocked,
								 int experience, List<ItemStack> ingredients) {
		this.recipeId    = recipeId;
		this.enchantmentId = enchantmentId;
		this.level       = level;
		this.needEyes    = needEyes;
		this.unlocked    = unlocked;
		this.experience  = experience;
		this.ingredients = ingredients;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeResourceLocation(recipeId);
		buf.writeResourceLocation(enchantmentId);
		buf.writeVarInt(level);
		buf.writeVarInt(needEyes);
		buf.writeBoolean(unlocked);
		buf.writeVarInt(experience);
		buf.writeVarInt(ingredients.size());
		for (ItemStack s : ingredients) buf.writeItem(s);
	}

	public static EnchantmentRecipeData decode(FriendlyByteBuf buf) {
		ResourceLocation recipeId      = buf.readResourceLocation();
		ResourceLocation enchantmentId = buf.readResourceLocation();
		int level    = buf.readVarInt();
		int needEyes = buf.readVarInt();
		boolean unlocked = buf.readBoolean();
		int experience = buf.readVarInt();
		int ingCount = buf.readVarInt();
		List<ItemStack> ingredients = new ArrayList<>(ingCount);
		for (int i = 0; i < ingCount; i++) ingredients.add(buf.readItem());
		return new EnchantmentRecipeData(recipeId, enchantmentId, level, needEyes, unlocked,
				experience, ingredients);
	}

	public ResourceLocation getRecipeId()      { return recipeId; }
	public ResourceLocation getEnchantmentId() { return enchantmentId; }
	public int getLevel()                      { return level; }
	public int getNeedEyes()                   { return needEyes; }
	public boolean isUnlocked()                { return unlocked; }
	public int getExperience()                 { return experience; }
	public List<ItemStack> getIngredients()    { return ingredients; }
}
