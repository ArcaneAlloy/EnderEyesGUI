package eu.asangarin.endereyesgui.packet;

import eu.asangarin.endereyesgui.util.SimpleRecipeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CNatureListPacket {
    private final List<SimpleRecipeData> explorerRecipes;
    private final List<SimpleRecipeData> druidRecipes;

    public S2CNatureListPacket(List<SimpleRecipeData> explorerRecipes, List<SimpleRecipeData> druidRecipes) {
        this.explorerRecipes = explorerRecipes;
        this.druidRecipes    = druidRecipes;
    }

    public List<SimpleRecipeData> getExplorerRecipes() { return explorerRecipes; }
    public List<SimpleRecipeData> getDruidRecipes()    { return druidRecipes; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(explorerRecipes.size());
        for (SimpleRecipeData d : explorerRecipes) d.encode(buf);
        buf.writeVarInt(druidRecipes.size());
        for (SimpleRecipeData d : druidRecipes) d.encode(buf);
    }

    public static S2CNatureListPacket decode(FriendlyByteBuf buf) {
        int eSize = buf.readVarInt();
        List<SimpleRecipeData> exp = new ArrayList<>(eSize);
        for (int i = 0; i < eSize; i++) exp.add(SimpleRecipeData.decode(buf));
        int dSize = buf.readVarInt();
        List<SimpleRecipeData> druid = new ArrayList<>(dSize);
        for (int i = 0; i < dSize; i++) druid.add(SimpleRecipeData.decode(buf));
        return new S2CNatureListPacket(exp, druid);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                eu.asangarin.endereyesgui.client.ClientPackets.handleNaturePacket(this, ctx)
            )
        );
        ctx.get().setPacketHandled(true);
    }
}
