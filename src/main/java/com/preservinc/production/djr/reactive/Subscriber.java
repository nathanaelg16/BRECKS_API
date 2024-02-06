package com.preservinc.production.djr.reactive;

import java.util.concurrent.CompletableFuture;

public abstract class Subscriber<S, T> implements org.reactivestreams.Subscriber<T> {
    protected final CompletableFuture<S> future;

    public Subscriber(CompletableFuture<S> future) {
        this.future = future;
    }
}
