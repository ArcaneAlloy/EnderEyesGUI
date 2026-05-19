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
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class C2SRequestNaturePacket {

    public void encode(FriendlyByteBuf buf) {}
    public static C2SRequestNaturePacket decode(FriendlyByteBuf buf) { return new C2SRequestNaturePacket(); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            List<SimpleRecipeData> explorerList = new ArrayList<>();
            List<SimpleRecipeData> druidList    = new ArrayList<>();

            try {
                List<ExplorerRecipe> allExp = sender.getServer().getRecipeManager()
                        .getAllRecipesFor(BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get());
                List<ExplorerRecipe> unlockedExp = BteMobsMod.getListRecipe(
                        BteMobsRecipeTypes.EXPLORER_RECIPE_TYPE.get(), sender);
                for (ExplorerRecipe r : allExp) {
                    explorerList.add(new SimpleRecipeData(
                            r.getId(), r.getResultItem(),
                            r.getTier(), unlockedExp.contains(r),
                            toStacks(r.getIngredients())
                    ));
                }
            } catch (Exception ignored) {}

            try {
                List<DruidRecipe> allDruid = sender.getServer().getRecipeManager()
                        .getAllRecipesFor(BteMobsRecipeTypes.DRUID_RECIPE_TYPE.get());
                List<DruidRecipe> unlockedDruid = BteMobsMod.getListRecipe(
                        BteMobsRecipeTypes.DRUID_RECIPE_TYPE.get(), sender);
                for (DruidRecipe r : allDruid) {
                    druidList.add(new SimpleRecipeData(
                            r.getId(), r.getResultItem(),
                            r.getTier(), unlockedDruid.contains(r),
                            toStacks(r.getIngredients())
                    ));
                }
            } catch (Exception ignored) {}

            Comparator<SimpleRecipeData> order = Comparator
                    .comparing((SimpleRecipeData d) -> !d.isUnlocked())
                    .thenComparing(d -> d.getResult().getHoverName().getString().toLowerCase());
            explorerList.sort(order);
            druidList.sort(order);

            Networking.sendNatureList(sender, explorerList, druidList);
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
