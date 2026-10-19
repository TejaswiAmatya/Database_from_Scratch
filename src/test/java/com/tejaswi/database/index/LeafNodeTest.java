package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    void rejectsNullRecordIds() {
        LeafNode node = new LeafNode(4);

        assertThrows(NullPointerException.class, () -> node.insert(10, null));
        assertTrue(node.isEmpty());
    }

    @Test
    void maintainsKeyAndValueOrderWhenInsertedOutOfOrder() {
        LeafNode node = new LeafNode(4);

        node.insert(30, new RecordId(300L));
        node.insert(10, new RecordId(100L));
        node.insert(20, new RecordId(200L));

        assertEquals(List.of(10, 20, 30), node.keys);
        assertEquals(new RecordId(100L), node.find(10));
        assertEquals(new RecordId(200L), node.find(20));
        assertEquals(new RecordId(300L), node.find(30));
    }

    @Test
    void appendsInclusiveRangeValuesInKeyOrder() {
        LeafNode node = new LeafNode(5);
        node.insert(10, new RecordId(100L));
        node.insert(20, new RecordId(200L));
        node.insert(30, new RecordId(300L));

        List<RecordId> result = new ArrayList<>();
        node.appendRange(10, 20, result);

        assertEquals(List.of(new RecordId(100L), new RecordId(200L)), result);
    }

    @Test
    void removesAValidRangeAndKeepsRemainingEntriesAligned() {
        LeafNode node = new LeafNode(5);
        node.insert(10, new RecordId(100L));
        node.insert(20, new RecordId(200L));
        node.insert(30, new RecordId(300L));

        assertEquals(List.of(new RecordId(200L)), node.remove(1, 2));
        assertEquals(List.of(10, 30), node.keys);
        assertEquals(new RecordId(100L), node.find(10));
        assertEquals(new RecordId(300L), node.find(30));
    }

    @Test
    void rejectsInvalidRemovalRanges() {
        LeafNode node = new LeafNode(3);
        node.insert(10, new RecordId(100L));

        assertThrows(IndexOutOfBoundsException.class, () -> node.remove(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> node.remove(1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> node.remove(0, 2));
    }

    @Test
    void removesTheFirstKeyAndRecordIdAsAnEntry() {
        LeafNode node = new LeafNode(3);
        node.insert(10, new RecordId(100L));
        node.insert(20, new RecordId(200L));

        LeafNode.LeafEntry entry = node.removeFirstEntry();

        assertEquals(new LeafNode.LeafEntry(10, new RecordId(100L)), entry);
        assertEquals(List.of(20), node.keys);
        assertEquals(new RecordId(200L), node.find(20));
    }

    @Test
    void rejectsRemovingTheFirstEntryFromAnEmptyLeaf() {
        LeafNode node = new LeafNode(3);

        assertThrows(IllegalStateException.class, node::removeFirstEntry);
    }
}
