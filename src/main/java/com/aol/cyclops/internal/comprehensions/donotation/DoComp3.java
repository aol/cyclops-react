
package com.aol.cyclops.internal.comprehensions.donotation;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.BaseStream;

import org.pcollections.PStack;
import org.reactivestreams.Publisher;

import com.aol.cyclops.control.AnyM;
import com.aol.cyclops.control.Reader;
import com.aol.cyclops.internal.comprehensions.donotation.DoBuilderModule.Entry;
import com.aol.cyclops.internal.comprehensions.donotation.DoBuilderModule.Guard;
import com.aol.cyclops.internal.monads.MonadWrapper;
import com.aol.cyclops.types.anyM.AnyMSeq;
import com.aol.cyclops.util.function.CurryVariance;
import com.aol.cyclops.util.function.TriFunction;
public class DoComp3<T1,T2,T3> extends DoComp{
		public DoComp3(PStack<Entry> assigned, Class orgType) {
			super(assigned,orgType);
			
		}

		
		public <T4> DoComp4<T1,T2,T3,T4> reader(Function<? super T1,Function<? super T2,Function<? super T3,Reader<?,? extends T4>>>> f){
			return new DoComp4<>(addToAssigned(f),getOrgType());
			
		}
		

		public <T4> DoComp4<T1,T2,T3,T4> iterable(Function<? super T1,Function<? super T2,Function<? super T3,Iterable<T4>>>> f){
			return new DoComp4<>(addToAssigned(f),getOrgType());
			
		}
		public <T4> DoComp4<T1,T2,T3,T4> publisher(Function<? super T1,Function<? super T2,Function<? super T3,Publisher<T4>>>> f){
            return new DoComp4<>(addToAssigned(f),getOrgType());
            
        }
		
		
		public <T4> DoComp4<T1,T2,T3,T4> stream(Function<? super T1,Function<? super T2,Function<? super T3,BaseStream<T4,?>>>> f){
			return new DoComp4<>(addToAssigned(f),getOrgType());
			
		}
		


		public <T4> DoComp4<T1,T2,T3,T4> optional(Function<? super T1,Function<? super T2,Function<? super T3,Optional<T4>>>> f){
			return new DoComp4<>(addToAssigned(f),getOrgType());
			
		}
		

		public <T4> DoComp4<T1,T2,T3,T4> future(Function<? super T1,Function<? super T2,Function<? super T3,CompletableFuture<T4>>>> f){
			return new DoComp4<>(addToAssigned(f),getOrgType());
			
		}
		


		public <T4> DoComp4<T1,T2,T3,T4> anyM(Function<? super T1,Function<? super T2,Function<? super T3,AnyM<T4>>>> f){
			return new DoComp4<>(addToAssigned(f),getOrgType());
			
		}
		


		
		public <R> AnyMSeq<R> yield(Function<? super T1,Function<? super T2,Function<? super T3,? extends R>>> f){
			if(getOrgType()!=null)
				return new MonadWrapper<>(this.yieldInternal(f),this.getOrgType()).anyMSeq();
			else
				return AnyM.ofSeq(this.yieldInternal(f));
		}
		public <R> AnyMSeq<R> yield(TriFunction<? super T1,? super T2,? super T3,? extends R> f){
            return this.yield(CurryVariance.curry3(f));
        }
		
		
		public  DoComp3<T1,T2,T3> filter(Function<? super T1,Function<? super T2,Function<? super T3,Boolean>>> f){
			return new DoComp3<>(getAssigned().plus(getAssigned().size(),new Entry("$$internalGUARD"+getAssigned().size(),new Guard(f))),getOrgType());
		}
		
	}

