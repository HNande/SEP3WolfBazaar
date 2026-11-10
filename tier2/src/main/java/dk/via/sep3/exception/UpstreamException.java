package dk.via.sep3.exception;

public class UpstreamException extends RuntimeException {
    public UpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
