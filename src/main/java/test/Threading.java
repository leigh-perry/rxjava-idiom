package test;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static test.TestUtil.output;

public class Threading {
    private long startMsec;
    private final Scheduler scheduler1 = Schedulers.from(Executors.newFixedThreadPool(2));
    private final Scheduler scheduler2 = Schedulers.from(Executors.newFixedThreadPool(2));

    public static void main(final String[] args) {
        new Threading().testThreading();
    }

    private void testThreading() {
        final Action1<String> action = s -> {
            output("start handle rxed %s", s);
            TestUtil.pause(500);
            output("end handle rxed %s", s);
        };

        // no observeOn or subscribeOn means all on main thread
        if (true) {
            //     0     1: calling subscribe
            //    37     1: starting stream
            //   238     1: emitting item1
            //   238     1: start handle rxed item1
            //   738     1: end handle rxed item1
            //   938     1: emitting item2
            //   938     1: start handle rxed item2
            //  1438     1: end handle rxed item2
            //  1638     1: emitting item3
            //  1638     1: start handle rxed item3
            //  2138     1: end handle rxed item3
            //  2138     1: ending stream
            //  2138     1: back from subscribe
            final CountDownLatch latch = new CountDownLatch(1);
            startMsec = System.currentTimeMillis();

            output("calling subscribe");
            final Observable<String> observable = getDataObservable();
            observable.subscribe(
                action,
                Throwable::printStackTrace,
                latch::countDown
            );
            output("back from subscribe");

            try {
                latch.await();
            } catch (final InterruptedException e) {
            }
        }

        // subscribeOn but no observeOn means all on subscribeOn thread
        //      subscribing means generating the events
        //      subscribeOn allows subscribe () call to return immediately
        if (true) {
            //     0     1: calling subscribe
            //    10     1: back from subscribe
            //    11    12: starting stream
            //   212    12: emitting item1
            //   212    12: start handle rxed item1
            //   712    12: end handle rxed item1
            //   912    12: emitting item2
            //   912    12: start handle rxed item2
            //  1412    12: end handle rxed item2
            //  1612    12: emitting item3
            //  1612    12: start handle rxed item3
            //  2112    12: end handle rxed item3
            //  2112    12: ending stream
            final CountDownLatch latch = new CountDownLatch(1);
            startMsec = System.currentTimeMillis();

            output("calling subscribe");
            final Observable<String> observable = getDataObservable();
            observable
                .subscribeOn(scheduler1)
                .subscribe(
                    action,
                    Throwable::printStackTrace,
                    latch::countDown
                );
            output("back from subscribe");

            try {
                latch.await();
            } catch (final InterruptedException e) {
            }
        }

        // observeOn but no subscribeOn means emit on main thread but handle asynchronously
        //      observing means handling the callbacks
        if (true) {
            //     0     1: calling subscribe
            //    23     1: starting stream
            //   223     1: emitting item1
            //   223    16: start handle rxed item1
            //   423     1: emitting item2
            //   623     1: emitting item3
            //   623     1: ending stream
            //   623     1: back from subscribe
            //   723    16: end handle rxed item1
            //   723    16: start handle rxed item2
            //  1223    16: end handle rxed item2
            //  1223    16: start handle rxed item3
            //  1723    16: end handle rxed item3
            final CountDownLatch latch = new CountDownLatch(1);
            startMsec = System.currentTimeMillis();

            output("calling subscribe");
            final Observable<String> observable = getDataObservable();
            observable
                .observeOn(scheduler2)
                .subscribe(
                    action,
                    Throwable::printStackTrace,
                    latch::countDown
                );
            output("back from subscribe");

            try {
                latch.await();
            } catch (final InterruptedException e) {
            }
        }

        // subscribeOn + observeOn means emit on one thread and handle asynchronously on another
        //      subscribeOn allows subscribe () call to return immediately
        //      subscribing means generating the events
        //      observing means handling the callbacks
        if (true) {
            //     0     1: calling subscribe
            //     3     1: back from subscribe
            //     3    17: starting stream
            //   203    17: emitting item1
            //   203    16: start handle rxed item1
            //   403    17: emitting item2
            //   603    17: emitting item3
            //   603    17: ending stream
            //   703    16: end handle rxed item1
            //   703    16: start handle rxed item2
            //  1203    16: end handle rxed item2
            //  1203    16: start handle rxed item3
            //  1703    16: end handle rxed item3
            final CountDownLatch latch = new CountDownLatch(1);
            startMsec = System.currentTimeMillis();

            output("calling subscribe");
            final Observable<String> observable = getDataObservable();
            observable
                .subscribeOn(scheduler1)
                .observeOn(scheduler2)
                .subscribe(
                    action,
                    Throwable::printStackTrace,
                    latch::countDown
                );
            output("back from subscribe");

            try {
                latch.await();
            } catch (final InterruptedException e) {
            }
        }
    }

    private Observable<String> getDataObservable() {
        return Observable.interval(200,200, TimeUnit.MILLISECONDS)
            .take(3)
            .doOnSubscribe(() -> output("starting stream"))
            .doOnCompleted(() -> output("ending stream"))
            .doOnNext(i -> output("emitting %s", "item" + i))
            .map(i -> Long.toString(i));
    }
}



