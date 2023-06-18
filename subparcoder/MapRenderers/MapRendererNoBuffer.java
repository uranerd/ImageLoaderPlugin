package subparcoder.MapRenderers;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class MapRendererNoBuffer extends MapRenderer {
    /*This class is just a singular-map version of MapRendererBuffer, if you try to use this class on multiple maps, they will tear*/
    private final Image[] images;
    private final int ticksBetweenImages;
    private int index = 0, ticksSinceLastImage = 0;
    public MapRendererNoBuffer(@NotNull final Image[] images, final int ticksBetweenImages) {
        this.images = images;
        this.ticksBetweenImages = ticksBetweenImages;
    }
    @Override
    public void render(@NotNull final MapView mapView, @NotNull final MapCanvas mapCanvas, @NotNull final Player player) {
        /*Render the image if enough ticks have passed*/
        if (ticksSinceLastImage++ == 0) {
            mapCanvas.drawImage(0, 0, this.images[index++]);
            index %= images.length;
        }
        ticksSinceLastImage %= ticksBetweenImages;
    }
}