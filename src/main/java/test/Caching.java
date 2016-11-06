package test;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static rx.schedulers.Schedulers.io;
import static test.TestUtil.observeSyncTestOperation;
import static test.TestUtil.observeSynchronous;
import static test.TestUtil.output;
import static test.TestUtil.pause;
import static test.TestUtil.syncTestOperation;

public class Caching {
    public static void main(final String[] args) {
        new Caching().runAll();
    }

    public Observable<String> observeCachedSyncTestOperation(final Integer input) {
        return observeSynchronous(cacheSyncTestOperation::getValue, input);
    }

    private final TimeLimitedIndexedCache<Integer, String> cacheSyncTestOperation =
        TimeLimitedIndexedCache.of(Void.class, "name", i -> syncTestOperation(i, "tlic"), 3000);

    public void runAll() {
        final Scheduler scheduler = io();    // io has more threads

        startTest("cache time limited");
        final Observable<String> serviceCachedTimeLimited =
            observeCachedSyncTestOperation(1)
                .subscribeOn(scheduler);

        for (int i = 0; i < 5; ++i) {
            if (i == 3) {
                output("                pause started");
                pause(2000);
                output("                pause done");
            }
            syncCallAndOutput(serviceCachedTimeLimited, i);
        }

        startTest("cache using OnSubscribeInvalidatingCache");
        final OnSubscribeInvalidatingCache<String> cacheLayer =
            new OnSubscribeInvalidatingCache<>(
                observeSyncTestOperation(1, "")
                    .subscribeOn(scheduler)
            );
        final Observable<String> serviceCachedInvalidating =
            Observable.create(cacheLayer);

        for (int i = 0; i < 5; ++i) {
            if (i == 3) {
                cacheLayer.reset();
            }
            syncCallAndOutput(serviceCachedInvalidating, i);
        }

        startTest("cache indefinitely");
        final Observable<String> serviceCached =
            observeSyncTestOperation(1, "")
                .subscribeOn(scheduler)
                .cache();

        runTest(serviceCached);
    }

    public static class OnSubscribeInvalidatingCache<T> implements Observable.OnSubscribe<T> {

        private final AtomicBoolean refresh = new AtomicBoolean(true);
        private final Observable<T> source;
        private volatile Observable<T> current;

        public OnSubscribeInvalidatingCache(final Observable<T> source) {
            this.source = source;
            this.current = source;
        }

        public void reset() {
            refresh.set(true);
        }

        @Override
        public void call(final Subscriber<? super T> subscriber) {
            if (refresh.compareAndSet(true, false)) {
                current = source.cache();
            }
            current.unsafeSubscribe(subscriber);
        }

    }


    private void runTest(final Observable<String> service) {
        for (int i = 0; i < 5; ++i) {
            syncCallAndOutput(service, i);
        }
    }

    private void syncCallAndOutput(final Observable<String> service, final int i) {
        final String result = syncCall(service);
        output("result %s: %s", i, result);
    }

    private String syncCall(final Observable<String> service) {
        return service
            .toBlocking()
            .single();
    }

    private void startTest(final String name) {
        TestUtil.startTest(name);
        startCounting();
    }

    private volatile int count = 0;

    private void startCounting() {
        count = 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // caching impl
    ////////////////////////////////////////////////////////////////////////////////////

    public static class TimeLimitedIndexedCache<A, V> {
        public TimeLimitedIndexedCache(
            final Class<?> usingClass, final String name, final Function<A, V> fetcher,
            final long lifetimeMsec
        ) {
            m_usingClass = usingClass;
            m_name = name;
            m_fetcher = fetcher;
            m_lifetimeMsec = lifetimeMsec;
        }

        public V getValue(final A arg) {
            TimeLimitedLazyValue<V> f = m_dataMap.get(arg);
            if (f == null) {
                f = fetchAndStore(arg);
            }

            return f.get();
        }

        private TimeLimitedLazyValue<V> fetchAndStore(final A arg) {
            final TimeLimitedLazyValue<V> vNew =
                TimeLimitedLazyValue.of(m_usingClass, m_name, () -> m_fetcher.apply(arg), m_lifetimeMsec);

            TimeLimitedLazyValue<V> v = m_dataMap.putIfAbsent(arg, vNew);

            // For first time, vNew will now be stored in the map. Otherwise vNew is discarded.
            if (v == null) {
                v = vNew;
            }

            return v;
        }

        public static <A, V> TimeLimitedIndexedCache<A, V> of(
            final Class<?> usingClass,
            final String name,
            final Function<A, V> f,
            final long lifetimeMsec
        ) {
            return new TimeLimitedIndexedCache<>(usingClass, name, f, lifetimeMsec);
        }

        private final ConcurrentMap<A, TimeLimitedLazyValue<V>> m_dataMap = new ConcurrentHashMap<>();

        /** Function that retrieves values for the specified index */
        private final Function<A, V> m_fetcher;

        private final long m_lifetimeMsec;

        private final String m_name;

        /** Used for instrumentation counters etc */
        private final Class<?> m_usingClass;
    }

    public static class TimeLimitedLazyValue<T> {
        public TimeLimitedLazyValue(
            final Class<?> usingClass, final String name, final Supplier<T> fetcher,
            final long lifetimeMsec
        ) {
            m_lifetimeNs = msecToNs(lifetimeMsec);
            m_fetcher = fetcher;

            final Context context = new Context(new SafeLazyValue<>(fetcher), System.nanoTime() + m_lifetimeNs);
            m_context = new AtomicReference<>(context);
        }

        public T get() {
            reviewContext();

            final Context c = m_context.get();
            final T value = c.m_lazyValue.get();

            return value;
        }

        private void reviewContext() {
            // Lock-free implementation (optimistic 'locking' with retry).
            boolean done;
            boolean isStale;
            do {
                final Context prevContext = m_context.get();

                final long nsExpiry = prevContext.m_systemTimeNsExpiry;
                isStale = !nsIsBefore(System.nanoTime(), nsExpiry);
                if (!isStale) {
                    done = true;
                } else {
                    // Chuck the previous LazyValue (FutureTask) so that it gets reevaluated.
                    final Context newContext =
                        new Context(SafeLazyValue.of(m_fetcher), System.nanoTime() + m_lifetimeNs);

                    done = m_context.compareAndSet(prevContext, newContext);
                }
            }
            while (!done);
        }

        /** Context data, immutable for concurrency reasons */
        public class Context {
            private Context(final SafeLazyValue<T> lazyValue, final long systemTimeNsExpiry) {
                m_lazyValue = lazyValue;
                m_systemTimeNsExpiry = systemTimeNsExpiry;
            }

            public final SafeLazyValue<T> m_lazyValue;
            public final long m_systemTimeNsExpiry;
        }

        public static <T> TimeLimitedLazyValue<T> of(
            final Class<?> usingClass, final String name,
            final Supplier<T> evaluator, final long lifetimeMsec
        ) {
            return new TimeLimitedLazyValue<>(usingClass, name, evaluator, lifetimeMsec);
        }

        /** Gather context in lock-free / immutable manner for max concurrency */
        private final AtomicReference<Context> m_context;
        private final Supplier<T> m_fetcher;
        private final long m_lifetimeNs;
    }

    public static class SafeLazyValue<T> implements Supplier<T> {
        /** Threadsafe lock-free Future<T> implementation */
        private final FutureTask<T> futureTask;

        public SafeLazyValue(final Supplier<? extends T> fetcher) {
            futureTask = new FutureTask<>(fetcher::get);
        }

        @Override
        public T get() {
            // Calculate the value if not already done. Does nothing if already calculated.
            futureTask.run();

            try {
                return futureTask.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        public static <T> SafeLazyValue<T> of(final Supplier<? extends T> fetcher) {
            return new SafeLazyValue<>(fetcher);
        }
    }

    public static boolean nsIsBefore(final long ns, final long nsLimit) {
        // Because of the possibility of numerical overflow, to compare two nanoTime
        // values, use t1 - t2 < 0, not t1 < t2.

        // return ns < nsLimit;
        return ns - nsLimit < 0;
    }

    public static long msecToNs(final long nsElapsed) {
        return nsElapsed * 1000000L;
    }
}



