gcc -shared -I/opt/j2sdk1.4.1_02/include/ -I/opt/j2sdk1.4.1_02/include/linux NativeSpeed.c -o libnativespeed.so
export LD_LIBRARY_PATH=`pwd`
