package com.preservinc.production.djr.util.function;

@FunctionalInterface
public interface CheckedBiConsumer<S, T, E extends Exception> {
    void accept(S s, T t) throws E;
}
