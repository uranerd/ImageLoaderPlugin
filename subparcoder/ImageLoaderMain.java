package subparcoder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import subparcoder.EventListeners.CustomEventListener;
import subparcoder.MapRenderers.MapRendererNoBuffer;
import subparcoder.Utils.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Objects;


public class ImageLoaderMain extends JavaPlugin {
    /*The main class for the plugin, also works with the plugin commands*/
    private static int MAX_ADJACENT_SEARCHES = 10;
    private static Plugin THIS_PLUGIN;
    @Override
    public void onEnable() {
        /*Method called when plugin is loaded*/
        THIS_PLUGIN = this;
        Bukkit.getPluginManager().registerEvents(new CustomEventListener(), this);
        Bukkit.getLogger().info("[ImageLoader] ImageLoader Plugin Loaded.");
        MapUpdater.init();
    }

    @Override
    public void onDisable() {
        /*Method called on plugin disable*/
        Bukkit.getLogger().info("[ImageLoader] ImageLoader Plugin Unloaded.");
        for (int id = 0 ; id < MapUpdater.getUniqueID() ; id++) {
            //go through and cancel any scheduled map updaters
            MapUpdater.cancelUpdaterRunnable(id);
        }
    }

    @Override
    public final boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
        /*Method called whenever a player sends a command as registered in the plugin.yml*/
        Objects.requireNonNull(sender, "Command sender can not be null!");
        Objects.requireNonNull(cmd, "Command can not be null!");
        Objects.requireNonNull(label, "Label can not be null!");
        Objects.requireNonNull(args, "Args can not be null!");
        //matches the command using regex
        if (!label.toLowerCase().matches("(getmap|getfillmap|setsearches|imgload|getsearches)")) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ImageLoaderMain.getAstroPIMessage("Only players can use this command!", true));
            return true;
        }
        switch(label.toLowerCase()) {
            //switch statement to handle the commands
            case "imgload" -> {
                if (args.length != 3) {
                    player.sendMessage(ImageLoaderMain.getAstroPIMessage("Usage: /imgload <image folder> <image format> <replacer> (i.e /imgload images image_<i>.jpg <i>)", true));
                } else {
                    this.asyncImageLoad(args, player);
                }
            }
            case "getmap" -> {
                if (args.length != 2) {
                    player.sendMessage(ImageLoaderMain.getAstroPIMessage("Usage: /getmap <fps (max fps = 20)> <threads> (i.e /getmap 20 2))", true));
                } else {
                    this.giveMap(args, player);
                }
            }
            case "getfillmap" -> {
                if (args.length != 2) {
                    player.sendMessage(ImageLoaderMain.getAstroPIMessage("Usage: /getfillmap <fps (max fps = 20)> <threads> (i.e /getfillmap 20 2))", true));
                } else {
                    this.giveFillMap(args, player);
                }
            }
            case "setsearches" -> {
                if (args.length != 1) {
                    player.sendMessage(ImageLoaderMain.getAstroPIMessage("Usage: /setsearches <max searches> (i.e /setsearches 8))", true));
                } else {
                    this.setMaxAdjacentSearches(args, player);
                }
            }
            case "getsearches" -> this.getMaxAdjacentSearches(player);
        }
        return true;
    }
    private void asyncImageLoad(@NotNull final String[] args, @NotNull final Player player) {
        /*Loads images in an async thread to not lag the server*/
        Objects.requireNonNull(args, "Args can not be null");
        Objects.requireNonNull(player, "Player can not be null");
        player.sendMessage(ImageLoaderMain.getAstroPIMessage("Attempting to load the images to memory!", false));
        new BukkitRunnable() {
            @Override
            public void run() {
                final byte[][] bytes = ImageManagerUtil.getBytes(args[0], args[1], args[2]);
                if (bytes == null || !ImageManagerUtil.setLoadedBytes(bytes)) {
                    player.sendMessage(ImageLoaderMain.getAstroPIMessage(ImageManagerUtil.getLastError(), true));
                } else {
                    player.sendMessage(ImageLoaderMain.getAstroPIMessage("Successfully loaded the images into memory!", false));
                }
            }
        }.runTaskLaterAsynchronously(THIS_PLUGIN, 0L);
    }

    private void giveMap(@NotNull final String[] args, @NotNull final Player player) {
        /*Gives the player a map that shows all the loaded images*/
        Objects.requireNonNull(args, "Args can not be null!");
        Objects.requireNonNull(player, "Player can not be null!");
        if (!ImageManagerUtil.hasLoadedBytes()) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("Please make sure there are loaded images first! (/imgload)", true));
            return;
        }
        final int ticksBetween = MiscUtils.getTicksBetween(args[0]);
        if (ticksBetween == -1) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("Usage: /getmap <fps (max fps = 20)> <threads> (i.e /getmap 20 2))", true));
            return;
        }
        int threads;
        try {
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("Usage: /getmap <fps (max fps = 20)> <threads> (i.e /getmap 20 2))", true));
            return;
        }
        final ItemStack map = MapUtils.generateBlankMap(player.getWorld());
        if (map == null) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage(MapUtils.getLastError(), true));
            return;
        }
        AtomicArray<BufferedImage> atomicArray = new AtomicArray<>(null);
        new BukkitRunnable() {
                @Override
                public void run() {
                    //we resize the images in an async thread to not lag the main server
                    atomicArray.set(ImageManagerUtil.getResizedFromLoadedBytes(MapUtils.MAP_SIZE_PIXELS, MapUtils.MAP_SIZE_PIXELS, threads));
            }
        }.runTaskLaterAsynchronously(this, 0L);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (atomicArray.isArrayUpdated()) {
                    if (!atomicArray.hasArray()) {
                        player.sendMessage(ImageLoaderMain.getAstroPIMessage(ImageManagerUtil.getLastError(), true));
                        cancel();
                        return;
                    }
                    if (!MapUtils.addMapRendererToMap(map, new MapRendererNoBuffer(atomicArray.get(), ticksBetween))) {
                        player.sendMessage(ImageLoaderMain.getAstroPIMessage(MapUtils.getLastError(), true));
                        cancel();
                        return;
                    }
                    final ItemMeta meta = map.getItemMeta();
                    if (meta == null) {
                        player.sendMessage(ImageLoaderMain.getAstroPIMessage("ItemMeta was null!", true));
                        cancel();
                        return;
                    }
                    meta.setDisplayName(ChatColor.RED + "Display map (" + args[0] + " fps)");
                    map.setItemMeta(meta);
                    player.getInventory().addItem(map);
                    player.sendMessage(ImageLoaderMain.getAstroPIMessage("Made map!", false));
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    private void giveFillMap(@NotNull final String[] args, @NotNull final Player player) {
        /*gives the player a map that, when placed, fills all the adjacent item frames with a map to display the loaded images*/
        Objects.requireNonNull(args, "Args can not be null!");
        Objects.requireNonNull(player, "Player can not be null!");
        if (!ImageManagerUtil.hasLoadedBytes()) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("Please make sure there are loaded images first! (/imgload)", true));
            return;
        }
        final int ticksBetween = MiscUtils.getTicksBetween(args[0]);
        if (ticksBetween == -1) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("Usage: /getfillmap <fps (max fps = 20)> <threads> (i.e /getfillmap 20 2))", true));
            return;
        }
        try {
            Integer.parseInt(args[1]);
        } catch(NumberFormatException e) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("Usage: /getfillmap <fps (max fps = 20)> <threads> (i.e /getfillmap 20 2))", true));
            return;
        }
        final ItemStack map = MapUtils.generateBlankMap(player.getWorld());
        if (map == null) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage(MapUtils.getLastError(), true));
            return;
        }
        final ItemMeta meta = map.getItemMeta();
        if (meta == null) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("ItemMeta was null!", true));
            return;
        }
        //we use lore to differentiate between normal maps and custom maps
        meta.setLore(new ArrayList<>() {{
            add(MapUtils.MAP_ITEM_RENDERER_STRING);
            add(String.valueOf(ticksBetween));
            add(args[1] + " Threads");
        }});
        meta.setDisplayName(ChatColor.RED + "Fill map (" + args[0] + " fps)");
        map.setItemMeta(meta);
        player.getInventory().addItem(map);
        player.sendMessage(ImageLoaderMain.getAstroPIMessage("Made map!", false));
    }

    private void setMaxAdjacentSearches(@NotNull final String[] args, @NotNull final Player player) {
        /*This method sets the max adjacent searches to whatever the player says*/
        Objects.requireNonNull(player, "Player can not be null!");
        Objects.requireNonNull(args, "Args can not be null!");
        int maxSearches;
        try {
            maxSearches = Integer.parseInt(args[0]);
        } catch(NumberFormatException e) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("Could not parse the max searches! (It has to be an integer)", true));
            return;
        }
        if (maxSearches < 1) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("Max searches has to be bigger than 0! (i.e /setsearches 8)", true));
            return;
        }
        if (maxSearches > 10) {
            player.sendMessage(ImageLoaderMain.getAstroPIMessage("Warning: Having more than a 10x10 area of maps can significantly lag your server!", true));
        }
        MAX_ADJACENT_SEARCHES = maxSearches;
        player.sendMessage(ImageLoaderMain.getAstroPIMessage("Set MAX_ADJACENT_SEARCHES to " + maxSearches, false));
    }
    private void getMaxAdjacentSearches(@NotNull final Player player) {
        /*Sends a message with the max adjacent searches to the specified player*/
        Objects.requireNonNull(player, "Player can not be null!");
        player.sendMessage(ImageLoaderMain.getAstroPIMessage("Max adjacent searches is " + MAX_ADJACENT_SEARCHES, false));
    }
    public static String getAstroPIMessage(@NotNull final String message, final boolean error) {
        /*An easy way to add the [ImageLoader] prefix to a message*/
        return ChatColor.BLUE + "[ImageLoader]" + (error ? ChatColor.RED : ChatColor.GREEN) + Objects.requireNonNull(message, "Message can not be null!");
    }
    public static Plugin getThisPlugin() {
        /*Returns this plugin, used to schedule bukkit runnable in other classes*/
        return THIS_PLUGIN;
    }
    public static int getMaxAdjacentSearches() {
        /*Returns max adjacent searches, used to limit the amount of searches in PositionUtils*/
        return MAX_ADJACENT_SEARCHES;
    }
}