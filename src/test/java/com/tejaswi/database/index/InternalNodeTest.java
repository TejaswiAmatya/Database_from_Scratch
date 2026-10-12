package com.tejaswi.database.index;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void routesKeysToTheCorrectChildAroundASeparator() {
        LeafNode left = new LeafNode(3);
        LeafNode right = new LeafNode(3);
        InternalNode node = new InternalNode(3, left, 10, right);

        assertSame(left, node.childFor(9));
        assertSame(right, node.childFor(10));
        assertSame(right, node.childFor(11));
    }

    @Test
    void insertsSeparatorsAndChildrenInSortedOrder() {
        LeafNode left = new LeafNode(3);
        LeafNode middle = new LeafNode(3);
        LeafNode right = new LeafNode(3);
        InternalNode node = new InternalNode(3, left, 20, right);

        node.insertSeparator(10, middle);

        assertEquals(List.of(10, 20), node.keys);
        assertEquals(List.of(left, middle, right), node.children());
        assertSame(left, node.childFor(9));
        assertSame(middle, node.childFor(10));
        assertSame(right, node.childFor(20));
    }

    @Test
    void rejectsDuplicateSeparators() {
        InternalNode node = new InternalNode(3, new LeafNode(3), 10, new LeafNode(3));

        assertThrows(
                IllegalArgumentException.class,
                () -> node.insertSeparator(10, new LeafNode(3))
        );
    }

    @Test
    void rejectsAnInvalidNumberOfChildren() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InternalNode(List.of(10), List.of(new LeafNode(3)), 3)
        );
    }

    @Test
    void rejectsNullChildrenInTheTwoChildConstructor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InternalNode(3, null, 10, new LeafNode(3))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new InternalNode(3, new LeafNode(3), 10, null)
        );
    }
}
