package com.tejaswi.database.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BPlusTreeNodeTest {

    @Test
    void returnsFalseWhenNodeIsEmpty() {
        BPlusTreeNode node = new TestNode(4);

        assertFalse(node.contains(10));
    }

    @Test
    void returnsTrueWhenKeyExists() {
        BPlusTreeNode node = nodeWithKeys(-10, 0, 10, 20, 30);

        assertTrue(node.contains(10));
    }

    @Test
    void returnsFalseWhenKeyDoesNotExist() {
        BPlusTreeNode node = nodeWithKeys(10, 20, 30);

        assertFalse(node.contains(25));
    }

    @Test
    void findsFirstAndLastKeys() {
        BPlusTreeNode node = nodeWithKeys(10, 20, 30, 40);

        assertTrue(node.contains(10));
        assertTrue(node.contains(40));
    }

    @Test
    void handlesNegativeAndZeroKeys() {
        BPlusTreeNode node = nodeWithKeys(-20, -10, 0, 10);

        assertTrue(node.contains(-20));
        assertTrue(node.contains(0));
        assertFalse(node.contains(-15));
    }

    private static BPlusTreeNode nodeWithKeys(int... keys) {
        BPlusTreeNode node = new TestNode(keys.length + 1);
        for (int key : keys) {
            node.keys.add(key);
        }
        return node;
    }

    private static final class TestNode extends BPlusTreeNode {

        private TestNode(int maxKeys) {
            super(maxKeys);
        }

        @Override
        boolean isLeaf() {
            return true;
        }
    }
}
