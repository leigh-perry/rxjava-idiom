package test;

import rx.Observable;
import rx.Scheduler;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static rx.Observable.combineLatest;
import static rx.Observable.interval;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.startTest;

public class CombineLatest {
    public static void main(final String[] args) {
        new CombineLatest().runAll();
    }

    public <T> void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        startTest("combineLatest");
        final Observable<String> o1 =
            combineLatest(
                interval(17, MILLISECONDS)
                    .map(x -> "Slow" + x)
                    .take(10)
                    .subscribeOn(scheduler),
                interval(10, MILLISECONDS)
                    .map(x -> "Fast" + x)
                    .take(10)
                    .subscribeOn(scheduler),
                (s, f) -> f + ":" + s
            );

        o1
            .toBlocking()
            .subscribe(TestUtil::output);

        startTest("withLatestFrom");
        final Observable<String> o2 =
            interval(17, MILLISECONDS)
                .map(x -> "Slow" + x)
                .take(10)
                .subscribeOn(scheduler)
                .withLatestFrom(
                    interval(10, MILLISECONDS)
                        .map(x -> "Fast" + x)
                        .take(10)
                        .subscribeOn(scheduler),
                    (s, f) -> f + ":" + s
                );

        o2
            .toBlocking()
            .subscribe(TestUtil::output);
    }
}

