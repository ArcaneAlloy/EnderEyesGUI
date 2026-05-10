package eu.asangarin.endereyesgui.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos de una receta de Blacksmith para el cliente.
 * Las recetas con mismo resultado e ingredientes pero distinto base se agrupan:
 * - ingredients: los ingredientes comunes (sin la base)
 * - baseVariants: lista de bases posibles (cada una es una variante)
 */
public class BlacksmithRecipeData {
	private final ResourceLocation recipeId;
	private final ItemStack result;
	private final String category;
	private final int tier;
	private final boolean unlocked;

	// Ingredientes comunes (sin base)
	private final List<ItemStack> ingredients;
	// Variantes de base (vacío si es BlacksmithRecipe normal)
	private final List<ItemStack> baseVariants;

	// Stats
	private final int durability;
	private final int armorPoints;
	private final float armorToughness;
	private final float attackDamage;
	private final float attackSpeed;
	private final float attackRange;
	private final int harvestLevel;
	private final float miningSpeed;
	private final float rangedDamageMin;
	private final float rangedDamageMax;

	public BlacksmithRecipeData(ResourceLocation recipeId, ItemStack result,
								String category, int tier, boolean unlocked,
								List<ItemStack> ingredients, List<ItemStack> baseVariants,
								int durability, int armorPoints, float armorToughness,
								float attackDamage, float attackSpeed, float attackRange,
								int harvestLevel, float miningSpeed, float rangedDamageMin, float rangedDamageMax) {
		this.recipeId      = recipeId;
		this.result        = result;
		this.category      = category;
		this.tier          = tier;
		this.unlocked      = unlocked;
		this.ingredients   = ingredients;
		this.baseVariants  = baseVariants;
		this.durability    = durability;
		this.armorPoints   = armorPoints;
		this.armorToughness = armorToughness;
		this.attackDamage  = attackDamage;
		this.attackSpeed   = attackSpeed;
		this.attackRange   = attackRange;
		this.harvestLevel  = harvestLevel;
		this.miningSpeed   = miningSpeed;
		this.rangedDamageMin = rangedDamageMin;
		this.rangedDamageMax = rangedDamageMax;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeResourceLocation(recipeId);
		buf.writeItem(result);
		buf.writeUtf(category);
		buf.writeVarInt(tier);
		buf.writeBoolean(unlocked);

		buf.writeVarInt(ingredients.size());
		for (ItemStack s : ingredients) buf.writeItem(s);

		buf.writeVarInt(baseVariants.size());
		for (ItemStack s : baseVariants) buf.writeItem(s);

		buf.writeVarInt(durability);
		buf.writeVarInt(armorPoints);
		buf.writeFloat(armorToughness);
		buf.writeFloat(attackDamage);
		buf.writeFloat(attackSpeed);
		buf.writeFloat(attackRange);
		buf.writeVarInt(harvestLevel);
		buf.writeFloat(miningSpeed);
		buf.writeFloat(rangedDamageMin);
		buf.writeFloat(rangedDamageMax);
	}

	public static BlacksmithRecipeData decode(FriendlyByteBuf buf) {
		ResourceLocation recipeId = buf.readResourceLocation();
		ItemStack result          = buf.readItem();
		String category           = buf.readUtf();
		int tier                  = buf.readVarInt();
		boolean unlocked          = buf.readBoolean();

		int ingCount = buf.readVarInt();
		List<ItemStack> ingredients = new ArrayList<>(ingCount);
		for (int i = 0; i < ingCount; i++) ingredients.add(buf.readItem());

		int baseCount = buf.readVarInt();
		List<ItemStack> baseVariants = new ArrayList<>(baseCount);
		for (int i = 0; i < baseCount; i++) baseVariants.add(buf.readItem());

		int durability    = buf.readVarInt();
		int armorPoints   = buf.readVarInt();
		float armorTough  = buf.readFloat();
		float atkDmg      = buf.readFloat();
		float atkSpd      = buf.readFloat();
		float atkRange    = buf.readFloat();
		int harvestLevel  = buf.readVarInt();
		float miningSpeed  = buf.readFloat();
		float rangedDamageMin = buf.readFloat();
		float rangedDamageMax = buf.readFloat();

		return new BlacksmithRecipeData(recipeId, result, category, tier, unlocked,
				ingredients, baseVariants, durability, armorPoints, armorTough,
				atkDmg, atkSpd, atkRange, harvestLevel, miningSpeed, rangedDamageMin, rangedDamageMax);
	}

	public ResourceLocation getRecipeId()      { return recipeId; }
	public ItemStack getResult()               { return result; }
	public String getCategory()                { return category; }
	public int getTier()                       { return tier; }
	public boolean isUnlocked()                { return unlocked; }
	public List<ItemStack> getIngredients()    { return ingredients; }
	public List<ItemStack> getBaseVariants()   { return baseVariants; }
	public int getDurability()                 { return durability; }
	public int getArmorPoints()                { return armorPoints; }
	public float getArmorToughness()           { return armorToughness; }
	public float getAttackDamage()             { return attackDamage; }
	public float getAttackSpeed()              { return attackSpeed; }
	public float getAttackRange()              { return attackRange; }
	public int getHarvestLevel()               { return harvestLevel; }
	public float getMiningSpeed()              { return miningSpeed; }
	public float getRangedDamageMin()          { return rangedDamageMin; }
	public float getRangedDamageMax()          { return rangedDamageMax; }
	public boolean hasBaseVariants()           { return !baseVariants.isEmpty(); }
}
