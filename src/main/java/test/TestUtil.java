package test;

import rx.Observable;
import rx.Scheduler;
import rx.observables.SyncOnSubscribe;
import rx.schedulers.Schedulers;

import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static rx.Observable.defer;
import static rx.Observable.just;

public class TestUtil {
    public static final String DELIBERATE_EXCEPTION = "Deliberate exception";
    private static volatile long startTime = System.currentTimeMillis();

    public static void startTiming() {
        startTime = System.currentTimeMillis();
    }

    public static Observable<Integer> stream(final int maxCount) {
        return stream(maxCount, i -> i);
    }

    public static Observable<String> stream(final int maxCount, final String prefix) {
        return stream(maxCount, i -> prefix + " " + i);
    }

    public static <T> Observable<T> stream(final int maxCount, final Function<Integer, T> mapper) {
        return stream(maxCount, -1, mapper, 100);
    }

    public static <T> Observable<T> stream(
        final int maxCount,
        final int errorAfter,
        final Function<Integer, T> mapper,
        final int periodMsec
    ) {
        return stream(maxCount, errorAfter, mapper, periodMsec, true);
    }

    public static <T> Observable<T> stream(
        final int maxCount,
        final int errorAfter,
        final Function<Integer, T> mapper,
        final int periodMsec,
        final boolean shouldComplete
    ) {
        return Observable.<Integer>create(
            SyncOnSubscribe.createStateful(
                () -> 0,
                (i, observer) -> {
                    pause((long) (Math.random() * periodMsec));

                    if (i == errorAfter) {
                        output("%s %s", i, DELIBERATE_EXCEPTION);
                        observer.onError(new RuntimeException(DELIBERATE_EXCEPTION));
                    } else {
                        final T v = mapper.apply(i);
                        output("%s -> %s", i, v);
                        observer.onNext(i);
                    }
                    return ++i;
                }
            )
        ).map(mapper::apply);
    }

    public static void pause(final long millis) {
        try {
            // NOTE: Only using Thread.sleep here as an artificial demo.
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            output("pause interrupted...should have been %s msec", millis);
        }
    }

    public static void output(final String format, final Object... args) {
        System.out.printf("%3d %4dms %s%n", Thread.currentThread().getId(), getElapsedMsec(), String.format(format, args));
    }

    public static long getElapsedMsec() {
        return System.currentTimeMillis() - startTime;
    }

    public static Observable<String> observeSyncTestOperation(final Integer input, final String prefix) {
        return observeSynchronous(i -> syncTestOperation(i, prefix), input);
    }

    public static String syncTestOperation(final Integer i, final String prefix) {
        return syncTestOperation(i, prefix, 2000);
    }

    public static String syncTestOperation(final Integer i, final String prefix, final int msec) {
        output("                %s%s started", prefix, i);
        pause(msec);
        output("                %s%s done", prefix, i);
        return "result:" + i * 100;
    }

    // the anSWER...
    //   Super when Writing to (passing into) the object, ie pass into the function
    //   Extends when Reading from the object, read from the function
    public static <T, R> Observable<R> observeSynchronous(final Function<? super T, ? extends R> operation, final T input) {
        return defer(
            () -> {
                final R value = operation.apply(input);
                return just(value);
            }
        );
    }

    public static void startTest(final String name) {
        startSection(name);
        startTiming();
    }

    public static void startSection(final String name) {
        System.out.println("---------------------------------------------------");
        System.out.println(name);
        System.out.println("---------------------------------------------------");
    }

    public static Scheduler schedulerA = Schedulers.from(newFixedThreadPool(10, new CustomisingThreadFactory("Sched-A-%d")));
    public static Scheduler schedulerB = Schedulers.from(newFixedThreadPool(10, new CustomisingThreadFactory("Sched-B-%d")));
    public static Scheduler schedulerC = Schedulers.from(newFixedThreadPool(10, new CustomisingThreadFactory("Sched-C-%d")));

    static class CustomisingThreadFactory implements ThreadFactory {
        public CustomisingThreadFactory(final String pattern) {
            this.name = String.format(pattern, Thread.currentThread().getId());
        }

        @Override
        public Thread newThread(final Runnable r) {

            final Thread thread = new Thread(r, name);
            thread.setDaemon(true);

            return thread;
        }

        private final String name;
    }
}

