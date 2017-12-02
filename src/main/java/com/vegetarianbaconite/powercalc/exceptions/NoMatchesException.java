package com.vegetarianbaconite.powercalc.exceptions;

public class NoMatchesException extends RuntimeException {
    @Override
    public String getMessage() {
        return "No matches were found for the given event. ";
    }
}
