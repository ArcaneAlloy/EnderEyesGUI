package eu.asangarin.endereyesgui.util;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import eu.asangarin.endereyesgui.api.IBottom;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum DimensionBottom implements IBottom {
	OVERWORLD(-8, -8,0,false,EnderEyeDifficult.HARD),
	NETHER(-4, -8,8,false,EnderEyeDifficult.EASY),
	THE_END(0, -8,16,false,EnderEyeDifficult.EASY),
	TWILIGHT_FOREST(4, -8,0,true,EnderEyeDifficult.EASY),
	BEYOND_THE_END(8, -8,24,false,EnderEyeDifficult.NORMAL);

	private static final DimensionBottom[] VALUES = values();

	private final int x, y, eyes;
	private final ResourceLocation advancement, activeIcon, inactiveIcon;
	private final boolean needAdvanced;
	private final EnderEyeDifficult difficult;
	DimensionBottom(int x, int y, int needEyes, boolean needAdvanced, EnderEyeDifficult difficult) {

        String path = id();
		this.advancement = new ResourceLocation("endrem", "main/" + path);
		this.activeIcon = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/ender_eyes/" + path + ".png");
		this.inactiveIcon = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/ender_eyes/" + path + "_off.png");
		this.x = x;
		this.y = y;
		this.difficult = difficult;
		this.eyes = needEyes;
		this.needAdvanced = needAdvanced;
	}

	public int getEyes() {
		return eyes;
	}

	public boolean isNeedAdvanced() {
		return needAdvanced;
	}

	public ResourceLocation getIconTexture(boolean active) {
		return active ? activeIcon : inactiveIcon;
	}

	public EnderEyeDifficult getDifficult() {
		return difficult;
	}

	public static DimensionBottom[] getValues() {
		return VALUES;
	}

	public String getTranslationKey() {
		return "endereyes.dim_" + id() + ".name";
	}

	public String getDescriptionKey(int index) {
		return "endereyes.dim_" + id() + ".description." + index;
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
