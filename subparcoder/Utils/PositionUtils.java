package subparcoder.Utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.jetbrains.annotations.NotNull;
import subparcoder.ImageLoaderMain;

import java.util.Collection;
import java.util.Objects;

public class PositionUtils {
    /*This class is the main class for position related things, like getting the bottom left and right itemframe,
    * or getting an itemframe at a location*/
    public static Location getItemFrameLoc(@NotNull final ItemFrame frame, @NotNull final World world, final boolean bottomLeft) {
        /*This method returns null if unsuccessful, if it works it returns a location that is at the bottom left or top right
        * of a square of itemframes based on the boolean bottomLeft*/
        Objects.requireNonNull(world, "World can not be null!");
        Location loc = Objects.requireNonNull(frame, "Frame can not be null!").getLocation();
        BlockFace face = frame.getAttachedFace();
        int multZ = (bottomLeft ? 1 : -1), multX = multZ, multY = multZ;
        boolean xSet = false, ySet = false, zSet = false;
        switch (face) {
            //switch faces, so we get the bottom left and right respectively
            case WEST -> multZ = -multZ;
            case SOUTH -> multX = -multX;
            case DOWN, UP -> {
                return null;
            }
        }
        int i;
        //i is outside the for loop so we can check if we used the maxAdjacentSearches
        for (i = 0; i < ImageLoaderMain.getMaxAdjacentSearches(); i++) {
            if (!zSet && PositionUtils.hasNoItemFrameAtLocation(loc.subtract(0, 0, multZ), world, face)) {
                //the condition !zSet && .. makes sure that the .. part will only be executed if the value of zSet is false
                zSet = true;
                loc.add(0, 0, multZ);
            }
            if (!ySet && PositionUtils.hasNoItemFrameAtLocation(loc.subtract(0, multY, 0), world, face)) {
                ySet = true;
                loc.add(0, multY, 0);
            }
            if (!xSet && PositionUtils.hasNoItemFrameAtLocation(loc.subtract(multX, 0, 0), world, face)) {
                xSet = true;
                loc.add(multX, 0, 0);
            }
            if (xSet && ySet && zSet) {
                break;
            }
        }
        if (i == ImageLoaderMain.getMaxAdjacentSearches() && (!xSet || !zSet || !ySet)) {
            return null;
        }
        return loc;
    }

    private static boolean hasNoItemFrameAtLocation(@NotNull final Location loc, @NotNull final World world, @NotNull BlockFace face) {
        /*Method is used for return a boolean based on whether there is or isn't an item frame at a specified location*/
        Objects.requireNonNull(loc, "Location can not be null!");
        Objects.requireNonNull(face, "BlockFace can not be null!");
        Collection<Entity> entities = Objects.requireNonNull(world, "World can not be null").getNearbyEntities(loc, 0, 0, 0);
        for (Entity entity : entities) {
            if (entity instanceof ItemFrame frame && frame.getAttachedFace() == face) {
                return false;
            }
        }
        return true;
    }

    public static ItemFrame getClosestItemFrameAtLocation(@NotNull final Location loc, @NotNull final World world, @NotNull final BlockFace face) {
        /*This method is a more general method of the hasNoItemFrameAtLocation, except it returns the itemframe object and has more room for error*/
        Objects.requireNonNull(loc, "Location can not be null!");
        Objects.requireNonNull(face, "BlockFace can not be null!");
        Collection<Entity> entities = Objects.requireNonNull(world, "World can not be null!").getNearbyEntities(loc, 0.6, 0.6, 0.6);
        ItemFrame itemFrame = null;
        double distance = 1.8;
        for (Entity entity : entities) {
            if (entity instanceof ItemFrame frame && frame.getAttachedFace() == face) {
                if (frame.getLocation().distance(loc) < distance) {
                    distance = frame.getLocation().distance(loc);
                    itemFrame = frame;
                }
            }
        }
        return itemFrame;
    }
}
