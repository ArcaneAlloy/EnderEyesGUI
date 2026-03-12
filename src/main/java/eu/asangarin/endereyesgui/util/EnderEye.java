package eu.asangarin.endereyesgui.util;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum EnderEye {
	ABYSS(-10, -5,EnderEyeDifficult.EASY),
	BLACK(-6, -5,EnderEyeDifficult.EASY),
	COLD(-2, -5,EnderEyeDifficult.EASY),
	CORRUPTED(2, -5,EnderEyeDifficult.EASY),
	CURSED(6, -5,EnderEyeDifficult.EASY),
	EVIL(10, -5,EnderEyeDifficult.EASY),
	EXOTIC(-10, -2,EnderEyeDifficult.EASY),
	FLAME(-6, -2,EnderEyeDifficult.EASY),
	GUARDIAN(-2, -2,EnderEyeDifficult.EASY),
	LOST(2, -2,EnderEyeDifficult.EASY),
	AURORA(6, -2,EnderEyeDifficult.EASY),
	MAGICAL(10, -2,EnderEyeDifficult.EASY),
	MECH(-10, 1,EnderEyeDifficult.EASY),
	MONSTROUS(-6, 1,EnderEyeDifficult.EASY),
	CARMINITE(-2, 1,EnderEyeDifficult.EASY),
	NETHER(2, 1,EnderEyeDifficult.EASY),
	DESERT(6, 1,EnderEyeDifficult.EASY),
	PARASITE(10, 1,EnderEyeDifficult.EASY),
	ROGUE(-10, 4,EnderEyeDifficult.EASY),
	SCULK(-6, 4,EnderEyeDifficult.EASY),
	FIERY(-2, 4,EnderEyeDifficult.EASY),
	UNDEAD(2, 4,EnderEyeDifficult.EASY),
	VOID(6, 4,EnderEyeDifficult.EASY),
	WITCH(10, 4,EnderEyeDifficult.EASY);

	private static final EnderEye[] VALUES = values();

	private final int x, y;
	private final ResourceLocation advancement, activeIcon, inactiveIcon;

	private final EnderEyeDifficult difficult;
	EnderEye(int x, int y,EnderEyeDifficult difficult) {
		String path = id() + "_eye";
		this.advancement = new ResourceLocation("endrem", "main/" + path);
		this.activeIcon = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/ender_eyes/" + path + ".png");
		this.inactiveIcon = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/ender_eyes/" + path + "_off.png");
		this.x = x;
		this.y = y;
		this.difficult = difficult;
	}

	public ResourceLocation getAdvancementLocation() {
		return advancement;
	}

	public ResourceLocation getIconTexture(boolean active) {
		return active ? activeIcon : inactiveIcon;
	}

	public EnderEyeDifficult getDifficult() {
		return difficult;
	}

	public static EnderEye[] getValues() {
		return VALUES;
	}

	public String getTranslationKey() {
		return "endereyes." + id() + ".name";
	}

	public String getDescriptionKey(int index) {
		return "endereyes." + id() + ".description." + index;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	private String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
