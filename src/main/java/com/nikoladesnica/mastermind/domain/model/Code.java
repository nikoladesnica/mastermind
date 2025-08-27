package com.nikoladesnica.mastermind.domain.model;

import java.util.List;
import java.util.Objects;

public final class Code {
    private final List<Integer> digits; // size validated

    public Code(List<Integer> digits, int expectedLen, int min, int max, boolean allowDuplicates) {
        Objects.requireNonNull(digits);
        if (digits.size() != expectedLen) throw new IllegalArgumentException("Invalid code length");
        for (int d : digits) {
            if (d < min || d > max) throw new IllegalArgumentException("Digit out of range");
        }
        if (!allowDuplicates && digits.stream().distinct().count() != digits.size())
            throw new IllegalArgumentException("Duplicates not allowed");
        this.digits = List.copyOf(digits);
    }

    public List<Integer> digits() { return digits; }
}
