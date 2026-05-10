package eu.asangarin.endereyesgui.packet;

import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.util.SimpleRecipeData;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.ExplorerRecipe;
import fr.shoqapik.btemobs.recipe.api.DruidRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet C2S con un campo "type" para pedir Explorer o Druid recetas.
 */
public class C2SRequestSimpleRecipesPacket {
    private final String type; // "explorer" o "druid"

    public C2SRequestSimpleRecipesPacket(String type) { this.type = type; }

    public void encode(FriendlyByteBuf buf) { buf.writeUtf(type); }

    public static C2SRequestSimpleRecipesPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestSimpleRecipesPacket(buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

                List<SimpleRecipeData> dataList = new ArrayList<>();

            try {
                if ("explorer".equals(type)) {
                    List<ExplorerRecipe> all = sender.getServer().getRecipeManager()
                            .getAllRecipesFor(BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get());
                    List<ExplorerRecipe> unlocked = BteMobsMod.getListRecipe(
                            BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get(), sender);
                    for (ExplorerRecipe r : all) {
                        dataList.add(new SimpleRecipeData(
                                r.getId(), r.getResultItem(),
                                r.getTier(), unlocked.contains(r),
                                toStacks(r.getIngredients())
                        ));
                    }
                } else { // druid
                    List<DruidRecipe> all = sender.getServer().getRecipeManager()
                            .getAllRecipesFor(BteMobsRecipeTypes.DRUID_RECIPE_TYPE.get());
                    List<DruidRecipe> unlocked = BteMobsMod.getListRecipe(
                            BteMobsRecipeTypes.DRUID_RECIPE_TYPE.get(), sender);
                    for (DruidRecipe r : all) {
                        dataList.add(new SimpleRecipeData(
                                r.getId(), r.getResultItem(),
                                r.getTier(), unlocked.contains(r),
                                toStacks(r.getIngredients())
                        ));
                    }
                }
            } catch (Exception e) { /* ignorar */ }

            dataList.sort(Comparator
                    .comparing((SimpleRecipeData d) -> !d.isUnlocked())
                    .thenComparing(d -> d.getResult().getHoverName().getString().toLowerCase()));

            Networking.sendSimpleRecipeList(sender, dataList, type);
        });
        ctx.get().setPacketHandled(true);
    }

    private static List<ItemStack> toStacks(net.minecraft.core.NonNullList<Ingredient> ings) {
        List<ItemStack> result = new ArrayList<>();
        for (Ingredient ing : ings) {
            ItemStack[] items = ing.getItems();
            if (items.length > 0) result.add(items[0]);
        }
        return result;
    }
}
