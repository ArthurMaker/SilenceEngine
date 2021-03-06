package com.shc.silenceengine.core;

import java.io.*;
import java.nio.file.Files;

/**
 * A package-private class that takes care of LWJGL natives. Useful
 * since it removes the head-ache of managing natives, it manages
 * by loading the natives from <pre>lwjgl-natives.jar</pre> file.
 * <p>
 * However, if you want to disable this component, you can do so by
 * setting the property <pre>NativesLoader</pre> to false.
 *
 * @author Sri Harsha Chilakapati
 */
class NativesLoader
{
    // Operating System and Architecture
    private static final String OS   = System.getProperty("os.name").toLowerCase();
    private static final String ARCH = System.getProperty("os.arch").toLowerCase();

    private static boolean isWindows = OS.contains("windows");
    private static boolean isLinux   = OS.contains("linux");
    private static boolean isMac     = OS.contains("mac");
    private static boolean is64Bit   = ARCH.equals("amd64") || ARCH.equals("x86_64");

    /**
     * Loads the natives from the JAR resources
     */
    public static void load()
    {
        // If NativesLoader is disabled in properties, do nothing
        if (System.getProperty("NativesLoader", "true").equalsIgnoreCase("false"))
            return;

        try
        {
            // Create temporary Directory
            File tmp = Files.createTempDirectory("SilenceEngine").toFile();

            // Delete the temp dir on exit
            tmp.deleteOnExit();

            if (isWindows)
            {
                if (!is64Bit)
                {
                    // Extract WIN32 natives
                    extractLibrary(tmp, "/windows/x86/lwjgl.dll");
                    extractLibrary(tmp, "/windows/x86/OpenAL32.dll");
                }
                else
                {
                    // Extract WIN64 natives
                    extractLibrary(tmp, "/windows/x64/lwjgl.dll");
                    extractLibrary(tmp, "/windows/x64/OpenAL32.dll");
                }
            }
            else if (isMac)
            {
                // Extract MacOS natives
                extractLibrary(tmp, "/macosx/x64/liblwjgl.dylib");
                extractLibrary(tmp, "/macosx/x64/libopenal.dylib");
            }
            else if (isLinux)
            {
                if (!is64Bit)
                {
                    // Extract x86 Linux natives
                    extractLibrary(tmp, "/linux/x86/liblwjgl.so");
                    extractLibrary(tmp, "/linux/x86/libopenal.so");
                }
                else
                {
                    // Extract x64 Linux natives
                    extractLibrary(tmp, "/linux/x64/liblwjgl.so");
                    extractLibrary(tmp, "/linux/x64/libopenal.so");
                }
            }

            // Set the LWJGL library path
            System.setProperty("org.lwjgl.librarypath", tmp.getAbsolutePath());
        }
        catch (Exception e)
        {
            // Oops, something went wrong, print to stack trace
            SilenceException.reThrow(e);
        }
    }

    /**
     * Extracts a file in JAR with path to a directory in FileSystem
     * @param dir  The directory to extract file to
     * @param path The path of the library in JAR to extract
     */
    private static void extractLibrary(File dir, String path)
    {
        if (!path.startsWith("/"))
            throw new IllegalArgumentException("The path has to be absolute! (Start with a '/' character)");

        // Get the InputStream from path

        // Get the filename from path
        String[] parts    = path.replaceAll("\\\\", "/").split("/");
        String   filename = (parts.length > 1) ? parts[parts.length-1] : null;

        if (filename == null)
            throw new SilenceException("Filename is null. Did you pass a directory?");

        try
        {
            // Create a file in the DIR which deletes itself
            File tmp = new File(dir, filename);
            tmp.deleteOnExit();

            // Create the OutputStream

            // Extract the native library
            byte[] buffer = new byte[1024];
            int readBytes;

            try (FileOutputStream os = new FileOutputStream(tmp); InputStream is = NativesLoader.class.getResourceAsStream(path))
            {
                while ((readBytes = is.read(buffer)) != -1)
                    os.write(buffer, 0, readBytes);
            }
        }
        catch (Exception e)
        {
            // Error again, :( Throw an exception
            throw new SilenceException(e.getMessage());
        }
    }
}
