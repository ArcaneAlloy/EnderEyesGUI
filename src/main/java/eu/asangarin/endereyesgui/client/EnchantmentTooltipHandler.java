package eu.asangarin.endereyesgui.client;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;


@Mod.EventBusSubscriber(modid = EnderEyesGUI.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EnchantmentTooltipHandler {

    private static final ResourceLocation ANCIENT_TOME_ID =
            new ResourceLocation("quark", "ancient_tome");

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        boolean isEnchantedBook = stack.getItem() instanceof EnchantedBookItem;
        ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        boolean isAncientTome   = ANCIENT_TOME_ID.equals(itemKey);

        if (!isEnchantedBook && !isAncientTome) return;
        if (!EnchantmentCache.hasData()) return;

        ListTag enchantments = stack.hasTag() && stack.getTag().contains("StoredEnchantments", Tag.TAG_LIST)
                ? stack.getTag().getList("StoredEnchantments", Tag.TAG_COMPOUND)
                : new ListTag();
        if (enchantments.isEmpty()) return;

        int eyesEarned = EnchantmentCache.getEyesEarned();

        for (int i = 0; i < enchantments.size(); i++) {
            var tag = enchantments.getCompound(i);
            ResourceLocation enchRL = ResourceLocation.tryParse(tag.getString("id"));
            int level = tag.getInt("lvl");
            if (enchRL == null) continue;

            // El ancient_tome consume un libro de nivel N y produce nivel N+1.
            // Las recetas Warlock están registradas con el nivel resultante,
            // así que para el tome hay que buscar level+1 en la caché.
            int lookupLevel = isAncientTome ? level + 1 : level;

            EnchantmentRecipeData data = EnchantmentCache.get(enchRL, lookupLevel);
            if (data == null) continue;

            if (data.isUnlocked()) {
                event.getToolTip().add(
                    Component.translatable("endereyesgui.tooltip.already_unlocked")
                        .withStyle(ChatFormatting.GREEN)
                );
            } else {
                int needed = data.getNeedEyes();
                event.getToolTip().add(
                    Component.translatable("endereyesgui.tooltip.needs_eyes", needed)
                        .withStyle(ChatFormatting.YELLOW)
                );
            }
        }
    }
}
