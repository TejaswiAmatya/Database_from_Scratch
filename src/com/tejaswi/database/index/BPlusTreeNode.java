package com.tejaswi.database.index;

import java.util.ArrayList;
import java.util.List;

abstract class BPlusTreeNode {
    protected final int maxKeys;
    protected final List<Integer> keys;

    protected BPlusTreeNode(int maxKeys) {
        if(maxKeys <= 0){
            throw new IndexOutOfBoundsException();
        }
        this.maxKeys = maxKeys;
        keys = new ArrayList<>();

    }

    abstract boolean isLeaf();

    int keyCount(){
        return keys.size();
    }

    boolean isEmpty() {
        return keys.isEmpty();
    }

    boolean isFull() {
        return keys.size() == maxKeys;
    }

    int keyAt(int index) {
        if (index < 0 || index >= keys.size()){
            throw new IndexOutOfBoundsException();
        }
        return keys.get(index);
    }

    int findInsertionIndex(int key) {
        int start = 0;
        int end = keys.size();
        while (start < end){
            int mid = (start + end) / 2;
            if (key > keys.get(mid)){
                start = mid + 1;
            } else if (key < keys.get(mid)) {
                end = mid;
            }

            else{
                return mid;
            }
        }
        return start;
    }

    // Optional later:
    // boolean hasOverflow()
    // int firstKey()
    // List<Integer> keysView()
}
