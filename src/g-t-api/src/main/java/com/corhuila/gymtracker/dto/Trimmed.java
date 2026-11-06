package com.corhuila.gymtracker.dto;

/** Text fields are trimmed before they are validated and stored; passwords never go through here. */
final class Trimmed {

    private Trimmed() {
    }

    static String of(String value) {
        return value == null ? null : value.strip();
    }
}
