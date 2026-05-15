package eu.asangarin.endereyesgui.client;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import eu.asangarin.endereyesgui.Networking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnderEyesGUI.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EnchantmentCacheLoader {

    private static boolean requested = false;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (requested) return;
        if (!(event.getEntity() instanceof LocalPlayer)) return;
        if (!event.getLevel().isClientSide()) return;

        requested = true;
        // Delay de 1 tick para asegurar que la conexión está lista
        Minecraft.getInstance().tell(() -> Networking.requestEnchantmentsCache());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Resetear al desconectar para que se vuelva a pedir al reconectar
        requested = false;
    }
}
