package cyclops.typeclasses;


import com.aol.cyclops2.hkt.Higher;
import com.aol.cyclops2.hkt.Higher2;
import com.aol.cyclops2.hkt.Higher3;
import com.aol.cyclops2.types.Filters;
import com.aol.cyclops2.types.foldable.To;
import com.aol.cyclops2.types.functor.Transformable;
import cyclops.collections.mutable.ListX;
import cyclops.companion.Monoids;
import cyclops.control.*;
import cyclops.function.Fn3;
import cyclops.function.Fn4;
import cyclops.function.Monoid;
import cyclops.monads.Witness;
import cyclops.monads.Witness.*;
import cyclops.stream.ReactiveSeq;
import cyclops.typeclasses.comonad.Comonad;
import cyclops.typeclasses.foldable.Foldable;
import cyclops.typeclasses.foldable.Unfoldable;
import cyclops.typeclasses.functions.MonoidK;
import cyclops.typeclasses.functions.SemigroupK;
import cyclops.typeclasses.functor.BiFunctor;
import cyclops.typeclasses.functor.Functor;
import cyclops.typeclasses.monad.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

import static org.jooq.lambda.tuple.Tuple.tuple;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of="run")
@Getter
public class Product<W1,W2,T> implements  Filters<T>,
                                            Higher3<product,W1,W2,T>,
                                            Transformable<T>,
                                            To<Product<W1,W2,T>> {

    private final Tuple2<Higher<W1,T>,Higher<W2,T>> run;
    private final InstanceDefinitions<W1> def1;
    private final InstanceDefinitions<W2> def2;


    public Tuple2<Higher<W1,T>,Higher<W2,T>> asTuple(){
        return run;
    }
    public Tuple2<Active<W1,T>,Active<W2,T>> asActiveTuple(){
        return run.map((a,b)->Tuple.tuple(Active.of(a,def1),Active.of(b,def2)));
    }

    public Active<W1,T> v1(){
        return Active.of(run.v1,def1);
    }
    public Active<W2,T> v2(){
        return Active.of(run.v2,def2);
    }

    public static  <W1,W2,T> Product<W1,W2,T> of(Tuple2<Higher<W1,T>,
            Higher<W2,T>> run, InstanceDefinitions<W1> def1, InstanceDefinitions<W2> def2){
        return new Product<>(run,def1,def2);
    }
    public static  <W1,W2,T> Product<W1,W2,T> of(Active<W1,T> a1, Active<W2,T> a2){
        return of(Tuple.tuple(a1.getSingle(),a2.getSingle()),a1.getDef1(),a2.getDef1());
    }
    public Coproduct<W1,W2,T> coproduct(BiPredicate<Higher<W1,T>,Higher<W2,T>> test){
        return test.test(run.v1,run.v2) ?Coproduct.left(run.v1,def1,def2) : Coproduct.right(run.v2,def1,def2);
    }
    public Product<W1,W2,T> filter(Predicate<? super T> test) {
        return of(run.map((m1,m2)->{
            Higher<W2, T> x2 = def2.monadZero().visit(p->p.filter(test,m2),()->m2);
            Higher<W1,T> x1 = def1.monadZero().visit(p->p.filter(test,m1),()->m1);
            return Tuple.tuple(x1, x2);
        }),def1,def2);
    }


    @Override
    public <U> Product<W1,W2,U> ofType(Class<? extends U> type) {
        return (Product<W1,W2,U>)Filters.super.ofType(type);
    }

    @Override
    public Product<W1,W2,T> filterNot(Predicate<? super T> predicate) {
        return filter(predicate.negate());
    }

    @Override
    public Product<W1,W2,T> notNull() {
        return (Product<W1,W2,T>)Filters.super.notNull();
    }

    @Override
    public <U> Product<W1,W2,U> cast(Class<? extends U> type) {
        return (Product<W1,W2,U>)Transformable.super.cast(type);
    }
    public <W2,T2,R> Active<W1,R> zipWithSecond(BiFunction<? super T,? super Maybe<T>,? extends R> f) {
        return Active.of(def1.traverse().zipWith(def2.foldable(),f,run.v1,run.v2),def1);
    }
    public Product<W1,W2,Tuple2<T,T>> zip(Product<W1,W2,T> p2){
        return zip(p2,Tuple::tuple);
    }
    public Product<W1,W2,Tuple3<T,T,T>> zip(Product<W1,W2,T> p2, Product<W1,W2,T> p3){
        return zip(p2,p3);
    }
    public <R> Product<W1,W2,R> zip(Product<W1,W2,T> p2, BiFunction<? super T,? super T, ? extends R> zipper){
        Active<W1,T> a1 = Active.of(run.v1,def1);
        Active<W1,T> a2 = Active.of(p2.run.v1,def1);

        Active<W1,R> a3 = a1.zip(a2,zipper);

        Active<W2,T> b1 = Active.of(run.v2,def2);
        Active<W2,T> b2 = Active.of(p2.run.v2,def2);

        Active<W2,R> b3 = b1.zip(b2,zipper);

        return of(a3,b3);

    }
    public <R> Product<W1,W2,R> zip(Product<W1,W2,T> p2, Product<W1,W2,T> p3, Fn3<? super T,? super T, ? super T,? extends R> zipper){
        Active<W1,T> a1 = Active.of(run.v1,def1);
        Active<W1,T> a2 = Active.of(p2.run.v1,def1);
        Active<W1,T> a3 = Active.of(p3.run.v1,def1);

        Active<W1,R> a4 = a1.zip(a2,a3,zipper);

        Active<W2,T> b1 = Active.of(run.v2,def2);
        Active<W2,T> b2 = Active.of(p2.run.v2,def2);
        Active<W2,T> b3 = Active.of(p3.run.v2,def2);

        Active<W2,R> b4 = b1.zip(b2,b3,zipper);

        return of(a4,b4);
    }
    public <R> Product<W1,W2,R> mapWithIndex(BiFunction<? super T,Long,? extends R> f) {
        return of(Tuple.tuple(def1.traverse().mapWithIndex(f,run.v1),def2.traverse().mapWithIndex(f,run.v2)),def1,def2);
    }
    public <R> Product<W1,W2,Tuple2<T,Long>> zipWithIndex() {
        return mapWithIndex(Tuple::tuple);
    }
    @Override
    public <R> Product<W1,W2,R> map(Function<? super T, ? extends R> fn) {

        return of(run.map((m1,m2)->{
            Higher<W2, R> x2 = def2.<T, R>functor().map(fn, m2);
            Higher<W1,R> x1 = def1.<T, R>functor().map(fn, m1);
            return Tuple.tuple(x1, x2);
        }),def1,def2);
    }
    public <R> Product<W1,W2,R> flatMap(Function<? super T, ? extends Product<W1,W2,R>> fn) {
        return of(map(fn).run.map((m1,m2)->Tuple.tuple(def1.monad().flatMap(p->p.asTuple().v1,m1),def2.monad().flatMap(p->p.asTuple().v2,m2))),def1,def2);
    }


    @Override
    public Product<W1,W2,T> peek(Consumer<? super T> c) {
        return map(a->{
            c.accept(a);
            return a;
        });
    }

    @Override
    public String toString() {
        return "Coproduct["+ run.toString()+"]";
    }

    @Override
    public <R> Product<W1,W2,R> trampoline(Function<? super T, ? extends Trampoline<? extends R>> mapper) {
        return (Product<W1,W2,R>)Transformable.super.trampoline(mapper);
    }

    @Override
    public <R> Product<W1,W2,R> retry(Function<? super T, ? extends R> fn) {
        return (Product<W1,W2,R>)Transformable.super.retry(fn);
    }

    @Override
    public <R> Product<W1,W2,R> retry(Function<? super T, ? extends R> fn, int retries, long delay, TimeUnit timeUnit) {
        return (Product<W1,W2,R>)Transformable.super.retry(fn,retries,delay,timeUnit);
    }

    public <R> Active<W1, R> tailRec1(T initial,Function<? super T,? extends Higher<W1, ? extends Xor<T, R>>> fn){
        return asActiveTuple().v1.tailRec(initial, fn);
    }

    public <R> Active<W2, R> tailRec2(T initial,Function<? super T,? extends Higher<W2, ? extends Xor<T, R>>> fn){
        return asActiveTuple().v2.tailRec(initial, fn);
    }


    public Active<W1,T> activeFirst(SemigroupK<W1,T> sg, Higher<W1,T> concat){
        return Active.of(sg.apply(run.v1,concat),def1);
    }
    public Active<W2,T> activeSecond(SemigroupK<W2,T> sg, Higher<W2,T> concat){
        return Active.of(sg.apply(run.v2,concat),def2);
    }

    public <R> R visit(BiFunction<? super Higher<W1,? super T>,? super Higher<W2,? super T>, ? extends R> visitor){
        return run.map(visitor);
    }
    public <R> R visitA(BiFunction<? super Active<W1,? super T>,? super Active<W2,? super T>, ? extends R> visitor){
        return run.map((a,b)->visitor.apply(Active.of(a,def1),Active.of(b,def2)));
    }
    public Product<W1,W2,T> plusFirst(SemigroupK<W1,T> semigroupK, Higher<W1,T> add){
        return of(Tuple.tuple(semigroupK.apply(run.v1,add),run.v2),def1,def2);
    }
    public Product<W1,W2,T> plusSecond(SemigroupK<W2,T> semigroupK, Higher<W2,T> add){
        return of(Tuple.tuple(run.v1,semigroupK.apply(run.v2,add)),def1,def2);
    }
    public Product<W2,W1,T> swap(){
        return of(run.swap(),def2,def1);
    }

    public Unfolds unfoldsDefault(){
        return new Unfolds(def1.unfoldable().visit(p->p,()->new Unfoldable.UnsafeValueUnfoldable<>()),
                def2.unfoldable().visit(p->p,()->new Unfoldable.UnsafeValueUnfoldable<>()));
    }

    public Maybe<Unfolds> unfolds(){
        if(def1.unfoldable().isPresent() && def2.unfoldable().isPresent())
            return Maybe.just(new Unfolds(def1.unfoldable().get(),def2.unfoldable().get()));
        return Maybe.none();
    }
    public Plus plusUnsafe(){
        return new Plus(def1.monadPlus().get(),def2.monadPlus().get());
    }
    public Maybe<Plus> plus(){
        if(def1.monadPlus().isPresent() && def2.monadPlus().isPresent())
            return Maybe.just(new Plus(def1.monadPlus().get(),def2.monadPlus().get()));
        return Maybe.none();
    }
    @AllArgsConstructor
    public  class Unfolds {

        private final Unfoldable<W1> unf1;
        private final Unfoldable<W2> unf2;

        public <R, T> Product<W1,W2, R> unfold(T b, Function<? super T, Optional<Tuple2<R, T>>> fn){
            Tuple2<Higher<W1, R>, Higher<W2, R>> res = run.map((left, right) -> Tuple.tuple(unf1.unfold(b, fn), unf2.unfold(b, fn)));
            return Product.of(res, def1, def2);
        }

        public <T> Product<W1,W2,T> replicate(int n, T value) {
            return unfold(n,i -> Optional.of(tuple(value, i-1)));
        }

        public <R> Product<W1,W2,R> none() {
            return unfold((T) null, t -> Optional.<Tuple2<R, T>>empty());
        }
        public <T> Product<W1,W2,T> one(T a) {
            return replicate(1, a);
        }
    }
    @AllArgsConstructor
    public class Plus{
        private final MonadPlus<W1> plus1;
        private final MonadPlus<W2> plus2;

        public Product<W1,W2,T> plus(Product<W1,W2,T> a){
            Active<W1, T> r1 = Active.of(run.v1, def1).plus(plus1).plusA(a.v1());
            Active<W2, T> r2 = Active.of(run.v2, def2).plus(plus2).plusA(a.v2());
            return of(r1,r2);
        }
        public Product<W1,W2,T> sum(ListX<Product<W1,W2,T>> list){

            Active<W1, T> r1 = Active.of(run.v1, def1).plus(plus1).sumA(list.map(p->p.asActiveTuple().v1));
            Active<W2, T> r2 = Active.of(run.v2, def2).plus(plus2).sumA(list.map(p->p.asActiveTuple().v2));
            return of(r1,r2);
        }

    }
    public <R> R foldMap(final Monoid<R> mb, final Function<? super T,? extends R> fn) {
        return run.map((a, b) -> {
            R r1 = def1.foldable().foldMap(mb, fn, a);
            R r2 = def2.foldable().foldMap(mb, fn, b);
            return mb.foldRightI(Arrays.asList(r2, r1));
        });
    }
    public T foldRight(Monoid<T> monoid) {
        return run.map((a, b) -> {
            T r1 = def1.foldable().foldRight(monoid, a);
            T r2 = def2.foldable().foldRight(monoid, b);
            return monoid.foldRightI(Arrays.asList(r2, r1));
        });

    }

    public T foldRight(T identity, BinaryOperator<T> semigroup) {
        return foldRight(Monoid.fromBiFunction(identity, semigroup));

    }

    public  Tuple2<ListX<T>,ListX<T>> toListX(){
        return run.map((a,b)->Tuple.tuple(def1.foldable().listX(a),
                def2.foldable().listX(b)));
    }
    public  ListX<T> toListXBoth(){
        return toListX().map((a,b)->a.plusAll(b));
    }
    public Tuple2<ReactiveSeq<T>,ReactiveSeq<T>> stream(){
        return toListX().map((a,b)->Tuple.tuple(a.stream(),b.stream()));
    }
    public ReactiveSeq<T> streamBoth(){
        return stream().map((a,b)->a.appendS(b));
    }

    public Product<W1,W2,T> reverse(){
        return Product.of(run.map((a,b)->Tuple.tuple(def1.traverse().reverse(a),def2.traverse().reverse(b))),def1,def2);
    }
    public  Tuple2<Long,Long> size() {
        return run.map((a,b)->Tuple.tuple(def1.foldable().size(a),
                def2.foldable().size(b)));
    }
    public long totalSize() {
        return size().map((a,b)->a+b);
    }

    public T foldLeft(Monoid<T> monoid) {
        return run.map((a, b) -> {
            T r1 = def1.foldable().foldRight(monoid, a);
            T r2 = def2.foldable().foldRight(monoid, b);
            return monoid.foldLeftI(Arrays.asList(r1, r2));
        });
    }

    public T foldLeft(T identity, BinaryOperator<T> semigroup) {
        return foldLeft(Monoid.fromBiFunction(identity, semigroup));
    }
    public <R> Tuple2<R,R> foldMapTuple(final Monoid<R> mb, final Function<? super T,? extends R> fn) {
        return run.map((a, b) -> {
            R r1 = def1.foldable().foldMap(mb, fn, a);
            R r2 = def2.foldable().foldMap(mb, fn, b);
            return Tuple.tuple(r2, r1);
        });
    }
    public Tuple2<T,T> foldRightTuple(Monoid<T> monoid) {
        return run.map((a, b) -> {
            T r1 = def1.foldable().foldRight(monoid, a);
            T r2 = def2.foldable().foldRight(monoid, b);
            return Tuple.tuple(r2, r1);
        });

    }

    public Tuple2<T,T> foldRightTuple(T identity, BinaryOperator<T> semigroup) {
        return foldRightTuple(Monoid.fromBiFunction(identity, semigroup));

    }

    public Tuple2<T,T> foldLeftTuple(Monoid<T> monoid) {
        return run.map((a, b) -> {
            T r1 = def1.foldable().foldRight(monoid, a);
            T r2 = def2.foldable().foldRight(monoid, b);
            return Tuple.tuple(r1, r2);
        });
    }

    public Tuple2<T,T> foldLeftTuple(T identity, BinaryOperator<T> semigroup) {
        return foldLeftTuple(Monoid.fromBiFunction(identity, semigroup));
    }


    public <R1, R> Product<W1,W2,R> forEach2(Function<? super T, ? extends Product<W1,W2,R1>> value1, final BiFunction<? super T, ? super R1, ? extends R> yieldingFunction) {
        return flatMap(a->{
            return value1.apply(a).map(b->yieldingFunction.apply(a,b));
        });

    }
    public <T2, R1, R2, R> Product<W1,W2,R> forEach3(final Function<? super T, ? extends Product<W1,W2,R1>> value1, final BiFunction<? super T, ? super R1, ? extends Product<W1,W2,R2>> value2,
                                                     final Fn3<? super T, ? super R1, ? super R2, ? extends R> yieldingFunction) {
        return flatMap(a->{
            return value1.apply(a).flatMap(b->value2.apply(a,b).map(c->yieldingFunction.apply(a,b,c)));
        });
    }
    public <T2, R1, R2, R3, R> Product<W1,W2,R> forEach4(final Function<? super T, ? extends Product<W1,W2,R1>> value1, final BiFunction<? super T, ? super R1, ? extends Product<W1,W2,R2>> value2, final Fn3<? super T, ? super R1, ? super R2, ? extends Product<W1,W2,R3>> value3,
                                                         final Fn4<? super T, ? super R1, ? super R2, ? super R3, ? extends R> yieldingFunction) {
        return flatMap(a->{
            return value1.apply(a).flatMap(b->value2.apply(a,b).flatMap(c->value3.apply(a,b,c).map(d->yieldingFunction.apply(a,b,c,d))));
        });
    }

    public static <W1,W2,T> Product<W1,W2,T> narrowK(Higher<Higher<Higher<product, W1>, W2>, T> ds){
        return (Product<W1,W2,T>)ds;
    }


    public  Active<Higher<Higher<product,W1>,W2>,T> allTypeClasses(){
        return  Active.of(this, Instances.<W1, W2>definitions(def1, def2));
    }

    public <C2, R> Higher<C2, Product<W1,W2,R>> traverseA(Applicative<C2> applicative, Function<? super T, ? extends Higher<C2, R>> fn){
        return traverseA(applicative,fn,this);

    }

    public static <W1,W2,T,C2, R> Higher<C2, Product<W1,W2,R>> traverseA(Applicative<C2> applicative, Function<? super T, ? extends Higher<C2, R>> fn,Product<W1,W2,T> n){

        Higher<C2, Active<W1, R>> v1 = n.v1().traverseA(applicative, fn);

        Higher<C2, Active<W2, R>> v2 = n.v2().traverseA(applicative, fn);


        return applicative.zip(v1,v2,(a,b)->a.concat(b));
    }
    public  <C2,T> Higher<C2, Product<W1,W2,T>> sequenceA(Applicative<C2> applicative,
                                                     Product<W1,W2,Higher<C2,T>> ds){
        return traverseA(applicative, i -> i, ds);

    }

    public   <C2, R> Higher<C2, Product<W1,W2,R>> flatTraverseA(Applicative<C2> applicative,
                                                           Function<? super T,? extends Higher<C2, Product<W1,W2, R>>> f) {
        return applicative.map_(traverseA(applicative, f), it->  it.flatMap(a->a));
    }

    public  <C2,T> Higher<C2, Product<W1,W2,T>> flatSequenceA(Applicative<C2> applicative, Product<W1,W2,Higher<C2,Product<W1,W2,T>>> fgfa) {
        return applicative.map(i -> i.flatMap(Function.identity()),sequenceA(applicative, fgfa) );
    }


    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Instances<W1,W2> implements InstanceDefinitions<Higher<Higher<product, W1>, W2>>{
        private final InstanceDefinitions<W1> def1;
        private final InstanceDefinitions<W2> def2;


        public static <W1,W2> InstanceDefinitions<Higher<Higher<product, W1>, W2>> definitions(InstanceDefinitions<W1> def1,InstanceDefinitions<W2> def2){
            return new Instances<>(def1,def2);
        }

        public  Functor<Higher<Higher<product, W1>, W2>> functor(){
            return new Functor<Higher<Higher<product, W1>, W2>>(){

                @Override
                public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> map(Function<? super T, ? extends R> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                    return narrowK(ds).map(fn);
                }
            };
        }
        public  Pure<Higher<Higher<product, W1>, W2>> unit(){
            return new Pure<Higher<Higher<product, W1>, W2>>(){

                @Override
                public <T> Higher<Higher<Higher<product, W1>, W2>, T> unit(T value) {
                    return Product.of(Active.of(def1,value), Active.of(def2, value));

                }
            };
        }

        public  Applicative<Higher<Higher<product, W1>, W2>> applicative(){
            return new Applicative<Higher<Higher<product, W1>, W2>>(){

                @Override
                public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> ap(Higher<Higher<Higher<product, W1>, W2>, ? extends Function<T, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> apply) {
                    return narrowK(fn).flatMap(x -> narrowK(apply).map(x));

                }

                @Override
                public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> map(Function<? super T, ? extends R> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                    return functor().map(fn,ds);
                }

                @Override
                public <T> Higher<Higher<Higher<product, W1>, W2>, T> unit(T value) {
                    return Instances.this.<T>unit().unit(value);
                }
            };
        }
        public Monad<Higher<Higher<product, W1>, W2>> monad(){
            return new Monad<Higher<Higher<product, W1>, W2>>(){

                @Override
                public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> ap(Higher<Higher<Higher<product, W1>, W2>, ? extends Function<T, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> apply) {
                    return applicative().ap(fn,apply);
                }

                @Override
                public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> map(Function<? super T, ? extends R> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                    return functor().map(fn,ds);
                }

                @Override
                public <T> Higher<Higher<Higher<product, W1>, W2>, T> unit(T value) {
                    return Instances.this.<T>unit().unit(value);
                }

                @Override
                public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> flatMap(Function<? super T, ? extends Higher<Higher<Higher<product, W1>, W2>, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                    return narrowK(ds).flatMap(fn.andThen(Product::narrowK));
                }
            };
        }

        @Override
        public <T, R> Maybe<MonadZero<Higher<Higher<product, W1>, W2>>> monadZero() {
            return def1.monadZero().flatMap(x->{
                return def2.monadZero().map(y->{
                return new MonadZero<Higher<Higher<product, W1>, W2>>() {
                    @Override
                    public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> ap(Higher<Higher<Higher<product, W1>, W2>, ? extends Function<T, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> apply) {
                        return applicative().ap(fn,apply);
                    }

                    @Override
                    public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> map(Function<? super T, ? extends R> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                        return functor().map(fn,ds);
                    }

                    @Override
                    public <T> Higher<Higher<Higher<product, W1>, W2>, T> filter(Predicate<? super T> predicate, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                        return narrowK(ds).filter(predicate);
                    }

                    @Override
                    public Higher<Higher<Higher<product, W1>, W2>, ?> zero() {

                        Active ac1 = Active.of( def1.monadZero().get().zero(), def1);
                        Active ac2 = Active.of(def2.monadZero().get().zero(), def2);
                        return Product.of(ac1, ac2);

                    }

                    @Override
                    public <T> Higher<Higher<Higher<product, W1>, W2>, T> unit(T value) {
                        return Instances.this.<T>unit().unit(value);
                    }

                    @Override
                    public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> flatMap(Function<? super T, ? extends Higher<Higher<Higher<product, W1>, W2>, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                        return monad().flatMap(fn,ds);
                    }
                };});
            });
        }
        public  MonadRec<Higher<Higher<product, W1>, W2>> monadRec() {

            return new MonadRec<Higher<Higher<product, W1>, W2>>(){
                @Override
                public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> tailRec(T initial, Function<? super T, ? extends Higher<Higher<Higher<product, W1>, W2>, ? extends Xor<T, R>>> fn) {
                    Product<W1,W2,? extends Xor<T, R>> next[] = new Product[1];
                    Xor<T, R> in = Xor.secondary(initial);

                    next[0] = Product.of(Tuple.tuple(def1.unit().unit(in),def2.unit().unit(in)),def1,def2);
                    boolean cont = true;
                    do {
                        cont = next[0].visit( (a,__) -> {
                            boolean[] internalCont = {true};

                            Higher<W1, ?> b = a;
                            Higher<W1, Boolean> r = def1.functor().map(p -> {
                                Xor<T, R> x = (Xor<T, R>) p;
                                internalCont[0] = internalCont[0] || x.visit(s -> {
                                    next[0] = narrowK(fn.apply(s));
                                    return true;
                                }, pr -> false);
                                return internalCont[0];
                            }, a);
                            return internalCont[0];

                        });
                    } while (cont);
                    return next[0].map(Xor::get);
                }

            };


        }
        @Override
        public <T> Maybe<MonadPlus<Higher<Higher<product, W1>, W2>>> monadPlus() {
            return def1.monadPlus().flatMap(x -> {
                return def2.monadPlus().map(y -> {
                    return new MonadPlus<Higher<Higher<product, W1>, W2>>() {

                        @Override
                        public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> ap(Higher<Higher<Higher<product, W1>, W2>, ? extends Function<T, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> apply) {
                            return applicative().ap(fn, apply);
                        }

                        @Override
                        public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> map(Function<? super T, ? extends R> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                            return functor().map(fn, ds);
                        }

                        @Override
                        public <T> Higher<Higher<Higher<product, W1>, W2>, T> unit(T value) {
                            return Instances.this.<T>unit().unit(value);
                        }

                        @Override
                        public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> flatMap(Function<? super T, ? extends Higher<Higher<Higher<product, W1>, W2>, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                            return monad().flatMap(fn, ds);
                        }

                        @Override
                        public Monoid<Higher<Higher<Higher<product, W1>, W2>, ?>> monoid() {
                            return Monoid.of(monadZero().get().zero(), (a, b) -> {
                                Product<W1, W2, ?> p1 = narrowK(a);
                                Product<W1, W2, ?> p2 = narrowK(b);
                                return p1.zip((Product) p2);
                            });
                        }
                    };
                });
            });
        }

        @Override
        public <T> Maybe<MonadPlus<Higher<Higher<product, W1>, W2>>> monadPlus(Monoid<Higher<Higher<Higher<product, W1>, W2>, T>> m) {
            return def1.monadPlus().flatMap(x -> {
                return def2.monadPlus().map(y -> {
                    return new MonadPlus<Higher<Higher<product, W1>, W2>>() {

                        @Override
                        public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> ap(Higher<Higher<Higher<product, W1>, W2>, ? extends Function<T, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> apply) {
                            return applicative().ap(fn, apply);
                        }

                        @Override
                        public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> map(Function<? super T, ? extends R> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                            return functor().map(fn, ds);
                        }

                        @Override
                        public <T> Higher<Higher<Higher<product, W1>, W2>, T> unit(T value) {
                            return Instances.this.<T>unit().unit(value);
                        }

                        @Override
                        public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> flatMap(Function<? super T, ? extends Higher<Higher<Higher<product, W1>, W2>, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                            return monad().flatMap(fn, ds);
                        }

                        @Override
                        public Monoid<Higher<Higher<Higher<product, W1>, W2>, ?>> monoid() {
                            return (Monoid)m;
                        }
                    };
                });
            });
        }

        @Override
        public <C2, T> Traverse<Higher<Higher<product, W1>, W2>> traverse() {
            return new Traverse<Higher<Higher<product, W1>, W2>>() {
                @Override
                public <C2, T, R> Higher<C2, Higher<Higher<Higher<product, W1>, W2>, R>> traverseA(Applicative<C2> applicative, Function<? super T, ? extends Higher<C2, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                    Product<W1, W2, T> pr = narrowK(ds);
                    Higher<C2, Product<W1, W2, R>> x = pr.traverseA(applicative, fn);
                    return (Higher)x;
                }

                @Override
                public <C2, T> Higher<C2, Higher<Higher<Higher<product, W1>, W2>, T>> sequenceA(Applicative<C2> applicative, Higher<Higher<Higher<product, W1>, W2>, Higher<C2, T>> ds) {
                    return traverseA(applicative,Function.identity(),ds);
                }

                @Override
                public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> ap(Higher<Higher<Higher<product, W1>, W2>, ? extends Function<T, R>> fn, Higher<Higher<Higher<product, W1>, W2>, T> apply) {
                    return Instances.this.applicative().ap(fn,apply);
                }

                @Override
                public <T> Higher<Higher<Higher<product, W1>, W2>, T> unit(T value) {
                    return Instances.this.<T>unit().unit(value);
                }

                @Override
                public <T, R> Higher<Higher<Higher<product, W1>, W2>, R> map(Function<? super T, ? extends R> fn, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                    return Instances.this.functor().map(fn,ds);
                }
            };
        }
        @Override
        public <T> Foldable<Higher<Higher<product, W1>, W2>> foldable() {
                return new Foldable<Higher<Higher<product, W1>, W2>>(){

                        @Override
                        public <T> T foldRight(Monoid<T> monoid, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                            Product<W1, W2, T> p = narrowK(ds);
                            return p.foldRight(monoid);
                        }

                        @Override
                        public <T> T foldLeft(Monoid<T> monoid, Higher<Higher<Higher<product, W1>, W2>, T> ds) {
                            Product<W1, W2, T> p = narrowK(ds);
                            return p.foldLeft(monoid);
                        }

                    @Override
                    public <T, R> R foldMap(Monoid<R> mb, Function<? super T, ? extends R> fn, Higher<Higher<Higher<product, W1>, W2>, T> nestedA) {
                        return foldLeft(mb,narrowK(nestedA).<R>map(fn));
                    }
                };
        }

        @Override
        public <T> Maybe<Comonad<Higher<Higher<product, W1>, W2>>> comonad() {
            return Maybe.none();
        }



        @Override
        public <T> Maybe<Unfoldable<Higher<Higher<product, W1>, W2>>> unfoldable() {
            if(!def1.unfoldable().isPresent() && !def2.unfoldable().isPresent())
                return Maybe.none();
            return Maybe.just(new  Unfoldable<Higher<Higher<product, W1>, W2>>(){

                @Override
                public <R, T> Higher<Higher<Higher<product, W1>, W2>, R> unfold(T b, Function<? super T, Optional<Tuple2<R, T>>> fn) {
                    Higher<W1, R> a1 = def1.unfoldable().get().unfold(b, fn);
                    Higher<W2, R> a2 = def2.unfoldable().get().unfold(b, fn);
                    return Product.of(Tuple.tuple(a1,a2),def1,def2);

                }

                @Override
                public <R, T> Higher<Higher<Higher<product, W1>, W2>, R> unfoldRight(T b, Function<? super T, Optional<Tuple2<T, R>>> fn) {
                    Higher<W1, R> a1 = def1.unfoldable().get().unfoldRight(b, fn);
                    Higher<W2, R> a2 = def2.unfoldable().get().unfoldRight(b, fn);
                    return Product.of(Tuple.tuple(a1,a2),def1,def2);

                }
            });
        }
    }





}
