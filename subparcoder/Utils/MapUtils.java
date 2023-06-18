package subparcoder.Utils;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import subparcoder.ImageLoaderMain;
import subparcoder.MapRenderers.MapRendererBuffer;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MapUtils {
    /*This class is used to deal with a lot of map-related things, like making new maps, and filling item frames*/
    private static String lastError;
    public static final String MAP_ITEM_RENDERER_STRING = "[Map-Item-Renderer]";
    public static final int MAP_SIZE_PIXELS = 128;
    //^ the pixel area of each map in minecraft is 128x128 pixels
    public static ItemStack generateBlankMap(@NotNull final World world) {
        /*This method is used to return a blank filled map, with no renderers, returns null if unsuccessful*/
        Objects.requireNonNull(world, "World can not be null!");
        final ItemStack item = new ItemStack(Material.FILLED_MAP);
        final MapMeta map = (MapMeta) item.getItemMeta();
        if (map == null) {
            lastError = "MapMeta was null!";
            return null;
        }
        final MapView view = Bukkit.createMap(world);
        for (MapRenderer renderer : view.getRenderers()) {
            view.removeRenderer(renderer);
        }
        map.setMapView(view);
        item.setItemMeta(map);
        return item;
    }

    public static boolean addMapRendererToMap(@NotNull final ItemStack item, @NotNull final MapRenderer renderer) {
        /*This method adds the specified renderer to the specified item provided item is a valid-filled map and renderer is not null*/
        Objects.requireNonNull(renderer, "Map renderer can not be null!");
        if (Objects.requireNonNull(item, "ItemStack can not be null!").getType() != Material.FILLED_MAP) {
            lastError = "ItemStack was not a Material.FILLED_MAP";
            return false;
        }
        final MapMeta meta = (MapMeta) item.getItemMeta();
        if (meta == null || meta.getMapView() == null) {
            lastError = "Map meta was null or had no map view!";
            return false;
        }
        meta.getMapView().addRenderer(renderer);
        return true;
    }

    public static boolean fillAllItemFrames(@NotNull final ItemFrame frame, @NotNull final World world, @NotNull final PlayerInteractEntityEvent event, final int ticksBetween, final int threads) {
        /*This method will fill all the item frames with a map that displays the current loaded images*/
        if (ticksBetween < 1) {
            lastError = "Ticks between has to be more than 0!";
            return false;
        }
        if (threads <= 0) {
            lastError = "Threads has to be at least 1!";
            return false;
        }
        Objects.requireNonNull(world, "World can not be null!");
        Objects.requireNonNull(event, "PlayerInteractEntityEvent can not be null!");
        BlockFace face = Objects.requireNonNull(frame, "ItemFrame can not be null!").getAttachedFace();
        final AtomicInteger multZ = new AtomicInteger(1), multX = new AtomicInteger(1);
        //we switch the faces so the blocks are always in the bottom left and top right respectively
        switch (face) {
            case WEST -> multZ.set(-1);
            case SOUTH -> multX.set(-1);
            case DOWN, UP  -> {
                lastError = "Unhandled attached face for the item frame: " + face;
                return false;
            }
        }
        final Location bottomLeft = PositionUtils.getItemFrameLoc(frame, world, true);
        final Location topRight = PositionUtils.getItemFrameLoc(frame, world, false);
        if (bottomLeft == null || topRight == null) {
            lastError = "Bottom left or top right block are null! Perhaps they exceeded the max searches! (Max searches: " + ImageLoaderMain.getMaxAdjacentSearches() + ")";
            return false;
        }
        final int xDif = Math.abs(bottomLeft.getBlockX() - topRight.getBlockX());
        final int yDif = Math.abs(bottomLeft.getBlockY() - topRight.getBlockY());
        final int zDif = Math.abs(bottomLeft.getBlockZ() - topRight.getBlockZ());
        final int resizeWidth = ((xDif > 0.0D ? xDif : zDif) + 1) * MAP_SIZE_PIXELS; //we need this because it could be using xDif or zDif based on the face
        final int resizeHeight = (yDif + 1) * MAP_SIZE_PIXELS;
        final AtomicArray<BufferedImage> atomicArray = new AtomicArray<>(null);
        new BukkitRunnable() {
            @Override
            public void run() {
                //we resize our images in an async thread to not lag the server
                atomicArray.set(ImageManagerUtil.getResizedFromLoadedBytes(resizeWidth, resizeHeight, threads));
            }
        }.runTaskLaterAsynchronously(ImageLoaderMain.getThisPlugin(), 0L);
        new BukkitRunnable() {
            //this runnable/thread is used to check if we have resized the images, if so, try filling all the item frames
            @Override
            public void run() {
                if (atomicArray.isArrayUpdated()) {
                    if (!atomicArray.hasArray()){
                        lastError = ImageManagerUtil.getLastError();
                        event.getPlayer().sendMessage(ImageLoaderMain.getAstroPIMessage(lastError, true));
                    } else {
                        boolean failed = !MapUtils.fillFramesWithMap(atomicArray.get(), topRight, bottomLeft, face, event, xDif, zDif, multX.get(), multZ.get(), ticksBetween, resizeHeight);
                        if (failed) {
                            event.getPlayer().sendMessage(ImageLoaderMain.getAstroPIMessage(lastError, true));
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(ImageLoaderMain.getThisPlugin(), 0L, 5L);
        //^ it runs and checks each 5 ticks (0.25 seconds)
        return true;
    }
    private static boolean fillFramesWithMap(@NotNull final BufferedImage[] resizedImages, @NotNull final Location topRight, @NotNull final Location bottomLeft, @NotNull final BlockFace face, @NotNull final PlayerInteractEntityEvent event,  final int @NotNull ... vars) {
        /*This method actually fills all the item frames with the maps, returns whether it was successful*/
        Objects.requireNonNull(resizedImages, "Resized images can not be null!");
        Objects.requireNonNull(topRight, "Top right location can not be null!");
        Objects.requireNonNull(bottomLeft, "Bottom left location can not be null!");
        Objects.requireNonNull(vars, "Int arguments can not be null can not be null!");
        Objects.requireNonNull(event, "PlayerInteractEntityEvent can not be null!");
        Objects.requireNonNull(face, "BlockFace can not be null!");
        if (vars.length != 6) {
            lastError = "Argument length must be equal to 6!";
            return false;
        }
        final int xDif = vars[0], zDif = vars[1], multX = vars[2], multZ = vars[3], ticksBetween = vars[4], resizeHeight = vars[5];
        //^ we parse the int values from our int..., not pretty, but I think it is the best way to doit
        final World world = event.getPlayer().getWorld();
        ItemStack newMap;
        ItemFrame frameAtPos;
        final int id = MapUpdater.getUniqueID();
        int imgX = (xDif == 0.0D && zDif == 0.0D ? 1 : 0), imgY = 0;
        //^imgX is like that to fix an edge case where there is only one map
        final List<MapView> maps = new ArrayList<>();
        final HashMap<ItemFrame, ItemStack> itemFrameMap = new HashMap<>();
        /*Loop through 3 nested loops, this is so we loop from bottom left to top right
        * on each y increment, get the item frame at that position, make a new map and add a new renderer to the map
        * if all goes well we add the itemframe and map to our itemFrameMap hashmap*/
        for (int x = bottomLeft.getBlockX() ; x != topRight.getBlockX() + multX ; x += multX) {
            if (xDif != 0.0D) imgX++;
            for (int z = bottomLeft.getBlockZ() ; z != topRight.getBlockZ() + multZ ; z += multZ) {
                if (zDif != 0.0D) imgX++;
                for (int y = bottomLeft.getBlockY() ; y < topRight.getBlockY() + 1 ; y++) {
                    imgY++;
                    frameAtPos = PositionUtils.getClosestItemFrameAtLocation(new Location(world, x + 0.5, y + 0.5, z + 0.5), world, face);
                    if (frameAtPos == null) {
                        lastError = "An item frame is missing!";
                        return false;
                    }
                    if (frameAtPos.getItem() != null && frameAtPos.getItem().getType() != Material.AIR) {
                        lastError = "Can not use item frames that have an item in them!";
                        return false;
                    }
                    newMap = MapUtils.generateBlankMap(world);
                    if (newMap == null) {
                        return false;
                    }
                    MapRenderer renderer = new MapRendererBuffer(resizedImages, id,(imgX - 1) * MapUtils.MAP_SIZE_PIXELS, resizeHeight - imgY * MapUtils.MAP_SIZE_PIXELS);
                    if (!MapUtils.addMapRendererToMap(newMap, renderer)) {
                        lastError = "Unexpected error: Could not add renderer to map!";
                        return false;
                    }
                    if (newMap.getItemMeta() == null) {
                        lastError = "Map meta is null!";
                        return false;
                    }
                    maps.add(((MapMeta)newMap.getItemMeta()).getMapView());
                    itemFrameMap.put(frameAtPos, newMap);
                }
                imgY = 0;
            }
        }
        int registeredID = MapUpdater.registerUniqueID(id);
        if (id != registeredID) {
            for (ItemStack map : itemFrameMap.values()) {
                MapRendererBuffer mrb = MiscUtils.getMapRendererBuffer(map);
                if (mrb == null) {
                    lastError = "MapRendererBuffer was null!";
                    return false;
                }
                mrb.setUniqueID(registeredID);
            }
        }
        if (!MapUpdater.scheduleMapUpdater(maps, itemFrameMap.keySet(), registeredID, resizedImages.length, ticksBetween)) {
            lastError = MapUpdater.getLastError();
            return false;
        }
        for (ItemFrame frame : itemFrameMap.keySet()) {
            frame.setRotation(Rotation.NONE);
            frame.setItemDropChance(0.0F);
            frame.setItem(itemFrameMap.get(frame), false);
        }
        //all went well, we could register our id and schedule our updater, so return true
        return true;
    }
    public static String getLastError() {
        /*debugging*/
        return lastError;
    }
}