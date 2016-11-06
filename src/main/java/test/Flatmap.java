package test;

import rx.Scheduler;

import static rx.Observable.range;
import static rx.schedulers.Schedulers.computation;
import static test.TestUtil.output;
import static test.TestUtil.startTest;
import static test.TestUtil.stream;

public class Flatmap {
    public static void main(final String[] args) {
        new Flatmap().runAll();
    }

    public void runAll() {
        final Scheduler scheduler = computation();    // io has more threads

        startTest("flatmap");
        stream(8)
            .flatMap(i -> range(1, i + 1).map(j -> "v" + i + ":" + j))
            .subscribeOn(scheduler)
            .toBlocking()
            .subscribe(s -> output("            " + s));
    }
}



