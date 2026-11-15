package com.tejaswi.database.index;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalPageDataTest {

    @Test
    void acceptsOneSeparatorAndTwoChildren() {
        InternalPageData page = new InternalPageData(List.of(0), List.of(4096L, 8192L));

        assertEquals(List.of(0), page.keys());
        assertEquals(List.of(4096L, 8192L), page.childOffsets());
    }

    @Test
    void acceptsOrderedSeparatorsAndAlignedChildren() {
        InternalPageData page = new InternalPageData(
                List.of(-10, 0, 20),
                List.of(4096L, 8192L, 12288L, 16384L));

        assertEquals(List.of(-10, 0, 20), page.keys());
        assertEquals(4, page.childOffsets().size());
    }

    @Test
    void copiesInputListsAndExposesImmutableLists() {
        List<Integer> keys = new ArrayList<>(List.of(5));
        List<Long> childOffsets = new ArrayList<>(List.of(4096L, 8192L));
        InternalPageData page = new InternalPageData(keys, childOffsets);

        keys.add(10);
        childOffsets.add(12288L);

        assertEquals(List.of(5), page.keys());
        assertEquals(List.of(4096L, 8192L), page.childOffsets());
        assertThrows(UnsupportedOperationException.class, () -> page.keys().add(10));
        assertThrows(UnsupportedOperationException.class, () -> page.childOffsets().add(12288L));
    }

    @Test
    void rejectsNullListsAndElements() {
        assertThrows(NullPointerException.class,
                () -> new InternalPageData(null, List.of(4096L, 8192L)));
        assertThrows(NullPointerException.class,
                () -> new InternalPageData(List.of(1), null));
        assertThrows(NullPointerException.class,
                () -> new InternalPageData(Arrays.asList((Integer) null), List.of(4096L, 8192L)));
        assertThrows(NullPointerException.class,
                () -> new InternalPageData(List.of(1), Arrays.asList(4096L, (Long) null)));
    }

    @Test
    void rejectsEmptyInternalPage() {
        assertThrows(IllegalArgumentException.class,
                () -> new InternalPageData(List.of(), List.of(4096L)));
    }

    @Test
    void rejectsMissingOrExtraChildren() {
        assertThrows(IllegalArgumentException.class,
                () -> new InternalPageData(List.of(5), List.of(4096L)));
        assertThrows(IllegalArgumentException.class,
                () -> new InternalPageData(List.of(5), List.of(4096L, 8192L, 12288L)));
    }

    @Test
    void rejectsDuplicateAndDescendingSeparators() {
        List<Long> childOffsets = List.of(4096L, 8192L, 12288L);

        assertThrows(IllegalArgumentException.class,
                () -> new InternalPageData(List.of(5, 5), childOffsets));
        assertThrows(IllegalArgumentException.class,
                () -> new InternalPageData(List.of(10, 5), childOffsets));
    }

    @Test
    void acceptsMaximumKeysAndRejectsOneMore() {
        List<Integer> fullKeys = IntStream.range(0, NodeFormat.MAX_INTERNAL_KEYS).boxed().toList();
        List<Long> fullChildren = IntStream.rangeClosed(1, NodeFormat.MAX_INTERNAL_KEYS + 1)
                .mapToLong(i -> (long) i * NodeFormat.PAGE_SIZE).boxed().toList();
        InternalPageData fullPage = new InternalPageData(fullKeys, fullChildren);
        assertEquals(NodeFormat.MAX_INTERNAL_KEYS, fullPage.keys().size());

        List<Integer> tooManyKeys = IntStream.rangeClosed(0, NodeFormat.MAX_INTERNAL_KEYS).boxed().toList();
        List<Long> tooManyChildren = IntStream.rangeClosed(1, NodeFormat.MAX_INTERNAL_KEYS + 2)
                .mapToLong(i -> (long) i * NodeFormat.PAGE_SIZE).boxed().toList();
        assertThrows(IllegalArgumentException.class,
                () -> new InternalPageData(tooManyKeys, tooManyChildren));
    }

    @Test
    void rejectsMissingAndUnalignedChildOffsets() {
        for (long invalidOffset : new long[]{NodeFormat.NO_OFFSET, 0, 100, 5000}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new InternalPageData(List.of(5), List.of(4096L, invalidOffset)));
        }
    }
}
