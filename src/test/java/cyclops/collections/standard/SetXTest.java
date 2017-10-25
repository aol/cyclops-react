package cyclops.collections.standard;

import static java.util.Comparator.comparing;
import static org.hamcrest.Matchers.equalTo;
import static org.jooq.lambda.tuple.Tuple.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;

import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.types.foldable.Evaluation;
import cyclops.collections.AbstractCollectionXTest;
import cyclops.collections.mutable.ListX;
import cyclops.control.Maybe;
import cyclops.stream.Spouts;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.Before;
import org.junit.Test;

import com.aol.cyclops2.data.collections.extensions.FluentCollectionX;
import cyclops.collections.mutable.SetX;

public class SetXTest extends AbstractCollectionXTest {

    @Override
    public <T> FluentCollectionX<T> of(T... values) {
        return SetX.of(values);
    }
    AtomicLong counter = new AtomicLong(0);
    @Before
    public void setup(){

        counter = new AtomicLong(0);
    }
    @Test
    public void asyncTest() throws InterruptedException {
        Spouts.async(Stream.generate(()->"next"), Executors.newFixedThreadPool(1))
                .onePer(1, TimeUnit.MILLISECONDS)
                .take(1000)
                .to()
                .setX(Evaluation.LAZY)
                .peek(i->counter.incrementAndGet())
                .materialize();

        long current = counter.get();
        Thread.sleep(400);
        assertTrue(current<counter.get());
    }

    @Test
    public void testSorted() {

        CollectionX<Tuple2<Integer, String>> t1 = of(tuple(2, "two"), tuple(1, "replaceWith"));

        List<Tuple2<Integer, String>> s1 = t1.sorted().toListX().sorted();
        //System.out.println(s1);
        assertEquals(tuple(1, "replaceWith"), s1.get(0));
        assertEquals(tuple(2, "two"), s1.get(1));

        CollectionX<Tuple2<Integer, String>> t2 = of(tuple(2, "two"), tuple(1, "replaceWith"));
        List<Tuple2<Integer, String>> s2 = t2.sorted(comparing(t -> t.v1())).toListX().sorted();
        assertEquals(tuple(1, "replaceWith"), s2.get(0));
        assertEquals(tuple(2, "two"), s2.get(1));

        CollectionX<Tuple2<Integer, String>> t3 = of(tuple(2, "two"), tuple(1, "replaceWith"));
        List<Tuple2<Integer, String>> s3 = t3.sorted(t -> t.v1()).toListX().sorted();
        assertEquals(tuple(1, "replaceWith"), s3.get(0));
        assertEquals(tuple(2, "two"), s3.get(1));

    }

    @Test
    public void onEmptySwitch() {
        assertThat(SetX.empty()
                       .onEmptySwitch(() -> SetX.of(1, 2, 3)),
                   equalTo(SetX.of(1, 2, 3)));
    }
    @Test
    public void coflatMap(){
       assertThat(SetX.of(1,2,3)
                   .coflatMap(s->s.sumInt(i->i))
                   .singleUnsafe(),equalTo(6));
        
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.aol.cyclops2.function.collections.extensions.AbstractCollectionXTest#
     * zero()
     */
    @Override
    public <T> FluentCollectionX<T> empty() {
        return SetX.empty();
    }

    @Test
    @Override
    public void forEach2() {

        assertThat(of(1, 2, 3).forEach2(a -> Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), (a, b) -> a + b)
                              .toList()
                              .size(),
                   equalTo(12));
    }

    @Override
    public FluentCollectionX<Integer> range(int start, int end) {
        return SetX.range(start, end);
    }

    @Override
    public FluentCollectionX<Long> rangeLong(long start, long end) {
        return SetX.rangeLong(start, end);
    }

    @Override
    public <T> FluentCollectionX<T> iterate(int times, T seed, UnaryOperator<T> fn) {
        return SetX.iterate(times, seed, fn);
    }

    @Override
    public <T> FluentCollectionX<T> generate(int times, Supplier<T> fn) {
        return SetX.generate(times, fn);
    }

    @Override
    public <U, T> FluentCollectionX<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return SetX.unfold(seed, unfolder);
    }

    @Test
    public void testUnfoldRight2() {
        SetX set = SetX.unfoldRight(10, x -> x == 0 ? Optional.ofNullable(null) : Optional.of(Tuple.tuple(x - 1, x)));
        assertEquals(10, set.size());
        for (int i = 0; i < 10; ++i) {
            assertEquals(10 - i, set.get(i).get());
        }
    }

    @Test
    public void testUnfoldRight() {
        SetX set = SetX.unfoldRight(10, x -> x == 0 ? Optional.ofNullable(null) : Optional.of(Tuple.tuple(x - 1, x)));
        for (int i = 0; i < 10; ++i) {
            assertEquals(10 - i, set.get(i).get());
        }
    }
}
