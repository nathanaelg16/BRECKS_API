package app.brecks.util;

public class Constants {
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 20;
    public static final long DEFAULT_TIMEOUT = 1_800_000; // in milliseconds, equivalent to 30 minutes
    public static final long DEFAULT_TOKEN_REFRESH_DELTA = 300_000; // in milliseconds, equivalent to 5 minutes
    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
}
