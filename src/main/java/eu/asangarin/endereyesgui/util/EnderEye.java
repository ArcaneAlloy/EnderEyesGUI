package eu.asangarin.endereyesgui.util;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import eu.asangarin.endereyesgui.api.IBottom;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum EnderEye implements IBottom {
	ABYSS(-10, -5,EnderEyeDifficult.HARD),
	BLACK(-6, -5,EnderEyeDifficult.EASY),
	COLD(-2, -5,EnderEyeDifficult.EASY),
	CORRUPTED(2, -5,EnderEyeDifficult.EASY),
	CURSED(6, -5,EnderEyeDifficult.NORMAL),
	EVIL(10, -5,EnderEyeDifficult.HARD),
	EXOTIC(-10, -2,EnderEyeDifficult.EASY),
	FLAME(-6, -2,EnderEyeDifficult.HARD),
	GUARDIAN(-2, -2,EnderEyeDifficult.NORMAL),
	LOST(2, -2,EnderEyeDifficult.EASY),
	AURORA(6, -2,EnderEyeDifficult.NORMAL),
	MAGICAL(10, -2,EnderEyeDifficult.NORMAL),
	MECH(-10, 1,EnderEyeDifficult.HARD),
	MONSTROUS(-6, 1,EnderEyeDifficult.HARD),
	CARMINITE(-2, 1,EnderEyeDifficult.NORMAL),
	NETHER(2, 1,EnderEyeDifficult.EASY),
	DESERT(6, 1,EnderEyeDifficult.HARD),
	PARASITE(10, 1,EnderEyeDifficult.HARD),
	ROGUE(-10, 4,EnderEyeDifficult.EASY),
	SCULK(-6, 4,EnderEyeDifficult.HARD),
	FIERY(-2, 4,EnderEyeDifficult.NORMAL),
	UNDEAD(2, 4,EnderEyeDifficult.HARD),
	VOID(6, 4,EnderEyeDifficult.HARD),
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
