package eu.asangarin.endereyesgui.api;

import eu.asangarin.endereyesgui.util.EnderEyeDifficult;
import net.minecraft.resources.ResourceLocation;

public interface IBottom {
    ResourceLocation getIconTexture(boolean active);
    EnderEyeDifficult getDifficult() ;
    String getDescriptionKey(int index);
}