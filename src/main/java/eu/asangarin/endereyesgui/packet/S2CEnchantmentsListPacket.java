package eu.asangarin.endereyesgui.packet;

import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import eu.asangarin.endereyesgui.util.WarlockPotionRecipeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CEnchantmentsListPacket {
    private final List<EnchantmentRecipeData> recipes;
    private final List<WarlockPotionRecipeData> potions;
    private final int eyesEarned;
    private final boolean openScreen;
    private final boolean openPotions;

    public S2CEnchantmentsListPacket(List<EnchantmentRecipeData> recipes,
                                      List<WarlockPotionRecipeData> potions,
                                      int eyesEarned, boolean openScreen, boolean openPotions) {
        this.recipes    = recipes;
        this.potions    = potions;
        this.eyesEarned = eyesEarned;
        this.openScreen  = openScreen;
        this.openPotions = openPotions;
    }

    public List<EnchantmentRecipeData>     getRecipes()        { return recipes; }
    public List<WarlockPotionRecipeData>   getPotions()        { return potions; }
    public int                             getEyesEarned()     { return eyesEarned; }
    public boolean                         shouldOpenScreen()  { return openScreen; }
    public boolean                         shouldOpenPotions() { return openPotions; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(eyesEarned);
        buf.writeBoolean(openScreen);
        buf.writeBoolean(openPotions);
        buf.writeVarInt(recipes.size());
        for (EnchantmentRecipeData d : recipes) d.encode(buf);
        buf.writeVarInt(potions.size());
        for (WarlockPotionRecipeData d : potions) d.encode(buf);
    }

    public static S2CEnchantmentsListPacket decode(FriendlyByteBuf buf) {
        int eyesEarned     = buf.readVarInt();
        boolean openScreen  = buf.readBoolean();
        boolean openPotions = buf.readBoolean();
        int rSize = buf.readVarInt();
        List<EnchantmentRecipeData> recipes = new ArrayList<>(rSize);
        for (int i = 0; i < rSize; i++) recipes.add(EnchantmentRecipeData.decode(buf));
        int pSize = buf.readVarInt();
        List<WarlockPotionRecipeData> potions = new ArrayList<>(pSize);
        for (int i = 0; i < pSize; i++) potions.add(WarlockPotionRecipeData.decode(buf));
        return new S2CEnchantmentsListPacket(recipes, potions, eyesEarned, openScreen, openPotions);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                eu.asangarin.endereyesgui.client.ClientPackets.handleEnchantmentsPacket(this, ctx)
            )
        );
        ctx.get().setPacketHandled(true);
    }
}
