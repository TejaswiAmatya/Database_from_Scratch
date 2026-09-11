package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeafNodeTest {

    @Test
    void createsAnEmptyLeafNodeWithConfiguredCapacity() {
        LeafNode node = new LeafNode(3);

        assertEquals(3, node.maxKeys);
        assertEquals(0, node.keyCount());
        assertTrue(node.isEmpty());
        assertFalse(node.isFull());
        assertFalse(node.hasOverflow());
    }

    @Test
    void identifiesAsALeafNode() {
        LeafNode node = new LeafNode(3);

        assertTrue(node.isLeaf());
    }

    @Test
    void rejectsNonPositiveMaximumKeyCounts() {
        assertThrows(IndexOutOfBoundsException.class, () -> new LeafNode(0));
        assertThrows(IndexOutOfBoundsException.class, () -> new LeafNode(-1));
    }

    @Test
    void findsRecordIdForInsertedKey() {
        LeafNode node = new LeafNode(4);

        node.insert(10, new RecordId(100L));

        assertEquals(new RecordId(100L), node.find(10));
    }

    @Test
    void returnsNullForAMissingKey() {
        LeafNode node = new LeafNode(4);

        node.insert(10, new RecordId(100L));

        assertNull(node.find(99));
    }

    @Test
    void findsKeysInsertedOutOfOrder() {
        LeafNode node = new LeafNode(4);

        node.insert(50, new RecordId(500L));
        node.insert(10, new RecordId(100L));
        node.insert(30, new RecordId(300L));

        assertEquals(new RecordId(100L), node.find(10));
        assertEquals(new RecordId(300L), node.find(30));
        assertEquals(new RecordId(500L), node.find(50));
    }

    @Test
    void rejectsInsertingADuplicateKey() {
        LeafNode node = new LeafNode(4);

        node.insert(10, new RecordId(100L));

        assertThrows(IllegalArgumentException.class, () -> node.insert(10, new RecordId(999L)));
    }
}
