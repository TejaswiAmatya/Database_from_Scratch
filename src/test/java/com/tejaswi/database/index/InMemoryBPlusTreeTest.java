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
	}

	@Test
	void rejectsNullRecordIds() {
		BPlusTree tree = new InMemoryBPlusTree(4);

		assertThrows(NullPointerException.class, () -> tree.insert(10, null));
	}

	@Test
	void reportsWhetherAKeyExists() {
		BPlusTree tree = new InMemoryBPlusTree(4);
		tree.insert(10, new RecordId(100L));

		assertTrue(tree.containsKey(10));
		assertFalse(tree.containsKey(99));
	}

	@Test
	void rangeScanCurrentlyReturnsTheEmptyScaffoldResult() {
		BPlusTree tree = new InMemoryBPlusTree(4);
		tree.insert(10, new RecordId(100L));

		assertEquals(List.of(), tree.rangeScan(1, 20));
	}

	@Test
	void deleteCurrentlyLeavesTheScaffoldTreeUnchanged() {
		BPlusTree tree = new InMemoryBPlusTree(4);
		tree.insert(10, new RecordId(100L));

		tree.delete(10);

		assertEquals(new RecordId(100L), tree.find(10));
	}

	@Test
	void findsRecordIdforInsertedKey() {
		BPlusTree tree = new InMemoryBPlusTree(4);

        tree.insert(10, new RecordId(100L));
        tree.insert(25, new RecordId(250L));
        tree.insert(90, new RecordId(900L));

        assertEquals(new RecordId(100L), tree.find(10));
        assertEquals(new RecordId(250L), tree.find(25));
        assertEquals(new RecordId(900L), tree.find(90));
	}
	
	@Test
    void returnsNullForAMissingKey() {
        BPlusTree tree = new InMemoryBPlusTree(4);

        tree.insert(10, new RecordId(100L));

        assertNull(tree.find(99));
    }

    @Test
    void findsKeysInsertedOutOfOrder() {
        BPlusTree tree = new InMemoryBPlusTree(4);

        tree.insert(50, new RecordId(500L));
        tree.insert(10, new RecordId(100L));
        tree.insert(30, new RecordId(300L));

        assertEquals(new RecordId(100L), tree.find(10));
        assertEquals(new RecordId(300L), tree.find(30));
        assertEquals(new RecordId(500L), tree.find(50));
    }

}
