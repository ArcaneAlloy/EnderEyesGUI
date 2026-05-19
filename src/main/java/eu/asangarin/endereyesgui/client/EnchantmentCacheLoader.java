package eu.asangarin.endereyesgui.client;

import eu.asangarin.endereyesgui.EnderEyesGUI;
import eu.asangarin.endereyesgui.Networking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnderEyesGUI.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class EnchantmentCacheLoader {

    private static boolean requested = false;
    // Timestamp del último refresco para no spamear al servidor
    private static long lastRefresh = 0;
    private static final long REFRESH_COOLDOWN_MS = 5000; // 5 segundos

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (requested) return;
        if (!(event.getEntity() instanceof LocalPlayer)) return;
        if (!event.getLevel().isClientSide()) return;
        requested = true;
        Minecraft.getInstance().tell(() -> Networking.requestEnchantmentsCache());
    }

    // Al ganar un avance (desbloquear receta del Warlock)
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer)) return;
        refreshIfNeeded();
    }

    // Al abrir un contenedor (cofre, inventario, etc.) — el jugador puede ver items con tooltips
    @SubscribeEvent
    public static void onContainerOpen(net.minecraftforge.event.entity.player.PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof LocalPlayer)) return;
        refreshIfNeeded();
    }

    // Al abrir el inventario del jugador (tecla E) o backpacked
    @SubscribeEvent
    public static void onGuiOpen(net.minecraftforge.client.event.ScreenEvent.Opening event) {
        if (event.getScreen() == null) return;
        String screenClass = event.getScreen().getClass().getName();
        if (screenClass.contains("Inventory") || screenClass.contains("inventory")
                || screenClass.contains("backpacked") || screenClass.contains("Backpack")
                || screenClass.contains("backpack")) {
            refreshIfNeeded();
        }
    }

    private static void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < REFRESH_COOLDOWN_MS) return;
        lastRefresh = now;
        Minecraft.getInstance().tell(() -> Networking.requestEnchantmentsCache());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        requested = false;
        lastRefresh = 0;
    }
}
