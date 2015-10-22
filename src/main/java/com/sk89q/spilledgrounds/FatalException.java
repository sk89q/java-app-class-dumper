package com.sk89q.spilledgrounds;

public class FatalException extends Exception {

    public FatalException() {
    }

    public FatalException(String message) {
        super(message);
    }

    public FatalException(String message, Throwable cause) {
        super(message, cause);
    }

    public FatalException(Throwable cause) {
        super(cause);
    }

}
