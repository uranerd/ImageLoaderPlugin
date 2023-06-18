package subparcoder.Utils;

import net.coobird.thumbnailator.Thumbnails;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class ImageManagerUtil {
    /*This method is the main class to manage, store and load images, we store the images in a 2d byte array,
    * because java is inefficient at storing buffered images, and make them take up a lot of space*/
    private static byte[][] loadedBytes;
    private static String lastError;

    public static boolean setLoadedBytes(final byte @NotNull [][] newBytes) {
        /*This method sets the current loaded bytes, this takes a 2d byte array as input*/
        Objects.requireNonNull(newBytes, "The byte array must not be null!");
        if (newBytes.length > 0) {
            loadedBytes = newBytes;
            return true;
        }
        lastError = "Byte[][] had no members!";
        return false;
    }
    public static byte[][] getBytes(@NotNull final String imageFolder, @NotNull final String imagePathFormat, @NotNull final String rep) {
        /*This method will try and load the images from the specified imageFolder, so long as they follow the imagePathFormat
        * it returns these in a 2d byte array to save space*/
        Objects.requireNonNull(imageFolder, "Image folder can not be null!");
        Objects.requireNonNull(imagePathFormat, "Image path format can not be null!");
        Objects.requireNonNull(rep, "Path replacer can not be null!");
        final File imgFolder = new File(imageFolder);
        if (!imgFolder.exists() || !imgFolder.isDirectory() || imgFolder.listFiles() == null) {
            lastError = "Invalid image folder!";
            return null;
        }
        final int imageCount = Objects.requireNonNull(imgFolder.listFiles((dir, name) -> name.matches(imagePathFormat.replace(rep, "\\d+")))).length;
        //^ that line counts the amount of images that match the imagePathFormat pattern, using some regex to account for decimals
        if (imageCount == 0) {
            lastError = "Image folder must contain at least 1 valid image!";
            return null;
        }
        final byte[][] bytes = new byte[imageCount][];
        File imageFile;
        for (int i = 0 ; i < imageCount ; i++) {
            imageFile = new File(imageFolder, imagePathFormat.replace(rep, String.valueOf(i)));
            try {
                bytes[i] = Files.readAllBytes(Paths.get(imageFile.getAbsolutePath()));
            } catch (IOException e) {
                lastError = "Error reading bytes from file: " + e.getMessage();
                return null;
            }
        }
        return bytes;
    }

    public static BufferedImage[] getResizedFromLoadedBytes(final int width, final int height, final int nThreads) {
        /*This method returns the resized (to width and height) buffered image array from the current loaded 2d byte array*/
        if (width <= 0 || height <= 0 || nThreads <= 0) {
            lastError = "Width, height and nThreads must be greater than 0 when resizing an image!";
            return null;
        }
        if (!ImageManagerUtil.hasLoadedBytes()) {
            lastError = "Must have loaded images before trying to resize!";
            return null;
        }
        final BufferedImage[] resizedImages = new BufferedImage[loadedBytes.length];
        final int batchSize = (int) Math.round((loadedBytes.length * 1.0D) / (nThreads * 1.0D));
        final Thread[] threads = new Thread[nThreads];
        final boolean caching = ImageIO.getUseCache();
        ImageIO.setUseCache(false);
        //use multithreading to each resize a 'batch' of images, making it faster to do all of them
        for (int threadIndex = 0 ; threadIndex < nThreads ; threadIndex++) {
            final int start = threadIndex * batchSize;
            final int end = threadIndex == nThreads - 1 ? loadedBytes.length : start + batchSize;
            threads[threadIndex] = new Thread("Image-Resize-Thread(" + threadIndex + ")") {
                @Override
                public void run() {
                    try {
                        for (int i = start; i < end; i++) {
                            //Use the Thumbnails library to resize the image, credit to https://github.com/coobird/thumbnailator
                            try {
                                resizedImages[i] = Thumbnails.of(new ByteArrayInputStream(loadedBytes[i])).forceSize(width, height).asBufferedImage();
                            } catch (IOException e) {
                                lastError = "Error resizing images in thread " + Thread.currentThread().getName() + ", " + e.getMessage();
                                return;
                            }
                        }
                    } catch (Exception e) {} //we dont care cus its going to be an interrupted exception
                }
            };
            threads[threadIndex].start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                lastError = "Error joining threads: " + e.getMessage();
                for (Thread t : threads) {
                    t.interrupt();
                }
                return null;
            }
        }
        //we will wait for all the threads to finish now, because we joined them
        ImageIO.setUseCache(caching);
        return resizedImages;
    }

    public static String getLastError() {
        /*method used for debugging*/
        return lastError;
    }
    public static boolean hasLoadedBytes() {
        /*Returns whether we have loadedBytes*/
        return loadedBytes != null;
    }
}