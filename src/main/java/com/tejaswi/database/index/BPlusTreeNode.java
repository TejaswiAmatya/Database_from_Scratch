package com.tejaswi.database.index;

import java.util.ArrayList;
import java.util.List;

abstract class BPlusTreeNode {
    protected final int maxKeys;
    protected List<Integer> keys;

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

    Integer keyAt(int index) {
        if (index < 0 || index >= keys.size()){
            throw new IndexOutOfBoundsException();
        }
        return keys.get(index);
    }

    int findInsertionIndex(int key) {
        int start = 0;
        int end = keys.size();
        while (start < end){
            int mid = start + (end - start) / 2;
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

    boolean hasOverflow() {
        return keys.size() > maxKeys;
    }

    void deleteKey(Integer key){
        int index = findInsertionIndex(key);
        keys.remove(index);
    }

    boolean contains(int key){
        int start = 0;
        int end = keys.size();
        while (start < end){
            int mid = start + (end - start) / 2;
            if (key > keys.get(mid)){
                start = mid + 1;
            } else if (key < keys.get(mid)) {
                end = mid;
            }

            else{
                return true;
            }
        }
        return false;
    }

    // Optional later:
    // boolean hasOverflow()
    // int firstKey()
    // List<Integer> keysView()


}
