package eu.asangarin.endereyesgui.client;

import eu.asangarin.endereyesgui.client.screen.*;
import eu.asangarin.endereyesgui.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class ClientPackets {
    public static void handlePacket(S2CEnderEyesGUIPacket msg, Supplier<NetworkEvent.Context> ctx) {
        Minecraft.getInstance().setScreen(new EnderEyesScreen(msg.getEyeSet()));
    }
    public static void handleEnchantmentsPacket(S2CEnchantmentsListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        Minecraft.getInstance().setScreen(new EnchantmentRecipesScreen(msg.getRecipes(), msg.getEyesEarned()));
    }
    public static void handleBlacksmithPacket(S2CBlacksmithListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        Minecraft.getInstance().setScreen(new BlacksmithRecipesScreen(msg.getRecipes()));
    }
    public static void handleSimpleRecipePacket(S2CSimpleRecipeListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        String titleKey = "explorer".equals(msg.getType())
                ? "endereyesgui.explorer.title"
                : "endereyesgui.druid.title";
        Minecraft.getInstance().setScreen(new SimpleRecipesScreen(msg.getRecipes(), titleKey));
    }
}
