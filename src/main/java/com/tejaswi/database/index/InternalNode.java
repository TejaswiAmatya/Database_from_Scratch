package com.tejaswi.database.index;

import java.util.ArrayList;
import java.util.List;

class InternalNode extends BPlusTreeNode{
    List<BPlusTreeNode> children;
    List<Integer> keys;
    int maxKeys;

    InternalNode(int maxKeys){
        super(maxKeys);
        children = new ArrayList<>();
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

    private BPlusTreeNode childFor(int key){
        return null;
    }

    private BPlusTreeNode childAt(int index){
        return null;
    }

    private boolean isOverfull(){
        return false;
    }

    private void insertSeparator(int separatorKey, BPlusTreeNode rightChild){

    }

    private void validateStructure(){

    }
}
