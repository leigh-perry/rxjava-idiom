package test;

import rx.Observable;
import rx.Scheduler;

import static rx.schedulers.Schedulers.io;
import static test.TestUtil.startTest;

public class CrossProduct {
    public static void main(final String[] args) {
        new CrossProduct().runAll();
    }

    public <T> void runAll() {
        startTest("cross product using flatMap");
        final Observable<Integer> oneToEight = Observable.range(1, 8);

        final Observable<String> squares =
            oneToEight
                .map(i -> "abcdefgh".substring(i - 1, i))
                .flatMap(file ->
                    oneToEight
                        .map(Object::toString)
                        .map(rank -> file + rank)
                );

        squares
            .toBlocking()
            .subscribe(TestUtil::output);
    }
}

