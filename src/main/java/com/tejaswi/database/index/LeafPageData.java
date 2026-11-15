package com.tejaswi.database.index;

import java.util.List;

import com.tejaswi.database.record.RecordId;

record LeafPageData(
        List<Integer> keys,
        List<RecordId> recordIds,
        long nextLeafOffset
) {
    LeafPageData {
        keys = List.copyOf(keys);
        recordIds = List.copyOf(recordIds);

        if (keys.size() != recordIds.size()) {
            throw new IllegalArgumentException(
                    "Each key needs exactly one record ID");
        }

        if(keys.size() > NodeFormat.MAX_LEAF_ENTRIES){
            throw new IllegalArgumentException("Key should have less than max leaf entries");
        }

        if (nextLeafOffset != NodeFormat.NO_OFFSET
                && (nextLeafOffset < NodeFormat.PAGE_SIZE
                || nextLeafOffset % NodeFormat.PAGE_SIZE != 0)) {
            throw new IllegalArgumentException("Invalid next leaf offset");
        }

        for (int i = 1; i < keys.size(); i++){
            if (keys.get(i-1) >= keys.get(i)){
                throw new IllegalArgumentException("Keys should be in increasing order.");
            }
        }
    }
}
