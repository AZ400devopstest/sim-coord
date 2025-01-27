package net.neology.tolling.tzc.simulator.exception.utils;

// TODO: Update and add dependency on PETBRA core project?  This was copied from there...
public class ExceptionUtils {

    public static String getRootCauseMessage(Throwable throwable) {
        return getMessage(getRootCause(throwable));
    }

    private static String getMessage(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        builder.append(throwable.getClass().getSimpleName());
        builder.append(": ");
        String message = throwable.getLocalizedMessage();
        if (message != null) {
            builder.append(message);
        }
        return builder.toString();
    }

    private static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause == null || cause == throwable) {
            return throwable;
        } else {
            return getRootCause(cause);
        }
    }
}
