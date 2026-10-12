package com.tejaswi.database.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalNodeTest {

    @Test
    void createsAnEmptyInternalNodeWithConfiguredCapacity() {
        InternalNode node = new InternalNode(3);

        assertEquals(3, node.maxKeys);
        assertEquals(0, node.keyCount());
        assertTrue(node.isEmpty());
        assertFalse(node.isFull());
        assertFalse(node.hasOverflow());
    }

    @Test
    void identifiesAsAnInternalNode() {
        InternalNode node = new InternalNode(3);

        assertFalse(node.isLeaf());
    }

    @Test
    void rejectsNonPositiveMaximumKeyCounts() {
        assertThrows(IndexOutOfBoundsException.class, () -> new InternalNode(0));
        assertThrows(IndexOutOfBoundsException.class, () -> new InternalNode(-1));
    }

    @Test
    void reportsCapacityAndOverflowUsingInheritedNodeState() {
        InternalNode node = new InternalNode(2);
        node.keys.add(10);
        node.keys.add(20);

        assertTrue(node.isFull());
        assertFalse(node.hasOverflow());

        node.keys.add(30);

        assertTrue(node.hasOverflow());
    }
}
