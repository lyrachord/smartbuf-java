package com.github.sisyphsu.nakedata.context.output;

import com.github.sisyphsu.nakedata.context.common.IDAllocator;
import com.github.sisyphsu.nakedata.utils.ArrayUtils;
import com.github.sisyphsu.nakedata.utils.TimeUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * StructPool represents an area holds struct for sharing, which support temporary and context using.
 * <p>
 * It will allocate unique ID for every struct by its fields.
 *
 * @author sulin
 * @since 2019-10-07 21:29:36
 */
public final class OutputStructPool {

    private int      tmpStructCount;
    private Struct[] tmpStructs       = new Struct[4];
    private int      cxtStructAddedCount;
    private Struct[] cxtStructAdded   = new Struct[4];
    private int      cxtStructExpiredCount;
    private Struct[] cxtStructExpired = new Struct[4];

    private       Struct[]    cxtStructs = new Struct[4];
    private final IDAllocator cxtIdAlloc = new IDAllocator();

    private final int                 limit;
    private final Map<Struct, Struct> index    = new HashMap<>();
    private final Struct              reuseKey = new Struct(false, 0, 0, null);

    public OutputStructPool(int limit) {
        this.limit = limit;
    }

    /**
     * Register the specified struct into pool by its field-names.
     *
     * @param temporary It's temporary or not
     * @param names     FieldNames which represents an struct
     */
    public void register(boolean temporary, String[] names) {
        if (names == null) {
            throw new NullPointerException();
        }
        int now = (int) TimeUtils.fastUpTime();
        Struct struct = index.get(reuseKey.wrap(names));
        if (struct != null) {
            if (temporary || !struct.temporary) {
                struct.lastTime = now;
                return;
            }
            tmpStructCount--;
            if (struct.offset < tmpStructCount) {
                Struct lastStruct = tmpStructs[tmpStructCount];
                lastStruct.offset = struct.offset;
                tmpStructs[struct.offset] = lastStruct;
            }
            this.index.remove(struct);
        }
        if (temporary) {
            struct = new Struct(true, tmpStructCount, now, names);
            this.tmpStructs = ArrayUtils.put(tmpStructs, tmpStructCount++, struct);
        } else {
            int offset = cxtIdAlloc.acquire();
            struct = new Struct(false, offset, now, names);
            this.cxtStructs = ArrayUtils.put(cxtStructs, offset, struct);
            this.cxtStructAdded = ArrayUtils.put(cxtStructAdded, cxtStructAddedCount++, struct);
        }
        this.index.put(struct, struct);
    }

    /**
     * Find unique id of the specified struct by its field-names
     *
     * @param fields field-names which represents an struct
     * @return Its unique id
     */
    public int findStructID(String[] fields) {
        Struct struct = index.get(reuseKey.wrap(fields));
        if (struct == null) {
            throw new IllegalArgumentException("struct not exists: " + Arrays.toString(fields));
        }
        if (struct.temporary) {
            return struct.offset;
        }
        return tmpStructCount + struct.offset;
    }

    /**
     * Find the specified struct's fields by its unique id
     *
     * @param id Struct's unique ID
     * @return Struct's fields
     */
    public String[] findStructByID(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("negative id: " + id);
        }
        if (id < tmpStructCount) {
            return tmpStructs[id].names;
        }
        id -= tmpStructCount;
        if (id > cxtStructs.length) {
            throw new IllegalArgumentException("invalid id: " + id);
        }
        Struct struct = cxtStructs[id];
        if (struct == null) {
            throw new IllegalArgumentException("invalid id: " + id);
        }
        return struct.names;
    }

    /**
     * Fetch total number of struct in temporary and context area.
     *
     * @return Total number
     */
    public int size() {
        return index.size();
    }

    /**
     * Reset this struct pool, and execute struct-expire automatically
     */
    public void reset() {
        for (int i = 0; i < tmpStructCount; i++) {
            index.remove(tmpStructs[i]);
        }
        this.tmpStructCount = 0;
        this.cxtStructAddedCount = 0;
        this.cxtStructExpiredCount = 0;
        // auto expire
        int size = index.size();
        if (size > limit) {
            this.autoRelease(size - limit);
        }
    }

    // execute automatically expire for context-struct
    private void autoRelease(int count) {
        long[] heap = new long[count];

        // 0 means init, 1 means stable, -1 means not-stable.
        int heapStatus = 0;
        int itemCount = cxtStructs.length;
        for (int i = 0, heapOffset = 0; i < itemCount; i++) {
            if (cxtStructs[i] == null) {
                continue;
            }
            int itemTime = cxtStructs[i].lastTime;
            int itemOffset = cxtStructs[i].offset;
            if (heapOffset < count) {
                heap[heapOffset++] = ((long) itemTime) << 32 | (long) itemOffset;
                continue;
            }
            if (heapStatus == 0) {
                ArrayUtils.descFastSort(heap, 0, count - 1); // sort by activeTime, heap[0] has biggest activeTime
                heapStatus = 1;
            } else if (heapStatus == -1) {
                ArrayUtils.maxHeapAdjust(heap, 0, count); // make sure heap[0] has biggest activeTime
                heapStatus = 1;
            }
            if (itemTime > (int) (heap[0] >>> 32)) {
                continue; // item is newer than all items in heap
            }
            heap[0] = ((long) itemTime) << 32 | (long) itemOffset;
            heapStatus = -1;
        }

        for (long l : heap) {
            Struct expiredStruct = cxtStructs[(int) (l)];
            index.remove(expiredStruct);
            cxtIdAlloc.release(expiredStruct.offset);
            cxtStructs[expiredStruct.offset] = null;

            this.cxtStructExpired = ArrayUtils.put(cxtStructExpired, cxtStructExpiredCount++, expiredStruct);
        }
    }

    /**
     * Struct model for inner usage
     */
    static final class Struct {
        boolean  temporary;
        int      offset;
        int      lastTime;
        String[] names;

        public Struct(boolean temporary, int offset, int lastTime, String[] names) {
            this.temporary = temporary;
            this.offset = offset;
            this.lastTime = lastTime;
            this.names = names;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(names);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Struct) {
                return Arrays.equals(names, ((Struct) obj).names);
            }
            return false;
        }

        Struct wrap(String[] names) {
            this.names = names;
            return this;
        }
    }

}
