package test;

import rx.Observable;
import rx.Scheduler;

import static rx.schedulers.Schedulers.io;
import static test.TestUtil.observeSyncTestOperation;
import static test.TestUtil.output;
import static test.TestUtil.startTest;

public class InterimResults {
    public static void main(final String[] args) {
        new InterimResults().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        startTest("interim until");
        final Observable<String> diskCacheFetch =
            observeSyncTestOperation(1, "disk")
                .subscribeOn(scheduler);
        final Observable<String> networkFetch =
            observeSyncTestOperation(2, "network")
                .subscribeOn(scheduler);

        // kick off disk and network fetch together, take disk if arrives first, always finish
        // with network
        diskCacheFetch
            .mergeWith(networkFetch)
            .takeUntil(result -> result.equals("result:200"))
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(s -> output("                                " + s));
    }
}



