package subparcoder.Utils;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;
import subparcoder.ImageLoaderMain;
import subparcoder.MapRenderers.MapRendererBuffer;

import java.util.List;
import java.util.Objects;

public class MiscUtils {
    /*All this class does is convert a string fps value to a tick value*/
    public static int getTicksBetween(@NotNull final String str) {
        /*This is the method, returns -1 if unsuccessful*/
        int fps;
        try {
            fps = Integer.parseInt(Objects.requireNonNull(str, "String passed into getTicksBetween can not be null!"));
        } catch (NumberFormatException e) {
            return -1;
        }
        //we use Math.min and Math.max to make sure out fps is a valid value
        return (int) (20.0 / Math.min(20, Math.max(fps, 1)));
    }

    public static void cleanupIfNeeded(@NotNull final Entity entity, final Entity cause) {
        /*We use this method to clean up/destroy the item frames if they get broken*/
        Objects.requireNonNull(entity, "Entity can not be null!");
        MapRendererBuffer mapRendererBuffer;
        if (!(entity instanceof ItemFrame frame) || (mapRendererBuffer = MiscUtils.getMapRendererBuffer(frame.getItem())) == null) {
            return;
        }
        if (!MapUpdater.cancelUpdaterRunnable(mapRendererBuffer.getUniqueID()) && cause != null) {
            cause.sendMessage(ImageLoaderMain.getAstroPIMessage(MapUpdater.getLastError(), true));
        }
    }
    public static MapRendererBuffer getMapRendererBuffer(final ItemStack map) {
        /*This method will return the map renderer buffer if the itemstack has it, otherwise it returns null*/
        if (map == null || map.getType() != Material.FILLED_MAP) {
            return null;
        }
        if (map.getItemMeta() == null) {
            return null;
        }
        MapMeta meta = (MapMeta) map.getItemMeta();
        if (meta.getMapView() == null) {
            return null;
        }
        MapView view = meta.getMapView();
        List<MapRenderer> renderers = view.getRenderers();
        if (renderers.size() != 1) {
            return null;
        }
        if (!(renderers.get(0) instanceof MapRendererBuffer mrb)) {
            return null;
        }
        return mrb;
    }
}
