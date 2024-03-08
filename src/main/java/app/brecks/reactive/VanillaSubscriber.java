package app.brecks.reactive;

import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

public class VanillaSubscriber<T> extends Subscriber<T, T> {
    public VanillaSubscriber(CompletableFuture<T> future) {
        super(future);
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(1);
    }

    @Override
    public void onNext(T result) {
        super.future.complete(result);
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
