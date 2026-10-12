package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class LeafNode extends BPlusTreeNode{
    private final List<RecordId> values;
    private LeafNode nextLeaf;

    LeafNode(int maxKeys){
        super(maxKeys);
        values = new ArrayList<RecordId>();
    }

    LeafNode(List<Integer> keys, List<RecordId> values, int maxKeys) {
        super(maxKeys);
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("A leaf needs one value for every key");
        }
        this.keys = new ArrayList<>(keys);
        this.values = new ArrayList<>(values);
    }
    /**
     * Finds the correct sorted position.
     * Rejects a duplicate key with a meaningful exception.
     * Inserts the key and record ID at the same position in both lists.
     * **/
    void insert(int key, RecordId recordId){
        Objects.requireNonNull(recordId, "recordId cannot be null");

        int index = findInsertPosition(key);

        if (index < keys.size() && keys.get(index) == key){
            throw new IllegalArgumentException("Duplicate key: " + key);
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
     * Appends values whose keys fall within the inclusive range to the result.
     * The values are appended in key order because this leaf stores both lists
     * in matching sorted order.
     */
    void appendRange(int lowerBound, int upperBound, List<RecordId> result) {
        int start = findInsertPosition(lowerBound);

        for (int index = start; index < keys.size(); index++) {
            int key = keys.get(index);
            if (key > upperBound) {
                break;
            }
            result.add(values.get(index));
        }
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

    int size(){
        return values.size();
    }

    @Override
    boolean isLeaf() {
        return true;
    }

    List<RecordId> remove(int startIndex, int endIndex){
        if (startIndex < 0 || startIndex >= endIndex || endIndex > values.size()) {
            throw new IndexOutOfBoundsException("Invalid leaf range: " + startIndex + ".." + endIndex);
        }

        List<RecordId> result = new ArrayList<>(values.subList(startIndex, endIndex));
        keys.subList(startIndex, endIndex).clear();
        values.subList(startIndex, endIndex).clear();
        return result;
    }

    LeafNode nextLeaf() {
        return nextLeaf;
    }

    void setNextLeaf(LeafNode nextLeaf) {
        this.nextLeaf = nextLeaf;
    }
}
