// A: AHE
// B: AHEImpl
// C: AGE
// D: KGroup
// E: KList
// F: KPoint
// S: parent type
// T: child type

import java.util.*;

interface A<S extends C>
{
    public void foo();
}

abstract class B<S extends C> implements A<S>
{
}

class C<S extends C, T extends A> extends B<S>
{
    public void foo()
    {
    }
    public Collection<T> get()
    {
        return new ArrayList<T>();
    }
}

class D<S extends C, T extends C> extends C<S,T>
{
}

class E<T extends F> extends C<D,T>
{
    public void bar()
    {
        for(T t : this.get()) {}
        for(F f : this.get()) {}
        
        // This is OK, though it gets marked as unsafe.
        E<F> e1 = new E();
        for(F f : e1.get()) {}

        // But this fails to compile!
        E e2 = new E();
        for(F f : e2.get()) {}
        /*
        javac fails with this error:
        
        found   : java.lang.Object
        required: F
                for(F f : e2.get()) {}
                                ^
        
        In principle, it should know that E is always E<? extends F>,
        because it says so right there in the class definition.
        But it seems to think plain old E is E<Object>, even though
        that should be disallowed.
        
        At the very least, it seems like it should erase to E<A>,
        which is the bound on the superclass C.
        
        Mac OS X 10.4.8, build 8L2127
        java version "1.5.0_06"
        Java(TM) 2 Runtime Environment, Standard Edition (build 1.5.0_06-112)
        Java HotSpot(TM) Client VM (build 1.5.0_06-64, mixed mode, sharing)
        */
    }
}

interface F extends A<E>
{
}
