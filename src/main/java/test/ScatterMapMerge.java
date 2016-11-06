package test;

import rx.Observable;
import rx.Scheduler;

import static rx.Observable.merge;
import static rx.schedulers.Schedulers.computation;
import static test.TestUtil.observeSyncTestOperation;
import static test.TestUtil.output;
import static test.TestUtil.startTest;
import static test.TestUtil.stream;

public class ScatterMapMerge {
    public static void main(final String[] args) {
        new ScatterMapMerge().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = computation();    // io has more threads

        startTest("scatter map merge");
        final Observable<Observable<String>> scattered =
            stream(15)
                .map(i -> observeSyncTestOperation(i, "")
                    .subscribeOn(scheduler));
        merge(scattered)
            .toBlocking()
            .subscribe(s -> output("                                " + s));
    }
}



