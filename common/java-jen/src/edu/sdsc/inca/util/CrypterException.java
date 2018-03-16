package edu.sdsc.inca.util;

import java.lang.*;

/**
 * Indicates that some configuration problem has occured within Inca.
 */
public class CrypterException extends Exception {

    /**
     * Default constructor.  No message no cause.  Using this is not recomended.
     */
    public CrypterException() {
    }

    /**
     * Create a configuration exception with the given message.
     *
     * @param s Message
     */
    public CrypterException(final String s) {
        super(s);
    }

    /**
     * Add a message to the Exception and a cause.
     *
     * @param s         message
     * @param throwable cause
     */
    public CrypterException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    /**
     * Add just a cause to the exception.
     *
     * @param throwable cause
     */
    public CrypterException(final Throwable throwable) {
        super(throwable);
    }
}
