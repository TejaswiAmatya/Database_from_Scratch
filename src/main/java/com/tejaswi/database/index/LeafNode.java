package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;

import java.util.ArrayList;
import java.util.List;

class LeafNode extends BPlusTreeNode{
    private final List<RecordId> values;

    public LeafNode(int maxKeys){
        super(maxKeys);
        values = new ArrayList<RecordId>();
    }
    /**
     * Finds the correct sorted position.
     * Rejects a duplicate key with a meaningful exception.
     * Inserts the key and record ID at the same position in both lists.
     * **/
    void insert(int key, RecordId recordId){
        /**
         * Call findInsertPosition(key) and store the returned position.
         * Check whether that position is within the current list bounds.
         * If it is, check whether keys[position] is equal to key.
         * If equal, throw an IllegalArgumentException for a duplicate key.
         * Otherwise, insert the new key at position.
         * Insert the associated recordId at the same position in the values list.
         * **/

        int index = findInsertPosition(key);

        if (index < keys.size() && keys.get(index) == key){
            throw new IllegalArgumentException();
        }

        keys.add(index, key);
        values.add(index, recordId);
    }


    /**
     * Locates candidate Position Key
     * Returns the matching recordingId if it exists
     * returns null if it doesn't exist
     * **/
    RecordId find(int key){
        int index = findInsertPosition(key);

        if (index >= keys.size() || keys.get(index) != key){
            return null;
        }
        return values.get(index);
    }

    /**
     * Binary Search type logic to return the index that where the given key should be inserted
     * **/
    private int findInsertPosition(int key){
        int start = 0;
        int end = keys.size();
        while (start < end){
            int mid = (start + end) / 2;
            if (key > keys.get(mid)){
                start = mid + 1;
            } else if (key < keys.get(mid)) {
                end = mid;
            } else {
                return mid;
            }
        }
        return start;
    }


    @Override
    boolean isLeaf() {
        return true;
    }
}
