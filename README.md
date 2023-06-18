# ImageLoaderPlugin
A Minecraft plugin for 1.19 version that loads the specified images and displays them on maps.
# How does it do this?
Using maps you can load images, combinding some code to load images from a directory, you can loop through all the images and display them on maps.
# Where to download?
To download this go to the 'releases' section and choose the latest release, you can then move the .jar file to the 'plugins' folder in your Minecraft server and reload the Minecraft server to load the plugin.
# How do I use this?
There are a few commands to use (you have to have OP to use all of them, and it has to be sent by a player, command blocks or console can not use these commands):

/imgload (image folder) (image format) (replacer): Loads the specified images in the image folder using the image format, image folder is the folder with all the images in it, image format is how the images are named, they should be named so that there is a number that increases, starting at 0, and replacer is where that number is, for example /imgload images (the folder) image_X.jpg (images are called image_0.jpg, image_1.jpg, image_2.jpg etc...) X (the replacer in image_X.jpg because the images are named like image_0.jpg, image_1.jpg)

/getmap (fps) (threads): Gives the sender a map that displays all the loaded images at the specified fps, threads is the amount of threads to use when resizing the images.

/getfillmap (fps) (threads): Gives the sender a map that, when placed on item frames will fill all the adjacent item frames with a higher-resolution version of the images, fps is the images to display each second and threads is the amount of threads to use when resizing the image.

/setsearches (searches): Sets the max adjacent searches when looking for the bottom left and top right of item frames that a fill map was placed on, searches is the max searches as an integer.

/getsearches: Sends the sender the max adjacent searches

# Will this lag?
It depends, the more images you use, the more it will lag, and the more item frames you display them on, the laggier and more 'tearing' can be seen in the display maps.
Also the more threads you use for resizing an image, the more intensive it will be but it will be quicker.
It is reccomended to have at least 8MB of ram allocated to the server, although you can get away using less ram if you plan on loading only a few images.
# What to do if I get an error?
If it is an error that gets sent to the player, chances are it is expected, i.e there were no loaded images, or you tried to fill too many item frames, however, if there is an uncaught error in the server console, or it says "An internal error occurred while attempting to perform this command", try and get the console output to debug it further, you can try opening an error in github, but I am not that good at github and this project was made for a school project (I do not know if I will maintain it), so chances are I will not reply.  
