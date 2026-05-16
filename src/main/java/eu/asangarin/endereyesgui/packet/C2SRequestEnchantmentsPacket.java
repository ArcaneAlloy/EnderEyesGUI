package eu.asangarin.endereyesgui.packet;

import eu.asangarin.endereyesgui.Networking;
import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import eu.asangarin.endereyesgui.util.WarlockPotionRecipeData;
import fr.shoqapik.btemobs.BteMobsMod;
import fr.shoqapik.btemobs.recipe.WarlockRecipe;
import fr.shoqapik.btemobs.recipe.WarlockPotionRecipe;
import fr.shoqapik.btemobs.registry.BteMobsRecipeTypes;
import mc.duzo.ender_journey.capabilities.PortalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
	private final boolean openScreen;

	public C2SRequestEnchantmentsPacket(boolean openScreen) { this.openScreen = openScreen; }
	public C2SRequestEnchantmentsPacket() { this(true); }
	public boolean shouldOpenScreen() { return openScreen; }

    public void encode(FriendlyByteBuf buf) { buf.writeBoolean(openScreen); }

    public static C2SRequestEnchantmentsPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestEnchantmentsPacket(buf.readBoolean());
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
                Networking.sendEnchantmentsList(sender, List.of(), List.of(), eyesEarned, openScreen);
                return;
            }

            List<EnchantmentRecipeData> dataList = new ArrayList<>();

            for (WarlockRecipe warlock : allRecipes) {
                ResourceLocation enchId = ForgeRegistries.ENCHANTMENTS.getKey(warlock.getEnchantment());
                if (enchId == null) continue;

                boolean unlocked = unlockedIds.contains(warlock.getId());
                int eyesStillNeeded = warlock.getNeedEyes();

                // Todos los ingredientes de la receta
                List<ItemStack> ings = new java.util.ArrayList<>();
                for (net.minecraft.world.item.crafting.Ingredient ing : warlock.getIngredients()) {
                    ItemStack[] items = ing.getItems();
                    if (items.length > 0) ings.add(items[0]);
                }

                dataList.add(new EnchantmentRecipeData(
                        warlock.getId(), enchId,
                        warlock.getLevel(), eyesStillNeeded, unlocked,
                        warlock.getExperience(), ings
                ));
            }

            // Primero desbloqueadas, luego por nombre de encantamiento y nivel
            // Orden alfabético por nombre traducido del encantamiento, ignorando
            // namespace del mod y estado de desbloqueo. Mismo nivel va por nivel ascendente.
            dataList.sort(Comparator
                    .comparing((EnchantmentRecipeData d) -> {
                        // Ordenar por nombre del encantamiento SIN el nivel
                        var ench = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS
                                .getValue(d.getEnchantmentId());
                        return ench != null
                                ? ench.getFullname(1).getString().toLowerCase()
                                : d.getEnchantmentId().getPath();
                    })
                    .thenComparingInt(EnchantmentRecipeData::getLevel));

            // ── Pociones del Warlock ─────────────────────────────────────────────
            List<WarlockPotionRecipeData> potionList = new java.util.ArrayList<>();
            final net.minecraft.stats.ServerRecipeBook finalRecipeBook = sender.getRecipeBook();
            try {
                List<WarlockPotionRecipe> potionRecipes = sender.getServer().getRecipeManager()
                        .getAllRecipesFor(fr.shoqapik.btemobs.registry.BteMobsRecipeTypes.WARLOCK_POTION_RECIPE.get());
                // Filtrar solo recetas con efecto base
                // Excluir: strong_X, long_X, y sufijos _ii, _iii, etc.
                java.util.regex.Pattern variantPattern = java.util.regex.Pattern.compile("_(ii|iii|iv|v|vi|vii|viii|ix|x|2|3|4|5)$");
                // Solo los effectIds que están en los JSONs del datapack (26 recetas base)
                java.util.Set<String> allowedEffects = new java.util.HashSet<>(java.util.Arrays.asList(
                    "alexsmobs:bug_pheromones", "alexsmobs:clinging", "alexsmobs:knockback_resistance",
                    "alexsmobs:lava_vision", "alexsmobs:poison_resistance", "alexsmobs:soulsteal",
                    "minecraft:fire_resistance", "minecraft:harming", "minecraft:healing",
                    "minecraft:invisibility", "minecraft:leaping", "minecraft:night_vision",
                    "minecraft:poison", "minecraft:regeneration", "minecraft:slow_falling",
                    "minecraft:slowness", "minecraft:strength", "minecraft:swiftness",
                    "minecraft:turtle_master", "minecraft:water_breathing", "minecraft:weakness",
                    "quark:resilience", "upgrade_aquatic:insomnia", "upgrade_aquatic:repellence",
                    "upgrade_aquatic:restfulness", "upgrade_aquatic:vibing"
                ));
                for (WarlockPotionRecipe p : potionRecipes) {
                    if (!allowedEffects.contains(p.effect)) continue; // solo recetas base
                    boolean pUnlocked = finalRecipeBook.contains(p.getId());
                    // Construir la poción base para mostrar como icono
                    net.minecraft.resources.ResourceLocation effectId =
                            net.minecraft.resources.ResourceLocation.tryParse(p.effect);
                    net.minecraft.world.effect.MobEffect effect = effectId != null
                            ? net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(effectId)
                            : null;

                    ItemStack basePotion = ItemStack.EMPTY;
                    if (effect != null) {
                        // Buscar primero por el mismo ResourceLocation del efecto
                        net.minecraft.world.item.alchemy.Potion directPotion =
                                net.minecraftforge.registries.ForgeRegistries.POTIONS.getValue(effectId);
                        if (directPotion == null) {
                            directPotion = net.minecraftforge.registries.ForgeRegistries.POTIONS.getKeys().stream()
                                    .filter(rl -> {
                                        String pid = rl.getPath();
                                        return !pid.startsWith("strong_") && !pid.startsWith("long_");
                                    })
                                    .map(rl -> net.minecraftforge.registries.ForgeRegistries.POTIONS.getValue(rl))
                                    .filter(pot -> pot != null && !pot.getEffects().isEmpty()
                                            && pot.getEffects().get(0).getEffect() == effect
                                            && pot.getEffects().get(0).getAmplifier() == 0)
                                    .min(java.util.Comparator.comparingInt(
                                            pot -> pot.getEffects().get(0).getDuration()))
                                    .orElse(null);
                        }
                        if (directPotion != null) {
                            basePotion = net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                                    new ItemStack(net.minecraft.world.item.Items.POTION), directPotion);
                        } else if (effect != null) {
                            // Fallback: construir poción con MobEffectInstance directo (para efectos de mods)
                            basePotion = net.minecraft.world.item.alchemy.PotionUtils.setCustomEffects(
                                    new ItemStack(net.minecraft.world.item.Items.POTION),
                                    java.util.List.of(new net.minecraft.world.effect.MobEffectInstance(
                                            effect, 3600, 0)));
                        }
                    }

                    potionList.add(new WarlockPotionRecipeData(
                            p.getId(), p.effect, p.getTier(), pUnlocked,
                            p.getIngredientPrimary(), basePotion
                    ));
                }
            } catch (Exception e) { /* ignorar */ }
            // Deduplicar por effectId (puede haber duplicados si el registro tiene variantes)
            java.util.Map<String, WarlockPotionRecipeData> deduped = new java.util.LinkedHashMap<>();
            for (WarlockPotionRecipeData d : potionList) {
                deduped.putIfAbsent(d.getEffectId(), d);
            }
            potionList = new java.util.ArrayList<>(deduped.values());
            potionList.sort(java.util.Comparator.comparing(d -> d.getEffectId()));

            Networking.sendEnchantmentsList(sender, dataList, potionList, eyesEarned, openScreen);
        });
        ctx.get().setPacketHandled(true);
    }
}
