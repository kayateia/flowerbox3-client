# Flowerbox Client (Kotlin)

This is the client for Flowerbox 3, aka the voxel version. Running this requires
a working Kotlin/Gradle build environment, a supported LWJGL platform, and a few
textures that you currently have to supply for yourself. (see Textures.kt for a list)
You can obtain these textures from any of the zillions of Minecraft texture packs out there.
(Eventually we'll distribute our own here, but for now, that's what you do.)

This is licensed under the MIT license. Please see LICENSE.md for more details.

## Building

Run gradlew/gradlew.bat in the project root, or import the project to IntelliJ. Should be pretty
straightforward after that.

## Running

You may need to include the following parameter on the JVM:

-XstartOnFirstThread

If you want to run it without an IDE, the main class is this:

net.kayateia.flowerbox.client.MainKt

As mentioned above, you'll need to copy some texture files to the "textures" directory under
the main project's directory.

## Older history

There is a bit of development history before this repo. You can find it here if you're curious:

https://github.com/kayateia/flowerbox3-client-scala
