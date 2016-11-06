package test;

import rx.Scheduler;
import rx.Single;

import static rx.Single.defer;
import static rx.Single.just;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.output;
import static test.TestUtil.startTest;
import static test.TestUtil.syncTestOperation;

public class SingleTest {
    public static void main(final String[] args) {
        new SingleTest().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        startTest("single");
        final Single<String> syncOperation =
            defer(() -> just(syncTestOperation(1, "")))
                .subscribeOn(scheduler);

        final String s = syncOperation
            .subscribeOn(scheduler)
            .toBlocking()
            .value();
        output("                                " + s);
    }
}


