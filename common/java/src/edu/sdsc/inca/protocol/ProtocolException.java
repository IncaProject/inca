package edu.sdsc.inca.protocol;

/**
 * Indicates the detection of malformed content in a protocol stream.
 */
public class ProtocolException extends Exception {

    /**
     * Creates a new ProtocolException Object.
     */
    public ProtocolException() {
    }

    /**
     * Creates a new ProtocolException Object.
     *
     * @param s the message
     */
    public ProtocolException(final String s) {
        super(s);
    }

    /**
     * Creates a new ProtocolException Object.
     *
     * @param s         message
     * @param throwable cause
     */
    public ProtocolException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    /**
     * Creates a new ProtocolException Object.
     *
     * @param throwable cause
     */
    public ProtocolException(final Throwable throwable) {
        super(throwable);
    }

}
