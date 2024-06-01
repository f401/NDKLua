package net.fred.lua.foreign.internal;

import net.fred.lua.foreign.NativeMethodException;
import net.fred.lua.foreign.Pointer;
import net.fred.lua.foreign.Resource;

// 装饰器设计模式
public class ResourceWrapper extends Resource {
    private final Resource impl;

    protected ResourceWrapper(Resource impl) {
        // 不将其设置为我们的Child, 并将使用权归于我们
        impl.detachParent();
        this.impl = impl;
    }

    @Override
    public final Pointer getBasePointer() {
        return impl.getBasePointer();
    }

    @Override
    public long size() {
        return impl.size();
    }

    @Override
    public void dispose(boolean finalized) throws NativeMethodException {
        super.dispose(finalized);
        impl.dispose(finalized);
    }


}
