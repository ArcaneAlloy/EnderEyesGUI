package eu.asangarin.endereyesgui.client;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import eu.asangarin.endereyesgui.util.EnchantmentRecipeData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnderEyesGUI.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EnchantmentTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        if (!(stack.getItem() instanceof EnchantedBookItem)) return;
        if (!EnchantmentCache.hasData()) return;

        var enchantments = EnchantedBookItem.getEnchantments(stack);
        if (enchantments.isEmpty()) return;

        int eyesEarned = EnchantmentCache.getEyesEarned();

        for (int i = 0; i < enchantments.size(); i++) {
            var tag = enchantments.getCompound(i);
            ResourceLocation enchRL = ResourceLocation.tryParse(tag.getString("id"));
            int level = tag.getInt("lvl");
            if (enchRL == null) continue;

            EnchantmentRecipeData data = EnchantmentCache.get(enchRL, level);
            if (data == null) continue;

            int needed = data.getNeedEyes();
            event.getToolTip().add(
                Component.translatable("endereyesgui.tooltip.needs_eyes", needed)
                    .withStyle(ChatFormatting.YELLOW)
            );
        }
    }
}
