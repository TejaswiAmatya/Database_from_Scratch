package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryBPlusTreeDeletionTest {

    @Test
    void deletesAllKeysAndAllowsReuseAcrossNodeCapacities() throws Exception {
        for (int capacity : new int[]{1, 2, 3, 4, 7}) {
            InMemoryBPlusTree tree = new InMemoryBPlusTree(capacity);
            for (int key = -60; key <= 60; key++) {
                tree.insert(key, new RecordId(key + 61L));
            }
            for (int key = 60; key >= -60; key--) {
                tree.delete(key);
                assertNull(tree.search(key));
                assertEquals(key + 60, tree.size());
                assertStructure(tree, capacity);
            }
            assertTrue(tree.isEmpty());
            assertTrue(root(tree).isLeaf());
            tree.insert(9, new RecordId(200L));
            assertEquals(new RecordId(200L), tree.search(9));
        }
    }

    @Test
    void randomizedDeletesMatchTreeMapAndPreserveTreeStructure() throws Exception {
        for (int capacity : new int[]{1, 2, 3, 4, 7}) {
            InMemoryBPlusTree tree = new InMemoryBPlusTree(capacity);
            TreeMap<Integer, RecordId> expected = new TreeMap<>();
            Random random = new Random(42L + capacity);
            for (int step = 0; step < 1_000; step++) {
                int key = random.nextInt(301) - 150;
                if (random.nextBoolean()) {
                    if (!expected.containsKey(key)) {
                        RecordId id = new RecordId(key + 151L);
                        tree.insert(key, id);
                        expected.put(key, id);
                    }
                } else {
                    tree.delete(key);
                    expected.remove(key);
                }
                assertEquals(expected.size(), tree.size());
                assertEquals(new ArrayList<>(expected.values()),
                        tree.rangeScan(Integer.MIN_VALUE, Integer.MAX_VALUE));
                for (int probe : new int[]{key, -150, 0, 150}) {
                    assertEquals(expected.get(probe), tree.search(probe));
                }
                assertStructure(tree, capacity);
            }
        }
    }

    private static BPlusTreeNode root(InMemoryBPlusTree tree) throws Exception {
        Field field = InMemoryBPlusTree.class.getDeclaredField("root");
        field.setAccessible(true);
        return (BPlusTreeNode) field.get(tree);
    }

    private static void assertStructure(InMemoryBPlusTree tree, int capacity) throws Exception {
        List<LeafNode> leaves = new ArrayList<>();
        checkNode(root(tree), true, capacity, 0, leaves);
        for (int i = 0; i < leaves.size(); i++) {
            assertSame(i + 1 < leaves.size() ? leaves.get(i + 1) : null, leaves.get(i).nextLeaf());
        }
    }

    private static int checkNode(BPlusTreeNode node, boolean isRoot, int capacity,
                                 int depth, List<LeafNode> leaves) {
        assertTrue(node.keyCount() <= capacity);
        for (int i = 1; i < node.keyCount(); i++) {
            assertTrue(node.keyAt(i - 1) < node.keyAt(i));
        }
        if (node.isLeaf()) {
            if (!isRoot) assertTrue(node.keyCount() >= (capacity + 1) / 2);
            if (!leaves.isEmpty() && !node.isEmpty()) {
                assertTrue(leaves.getLast().keyAt(leaves.getLast().keyCount() - 1) < node.keyAt(0));
            }
            leaves.add((LeafNode) node);
            return depth;
        }
        InternalNode internal = (InternalNode) node;
        assertEquals(node.keyCount() + 1, internal.children().size());
        assertTrue(internal.children().size() >= (isRoot ? 2 : (capacity + 2) / 2));
        int leafDepth = -1;
        for (int i = 0; i < internal.children().size(); i++) {
            BPlusTreeNode child = internal.children().get(i);
            if (i > 0) {
                BPlusTreeNode first = child;
                while (!first.isLeaf()) first = ((InternalNode) first).getFirst();
                assertEquals(first.keyAt(0), internal.keyAt(i - 1));
            }
            int childDepth = checkNode(child, false, capacity, depth + 1, leaves);
            if (leafDepth == -1) leafDepth = childDepth;
            else assertEquals(leafDepth, childDepth);
        }
        return leafDepth;
    }
}
