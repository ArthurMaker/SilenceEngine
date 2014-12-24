package com.shc.silenceengine.core;

import com.shc.silenceengine.graphics.Batcher;
import com.shc.silenceengine.graphics.opengl.GL3Context;
import com.shc.silenceengine.graphics.opengl.GLError;
import com.shc.silenceengine.graphics.opengl.Texture;
import com.shc.silenceengine.input.Keyboard;
import com.shc.silenceengine.input.Mouse;
import com.shc.silenceengine.utils.TimeUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import static org.lwjgl.opengl.GL11.*;

/**
 * The basic class for all the games made with SilenceEngine. Every game
 * will simply extend this Game class, and call the start method to play.
 * <p>
 * <pre>
 *     public class MyGame extends Game
 *     {
 *         // Initialize the resources
 *         public void init() {}
 *
 *         // Update game logic
 *         public void update(float delta) {}
 *
 *         // Render to screen
 *         public void render(float delta, Batcher batcher) {}
 *
 *         // Handle window resize event
 *         public void resize() {}
 *
 *         // Dispose the resources
 *         public void dispose() {}
 *
 *         public static void main(String[] args)
 *         {
 *             new MyGame().start();
 *         }
 *     }
 * </pre>
 * <p>
 * Creating a game in SilenceEngine is as simple as that. This is the
 * skeleton of your game.
 *
 * @author Sri Harsha Chilakapati
 */
public class Game
{
    static
    {
        // Every exception occurs after SilenceException, even
        // the uncaught exceptions are thrown as runtime exceptions
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread t, Throwable e)
            {
                try
                {
                    Writer result = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(result);
                    e.printStackTrace(printWriter);

                    throw new SilenceException(result.toString());
                }
                catch (SilenceException ex)
                {
                    ex.printStackTrace();
                    System.exit(-1);
                }
            }
        });

        // Load the natives
        NativesLoader.load();
    }

    /**
     * Specifies the development status of the game. Before distributing
     * make sure to change this to false, leaving this enabled causes the
     * GLExceptions, if any, to be displayed to the user.
     */
    public static boolean development = true;

    // Is the game running?
    private static boolean running = false;

    // Game logic rate
    private static int ups       = 60;
    private static int targetUPS = 60;

    // Game frame rate
    private static int fps = 60;

    private static Batcher batcher;

    /**
     * Initialize the Game. Loads the resources, and
     * sets the game states.
     */
    public void init()
    {
    }

    /**
     * Performs game logic. Also, it is a place to check
     * for input, collisions, what-not, everything except
     * rendering.
     *
     * @param delta It is the time taken by the last update (in ms)
     */
    public void update(float delta)
    {
    }

    /**
     * Renders the game to the OpenGL Scene.
     *
     * @param delta   It is the time taken by the last render (in ms)
     * @param batcher The Batcher to batch OpenGL calls
     */
    public void render(float delta, Batcher batcher)
    {
    }

    /**
     * Handle the window-resize event. Used to set the view-port
     * and re-size the camera.
     */
    public void resize()
    {
    }

    /**
     * Properly disposes all the resources created in init method
     */
    public void dispose()
    {
    }

    /**
     * Starts the game. Initiates the game life-cycle and starts
     * the main game-loop.
     */
    public void start()
    {
        running = true;

        // Create and show the display
        Display.create();
        Display.show();

        // Initialize the Game
        init();

        // GameLoop constants
        final double secondsPerFrame = 1.0 / targetUPS;
        final double maxFrameSkips = 5;

        // Timing mechanism
        double previous;
        double current;
        double elapsed;
        double lag;

        // FPS and UPS calculation timing
        double lastUPSUpdate;
        double lastFPSUpdate;

        // Counters for updates and frames
        int updatesProcessed;
        int framesProcessed;
        int skippedFrames;

        // Initial values
        lag = 0;
        previous = TimeUtils.currentSeconds();

        updatesProcessed = 0;
        framesProcessed = 0;

        lastUPSUpdate = 0;
        lastFPSUpdate = 0;

        while (true)
        {
            current = TimeUtils.currentSeconds();
            elapsed = current - previous;

            skippedFrames = 0;

            lag += elapsed;

            // Do updates in small steps playing catchup
            while (lag >= secondsPerFrame && skippedFrames < maxFrameSkips)
            {
                Keyboard.startEventFrame();
                Mouse.startEventFrame();

                update((float) elapsed);

                Keyboard.clearEventFrame();
                Mouse.clearEventFrame();

                updatesProcessed++;

                // If a second has passed, update the UPS counter
                if (current - lastUPSUpdate >= 1000)
                {
                    ups = updatesProcessed;
                    updatesProcessed = 0;
                    lastUPSUpdate = current;
                }

                lag -= secondsPerFrame;
                skippedFrames++;
            }

            // Check if the Game should end
            if (Display.isCloseRequested() || !running)
                break;

            // The Display was resized
            if (Display.wasResized())
            {
                GL3Context.viewport(0, 0, Display.getWidth(), Display.getHeight());
                resize();
            }

            // Clear the screen
            GL3Context.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            Texture.setActiveUnit(0);

            // Render the Game
            render((float) elapsed, batcher);
            GLError.check();

            framesProcessed++;

            // If a second is passed, update FPS value
            if (current - lastFPSUpdate >= 1000)
            {
                fps = framesProcessed;
                framesProcessed = 0;
                lastFPSUpdate = current;
            }

            previous = current;

            // Update the display and swap the buffers
            Display.update();
        }

        // Dispose the Batcher
        batcher.dispose();

        // Call dispose() and destroy the Display
        dispose();
        Display.destroy();
    }

    /**
     * Kills the running game!
     */
    public static void end()
    {
        running = false;
    }

    /**
     * @return number of updates done in last second
     */
    public static int getUPS()
    {
        return ups;
    }

    /**
     * @return number of frames rendered in last second
     */
    public static int getFPS() { return fps; }

    /**
     * @return The target updates per second
     */
    public static int getTargetUPS()
    {
        return targetUPS;
    }

    /**
     * @return True if running, else false
     */
    public static boolean isRunning()
    {
        return running;
    }

    /**
     * Sets the target logic speed of the Game.
     *
     * @param targetUPS The number of steps the game should try to make in a second
     */
    public static void setTargetUPS(int targetUPS)
    {
        Game.targetUPS = targetUPS;
    }

    /**
     * @return The Global Batcher of the Game
     */
    public static Batcher getBatcher()
    {
        return batcher;
    }

    /**
     * Sets the batcher to be passed to the render() method
     *
     * @param batcher The Batcher instance to use
     */
    public static void setBatcher(Batcher batcher)
    {
        Game.batcher = batcher;
    }
}
