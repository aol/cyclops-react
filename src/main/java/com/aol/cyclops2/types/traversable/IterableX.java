package com.aol.cyclops2.types.traversable;

import com.aol.cyclops2.data.collections.extensions.CollectionX;
import com.aol.cyclops2.types.Filters;
import com.aol.cyclops2.types.Zippable;
import com.aol.cyclops2.types.foldable.ConvertableSequence;
import com.aol.cyclops2.types.foldable.Folds;
import com.aol.cyclops2.types.functor.FilterableTransformable;
import com.aol.cyclops2.types.functor.Transformable;
import com.aol.cyclops2.types.reactive.ReactiveStreamsTerminalOperations;
import cyclops.collections.immutable.VectorX;
import cyclops.collections.mutable.ListX;
import cyclops.collections.mutable.SetX;
import cyclops.collections.tuple.Tuple2;
import cyclops.collections.tuple.Tuple3;
import cyclops.collections.tuple.Tuple4;
import cyclops.companion.Iterables;
import cyclops.companion.Iterables.IterableComprehension;
import cyclops.control.Eval;
import cyclops.async.Future;
import cyclops.control.Trampoline;
import cyclops.function.Function3;
import cyclops.function.Function4;
import cyclops.function.Monoid;
import cyclops.stream.ReactiveSeq;
import cyclops.control.Try;
import com.aol.cyclops2.types.stream.HeadAndTail;
import cyclops.function.Function1;
import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.xml.ws.soap.Addressing;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * Iterable on steroids.
 * Created by johnmcclean on 17/12/2016.
 */
@FunctionalInterface
public interface IterableX<T> extends ExtendedTraversable<T>,
                                                Folds<T>,
                                                Iterable<T>,
                                                ReactiveStreamsTerminalOperations<T> {


    default int size(){
        return (int)count();
    }



    @Override
    default <U> IterableX<U> unitIterator(Iterator<U> U) {
        return ReactiveSeq.fromIterator(U);
    }

    @Override
    default IterableX<T> filter(Predicate<? super T> fn) {
        return stream().filter(fn);
    }

    @Override
    default <R> IterableX<R> map(Function<? super T, ? extends R> fn) {
        return stream().map(fn);
    }

    @Override
    default ReactiveSeq<T> stream() {
        return ReactiveSeq.fromIterator(iterator());
    }



    default ConvertableSequence<T> to(){
        return new ConvertableSequence<>(this);
    }

    default ListX<T> toListX(){
        return to().listX();
    }
    default SetX<T> toSetX(){
        return to().setX();
    }
    /**
     * Perform an async fold on the provided executor
     *
     *  <pre>
     *  {@code
     *    Future<Integer> sum =  ListX.of(1,2,3)
     *                                 .transform(this::load)
     *                                 .foldFuture(exec,list->list.reduce(0,(a,b)->a+b))
     *
     *  }
     *  </pre>
     *
     * Similar to @see {@link ReactiveSeq#futureOperations(Executor)}, but returns Future
     *
     * @param fn Folding function
     * @param ex Executor to perform fold on
     * @return Future that will contain the result when complete
     */
    default <R> Future<R> foldFuture(Executor ex,Function<? super IterableX<T>,? extends R> fn){

        return Future.of(()->fn.apply(this),ex);
    }
    default Future<Void> runFuture(Executor ex, Consumer<? super IterableX<T>> fn){
        return Future.of(()-> { fn.accept(this); return null;},ex);
    }

    /**
     * Perform a maybe caching fold (results are memoized)
     *  <pre>
     *  {@code
     *    Eval<Integer> sum =  ListX.of(1,2,3)
     *                                 .transform(this::load)
     *                                 .foldLazy(list->list.reduce(0,(a,b)->a+b))
     *
     *  }
     *  </pre>
     *
     *  Similar to @see {@link ReactiveSeq#lazyOperations()}, but always returns Eval (e.g. with nested Optionals)
     *
     * @param fn Folding function
     * @return Eval that lazily performs the fold once
     */
    default <R> Eval<R> foldLazy(Function<? super IterableX<T>,? extends R> fn){
        return Eval.later(()->fn.apply(this));
    }
    default Eval<Void> runLazy(Consumer<? super IterableX<T>> fn){
        return Eval.later(()->{ fn.accept(this); return null;});
    }

    /**
     * Try a fold, capturing any unhandling execution exceptions (that fold the provided classes)
     *  <pre>
     *  {@code
     *    Try<Integer,Throwable> sum =  ListX.of(1,2,3)
     *                                       .transform(this::load)
     *                                       .foldLazy(list->list.reduce(0,(a,b)->a+b),IOException.class)
     *
     *  }
     *  </pre>
     * @param fn Folding function
     * @param classes Unhandled Exception types to capture in Try
     * @return Try that eagerly executes the fold and captures specified unhandled exceptions
     */
    default <R, X extends Throwable> Try<R, X> foldTry(Function<? super IterableX<T>,? extends R> fn,
                                                       final Class<X>... classes){
        return Try.catchExceptions(classes).tryThis(()->fn.apply(this));
    }

    default Function1<Long,T> asFunction(){
        return index->this.get(index).orElse(null);
    }






    /**
     * Destructures this Traversable into it's head and tail. If the traversable instance is not a SequenceM or Stream type,
     * whenStream may be more efficient (as it is guaranteed to be maybe).
     *
     * <pre>
     * {@code
     * ListX.of(1,2,3,4,5,6,7,8,9)
    .dropRight(5)
    .plus(10)
    .visit((x,xs) ->
    xs.join(x.>2?"hello":"world")),()->"NIL"
    );
     *
     * }
     * //2world3world4
     *
     * </pre>
     *
     *
     * @param match
     * @return
     */
    default <R> R visit(final BiFunction<? super T, ? super ReactiveSeq<T>, ? extends R> match, final Supplier<? extends R> ifEmpty) {
        final HeadAndTail<T> ht = stream().headAndTail();
        if (ht.isHeadPresent())
            return match.apply(ht.head(), ht.tail());
        return ifEmpty.get();

    }


    /**
     * extract head and tail together, where head is expected to be present
     * Example :
     *
     * <pre>
     * {@code
     *  ReactiveSeq<String> helloWorld = ReactiveSeq.Of("hello","world","last");
    HeadAndTail<String> headAndTail = helloWorld.headAndTail();
    String head = headAndTail.head();

    //head == "hello"

    ReactiveSeq<String> tail =  headAndTail.tail();
    //["world","last]

    }
     *  </pre>
     *
     * @return
     */
    default HeadAndTail<T> headAndTail() {
        return stream().headAndTail();
    }
    @Override
    default <X extends Throwable> Subscription forEachSubscribe(Consumer<? super T> consumer){
        Subscription result = ReactiveStreamsTerminalOperations.super.forEachSubscribe(consumer, e->e.printStackTrace(),()->{});
        return result;
    }

    @Override
    default <X extends Throwable> Subscription forEachSubscribe(Consumer<? super T> consumer, Consumer<? super Throwable> consumerError){
        Subscription result = ReactiveStreamsTerminalOperations.super.forEachSubscribe(consumer,consumerError,()->{});
        return result;
    }

    @Override
    default <X extends Throwable> Subscription forEachSubscribe(Consumer<? super T> consumer, Consumer<? super Throwable> consumerError, Runnable onComplete){
        Subscription result = ReactiveStreamsTerminalOperations.super.forEachSubscribe(consumer,consumerError,onComplete);
        return result;
    }
    @Override
    default <X extends Throwable> Subscription forEach(long numberOfElements, Consumer<? super T> consumer){
        return stream().forEach(numberOfElements,consumer);
    }

    @Override
    default <X extends Throwable> Subscription forEach(long numberOfElements, Consumer<? super T> consumer, Consumer<? super Throwable> consumerError){
        return stream().forEach(numberOfElements,consumer,consumerError);
    }

    @Override
    default <X extends Throwable> Subscription forEach(long numberOfElements, Consumer<? super T> consumer, Consumer<? super Throwable> consumerError, Runnable onComplete){
        return stream().forEach(numberOfElements,consumer,consumerError,onComplete);
    }

    @Override
    default <X extends Throwable> void forEach(Consumer<? super T> consumerElement, Consumer<? super Throwable> consumerError){
        stream().forEach(consumerElement,consumerError);
    }

    @Override
    default <X extends Throwable> void forEach(Consumer<? super T> consumerElement, Consumer<? super Throwable> consumerError, Runnable onComplete){
        stream().forEach(consumerElement, consumerError, onComplete);
    }

    @Override
    default <U> IterableX<U> ofType(final Class<? extends U> type) {
        return (IterableX<U>)ExtendedTraversable.super.ofType(type);
    }

    @Override
    default IterableX<T> filterNot(final Predicate<? super T> predicate) {
        return (IterableX<T>)ExtendedTraversable.super.filterNot(predicate);
    }

    @Override
    default IterableX<T> notNull() {
        return (IterableX<T>)ExtendedTraversable.super.notNull();
    }

    @Override
    default IterableX<T> removeAllS(final Stream<? extends T> stream) {
        return (IterableX<T>)ExtendedTraversable.super.removeAllS(stream);
    }

    @Override
    default IterableX<T> removeAllI(final Iterable<? extends T> it) {
        return (IterableX<T>)ExtendedTraversable.super.removeAllI(it);
    }

    @Override
    default IterableX<T> removeAll(final T... values) {
        return (IterableX<T>)ExtendedTraversable.super.removeAll(values);
    }

    @Override
    default IterableX<T> retainAllI(final Iterable<? extends T> it) {
        return (IterableX<T>)ExtendedTraversable.super.retainAllI(it);
    }

    @Override
    default IterableX<T> retainAllS(final Stream<? extends T> stream) {
        return (IterableX<T>)ExtendedTraversable.super.retainAllS(stream);
    }

    @Override
    default IterableX<T> retainAll(final T... values) {
        return (IterableX<T>)ExtendedTraversable.super.retainAll(values);
    }

    @Override
    default IterableX<ReactiveSeq<T>> permutations() {
        return (IterableX<ReactiveSeq<T>>)ExtendedTraversable.super.permutations();
    }

    @Override
    default IterableX<ReactiveSeq<T>> combinations(final int size) {
        return (IterableX<ReactiveSeq<T>>)ExtendedTraversable.super.combinations(size);
    }

    @Override
    default IterableX<ReactiveSeq<T>> combinations() {
        return (IterableX<ReactiveSeq<T>>)ExtendedTraversable.super.combinations();
    }

    @Override
    default IterableX<T> zip(BinaryOperator<Zippable<T>> combiner, final Zippable<T> app) {
        return (IterableX<T>)ExtendedTraversable.super.zip(combiner,app);
    }

    @Override
    default <R> IterableX<R> zipWith(Iterable<Function<? super T, ? extends R>> fn) {
        return (IterableX<R>)ExtendedTraversable.super.zipWith(fn);
    }

    @Override
    default <R> IterableX<R> zipWithS(Stream<Function<? super T, ? extends R>> fn) {
        return (IterableX<R>)ExtendedTraversable.super.zipWithS(fn);
    }

    @Override
    default <R> IterableX<R> zipWithP(Publisher<Function<? super T, ? extends R>> fn) {
        return (IterableX<R>)ExtendedTraversable.super.zipWithP(fn);
    }

    @Override
    default <T2, R> IterableX<R> zipP(final Publisher<? extends T2> publisher, final BiFunction<? super T, ? super T2, ? extends R> fn) {
        return (IterableX<R>)ExtendedTraversable.super.zipP(publisher,fn);
    }

    @Override
    default <U, R> IterableX<R> zipS(final Stream<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {
        return (IterableX<R>)ExtendedTraversable.super.zipS(other,zipper);
    }

    @Override
    default <U> IterableX<Tuple2<T, U>> zipP(final Publisher<? extends U> other) {
        return (IterableX)ExtendedTraversable.super.zipP(other);
    }

    @Override
    default <U> IterableX<Tuple2<T, U>> zip(final Iterable<? extends U> other) {
        return (IterableX)ExtendedTraversable.super.zip(other);
    }

    @Override
    default <S, U, R> IterableX<R> zip3(final Iterable<? extends S> second, final Iterable<? extends U> third, final Function3<? super T, ? super S, ? super U, ? extends R> fn3) {
        return (IterableX)ExtendedTraversable.super.zip3(second,third,fn3);
    }

    @Override
    default <T2, T3, T4, R> IterableX<R> zip4(final Iterable<? extends T2> second, final Iterable<? extends T3> third, final Iterable<? extends T4> fourth, final Function4<? super T, ? super T2, ? super T3, ? super T4, ? extends R> fn) {
        return (IterableX)ExtendedTraversable.super.zip4(second,third,fourth,fn);
    }


    @Override
    default IterableX<T> combine(final BiPredicate<? super T, ? super T> predicate, final BinaryOperator<T> op) {
        return (IterableX)ExtendedTraversable.super.combine(predicate,op);
    }

    @Override
    default IterableX<T> combine(final Monoid<T> op, final BiPredicate<? super T, ? super T> predicate) {
        return (IterableX)ExtendedTraversable.super.combine(op,predicate);
    }

    @Override
    default IterableX<T> cycle(final long times) {
        return (IterableX)ExtendedTraversable.super.cycle(times);
    }

    @Override
    default IterableX<T> cycle(final Monoid<T> m, final long times) {
        return (IterableX)ExtendedTraversable.super.cycle(m,times);
    }

    @Override
    default IterableX<T> cycleWhile(final Predicate<? super T> predicate) {
        return (IterableX)ExtendedTraversable.super.cycleWhile(predicate);
    }

    @Override
    default IterableX<T> cycleUntil(final Predicate<? super T> predicate) {
        return (IterableX)ExtendedTraversable.super.cycleUntil(predicate);
    }

    @Override
    default <U, R> IterableX<R> zip(final Iterable<? extends U> other, final BiFunction<? super T, ? super U, ? extends R> zipper) {
        return (IterableX<R>)ExtendedTraversable.super.zip(other,zipper);
    }

    @Override
    default <S, U> IterableX<Tuple3<T, S, U>> zip3(final Iterable<? extends S> second, final Iterable<? extends U> third) {
        return (IterableX)ExtendedTraversable.super.zip3(second,third);
    }

    @Override
    default <T2, T3, T4> IterableX<Tuple4<T, T2, T3, T4>> zip4(final Iterable<? extends T2> second, final Iterable<? extends T3> third, final Iterable<? extends T4> fourth) {
        return (IterableX)ExtendedTraversable.super.zip4(second,third,fourth);
    }

    @Override
    default IterableX<Tuple2<T, Long>> zipWithIndex() {
        return (IterableX)ExtendedTraversable.super.zipWithIndex();
    }

    @Override
    default IterableX<VectorX<T>> sliding(final int windowSize) {
        return (IterableX<VectorX<T>>)ExtendedTraversable.super.sliding(windowSize);
    }

    @Override
    default IterableX<VectorX<T>> sliding(final int windowSize, final int increment) {
        return (IterableX<VectorX<T>>)ExtendedTraversable.super.sliding(windowSize,increment);
    }

    @Override
    default <C extends Collection<? super T>> IterableX<C> grouped(final int size, final Supplier<C> supplier) {
        return (IterableX<C>)ExtendedTraversable.super.grouped(size,supplier);
    }

    @Override
    default IterableX<ListX<T>> groupedUntil(final Predicate<? super T> predicate) {
        return (IterableX<ListX<T>>)ExtendedTraversable.super.groupedUntil(predicate);
    }

    @Override
    default IterableX<ListX<T>> groupedStatefullyUntil(final BiPredicate<ListX<? super T>, ? super T> predicate) {
        return (IterableX<ListX<T>>)ExtendedTraversable.super.groupedStatefullyUntil(predicate);
    }

    @Override
    default <U> IterableX<Tuple2<T, U>> zipS(final Stream<? extends U> other) {
        return (IterableX)ExtendedTraversable.super.zipS(other);
    }

    @Override
    default IterableX<ListX<T>> groupedWhile(final Predicate<? super T> predicate) {
        return (IterableX<ListX<T>>)ExtendedTraversable.super.groupedWhile(predicate);
    }

    @Override
    default <C extends Collection<? super T>> IterableX<C> groupedWhile(final Predicate<? super T> predicate, final Supplier<C> factory) {
        return (IterableX<C>)ExtendedTraversable.super.groupedWhile(predicate,factory);
    }

    @Override
    default <C extends Collection<? super T>> IterableX<C> groupedUntil(final Predicate<? super T> predicate, final Supplier<C> factory) {
        return (IterableX<C>)ExtendedTraversable.super.groupedUntil(predicate,factory);
    }

    @Override
    default IterableX<ListX<T>> grouped(final int groupSize) {
        return (IterableX<ListX<T>>)ExtendedTraversable.super.grouped(groupSize);
    }

    @Override
    default IterableX<T> distinct() {
        return (IterableX<T>)ExtendedTraversable.super.distinct();
    }

    @Override
    default IterableX<T> scanLeft(final Monoid<T> monoid) {
        return (IterableX<T>)ExtendedTraversable.super.scanLeft(monoid);
    }

    @Override
    default <U> IterableX<U> scanLeft(final U seed, final BiFunction<? super U, ? super T, ? extends U> function) {
        return (IterableX<U>)ExtendedTraversable.super.scanLeft(seed,function);
    }

    @Override
    default IterableX<T> scanRight(final Monoid<T> monoid) {
        return (IterableX<T>)ExtendedTraversable.super.scanRight(monoid);
    }

    @Override
    default <U> IterableX<U> scanRight(final U identity, final BiFunction<? super T, ? super U, ? extends U> combiner) {
        return (IterableX<U>)ExtendedTraversable.super.scanRight(identity,combiner);
    }

    @Override
    default IterableX<T> sorted() {
        return (IterableX<T>)ExtendedTraversable.super.sorted();
    }

    @Override
    default IterableX<T> sorted(final Comparator<? super T> c) {
        return (IterableX<T>)ExtendedTraversable.super.sorted(c);
    }

    @Override
    default IterableX<T> takeWhile(final Predicate<? super T> p) {
        return (IterableX<T>)ExtendedTraversable.super.takeWhile(p);
    }

    @Override
    default IterableX<T> dropWhile(final Predicate<? super T> p) {
        return (IterableX<T>)ExtendedTraversable.super.dropWhile(p);
    }

    @Override
    default IterableX<T> takeUntil(final Predicate<? super T> p) {
        return (IterableX<T>)ExtendedTraversable.super.takeUntil(p);
    }

    @Override
    default IterableX<T> dropUntil(final Predicate<? super T> p) {
        return (IterableX<T>)ExtendedTraversable.super.dropUntil(p);
    }

    @Override
    default IterableX<T> dropRight(final int num) {
        return (IterableX<T>)ExtendedTraversable.super.dropRight(num);
    }

    @Override
    default IterableX<T> takeRight(final int num) {
        return (IterableX<T>)ExtendedTraversable.super.takeRight(num);
    }

    @Override
    default IterableX<T> drop(final long num) {
        return (IterableX<T>)ExtendedTraversable.super.drop(num);
    }

    @Override
    default IterableX<T> skip(final long num) {
        return (IterableX<T>)ExtendedTraversable.super.skip(num);
    }

    @Override
    default IterableX<T> skipWhile(final Predicate<? super T> p) {
        return (IterableX<T>)ExtendedTraversable.super.skipWhile(p);
    }

    @Override
    default IterableX<T> skipUntil(final Predicate<? super T> p) {
        return (IterableX<T>)ExtendedTraversable.super.skipUntil(p);
    }

    @Override
    default IterableX<T> take(final long num) {
        return (IterableX<T>)ExtendedTraversable.super.take(num);
    }

    @Override
    default IterableX<T> limit(final long num) {
        return (IterableX<T>)ExtendedTraversable.super.limit(num);
    }

    @Override
    default IterableX<T> limitWhile(final Predicate<? super T> p) {
        return (IterableX<T>)ExtendedTraversable.super.limitWhile(p);
    }

    @Override
    default IterableX<T> limitUntil(final Predicate<? super T> p) {
        return (IterableX<T>)ExtendedTraversable.super.limitUntil(p);
    }

    @Override
    default IterableX<T> intersperse(final T value) {
        return (IterableX<T>)ExtendedTraversable.super.intersperse(value);
    }

    @Override
    default IterableX<T> reverse() {
        return (IterableX<T>)ExtendedTraversable.super.reverse();
    }

    @Override
    default IterableX<T> shuffle() {
        return (IterableX<T>)ExtendedTraversable.super.shuffle();
    }

    @Override
    default IterableX<T> skipLast(final int num) {
        return (IterableX<T>)ExtendedTraversable.super.skipLast(num);
    }

    @Override
    default IterableX<T> limitLast(final int num) {
        return (IterableX<T>)ExtendedTraversable.super.limitLast(num);
    }

    @Override
    default IterableX<T> onEmpty(final T value) {
        return (IterableX<T>)ExtendedTraversable.super.onEmpty(value);
    }

    @Override
    default IterableX<T> onEmptyGet(final Supplier<? extends T> supplier) {
        return (IterableX<T>)ExtendedTraversable.super.onEmptyGet(supplier);
    }

    @Override
    default <X extends Throwable> IterableX<T> onEmptyThrow(final Supplier<? extends X> supplier) {
        return (IterableX<T>)ExtendedTraversable.super.onEmptyThrow(supplier);
    }

    @Override
    default IterableX<T> shuffle(final Random random) {
        return (IterableX<T>)ExtendedTraversable.super.shuffle(random);
    }

    @Override
    default IterableX<T> slice(final long from, final long to) {
        return (IterableX<T>) ExtendedTraversable.super.slice(from,to);
    }

    @Override
    default <U extends Comparable<? super U>> IterableX<T> sorted(final Function<? super T, ? extends U> function) {
        return (IterableX<T>)ExtendedTraversable.super.sorted(function);
    }


    @Override
    default IterableX<T> prependS(Stream<? extends T> stream) {
        return (IterableX<T>)ExtendedTraversable.super.prependS(stream);
    }
    //@TODO
    default IterableX<T> plusAll(Collection<? extends T> list){
        IterableX<T> res = this;
        for(T next : list){
            res = append(next);
        }
        return res;
    }
    //@TODO
    default IterableX<T> plus(T value){
        return append(value);
    }
/**
    //@TODO
    default IterableX<T> minus(Object value){
        return null;
    }
    //@TODO
    default IterableX<T> minusAll(Collection<?> value){
        return null;
    }
**/
    @Override
    default IterableX<T> append(T... values) {
        return (IterableX<T>)ExtendedTraversable.super.append(values);
    }

    @Override
    default IterableX<T> append(T value) {
        return (IterableX<T>)ExtendedTraversable.super.append(value);
    }

    @Override
    default IterableX<T> prepend(T value) {
        return (IterableX<T>)ExtendedTraversable.super.prepend(value);
    }

    @Override
    default IterableX<T> prepend(T... values) {
        return (IterableX<T>)ExtendedTraversable.super.prepend(values);
    }

    @Override
    default IterableX<T> insertAt(int pos, T... values) {
        return (IterableX<T>)ExtendedTraversable.super.insertAt(pos,values);
    }

    @Override
    default IterableX<T> deleteBetween(int start, int end) {
        return (IterableX<T>)ExtendedTraversable.super.deleteBetween(start,end);
    }

    @Override
    default IterableX<T> insertAtS(int pos, Stream<T> stream) {
        return (IterableX<T>)ExtendedTraversable.super.insertAtS(pos,stream);
    }

    @Override
    default IterableX<T> recover(final Function<? super Throwable, ? extends T> fn) {
        return (IterableX<T>)ExtendedTraversable.super.recover(fn);
    }

    @Override
    default <EX extends Throwable> IterableX<T> recover(Class<EX> exceptionClass, final Function<? super EX, ? extends T> fn) {
        return (IterableX<T>)ExtendedTraversable.super.recover(exceptionClass,fn);
    }

    @Override
    default <U> IterableX<U> cast(final Class<? extends U> type) {
        return (IterableX<U>)ExtendedTraversable.super.cast(type);
    }

    @Override
    default IterableX<T> peek(final Consumer<? super T> c) {
        return (IterableX<T>)ExtendedTraversable.super.peek(c);
    }

    @Override
    default <R> IterableX<R> trampoline(final Function<? super T, ? extends Trampoline<? extends R>> mapper) {
        return (IterableX<R>)ExtendedTraversable.super.trampoline(mapper);
    }

    @Override
    default <R> IterableX<R> retry(final Function<? super T, ? extends R> fn) {
        return (IterableX<R>)ExtendedTraversable.super.retry(fn);
    }

    @Override
    default <R> IterableX<R> retry(final Function<? super T, ? extends R> fn, final int retries, final long delay, final TimeUnit timeUnit) {
        return (IterableX<R>)ExtendedTraversable.super.retry(fn,retries,delay,timeUnit);
    }

    /**
     * Perform a flatMap operation on this IterableX. Results from the returned Iterables (from the
     * provided transformation function) are flattened into the resulting toX.
     *
     * @param mapper Transformation function to be applied (and flattened)
     * @return A toX containing the flattened results of the transformation function
     */
    default <R> IterableX<R> concatMap(Function<? super T, ? extends Iterable<? extends R>> mapper){
        return stream().flatMapI(mapper);
    }
/**
    default IterableComprehension<T> comprehension(){
        return new IterableComprehension<>(this);
    }
**/







}