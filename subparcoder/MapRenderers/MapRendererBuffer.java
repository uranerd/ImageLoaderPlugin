package subparcoder.MapRenderers;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;
import subparcoder.Utils.MapUpdater;
import subparcoder.Utils.MapUtils;

import java.awt.image.BufferedImage;
import java.util.Objects;

public class MapRendererBuffer extends MapRenderer {
    /*This class is basically a non-tearing version of MapRendererNoBuffer, it does this by synchronizing the images between all of maps*/
    private final BufferedImage[] images;
    private final int x, y;
    private int uniqueID;
    public MapRendererBuffer(@NotNull final BufferedImage[] images, final int uniqueID, final int x, final int y) {
        Objects.requireNonNull(images, "Buffered Image array can not be null!");
        this.images = images;
        this.uniqueID = uniqueID;
        this.x = x;
        this.y = y;
    }

    @Override
    public void render(@NotNull final MapView mapView, @NotNull final MapCanvas mapCanvas, final @NotNull Player player) {
        /*Renders the sub-image based on the index, this.x and this.y (sub-image because it is a part of a larger image)*/
        mapCanvas.drawImage(0, 0, this.images[MapUpdater.getIndexForUniqueID(this.uniqueID)].getSubimage(x, y, MapUtils.MAP_SIZE_PIXELS, MapUtils.MAP_SIZE_PIXELS));
    }
    public int getUniqueID() {
        /*Returns the unique id*/
        return this.uniqueID;
    }

    public void setUniqueID(int uniqueID) {
        this.uniqueID = uniqueID;
    }
}