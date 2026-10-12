package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryBPlusTreeRandomizedTest {

    @Test
    void matchesTreeMapForRandomizedInsertionSearchAndRangeScan() {
        int[] capacities = {1, 2, 3, 4, 7};
        long[] seeds = {7L, 42L, 2025L};

        for (int capacity : capacities) {
            for (long seed : seeds) {
                assertMatchesTreeMap(capacity, seed);
            }
        }
    }

    private void assertMatchesTreeMap(int capacity, long seed) {
        BPlusTree tree = new InMemoryBPlusTree(capacity);
        TreeMap<Integer, RecordId> expected = new TreeMap<>();
        Random random = new Random(seed);

        for (int operation = 0; operation < 500; operation++) {
            int key = random.nextInt(2_001) - 1_000;
            RecordId recordId = new RecordId(key + 1_000L);

            if (expected.containsKey(key)) {
                assertThrows(IllegalArgumentException.class, () -> tree.insert(key, recordId));
            } else {
                tree.insert(key, recordId);
                expected.put(key, recordId);
            }

            assertEquals(expected.size(), tree.size());
            assertEquals(expected.get(key), tree.search(key));

            int probe = random.nextInt(2_401) - 1_200;
            assertEquals(expected.get(probe), tree.search(probe));
        }

        for (int operation = 0; operation < 100; operation++) {
            int lowerBound = random.nextInt(2_401) - 1_200;
            int upperBound = random.nextInt(2_401) - 1_200;

            List<RecordId> expectedRange = expectedRange(expected, lowerBound, upperBound);
            assertEquals(expectedRange, tree.rangeScan(lowerBound, upperBound));
        }
    }

    private List<RecordId> expectedRange(
            TreeMap<Integer, RecordId> expected,
            int lowerBound,
            int upperBound
    ) {
        if (lowerBound > upperBound) {
            return List.of();
        }

        return new ArrayList<>(expected.subMap(lowerBound, true, upperBound, true).values());
    }

    @Test
    void preservesAllIntegerBoundaryKeys() {
        BPlusTree tree = new InMemoryBPlusTree(2);
        Map<Integer, RecordId> values = Map.of(
                Integer.MIN_VALUE, new RecordId(1L),
                -1, new RecordId(2L),
                0, new RecordId(3L),
                1, new RecordId(4L),
                Integer.MAX_VALUE, new RecordId(5L)
        );

        values.forEach(tree::insert);

        assertEquals(
                List.of(
                        values.get(Integer.MIN_VALUE),
                        values.get(-1),
                        values.get(0),
                        values.get(1),
                        values.get(Integer.MAX_VALUE)
                ),
                tree.rangeScan(Integer.MIN_VALUE, Integer.MAX_VALUE)
        );
        values.forEach((key, recordId) -> assertEquals(recordId, tree.search(key)));
    }
}
