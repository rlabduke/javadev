import java.lang.reflect.*;

// My best guess at this point is that each additional class takes
// about 200 bytes (maybe as many as 400-500) of additional storage.
// An empty class extending Object also takes about 200 bytes on disk.
// So the cost of using inner classes to handle e.g. ActionEvents is not
// that high, unless you have many thousands of such handlers.

// Based on simple greps, I estimate KiNG has less than 150 functions
// handled by ReflexiveAction.  The additional cost (besides readability)
// to have used "real" classes would probably be ~30 kB.

// To generate the classes B*, do one of the following:
//  for((i=0;i<1000;i++)); do echo "public class B$i { public void foo() { System.out.println(\"This is class B$i\"); } }" > B${i}.java; done
//  for((i=0;i<1000;i++)); do echo "public class B$i { }" > B${i}.java; done

public class A
{
    public static void main(String[] args)
    {
        Object[] b = new Object[1000];
        Runtime r = Runtime.getRuntime();
        long before, after;

        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        try { Thread.sleep(2000); } catch(InterruptedException ex) {}
        
        System.out.println("Checking memory used by a single new class.");
        before = r.totalMemory() - r.freeMemory();
        System.out.println("Before: "+before);

        try
        {
            for(int i = 0; i < b.length; i++)
            {
                Class cl = Class.forName("B"+i);
                Constructor co = cl.getConstructor();
                b[i] = co.newInstance();
                System.out.println("  Used: "+((r.totalMemory() - r.freeMemory() - before) / (i+1)));
            }
        }
        catch(Exception ex) { ex.printStackTrace(); }

        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        try { Thread.sleep(2000); } catch(InterruptedException ex) {}
        
        after = r.totalMemory() - r.freeMemory();
        System.out.println("After:  "+after);
        System.out.println("Diff:   "+(after-before));
        System.out.println("Each:   "+((after-before)/b.length));
    }
}