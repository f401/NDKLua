package net.fred.lua.jni.lua;

import java.io.Closeable;
import java.io.IOException;

public class LuaJavaBridge implements Closeable {
    
    private long ptr;
    private boolean opened;
    
    public LuaJavaBridge() {
        this(true);
    }
    
    public LuaJavaBridge(boolean newState) {
        this(newState, true);
    }
    
    public LuaJavaBridge(boolean newState, boolean openLibs) {
        if (newState) newState();
        if (openLibs) openlibs();
    }
    
    @Override
    public void close() {
        nativeClose(ptr);
    }
    
    public void newState() {
        if (opened) 
            nativeClose(ptr);
        ptr = nativeNewState();
        opened = true;
    }
    
    public void getGlobal(String name) {
        nativeGetGlobal(ptr, name);
    }
    
    public void pushNumber(int number) {
        nativePushNumber(ptr, number);
    }
    
    public void call(int paramsNums, int returnNums) {
        nativeCall(ptr, paramsNums, returnNums);
    }
    
    public int toInteger(int stack) {
        return nativeToInteger(ptr, stack);
    }
    
    public void pop(int stack) {
        nativePop(ptr, stack);
    }
    
    public void openlibs() {
       nativeOpenlibs(ptr);
    }
    
    public void dofile(String file) {
        nativeDofile(ptr, file);
    }
    
    private native void nativeClose(long ptr);
    private native long nativeNewState();
    private native void nativeGetGlobal(long ptr, String name);
    private native void nativePushNumber(long ptr, int number);
    private native int nativeToInteger(long ptr, int stack);
    private native void nativeCall(long ptr, int paramsNums, int returnNums);
    private native void nativePop(long ptr, int stack);
    
    private native void nativeOpenlibs(long ptr);
    private native void nativeDofile(long ptr, String path);
    
    static {
        System.loadLibrary("bridge");
    }
}
