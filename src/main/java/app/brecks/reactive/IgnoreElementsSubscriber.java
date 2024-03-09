package app.brecks.reactive;

import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

public class IgnoreElementsSubscriber<T> extends Subscriber<Void, T> {
    public IgnoreElementsSubscriber() {
        super(new CompletableFuture<>());
    }

    public IgnoreElementsSubscriber(CompletableFuture<Void> future) {
        super(future);
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        // do nothing
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
