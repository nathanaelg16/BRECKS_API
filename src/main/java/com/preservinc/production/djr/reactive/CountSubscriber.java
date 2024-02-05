package com.preservinc.production.djr.reactive;

import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

public class CountSubscriber extends Subscriber<Long, Long> {
    public CountSubscriber(CompletableFuture<Long> future) {
        super(future);
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(1);
    }

    @Override
    public void onNext(Long l) {
        super.future.complete(l);
    }

    @Override
    public void onError(Throwable t) {
        super.future.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        super.future.complete(null);
    }
}
