package com.github.nkzawa.util;

import java.util.NoSuchElementException;

public class Optional<T> {

    static final Optional EMPTY = Optional.ofNullable(null);

    private T value;

    public static <T> Optional<T> of(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return new Optional<T>(value);
    }

    public static <T> Optional<T> ofNullable(T value) {
        return new Optional<T>(value);
    }

    public static <T> Optional<T> empty() {
        return EMPTY;
    }

    private Optional(T value) {
        this.value = value;
    }

    public boolean isPresent() {
        return this.value != null;
    }

    public T get() {
        if (this.value == null) {
            throw new NoSuchElementException();
        }
        return this.value;
    }

    public T orElse(T other) {
        return this.value != null ? this.value : other;
    }
}
