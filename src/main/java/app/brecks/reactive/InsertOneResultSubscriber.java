package app.brecks.reactive;

import com.mongodb.client.result.InsertOneResult;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

public class InsertOneResultSubscriber extends Subscriber<InsertOneResult, InsertOneResult> {
    public InsertOneResultSubscriber(CompletableFuture<InsertOneResult> future) {
        super(future);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(1);
    }

    @Override
    public void onNext(InsertOneResult item) {
        super.future.complete(item);
    }

    @Override
    public void onError(Throwable throwable) {
        super.future.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        super.future.complete(null);
    }
}
