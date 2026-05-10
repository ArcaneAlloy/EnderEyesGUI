package eu.asangarin.endereyesgui.packet;

import eu.asangarin.endereyesgui.util.SimpleRecipeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CSimpleRecipeListPacket {
    private final List<SimpleRecipeData> recipes;
    private final String type; // "explorer" o "druid"

    public S2CSimpleRecipeListPacket(List<SimpleRecipeData> recipes, String type) {
        this.recipes = recipes;
        this.type    = type;
    }

    public List<SimpleRecipeData> getRecipes() { return recipes; }
    public String getType()                     { return type; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(type);
        buf.writeVarInt(recipes.size());
        for (SimpleRecipeData d : recipes) d.encode(buf);
    }

    public static S2CSimpleRecipeListPacket decode(FriendlyByteBuf buf) {
        String type = buf.readUtf();
        int size = buf.readVarInt();
        List<SimpleRecipeData> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(SimpleRecipeData.decode(buf));
        return new S2CSimpleRecipeListPacket(list, type);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                eu.asangarin.endereyesgui.client.ClientPackets.handleSimpleRecipePacket(this, ctx)
            )
        );
        ctx.get().setPacketHandled(true);
    }
}
