package cyclops.data;

import com.aol.cyclops2.data.collections.extensions.FluentCollectionX;
import cyclops.collections.tuple.Tuple2;
import cyclops.data.basetests.BaseImmutableListTest;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class SeqTest extends BaseImmutableListTest {
    @Override
    protected <T> Seq<T> fromStream(Stream<T> s) {
        return Seq.fromStream(s);
    }

    @Override
    public <T> Seq<T> empty() {
        return Seq.empty();
    }

    @Override
    public <T> Seq<T> of(T... values) {
        return Seq.of(values);
    }

    @Override
    public Seq<Integer> range(int start, int end) {
        return Seq.range(start,end);
    }

    @Override
    public Seq<Long> rangeLong(long start, long end) {
        return Seq.rangeLong(start,end);
    }

    @Override
    public <T> ImmutableList<T> iterate(int times, T seed, UnaryOperator<T> fn) {
        return Seq.iterate(seed,fn,times);
    }

    @Override
    public <T> Seq<T> generate(int times, Supplier<T> fn) {
        return Seq.generate(fn,times);
    }

    @Override
    public <U, T> Seq<T> unfold(U seed, Function<? super U, Optional<Tuple2<T, U>>> unfolder) {
        return Seq.unfold(seed,unfolder);
    }
}