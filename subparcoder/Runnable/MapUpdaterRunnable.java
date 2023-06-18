package subparcoder.Runnable;

import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MapUpdaterRunnable extends BukkitRunnable {
    /*This class is used to synchronize map images by updating the index*/
    private int index = 0, ticksSinceUpdated = -1;
    private final int maxTicks, maxIndex, ID;
    private final HashMap<Integer, Integer> uniqueIDMap;
    private final List<MapView> maps;
    private final List<Player> onlinePlayers;
    private final Set<ItemFrame> itemFrames;

    public MapUpdaterRunnable(final int maxTicks, final int maxIndex, final int ID, @NotNull final HashMap<Integer, Integer> uniqueIDMap, @NotNull final List<MapView> maps, @NotNull final Set<ItemFrame> itemFrames, @NotNull final List<Player> onlinePlayers)  {
        Objects.requireNonNull(uniqueIDMap, "UniqueIDMap can not be null!");
        Objects.requireNonNull(maps, "List of MapViews can not be null!");
        Objects.requireNonNull(itemFrames, "Set of ItemFrames can not be null!");
        Objects.requireNonNull(onlinePlayers, "Online players can not be null!");
        this.maps = maps;
        this.uniqueIDMap = uniqueIDMap;
        this.itemFrames = itemFrames;
        this.onlinePlayers = onlinePlayers;
        this.maxIndex = maxIndex;
        this.maxTicks = maxTicks;
        this.ID = ID;
    }

    @Override
    public void run() {
        /*This method updates the index, it runs each tick*/
        this.ticksSinceUpdated = (this.ticksSinceUpdated + 1) % this.maxTicks;
        if (this.ticksSinceUpdated == 0) {
            this.index = (this.index + 1) % this.maxIndex;
        }
        this.uniqueIDMap.put(this.ID, this.index);
        for (MapView view : this.maps) {
            for (Player player : this.onlinePlayers) {
                player.sendMap(view);
            }
        }
    }

    @Override
    public void cancel() {
        /*This method will cancel the runnable, remove all renderers from the maps and remove all items in the item frames*/
        super.cancel();
        for (int i = 0 ; i < this.maps.size() ; i++) {
            MapView view = this.maps.get(i);
            for (MapRenderer renderer : view.getRenderers()) {
                view.removeRenderer(renderer);
            }
        }
        for (ItemFrame frame : this.itemFrames) {
            frame.setItem(null);
            frame.setItemDropChance(1.0F);
        }
    }

}
