package eu.asangarin.endereyesgui;

import eu.asangarin.endereyesgui.packet.*;
import eu.asangarin.endereyesgui.util.BlacksmithRecipeData;
import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import eu.asangarin.endereyesgui.util.WarlockPotionRecipeData;
import eu.asangarin.endereyesgui.util.SimpleRecipeData;
import eu.asangarin.endereyesgui.util.EnderEye;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.EnumSet;
import java.util.List;

public class Networking {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EnderEyesGUI.MODID, "schannel"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void init() {
        INSTANCE.registerMessage(0, S2CEnderEyesGUIPacket.class,
                S2CEnderEyesGUIPacket::encode, S2CEnderEyesGUIPacket::decode, S2CEnderEyesGUIPacket::handle);
        INSTANCE.registerMessage(1, C2SEnderEyesButtonPacket.class,
                C2SEnderEyesButtonPacket::encode, C2SEnderEyesButtonPacket::decode, C2SEnderEyesButtonPacket::handle);
        INSTANCE.registerMessage(2, C2SRequestEnchantmentsPacket.class,
                C2SRequestEnchantmentsPacket::encode, C2SRequestEnchantmentsPacket::decode, C2SRequestEnchantmentsPacket::handle);
        INSTANCE.registerMessage(3, S2CEnchantmentsListPacket.class,
                S2CEnchantmentsListPacket::encode, S2CEnchantmentsListPacket::decode, S2CEnchantmentsListPacket::handle);
        INSTANCE.registerMessage(4, C2SRequestBlacksmithPacket.class,
                C2SRequestBlacksmithPacket::encode, C2SRequestBlacksmithPacket::decode, C2SRequestBlacksmithPacket::handle);
        INSTANCE.registerMessage(5, S2CBlacksmithListPacket.class,
                S2CBlacksmithListPacket::encode, S2CBlacksmithListPacket::decode, S2CBlacksmithListPacket::handle);
        INSTANCE.registerMessage(6, C2SRequestSimpleRecipesPacket.class,
                C2SRequestSimpleRecipesPacket::encode, C2SRequestSimpleRecipesPacket::decode, C2SRequestSimpleRecipesPacket::handle);
        INSTANCE.registerMessage(7, S2CSimpleRecipeListPacket.class,
                S2CSimpleRecipeListPacket::encode, S2CSimpleRecipeListPacket::decode, S2CSimpleRecipeListPacket::handle);
        INSTANCE.registerMessage(8, C2SRequestNaturePacket.class,
                C2SRequestNaturePacket::encode, C2SRequestNaturePacket::decode, C2SRequestNaturePacket::handle);
        INSTANCE.registerMessage(9, S2CNatureListPacket.class,
                S2CNatureListPacket::encode, S2CNatureListPacket::decode, S2CNatureListPacket::handle);
    }

    public static void openEnderGUI(ServerPlayer player, EnumSet<EnderEye> eyes) {
        INSTANCE.sendTo(new S2CEnderEyesGUIPacket(eyes), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
    public static void requestEnderGUI() { INSTANCE.sendToServer(new C2SEnderEyesButtonPacket()); }
    public static void requestEnchantmentsList() { INSTANCE.sendToServer(new C2SRequestEnchantmentsPacket(true, false)); }
    public static void requestPotionsList()         { INSTANCE.sendToServer(new C2SRequestEnchantmentsPacket(true, true)); }
    public static void requestEnchantmentsCache()  { INSTANCE.sendToServer(new C2SRequestEnchantmentsPacket(false, false)); }
    public static void sendEnchantmentsList(ServerPlayer p, List<EnchantmentRecipeData> r, List<WarlockPotionRecipeData> potions, int eyes, boolean openScreen, boolean openPotions) {
        INSTANCE.sendTo(new S2CEnchantmentsListPacket(r, potions, eyes, openScreen, openPotions), p.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
    public static void requestBlacksmithList() { INSTANCE.sendToServer(new C2SRequestBlacksmithPacket()); }
    public static void sendBlacksmithList(ServerPlayer p, List<BlacksmithRecipeData> r) {
        INSTANCE.sendTo(new S2CBlacksmithListPacket(r), p.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
    public static void requestNatureList()   { INSTANCE.sendToServer(new C2SRequestNaturePacket()); }
    public static void requestExplorerList() { INSTANCE.sendToServer(new C2SRequestSimpleRecipesPacket("explorer")); }
    public static void requestDruidList()    { INSTANCE.sendToServer(new C2SRequestSimpleRecipesPacket("druid")); }
    public static void sendSimpleRecipeList(ServerPlayer p, List<SimpleRecipeData> r, String type) {
        INSTANCE.sendTo(new S2CSimpleRecipeListPacket(r, type), p.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
    public static void sendNatureList(ServerPlayer p, List<SimpleRecipeData> explorer, List<SimpleRecipeData> druid) {
        INSTANCE.sendTo(new S2CNatureListPacket(explorer, druid), p.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
