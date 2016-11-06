package test;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.util.function.Supplier;

public class ObservableEventLoop<T> {
    //    private final ExecutorService threadPool = Executors.newFixedThreadPool (1);
    private final String name;
    private final Supplier<? extends T> eventSupplier;

    public ObservableEventLoop(final Supplier<? extends T> eventSupplier) {
        this.name = "";
        this.eventSupplier = eventSupplier;
    }

    public ObservableEventLoop(final String name, final Supplier<? extends T> eventSupplier) {
        this.name = name;
        this.eventSupplier = eventSupplier;
    }

    public Observable<T> getEventObservable() {
        System.out.printf("tid:%3d        %s Get observable%n", Thread.currentThread().getId(), name);

        return
            Observable.<T>create(this::runEventLoop)
                .doOnSubscribe(() -> System.out.printf("tid:%3d        %s Subscribed%n", Thread.currentThread().getId(), name))
                .doOnUnsubscribe(() -> {
                    //threadPool.shutdown ();
                    System.out.printf("tid:%3d        %s Unsubscribed%n", Thread.currentThread().getId(), name);
                })
                //.observeOn (Schedulers.computation ())
                .subscribeOn(Schedulers.io())      // execute subscription on separate thread
            ;
    }

    private void runEventLoop(final Subscriber<? super T> subscriber) {
        System.out.printf("tid:%3d        %s Entering event loop%n", Thread.currentThread().getId(), name);

        boolean more = true;
        do {
            final T msg = eventSupplier.get();
            if (msg == null) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(new NullPointerException());
                }
            } else if (subscriber.isUnsubscribed()) {
                more = false;
            } else {
                subscriber.onNext(msg);
            }
        } while (more);

        System.out.printf("tid:%3d        %s Exiting event loop%n", Thread.currentThread().getId(), name);
    }
}


