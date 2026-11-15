package com.tejaswi.database.index;

public class NodeFormat {
    static final int PAGE_SIZE = 4096;
    static final int HEADER_SIZE = 24;

    static final int MAGIC = 0x42504E44; // ASCII "BPND"
    static final short VERSION = 1;
    static final byte LEAF_TYPE = 1;
    static final byte INTERNAL_TYPE = 2;

    static final long NO_OFFSET = -1L;

    static final int KEY_SIZE = Integer.BYTES;
    static final int RECORD_ID_SIZE = Long.BYTES;
    static final int CHILD_OFFSET_SIZE = Long.BYTES;

    static final int LEAF_ENTRY_SIZE = KEY_SIZE + RECORD_ID_SIZE;
    static final int INTERNAL_ENTRY_SIZE = KEY_SIZE + CHILD_OFFSET_SIZE;

    static final int MAX_LEAF_ENTRIES =
            (PAGE_SIZE - HEADER_SIZE) / LEAF_ENTRY_SIZE;

    static final int MAX_INTERNAL_KEYS =
            (PAGE_SIZE - HEADER_SIZE - CHILD_OFFSET_SIZE) / INTERNAL_ENTRY_SIZE;

    private NodeFormat() {
        // Constants holder; no instances.
    }
}
