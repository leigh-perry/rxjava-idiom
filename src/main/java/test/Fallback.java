package test;

import rx.Observable;
import rx.Scheduler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static rx.Observable.empty;
import static rx.Observable.just;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.observeSyncTestOperation;
import static test.TestUtil.output;
import static test.TestUtil.startTest;

public class Fallback {
    public static void main(final String[] args) {
        new Fallback().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        startTest("fallback on timeout");
        observeSyncTestOperation(1, "")
            .subscribeOn(scheduler)
            .timeout(1000, TimeUnit.MILLISECONDS)
            .onErrorReturn(exception -> {
                if (exception instanceof TimeoutException) {
                    return "default-value";
                } else {
                    throw new RuntimeException(exception);
                }
            })
            .onExceptionResumeNext(just("ooops"))
            .toBlocking()
            .subscribe(s -> output("                                " + s));

        final Observable<String> syncOperation1 =
            observeSyncTestOperation(1, "")
                .subscribeOn(scheduler);
        final Observable<String> syncOperation2 =
            observeSyncTestOperation(2, "")
                .subscribeOn(scheduler);

        startTest("simple fallback a");
        syncOperation1
            .concatWith(syncOperation2)
            .first()    // o2 should never subscribe
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(s -> output("                                " + s));

        startTest("simple fallback b");
        empty()
            .concatWith(syncOperation2)
            .first()    // o2 should subscribe this time
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(s -> output("                                " + s));
    }
}



