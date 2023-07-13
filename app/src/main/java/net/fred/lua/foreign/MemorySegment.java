package net.fred.lua.foreign;

import androidx.annotation.NonNull;

import net.fred.lua.foreign.util.ForeignCloseable;
import net.fred.lua.foreign.util.Pointer;

public class MemorySegment extends ForeignCloseable {
    private final long size;

    /**
     * See {@link MemorySegment#create}
     */
    protected MemorySegment(Pointer src, long size) {
        super(src);
        this.size = size;
    }

    /**
     * Create a memory segment of size {@code size}.
     *
     * @param size The size of the memory segment needs to be created.
     * @return This object.
     * @see MemorySegment#allocate
     */
    @NonNull
    public static MemorySegment create(long size) throws NativeMethodException {
        return new MemorySegment(allocate(size), size);
    }

    /**
     * Create a 'size' length and return a pointer to it.
     *
     * @param size The size needs to be created.
     * @return Pointer to.
     * @throws NativeMethodException    When creation fails
     * @throws IllegalArgumentException When {@code size} is less than or equal to 0.
     */
    @NonNull
    public static Pointer allocate(long size) throws NativeMethodException {
        if (size <= 0) {
            throw new IllegalArgumentException("`Size 'cannot be less than or equal to 0");
        }
        final long ptr = ForeignFunctions.alloc(size);
        if (ptr == ForeignValues.NULL) {
            throw new NativeMethodException(
                    "Failed to alloc size: " + size + ".Reason: " +
                            ForeignFunctions.strerror());
        }
        return Pointer.from(ptr);
    }

    /**
     * Get the size of the segment.
     *
     * @return the size of the segment
     */
    public long size() {
        return size;
    }
}