# Up-to-date LWJGL3 for all MC versions

There are certain thing in LWJGL3 which we would like to use but given MC already uses LWJGL itself, we can't just add it as a normal dependency.
\
Instead we load it in its own (mostly) isolated class loader (the same type of class loader we use for relaunching on LaunchWrapper versions).
Communication between the two environments needs to be very explicit, by necessity and so it is always clear which LWJGL one is talking to.

This Gradle subproject is loaded in the regular class loader and contains the code responsible for creating the isolated loader and the interface definitions which are shared between the normal and isolated loaders.
It must not declare classes in the `gg.essential.util.lwjgl3.impl` package.
\
The subproject `impl` is loaded within the isolated loader and has full access to all the LWJGL3 libraries we ship. It consists mostly of implementations for the interfaces.
It must not declare classes in the `gg.essential.util.lwjgl3.api` package (it should only declare classes in the `gg.essential.util.lwjgl3.impl` package).

From the outside, the impl classes can only be accessed via reflection.
For convenient access, the loader provides a factory for classes implementing interfaces in the api package.
These classes must be named based on the interface they're implementing with an additional `Impl` suffix, and must be located in the `impl` rather than the `api` package.
