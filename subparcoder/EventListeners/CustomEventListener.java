package subparcoder.EventListeners;

import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.jetbrains.annotations.NotNull;
import subparcoder.ImageLoaderMain;
import subparcoder.MapRenderers.MapRendererNoBuffer;
import subparcoder.Utils.ImageManagerUtil;
import subparcoder.Utils.MapUpdater;
import subparcoder.Utils.MapUtils;
import subparcoder.Utils.MiscUtils;

import java.util.List;
import java.util.Objects;

public class CustomEventListener implements Listener {
    /*This class is used to listen for all types of events we need*/
    @EventHandler
    public void onPlayerEntityInteract(@NotNull final PlayerInteractEntityEvent event) {
        /*We use this method to check whenever the player interacts with an entity, since ItemFrame is an entity,
        * we can use this method to check if the place places an item frame in a map, and use that to do other things*/
        Objects.requireNonNull(event, "PlayerInteractEntityEvent can not be null!");
        if (!(event.getRightClicked() instanceof ItemFrame frame)) {
            return;
        }
        if (MiscUtils.getMapRendererBuffer(frame.getItem()) != null) {
            //we don't want to allow players to interact or rotate the custom maps
            event.setCancelled(true);
            return;
        }
        final Player player = event.getPlayer();
        final ItemStack heldItem = player.getInventory().getItem(event.getHand());
        if (heldItem == null || heldItem.getType() != Material.FILLED_MAP) {
            return;
        }
        final MapMeta meta = (MapMeta) heldItem.getItemMeta();
        if (meta == null) {
            event.setCancelled(true);
            return;
        }
        if (!meta.hasLore()) {
            if (meta.getMapView() != null) {
                List<MapRenderer> renderers = meta.getMapView().getRenderers();
                if (renderers.size() == 1 && renderers.get(0) instanceof MapRendererNoBuffer) {
                    //this map is a fill map, which we don't want to allow in an item frame
                    event.getPlayer().sendMessage(ImageLoaderMain.getAstroPIMessage("You can not place that map in an item frame!", true));
                    event.setCancelled(true);
                }
            }
            return;
        }
        final List<String> lore = meta.getLore();
        if (lore == null || lore.size() != 3 || !lore.get(0).equals(MapUtils.MAP_ITEM_RENDERER_STRING)) {
            return;
        }
        event.setCancelled(true);
        if (!ImageManagerUtil.hasLoadedBytes()) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("Make sure there are loaded images! (/imgload)", true));
            return;
        }
        int ticksBetween, threads;
        try {
            ticksBetween = Integer.parseInt(lore.get(1));
            threads = Integer.parseInt(lore.get(2).split(" ")[0]);
            //^ the format is <int> threads, so we get the first argument by splitting it at space and getting 0 index
        } catch (NumberFormatException e) {
            return;
        }
        //this method actually fills all the item frames if possible
        if (!MapUtils.fillAllItemFrames((ItemFrame) event.getRightClicked(), player.getWorld(), event, ticksBetween, threads)) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage(MapUtils.getLastError(), true));
        }
    }

    @EventHandler
    public void onHangingBreakEvent(@NotNull final HangingBreakEvent event) {
        /*This method is called whenever a hanging is broken (i.e. painting, item frame, etc.), and we want to check if it is an item frame*/
        Objects.requireNonNull(event, "Hanging break event can not be null!");
        MiscUtils.cleanupIfNeeded(event.getEntity(), event instanceof HangingBreakByEntityEvent ? ((HangingBreakByEntityEvent)event).getRemover() : null);
    }

    @EventHandler
    public void onEntityDamageEvent(@NotNull final EntityDamageEvent event) {
        /*This method is called whenever an item is damaged, we can use this method to check if a player removes an item from an item frame*/
        Objects.requireNonNull(event, "EntityDamageEvent can not be null!");
        MiscUtils.cleanupIfNeeded(event.getEntity(), event instanceof EntityDamageByEntityEvent ? ((EntityDamageByEntityEvent)event).getDamager() : null);
    }

    @EventHandler
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        /*This method is called whenever a player joins the server, we use this to register the player*/
        Objects.requireNonNull(event, "PlayerJoinEvent can not be null!");
        MapUpdater.registerPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        /*This method is called whenever a player quits the server, we use this to unregister the player*/
        Objects.requireNonNull(event, "PlayerQuitEvent can not be null!");
        MapUpdater.unregisterPlayer(event.getPlayer());
    }

}