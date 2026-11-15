package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeafPageDataTest {

    @Test
    void acceptsEmptyLeafWithoutNextSibling() {
        LeafPageData page = new LeafPageData(List.of(), List.of(), NodeFormat.NO_OFFSET);

        assertEquals(List.of(), page.keys());
        assertEquals(List.of(), page.recordIds());
        assertEquals(NodeFormat.NO_OFFSET, page.nextLeafOffset());
    }

    @Test
    void acceptsSortedKeysAndAlignedSiblingOffset() {
        List<Integer> keys = List.of(-5, 0, 10);
        List<RecordId> recordIds = List.of(new RecordId(0), new RecordId(2), new RecordId(3));

        LeafPageData page = new LeafPageData(keys, recordIds, 8192);

        assertEquals(keys, page.keys());
        assertEquals(recordIds, page.recordIds());
        assertEquals(8192, page.nextLeafOffset());
    }

    @Test
    void copiesInputListsAndExposesImmutableLists() {
        List<Integer> keys = new ArrayList<>(List.of(1));
        List<RecordId> recordIds = new ArrayList<>(List.of(new RecordId(7)));
        LeafPageData page = new LeafPageData(keys, recordIds, NodeFormat.NO_OFFSET);

        keys.add(2);
        recordIds.add(new RecordId(8));

        assertEquals(List.of(1), page.keys());
        assertEquals(List.of(new RecordId(7)), page.recordIds());
        assertThrows(UnsupportedOperationException.class, () -> page.keys().add(3));
        assertThrows(UnsupportedOperationException.class, () -> page.recordIds().add(new RecordId(9)));
    }

    @Test
    void rejectsNullListsAndNullElements() {
        assertThrows(NullPointerException.class,
                () -> new LeafPageData(null, List.of(), NodeFormat.NO_OFFSET));
        assertThrows(NullPointerException.class,
                () -> new LeafPageData(List.of(), null, NodeFormat.NO_OFFSET));
        assertThrows(NullPointerException.class,
                () -> new LeafPageData(Arrays.asList((Integer) null), List.of(new RecordId(1)), NodeFormat.NO_OFFSET));
        assertThrows(NullPointerException.class,
                () -> new LeafPageData(List.of(1), Arrays.asList((RecordId) null), NodeFormat.NO_OFFSET));
    }

    @Test
    void rejectsDifferentKeyAndRecordIdCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new LeafPageData(List.of(1), List.of(), NodeFormat.NO_OFFSET));
    }

    @Test
    void acceptsAFullLeafAndRejectsOneExtraEntry() {
        List<Integer> fullKeys = IntStream.range(0, NodeFormat.MAX_LEAF_ENTRIES)
                .boxed().toList();
        List<RecordId> fullRecordIds = IntStream.range(0, NodeFormat.MAX_LEAF_ENTRIES)
                .mapToObj(RecordId::new).toList();
        LeafPageData fullPage = new LeafPageData(fullKeys, fullRecordIds, NodeFormat.NO_OFFSET);
        assertEquals(NodeFormat.MAX_LEAF_ENTRIES, fullPage.keys().size());

        List<Integer> keys = IntStream.range(0, NodeFormat.MAX_LEAF_ENTRIES + 1)
                .boxed().toList();
        List<RecordId> recordIds = IntStream.range(0, NodeFormat.MAX_LEAF_ENTRIES + 1)
                .mapToObj(RecordId::new).toList();

        assertThrows(IllegalArgumentException.class,
                () -> new LeafPageData(keys, recordIds, NodeFormat.NO_OFFSET));
    }

    @Test
    void rejectsDuplicateAndDescendingKeys() {
        List<RecordId> recordIds = List.of(new RecordId(1), new RecordId(2));

        assertThrows(IllegalArgumentException.class,
                () -> new LeafPageData(List.of(2, 2), recordIds, NodeFormat.NO_OFFSET));
        assertThrows(IllegalArgumentException.class,
                () -> new LeafPageData(List.of(10, 2), recordIds, NodeFormat.NO_OFFSET));
    }

    @Test
    void rejectsInvalidSiblingOffsets() {
        for (long offset : new long[]{0, -2, 100, 5000}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new LeafPageData(List.of(), List.of(), offset));
        }
    }
}
