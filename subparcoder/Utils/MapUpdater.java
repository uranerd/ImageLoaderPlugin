package subparcoder.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;
import subparcoder.ImageLoaderMain;
import subparcoder.Runnable.MapUpdaterRunnable;

import java.util.*;

public class MapUpdater {
    /*This class is used to update maps, so they don't tear, in conjunction with MapUpdaterRunnable*/
    private static String lastError;
    private static volatile List<Player> onlinePlayers = new ArrayList<>();
    private static final HashMap<Integer, Integer> imageIndexMap = new HashMap<>();
    private static final HashMap<Integer, MapUpdaterRunnable> runnableMap = new HashMap<>();
    private static int uniqueID = 0;

    public static void init() {
        /*This method is just called so that onlinePlayers will be accurate*/
        onlinePlayers.addAll(Bukkit.getOnlinePlayers());
    }
    public static boolean scheduleMapUpdater(@NotNull final List<MapView> maps, @NotNull final Set<ItemFrame> itemFrames, final int ID, final int maxIndex, final int maxTicks) {
        /*This method is used to schedule map updaters to refresh the map each tick, returns whether it successfully scheduled an update*/
        Objects.requireNonNull(itemFrames, "Item Frame set can not be null!");
        if (Objects.requireNonNull(maps, "The list of map views can not be null!").size() < 1) {
            lastError = "The list of map views must contain at least 1 element!";
            return false;
        }
        if (ID >= uniqueID || ID < 0 || maxIndex < 0 || maxTicks < 1) {
            lastError = "You must use a valid ID, with a max index >= 0 and a difference between ticks >= 1";
            return false;
        }
        //use the unique id so MapUpdaterRunnable and MapRendererBuffer knows which images/index to get
        runnableMap.put(ID, new MapUpdaterRunnable(maxTicks, maxIndex, ID, imageIndexMap, maps, itemFrames, onlinePlayers));
        runnableMap.get(ID).runTaskTimerAsynchronously(ImageLoaderMain.getThisPlugin(), 0L, 1L);
        return true;
    }

    public static boolean cancelUpdaterRunnable(final int uniqueID) {
        /*Used to cancel an updater based on the uniqueID, returns whether it was successful*/
        final MapUpdaterRunnable updaterRunnable = runnableMap.getOrDefault(uniqueID, null);
        if (updaterRunnable == null) {
            lastError = "UpdaterRunnable was null!";
            return false;
        }
        updaterRunnable.cancel();
        return runnableMap.remove(uniqueID, updaterRunnable);
    }

    public static String getLastError() {
        /*Method used for debugging*/
        return lastError;
    }
    public static int getUniqueID() {
        /*Returns the current uniqueID*/
        return uniqueID;
    }
    public static int registerUniqueID(int id) {
        /*Used to 'register' a unique id so no other MapUpdaterRunnable can use it, returns whether it was successful*/
        if (id != uniqueID) id = uniqueID;
        imageIndexMap.put(id, 0);
        uniqueID++;
        return id;
    }
    public static int getIndexForUniqueID(final int id) {
        /*Returns the image based on the id*/
        return imageIndexMap.getOrDefault(id, -1);
    }

    public static void registerPlayer(@NotNull final Player player) {
        /*This method adds the specified player to onlinePlayers*/
        Objects.requireNonNull(player, "Player can not be null!");
        onlinePlayers.add(player);
    }

    public static void unregisterPlayer(@NotNull final Player player) {
        /*This method removes the specified player from onlinePlayers*/
        Objects.requireNonNull(player, "Player can not be null!");
        onlinePlayers.remove(player);
    }

}
