package eu.asangarin.endereyesgui.util;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import eu.asangarin.endereyesgui.api.IBottom;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum EnderEye implements IBottom {
	// ── EASY (7) ────────────────────────────────────────────────────────────
	BLACK    (-10, -5, EnderEyeDifficult.EASY),
	LOST     ( -6, -5, EnderEyeDifficult.EASY),
	ROGUE    ( -2, -5, EnderEyeDifficult.EASY),
	NETHER   (  2, -5, EnderEyeDifficult.EASY),
	EXOTIC   (  6, -5, EnderEyeDifficult.EASY),
	WITCH    ( 10, -5, EnderEyeDifficult.EASY),
	TRADER   (-10, -2, EnderEyeDifficult.EASY),
	// ── NORMAL / MEDIUM (8) ────────────────────────────────────────────────
	COLD     ( -6, -2, EnderEyeDifficult.NORMAL),
	CASTLE   ( -2, -2, EnderEyeDifficult.NORMAL),
	MAGICAL  (  2, -2, EnderEyeDifficult.NORMAL),
	GUARDIAN (  6, -2, EnderEyeDifficult.NORMAL),
	EVIL     ( 10, -2, EnderEyeDifficult.NORMAL),
	CURSED   (-10,  1, EnderEyeDifficult.NORMAL),
	SPECTRAL ( -6,  1, EnderEyeDifficult.NORMAL),
	FORBIDDEN( -2,  1, EnderEyeDifficult.NORMAL),
	// ── HARD (9) ────────────────────────────────────────────────────────────
	DESERT   (  2,  1, EnderEyeDifficult.HARD),
	ABYSS    (  6,  1, EnderEyeDifficult.HARD),
	SCULK    ( 10,  1, EnderEyeDifficult.HARD),
	UNDEAD   (-10,  4, EnderEyeDifficult.HARD),
	FLAME    ( -6,  4, EnderEyeDifficult.HARD),
	MECH     ( -2,  4, EnderEyeDifficult.HARD),
	VOID     (  2,  4, EnderEyeDifficult.HARD),
	PARASITE (  6,  4, EnderEyeDifficult.HARD),
	FIERY    ( 10,  4, EnderEyeDifficult.HARD);

	private static final EnderEye[] VALUES = values();
	private static final Map<ResourceLocation, EnderEye> BY_ITEM_ID = new HashMap<>();

	static {
		for (EnderEye eye : VALUES) {
			BY_ITEM_ID.put(eye.itemId, eye);
		}
	}

	private final int x, y;
	private final ResourceLocation advancement, itemId, activeIcon, inactiveIcon;
	private final String criterion;
	private final EnderEyeDifficult difficult;

	EnderEye(int x, int y, EnderEyeDifficult difficult) {
		String path = id() + "_eye";
		this.advancement   = new ResourceLocation("endrem", "main/" + path);
		this.itemId        = new ResourceLocation("endrem", path);
		this.criterion     = path;
		this.activeIcon    = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/ender_eyes/" + path + ".png");
		this.inactiveIcon  = new ResourceLocation(EnderEyesGUI.MODID, "textures/gui/ender_eyes/" + path + "_off.png");
		this.x = x;
		this.y = y;
		this.difficult = difficult;
	}

	public ResourceLocation getAdvancementLocation() { return advancement; }
	public ResourceLocation getItemId() { return itemId; }
	public String getCriterion() { return criterion; }
	public ResourceLocation getIconTexture(boolean active) { return active ? activeIcon : inactiveIcon; }
	public EnderEyeDifficult getDifficult() { return difficult; }
	public static EnderEye[] getValues() { return VALUES; }
	public static EnderEye byItemId(ResourceLocation itemId) { return BY_ITEM_ID.get(itemId); }
	public String getTranslationKey() { return "endereyes." + id() + ".name"; }
	public String getDescriptionKey(int index) { return "endereyes." + id() + ".description." + index; }
	public int getX() { return x; }
	public int getY() { return y; }
	private String id() { return name().toLowerCase(Locale.ROOT); }
}
