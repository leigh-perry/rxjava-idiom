package test;

import rx.Observable;

import java.util.concurrent.TimeUnit;

import static rx.Observable.from;
import static rx.Observable.timer;

public class Retry {
    public static <T> Observable<T> observeWithRetry(final Observable<T> observable, final Integer[] backoffMsecs) {
        return observable
            .retryWhen(exceptions ->
                exceptions
                    .zipWith(from(backoffMsecs), (exception, backoffMsec) -> backoffMsec)
                    .flatMap(backoffMsec -> timer(backoffMsec, TimeUnit.MILLISECONDS))
            );
    }
}


