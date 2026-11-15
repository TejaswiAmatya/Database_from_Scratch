package com.tejaswi.database.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeFormatTest {

    @Test
    void calculatesLeafCapacity() {
        assertEquals(339, NodeFormat.MAX_LEAF_ENTRIES);
    }

    @Test
    void calculatesInternalCapacity() {
        assertEquals(338, NodeFormat.MAX_INTERNAL_KEYS);
    }
}
