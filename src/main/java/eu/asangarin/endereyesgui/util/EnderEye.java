package eu.asangarin.endereyesgui.util;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import eu.asangarin.endereyesgui.api.IBottom;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum EnderEye implements IBottom {
	// ── EASY (alfabético) ─────────────────────────────────────────────────────
	BLACK    (-10, -5, EnderEyeDifficult.EASY),
	COLD     ( -6, -5, EnderEyeDifficult.EASY),
	CORRUPTED( -2, -5, EnderEyeDifficult.EASY),
	EXOTIC   (  2, -5, EnderEyeDifficult.EASY),
	LOST     (  6, -5, EnderEyeDifficult.EASY),
	NETHER   ( 10, -5, EnderEyeDifficult.EASY),
	ROGUE    (-10, -2, EnderEyeDifficult.EASY),
	WITCH    ( -6, -2, EnderEyeDifficult.EASY),
	// ── NORMAL (alfabético) ───────────────────────────────────────────────────
	AURORA   ( -2, -2, EnderEyeDifficult.NORMAL),
	CARMINITE(  2, -2, EnderEyeDifficult.NORMAL),
	CURSED   (  6, -2, EnderEyeDifficult.NORMAL),
	FIERY    ( 10, -2, EnderEyeDifficult.NORMAL),
	GUARDIAN (-10,  1, EnderEyeDifficult.NORMAL),
	MAGICAL  ( -6,  1, EnderEyeDifficult.NORMAL),
	// ── HARD (alfabético) ─────────────────────────────────────────────────────
	ABYSS    ( -2,  1, EnderEyeDifficult.HARD),
	DESERT   (  2,  1, EnderEyeDifficult.HARD),
	EVIL     (  6,  1, EnderEyeDifficult.HARD),
	FLAME    ( 10,  1, EnderEyeDifficult.HARD),
	MECH     (-10,  4, EnderEyeDifficult.HARD),
	MONSTROUS( -6,  4, EnderEyeDifficult.HARD),
	PARASITE ( -2,  4, EnderEyeDifficult.HARD),
	SCULK    (  2,  4, EnderEyeDifficult.HARD),
	UNDEAD   (  6,  4, EnderEyeDifficult.HARD),
	VOID     ( 10,  4, EnderEyeDifficult.HARD);

	private static final EnderEye[] VALUES = values();

	private final int x, y;
	private final ResourceLocation advancement, activeIcon, inactiveIcon;
	private final EnderEyeDifficult difficult;

	EnderEye(int x, int y, EnderEyeDifficult difficult) {
		String path = id() + "_eye";
		this.advancement   = new ResourceLocation("endrem", "main/" + path);
		this.activeIcon    = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/ender_eyes/" + path + ".png");
		this.inactiveIcon  = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/ender_eyes/" + path + "_off.png");
		this.x = x;
		this.y = y;
		this.difficult = difficult;
	}

	public ResourceLocation getAdvancementLocation() { return advancement; }
	public ResourceLocation getIconTexture(boolean active) { return active ? activeIcon : inactiveIcon; }
	public EnderEyeDifficult getDifficult() { return difficult; }
	public static EnderEye[] getValues() { return VALUES; }
	public String getTranslationKey() { return "endereyes." + id() + ".name"; }
	public String getDescriptionKey(int index) { return "endereyes." + id() + ".description." + index; }
	public int getX() { return x; }
	public int getY() { return y; }
	private String id() { return name().toLowerCase(Locale.ROOT); }
}
