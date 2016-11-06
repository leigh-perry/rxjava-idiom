package test;

import rx.Completable;
import rx.Scheduler;

import static rx.Completable.defer;
import static rx.Completable.fromAction;
import static rx.schedulers.Schedulers.io;
import static test.TestUtil.startTest;
import static test.TestUtil.syncTestOperation;

public class CompletableTest {
    public static void main(final String[] args) {
        new CompletableTest().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        startTest("single");
        final Completable syncOperation =
            defer(() -> fromAction(() -> syncTestOperation(1, "")))
                .subscribeOn(scheduler);

        syncOperation
            .subscribeOn(scheduler)
            .await();
    }
}


