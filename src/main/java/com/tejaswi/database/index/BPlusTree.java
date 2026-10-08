package com.tejaswi.database.index;
import com.tejaswi.database.record.RecordId;

import java.util.List;

public interface BPlusTree {

	/**
	 * Inserts a unique key and its associated record identifier.
	 *
	 * @param key the key to insert
	 * @param recordId the associated record identifier
	 */
	void insert(int key, RecordId recordId);

    /**
     * Finds the record identifier associated with an exact key.
     *
     * @param key the key to find
     * @return the associated record identifier, or {@code null} if absent
     */
    RecordId search(int key);

    /**
     * Removes the record associated with an exact key.
     *
     * @param key the key to remove
     */
    void delete(int key);

    /**
     * Returns record identifiers whose keys fall within an inclusive range.
     *
     * @param lowerBound the smallest key to include
     * @param upperBound the largest key to include
     * @return matching record identifiers in key order
     */
    List<RecordId> rangeScan(int lowerBound, int upperBound);

    /**
     * Determines whether an exact key exists in the tree.
     *
     * @param key the key to check
     * @return {@code true} when the key exists; otherwise {@code false}
     */
    boolean containsKey(int key);

    int size();
    boolean isEmpty();
    void clear();

}
