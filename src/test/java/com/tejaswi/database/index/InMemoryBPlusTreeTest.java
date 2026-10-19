package com.tejaswi.database.index;


import com.tejaswi.database.record.RecordId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

class InMemoryBPlusTreeTest {

	@Test
	void rejectsNonPositiveMaximumKeyCounts() {
		assertThrows(IndexOutOfBoundsException.class, () -> new InMemoryBPlusTree(0));
		assertThrows(IndexOutOfBoundsException.class, () -> new InMemoryBPlusTree(-1));
	}

	@Test
	void rejectsDuplicateKeys() {
		BPlusTree tree = new InMemoryBPlusTree(4);
		tree.insert(10, new RecordId(100L));

		assertThrows(IllegalArgumentException.class,
				() -> tree.insert(10, new RecordId(200L)));
		assertEquals(1, tree.size());
		assertEquals(new RecordId(100L), tree.search(10));
	}

	@Test
	void rejectsNullRecordIds() {
		BPlusTree tree = new InMemoryBPlusTree(4);

		assertThrows(NullPointerException.class, () -> tree.insert(10, null));
		assertTrue(tree.isEmpty());
		assertEquals(0, tree.size());
	}

	@Test
	void reportsWhetherAKeyExists() {
		BPlusTree tree = new InMemoryBPlusTree(4);
		tree.insert(10, new RecordId(100L));

		assertTrue(tree.containsKey(10));
		assertFalse(tree.containsKey(99));
	}

	@Test
	void deletesAnExistingKey() {
		BPlusTree tree = new InMemoryBPlusTree(4);
		tree.insert(10, new RecordId(100L));
		tree.insert(20, new RecordId(200L));

		tree.delete(10);

		assertNull(tree.search(10));
		assertFalse(tree.containsKey(10));
		assertEquals(1, tree.size());
		assertEquals(new RecordId(200L), tree.search(20));
	}

	@Test
	void ignoresDeletingAMissingKey() {
		BPlusTree tree = new InMemoryBPlusTree(4);
		tree.insert(10, new RecordId(100L));

		tree.delete(99);

		assertEquals(1, tree.size());
		assertEquals(new RecordId(100L), tree.search(10));
	}

	@Test
	void updatesAnInternalSeparatorWhenItsLeafKeyIsDeleted() {
		BPlusTree tree = new InMemoryBPlusTree(2);
		for (int key = 1; key <= 5; key++) {
			tree.insert(key, new RecordId(key));
		}

		tree.delete(3);

		assertNull(tree.search(3));
		assertEquals(new RecordId(4), tree.search(4));
		assertEquals(new RecordId(5), tree.search(5));
		assertEquals(4, tree.size());
	}

	@Test
	void rangeScanReturnsMatchingValuesFromOneLeaf() {
		BPlusTree tree = new InMemoryBPlusTree(4);
		tree.insert(10, new RecordId(100L));
		tree.insert(20, new RecordId(200L));
		tree.insert(30, new RecordId(300L));

		assertEquals(
				List.of(new RecordId(100L), new RecordId(200L)),
				tree.rangeScan(1, 20)
		);
	}

	@Test
	void rangeScanFollowsLeafLinksInKeyOrder() {
		BPlusTree tree = new InMemoryBPlusTree(2);

		for (int key = 1; key <= 10; key++) {
			tree.insert(key, new RecordId(key * 10L));
		}

		assertEquals(
				List.of(
						new RecordId(30L),
						new RecordId(40L),
						new RecordId(50L),
						new RecordId(60L),
						new RecordId(70L)
				),
				tree.rangeScan(3, 7)
		);
	}

	@Test
	void rangeScanIncludesBothBounds() {
		BPlusTree tree = new InMemoryBPlusTree(3);
		tree.insert(-10, new RecordId(1L));
		tree.insert(0, new RecordId(2L));
		tree.insert(10, new RecordId(3L));

		assertEquals(
				List.of(new RecordId(2L), new RecordId(3L)),
				tree.rangeScan(0, 10)
		);
	}

	@Test
	void rangeScanReturnsEmptyForNoMatchesOrReversedBounds() {
		BPlusTree tree = new InMemoryBPlusTree(3);
		tree.insert(10, new RecordId(100L));

		assertEquals(List.of(), tree.rangeScan(11, 20));
		assertEquals(List.of(), tree.rangeScan(20, 11));
	}

	@Test
	void findsRecordIdforInsertedKey() {
		BPlusTree tree = new InMemoryBPlusTree(4);

        tree.insert(10, new RecordId(100L));
        tree.insert(25, new RecordId(250L));
        tree.insert(90, new RecordId(900L));

        assertEquals(new RecordId(100L), tree.search(10));
        assertEquals(new RecordId(250L), tree.search(25));
        assertEquals(new RecordId(900L), tree.search(90));
	}

	@Test
	void insertsKeysIntoOneLeafAndTracksTreeState() {
		BPlusTree tree = new InMemoryBPlusTree(4);

		tree.insert(50, new RecordId(500L));
		tree.insert(-10, new RecordId(10L));
		tree.insert(0, new RecordId(0L));
		tree.insert(Integer.MAX_VALUE, new RecordId(2L));

		assertEquals(4, tree.size());
		assertFalse(tree.isEmpty());
		assertEquals(new RecordId(10L), tree.search(-10));
		assertEquals(new RecordId(0L), tree.search(0));
		assertEquals(new RecordId(500L), tree.search(50));
		assertEquals(new RecordId(2L), tree.search(Integer.MAX_VALUE));
	}

	@Test
	void leafSplitPreservesEveryEntryAndRoutesSeparatorKeysCorrectly() {
		BPlusTree tree = new InMemoryBPlusTree(3);

		for (int key = 1; key <= 20; key++) {
			tree.insert(key, new RecordId(key * 10L));
		}

		assertEquals(20, tree.size());
		for (int key = 1; key <= 20; key++) {
			assertEquals(new RecordId(key * 10L), tree.search(key));
		}
		assertNull(tree.search(0));
		assertNull(tree.search(21));
	}

	@Test
	void multipleInternalSplitsPreserveEveryEntry() {
		BPlusTree tree = new InMemoryBPlusTree(2);

		for (int key = 100; key >= 1; key--) {
			tree.insert(key, new RecordId(key));
		}

		assertEquals(100, tree.size());
		for (int key = 1; key <= 100; key++) {
			assertEquals(new RecordId(key), tree.search(key));
		}
	}

	@Test
	void supportsTheSmallestPositiveNodeCapacity() {
		BPlusTree tree = new InMemoryBPlusTree(1);

		for (int key = 0; key < 12; key++) {
			tree.insert(key, new RecordId(key));
		}

		assertEquals(12, tree.size());
		for (int key = 0; key < 12; key++) {
			assertEquals(new RecordId(key), tree.search(key));
		}
	}

	@Test
	void clearRemovesAllInsertedEntriesAndAllowsReuse() {
		BPlusTree tree = new InMemoryBPlusTree(2);
		tree.insert(1, new RecordId(1));
		tree.insert(2, new RecordId(2));
		tree.insert(3, new RecordId(3));

		tree.clear();

		assertTrue(tree.isEmpty());
		assertEquals(0, tree.size());
		assertNull(tree.search(1));
		tree.insert(4, new RecordId(4));
		assertEquals(new RecordId(4), tree.search(4));
	}
	
	@Test
    void returnsNullForAMissingKey() {
        BPlusTree tree = new InMemoryBPlusTree(4);

        tree.insert(10, new RecordId(100L));

        assertNull(tree.search(99));
    }

    @Test
    void findsKeysInsertedOutOfOrder() {
        BPlusTree tree = new InMemoryBPlusTree(4);

        tree.insert(50, new RecordId(500L));
        tree.insert(10, new RecordId(100L));
        tree.insert(30, new RecordId(300L));

        assertEquals(new RecordId(100L), tree.search(10));
        assertEquals(new RecordId(300L), tree.search(30));
        assertEquals(new RecordId(500L), tree.search(50));
    }

}
