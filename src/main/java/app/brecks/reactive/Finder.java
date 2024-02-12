package app.brecks.reactive;

import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Finder<T> extends Subscriber<List<T>, T>  {
    private final List<T> results;

    public Finder(CompletableFuture<List<T>> future) {
        super(future);
        this.results = new ArrayList<>();
    }

    @Override
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        results.add(t);
    }

    @Override
    public void onError(Throwable t) {
        super.future.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        super.future.complete(this.results);
    }
}
