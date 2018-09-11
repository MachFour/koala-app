package com.machfour.koala;

import java.io.IOException;

public class UriOpenException extends IOException {
    public UriOpenException() {
    }

    public UriOpenException(String message) {
        super(message);
    }

    public UriOpenException(Exception cause) {
        super(cause.getLocalizedMessage(), cause);
    }
}
