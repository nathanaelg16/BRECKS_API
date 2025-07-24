package app.brecks.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class ServerException extends RuntimeException {
    private static final Logger logger = LogManager.getLogger();
    private static final Marker marker = MarkerManager.getMarker("[ServerException]");

    public ServerException() {}

    public ServerException(String message) {
        super(message);
        logger.error(marker, message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
        logger.error(marker, message, cause);
    }

    public ServerException(Throwable cause) {
        super(cause);
        logger.error(marker, cause.getMessage(), cause);
    }

    public ServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        logger.error(marker, message, cause);
    }
}
