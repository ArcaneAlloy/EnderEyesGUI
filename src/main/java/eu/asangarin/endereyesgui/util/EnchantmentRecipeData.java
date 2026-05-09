package eu.asangarin.endereyesgui.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Datos de una WarlockRecipe transmitidos del servidor al cliente.
 * needEyes = coste TOTAL de ojos de la receta (no los que faltan).
 * El cliente compara needEyes con eyesEarned para saber si el jugador puede desbloquearla.
 */
public class EnchantmentRecipeData {
	private final ResourceLocation recipeId;
	private final ResourceLocation enchantmentId;
	private final int level;
	private final int needEyes;   // coste total de la receta
	private final boolean unlocked;

	public EnchantmentRecipeData(ResourceLocation recipeId, ResourceLocation enchantmentId,
								 int level, int needEyes, boolean unlocked) {
		this.recipeId      = recipeId;
		this.enchantmentId = enchantmentId;
		this.level         = level;
		this.needEyes      = needEyes;
		this.unlocked      = unlocked;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeResourceLocation(recipeId);
		buf.writeResourceLocation(enchantmentId);
		buf.writeVarInt(level);
		buf.writeVarInt(needEyes);
		buf.writeBoolean(unlocked);
	}

	public static EnchantmentRecipeData decode(FriendlyByteBuf buf) {
		ResourceLocation recipeId      = buf.readResourceLocation();
		ResourceLocation enchantmentId = buf.readResourceLocation();
		int level    = buf.readVarInt();
		int needEyes = buf.readVarInt();
		boolean unlocked = buf.readBoolean();
		return new EnchantmentRecipeData(recipeId, enchantmentId, level, needEyes, unlocked);
	}

	public ResourceLocation getRecipeId()      { return recipeId; }
	public ResourceLocation getEnchantmentId() { return enchantmentId; }
	public int getLevel()                      { return level; }
	public int getNeedEyes()                   { return needEyes; }
	public boolean isUnlocked()                { return unlocked; }
}
