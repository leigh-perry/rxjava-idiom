package test;

import rx.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static rx.Observable.from;
import static rx.Observable.range;
import static test.TestUtil.output;
import static test.TestUtil.startTest;

public class FlattenChunkedFetch {
    public static void main(final String[] args) {
        new FlattenChunkedFetch().runAll();
    }

    public void runAll() {
        startTest("chunked LP");
        observeAllPersonsLP()
            .toBlocking()
            .subscribe(p -> output("Person: %s", p));

        startTest("chunked book");
        observeAllPersonsBook()
            .toBlocking()
            .subscribe(p -> output("Person: %s", p));
    }

    private Observable<Person> observeAllPersonsLP() {
        final Person marker = new Person(-1);
        return
            range(0, Integer.MAX_VALUE)
                .flatMap(page ->
                    from(fetchPage(page))
                        .defaultIfEmpty(marker)
                )
                .takeWhile(person -> person != marker);
    }

    private Observable<Person> observeAllPersonsBook() {
        // avoids marker... splits the flatmap into map (fetch) then flatmap (from)
        return
            range(0, Integer.MAX_VALUE)
                .map(this::fetchPage)
                .takeWhile(list -> !list.isEmpty())
                .flatMapIterable(list -> list);
        //.flatMap(Observable::from);
    }

    private static final int PAGE_SIZE = 5;

    List<Person> fetchPage(final int page) {
        return query(
            "SELECT * FROM PEOPLE ORDER BY id LIMIT ? OFFSET ?",
            page * PAGE_SIZE
        );
    }

    private List<Person> query(final String s, final int i) {
        return
            i >= 5 * PAGE_SIZE
                ? new ArrayList<>() :
                IntStream.rangeClosed(i, i + PAGE_SIZE - 1)
                    .boxed()
                    .map(Person::new)
                    .collect(toList());
    }

    static class Person {
        private final Integer id;

        public Person(final Integer j) {
            this.id = j;
        }

        @Override
        public String toString() {
            return "Person{" +
                "id=" + id +
                '}';
        }
    }
}

