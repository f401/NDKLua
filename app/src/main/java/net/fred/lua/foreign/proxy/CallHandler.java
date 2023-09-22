package net.fred.lua.foreign.proxy;

import net.fred.lua.common.Logger;
import net.fred.lua.foreign.NativeMethodException;
import net.fred.lua.foreign.core.DynamicLoadingLibrary;
import net.fred.lua.foreign.core.TypeRegistry;
import net.fred.lua.foreign.ffi.FunctionCaller;
import net.fred.lua.foreign.types.Type;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class CallHandler implements InvocationHandler {

    DynamicLoadingLibrary dll;

    public CallHandler(String libPath) {
        try {
            dll = DynamicLoadingLibrary.open(libPath);
        } catch (NativeMethodException e) {
            Logger.e("Failed to open library " + libPath + ". Exception: " + e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Type<?>[] paramsArray = null;
        if (method.getParameterTypes().length != 0) {
            ArrayList<Type<?>> params = new ArrayList<>(args.length);
            for (Class<?> param : method.getParameterTypes()) {
                params.add(TypeRegistry.getType(param));
            }
            paramsArray = params.toArray(new Type[params.size()]);
        }
        return FunctionCaller.of(false, dll.lookupSymbol(method.getName()),
                TypeRegistry.getType(method.getReturnType()), paramsArray
        ).callThenClose(args);
    }
}