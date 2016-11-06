package test;

import rx.Observable;
import rx.Scheduler;

import java.util.concurrent.TimeUnit;

import static rx.Observable.error;
import static rx.Observable.mergeDelayError;
import static rx.Observable.timer;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.output;
import static test.TestUtil.startTest;

public class MergeDelayError {
    public static void main(final String[] args) {
        new MergeDelayError().runAll();
    }

    public <T> void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        startTest("merge with delay errors");
        final Observable<String> merged =
            mergeDelayError(
                getObservable(1),
                getObservable(2),
                getObservable(3),
                getObservable(4),
                getObservable(5)
            );

        merged
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(TestUtil::output, e -> output("Error [%s]", e.getMessage()));
    }

    private Observable<String> getObservable(final Integer i) {
        return i % 2 == 0
            ? error(new RuntimeException("Error " + i))
            : timer(i * 200, TimeUnit.MILLISECONDS).map(x -> i.toString());
    }
}

