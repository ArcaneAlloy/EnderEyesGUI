package eu.asangarin.endereyesgui.util;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum EnderEye {
	ABYSS(-10, -5), BLACK(-6, -5), COLD(-2, -5), CORRUPTED(2, -5), CURSED(6, -5), EVIL(10, -5),
	EXOTIC(-10, -2), FLAME(-6, -2), GUARDIAN(-2, -2), LOST(2, -2), LUCKY(6, -2), MAGICAL(10, -2),
	MECH(-10, 1), MONSTROUS(-6, 1), MOSSY(-2, 1), NETHER(2, 1), DESERT(6, 1), PARASITE(10, 1),
	ROGUE(-10, 4), SCULK(-6, 4), SOUL(-2, 4), UNDEAD(2, 4), VOID(6, 4), WITCH(10, 4);

	private static final EnderEye[] VALUES = values();

	private final int x, y;
	private final ResourceLocation advancement, activeIcon, inactiveIcon;

	EnderEye(int x, int y) {
		String path = id() + "_eye";
		this.advancement = new ResourceLocation("endrem", "main/" + path);
		this.activeIcon = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/ender_eyes/" + path + ".png");
		this.inactiveIcon = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/ender_eyes/" + path + "_off.png");
		this.x = x;
		this.y = y;
	}

	public ResourceLocation getAdvancementLocation() {
		return advancement;
	}

	public ResourceLocation getIconTexture(boolean active) {
		return active ? activeIcon : inactiveIcon;
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
