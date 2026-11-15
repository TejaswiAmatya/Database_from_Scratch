package com.tejaswi.database.index;

import java.util.List;

record InternalPageData(
        List<Integer> keys,
        List<Long> childOffsets
) {
    InternalPageData {
        keys = List.copyOf(keys);
        childOffsets = List.copyOf(childOffsets);

        if (keys.isEmpty()) {
            throw new IllegalArgumentException("An internal page needs at least one separator key");
        }
        if (keys.size() > NodeFormat.MAX_INTERNAL_KEYS) {
            throw new IllegalArgumentException("Too many internal page keys: " + keys.size());
        }
        if (childOffsets.size() != keys.size() + 1) {
            throw new IllegalArgumentException("An internal page needs one more child than key");
        }

        for (int i = 1; i < keys.size(); i++) {
            if (keys.get(i - 1) >= keys.get(i)) {
                throw new IllegalArgumentException("Separator keys must be strictly increasing");
            }
        }

        for (long offset : childOffsets) {
            if (offset < NodeFormat.PAGE_SIZE || offset % NodeFormat.PAGE_SIZE != 0) {
                throw new IllegalArgumentException("Invalid child page offset: " + offset);
            }
        }
    }
}
