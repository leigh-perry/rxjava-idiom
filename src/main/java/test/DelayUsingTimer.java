package test;

import rx.Scheduler;

import java.util.concurrent.TimeUnit;

import static rx.Observable.just;
import static rx.Observable.timer;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.startTest;

public class DelayUsingTimer {
    public static void main(final String[] args) {
        new DelayUsingTimer().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        startTest("delay -> timer");
        just(5, 5, 2, 1, 3)
            .delay(i -> timer(i, TimeUnit.SECONDS))
            .map(Object::toString)
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(TestUtil::output);

        startTest("timer + flatMap");
        just(5, 5, 2, 1, 3)
            .flatMap(i ->
                timer(i, TimeUnit.SECONDS)
                    .map(x -> i)
            )
            .map(Object::toString)
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(TestUtil::output);
    }
}
