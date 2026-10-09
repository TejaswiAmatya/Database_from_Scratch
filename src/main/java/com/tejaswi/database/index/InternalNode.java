package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;

import java.util.ArrayList;
import java.util.List;

class InternalNode extends BPlusTreeNode{
    private List<BPlusTreeNode> children;

    InternalNode(int maxKeys){
        super(maxKeys);
        children = new ArrayList<>();
    }

    InternalNode(
            int maxKeys,
            BPlusTreeNode leftChild,
            int separatorKey,
            BPlusTreeNode rightChild
    ) {
        super(maxKeys);

        if (leftChild == null || rightChild == null) {
            throw new IllegalArgumentException("Children cannot be null");
        }

        keys.add(separatorKey);
        children.add(leftChild);
        children.add(rightChild);
    }

    InternalNode(List<Integer> keys, int maxKeys){
        super(maxKeys);
        this.keys = keys;
    }

    @Override
    boolean isLeaf() {
        return false;
    }

    /**
     * Given a key, determine which children does the key belong to
     * **/
    private int findChildIndex(int key){
        if (key < keys.get(0)){
            return 0;
        } else if (key > keys.get(keys.size()-1)) {
            return keys.size();
        }

        int start = 0;
        int end = keys.size() - 1;
        while (start <= end){
            int middle = (start + end) / 2;
            if (keys.get(middle) > key){
                end = middle - 1;
            } else if (keys.get(middle) < key) {
                start = middle + 1;
            }
            else{
                return middle + 1;
            }
        }
        return start;
    }


    BPlusTreeNode childFor(int key){
        int index = findChildIndex(key);
        return children.get(index);
    }

    private BPlusTreeNode childAt(int index){
        return children.get(index);
    }

    private boolean isOverfull(){
        return keys.size() > maxKeys;
    }

    void insertSeparator(int separatorKey, BPlusTreeNode rightChild){
        int i = findSeparatorInsertIndex(separatorKey);
        keys.add(i, separatorKey);

       children.add(i+1, rightChild);
    }

    private int findSeparatorInsertIndex(int separatorKey){

        int start = 0;
        int end = keys.size() - 1;

        while(start<=end){
            int mid = (start + end) / 2;

            if (separatorKey > keys.get(mid)){
                start = mid + 1;
            }

            else if (separatorKey < keys.get(mid)){
                end = mid - 1;
            }

            else{
                throw new IllegalArgumentException();
            }
        }

        return start;

    }

    List<Integer> remove(int startIndex, int endIndex){
        List<Integer> result = new ArrayList<Integer>();
        if (startIndex > 0 && endIndex < keys.size()+1){

            for(int i = startIndex + 1; i < endIndex; i++){
                result.add(keys.get(i));
            }
            keys.remove(startIndex);
            keys.remove(result);
            return result;
        }
        return null;
    }

    public int size(){
        return keys.size();
    }


    private void validateStructure() {
        if (keys == null) {
            throw new IllegalStateException("Internal node keys list cannot be null.");
        }

        if (children == null) {
            throw new IllegalStateException("Internal node children list cannot be null.");
        }

        if (children.size() != keys.size() + 1) {
            throw new IllegalStateException(
                    "Invalid internal node: expected " + (keys.size() + 1)
                            + " children for " + keys.size()
                            + " separator keys, but found " + children.size() + "."
            );
        }

        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i) == null) {
                throw new IllegalStateException(
                        "Invalid internal node: separator key at index " + i + " is null."
                );
            }
        }

        for (int i = 1; i < keys.size(); i++) {
            int previousKey = keys.get(i - 1);
            int currentKey = keys.get(i);

            if (previousKey >= currentKey) {
                throw new IllegalStateException(
                        "Invalid internal node: separator keys must be strictly increasing. "
                                + "Found " + previousKey + " before " + currentKey + "."
                );
            }
        }

        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == null) {
                throw new IllegalStateException(
                        "Invalid internal node: child at index " + i + " is null."
                );
            }
        }
    }
}
