class NativeSpeed {
    public native void doNativeFunc();

    static {
        System.loadLibrary("nativespeed");
    }
    
    public static void main(String[] args) {
        NativeSpeed ns = new NativeSpeed();
        final int num = 10000000; // 10 million
        long time = System.currentTimeMillis();
        for(int i = 0; i < num; i++)
            ns.doNativeFunc();
        time = System.currentTimeMillis() - time;
        System.err.println(num+" native calls in "+time+" ms");
    }
}

