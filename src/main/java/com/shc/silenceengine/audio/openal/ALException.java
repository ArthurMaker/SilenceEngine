package com.shc.silenceengine.audio.openal;

import com.shc.silenceengine.core.SilenceException;

/**
 * Represents an OpenAL exception which is non contextual.
 *
 * @author Sri Harsha Chilakapati
 */
public class ALException extends SilenceException
{
    /**
     * Constructs the SilenceException with a message
     *
     * @param message The exception message
     */
    public ALException(String message)
    {
        super(message);
    }
}
