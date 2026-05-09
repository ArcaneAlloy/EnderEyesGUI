package eu.asangarin.endereyesgui.packet;

import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import mc.duzo.ender_journey.capabilities.PortalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Packet vacío que el cliente envía al servidor para solicitar la lista
 * completa de WarlockRecipe con el estado de desbloqueo del jugador.
 *
 * Lógica de desbloqueo:
 * - Todas las WarlockRecipes del RecipeManager = lista total
 * - BteMobsMod.getListRecipe(WARLOCK_RECIPE, player) = recetas desbloqueadas del jugador
 * - Una receta está desbloqueada si su ID aparece en la lista personal del jugador
 * - eyesStillNeeded = max(recipe.needEyes - player.eyesEarned, 0)
 */
public class C2SRequestEnchantmentsPacket {

    public void encode(FriendlyByteBuf buf) { /* sin datos */ }

    public static C2SRequestEnchantmentsPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestEnchantmentsPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            // Ojos de Ender obtenidos por este jugador
            int eyesEarned = PortalPlayer.get(sender)
                    .map(mc.duzo.ender_journey.capabilities.PortalPlayer::getEyesEarn)
                    .orElse(0);

            // Recetas desbloqueadas para este jugador específico
            // BteMobsMod.getListRecipe usa RecipeCapability del jugador
            List<WarlockRecipe> unlockedForPlayer;
            try {
                unlockedForPlayer = BteMobsMod.getListRecipe(
                        BteMobsRecipeTypes.WARLOCK_RECIPE.get(), sender);
            } catch (Exception e) {
                unlockedForPlayer = List.of();
            }

            // Construir set de IDs desbloqueados para lookup O(1)
            Set<ResourceLocation> unlockedIds = new HashSet<>();
            for (WarlockRecipe r : unlockedForPlayer) {
                unlockedIds.add(r.getId());
            }

            // Todas las WarlockRecipes del mundo
            List<WarlockRecipe> allRecipes;
            try {
                allRecipes = sender.getServer().getRecipeManager()
                        .getAllRecipesFor(BteMobsRecipeTypes.WARLOCK_RECIPE.get());
            } catch (Exception e) {
                Networking.sendEnchantmentsList(sender, List.of(), eyesEarned);
                return;
            }

            List<EnchantmentRecipeData> dataList = new ArrayList<>();

            for (WarlockRecipe warlock : allRecipes) {
                ResourceLocation enchId = ForgeRegistries.ENCHANTMENTS.getKey(warlock.getEnchantment());
                if (enchId == null) continue;

                boolean unlocked = unlockedIds.contains(warlock.getId());
                int eyesStillNeeded = warlock.getNeedEyes();

                dataList.add(new EnchantmentRecipeData(
                        warlock.getId(), enchId,
                        warlock.getLevel(), eyesStillNeeded, unlocked
                ));
            }

            // Primero desbloqueadas, luego por nombre de encantamiento y nivel
            // Orden alfabético por nombre traducido del encantamiento, ignorando
            // namespace del mod y estado de desbloqueo. Mismo nivel va por nivel ascendente.
            dataList.sort(Comparator
                    .comparing((EnchantmentRecipeData d) -> {
                        var ench = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS
                                .getValue(d.getEnchantmentId());
                        return ench != null
                                ? ench.getFullname(d.getLevel()).getString().toLowerCase()
                                : d.getEnchantmentId().getPath();
                    }));

            Networking.sendEnchantmentsList(sender, dataList, eyesEarned);
        });
        ctx.get().setPacketHandled(true);
    }
}
