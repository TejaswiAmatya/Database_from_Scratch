package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;

import java.util.*;

/**
 * Provides an in-memory B+ tree for integer keys and record identifiers.
 *
 * <p>The tree starts with one leaf node and grows through leaf, internal-node,
 * and root splits as keys are inserted.</p>
 *
 * <p>Keys are unique. Attempting to insert an existing key is delegated to
 * the leaf node and results in an {@link IllegalArgumentException}.</p>
 */
public class InMemoryBPlusTree implements BPlusTree {
    private final int maxKeysPerNode;
    private int size;

    // The root must be replaceable when the root splits.
    private BPlusTreeNode root;

    /**
     * Creates an empty in-memory B+ tree.
     *
     * @param maxKeysPerNode the maximum number of keys a node may contain
     * @throws IndexOutOfBoundsException if {@code maxKeysPerNode} is not positive
     */
    public InMemoryBPlusTree(int maxKeysPerNode) {
        root = new LeafNode(maxKeysPerNode);
        this.maxKeysPerNode = maxKeysPerNode;
    }



    /**
     * Inserts a key and its associated record identifier into the tree.
     *
     * <p>The key is inserted into sorted position in the leaf reached by
     * traversal from the root. Duplicate keys are rejected.</p>
     *
     * @param key the integer key to insert
     * @param recordId the record identifier associated with the key
     * @throws IllegalArgumentException if the key already exists
     * @throws NullPointerException if {@code recordId} is {@code null}
     */
    @Override
    public void insert(int key, RecordId recordId) {
        Objects.requireNonNull(recordId, "recordId cannot be null");

        // 1. Find the leaf where this key belongs.
        Deque<InternalNode> path = new ArrayDeque<>();
        LeafNode leaf = findLeafFor(key, path);

        // 2. Insert in sorted order. A temporary overflow is allowed so the
        //    entries can be divided during the split step below.
        leaf.insert(key, recordId);
        size++;

        // 3. A non-overflowing leaf completes the insertion.
        if (!leaf.hasOverflow()) {
            return;
        }

        // 4. Split the overflowing leaf and promote a separator key.
        LeafSplit split = splitLeaf(leaf);

        // 5. A root split creates a new internal root.
        if (leaf == root) {
            createNewRoot(leaf, split.separatorKey(), split.rightLeaf());
            return;
        }

        // 6. Otherwise, add the new right child to the existing parent.
        //    Parent overflow must be propagated upward by this method.
        insertIntoParent(leaf, split.separatorKey(), split.rightLeaf(), path);
    }

    /**
     * Splits an overflowing leaf.
     *
     * Moves the upper half of the entries to a new right leaf, preserves the
     * leaf chain, and uses the first right-hand key as the separator.
     */
    private LeafSplit splitLeaf(LeafNode leaf) {
        int splitIndex = (leaf.size() + 1) / 2;
        List<Integer> rightKeys = new ArrayList<>(leaf.keys.subList(splitIndex, leaf.size()));
        List<RecordId> rightValues = leaf.remove(splitIndex, leaf.size());
        LeafNode rightLeaf = new LeafNode(rightKeys, rightValues, maxKeysPerNode);
        rightLeaf.setNextLeaf(leaf.nextLeaf());
        leaf.setNextLeaf(rightLeaf);

        return new LeafSplit(rightLeaf.keyAt(0), rightLeaf);
    }

    /** Creates the new root after the old root leaf has split. */
    private void createNewRoot(
            BPlusTreeNode leftChild,
            int separatorKey,
            BPlusTreeNode rightChild
    ) {
        root = new InternalNode(
                maxKeysPerNode,
                leftChild,
                separatorKey,
                rightChild
        );
    }

    /** Finding leaf for the key **/
    private LeafNode findLeafFor(int key, Deque<InternalNode> path){
            BPlusTreeNode current = root;

            while (!current.isLeaf()) {
                InternalNode internal = (InternalNode) current;
                path.push(internal);
                current = internal.childFor(key);
            }

            return (LeafNode) current;
    }

    /** Adds a separator and right child to the existing parent. */
    private void insertIntoParent(
            BPlusTreeNode leftChild,
            Integer separatorKey,
            BPlusTreeNode rightChild,
            Deque<InternalNode> path
    ) {
        if (path.isEmpty()) {
            throw new IllegalStateException("Cannot insert into a parent because the path is empty");
        }

        InternalNode parent = path.pop();
        parent.insertSeparator(separatorKey, rightChild);

        if (!parent.hasOverflow()) {
            return;
        }

        InternalSplit split = internalNodeSplit(parent);

        if (parent == root) {
            createNewRoot(parent, split.separatorKey, split.rightNode());
            return;
        }

        insertIntoParent(parent, split.separatorKey, split.rightNode(), path);

    }

    private record InternalSplit(
            int separatorKey,
            InternalNode rightNode
    ) {
    }
    private InternalSplit internalNodeSplit(InternalNode node){
        int separatorIndex = node.size() / 2;
        int separatorKey = node.keyAt(separatorIndex);

        List<Integer> rightKeys = new ArrayList<>(node.keys.subList(separatorIndex + 1, node.size()));
        List<BPlusTreeNode> rightChildren = new ArrayList<>(
                node.children().subList(separatorIndex + 1, node.children().size()));
        InternalNode rightNode = new InternalNode(rightKeys, rightChildren, maxKeysPerNode);

        node.keys.subList(separatorIndex, node.size()).clear();
        node.children().subList(separatorIndex + 1, node.children().size()).clear();

        return new InternalSplit(separatorKey, rightNode);
    }

    /** Result returned by a leaf split. */
    private record LeafSplit(int separatorKey, LeafNode rightLeaf) {
    }

    @Override
    public RecordId search(int key) {
        BPlusTreeNode current = root;

        while (!current.isLeaf()) {
            InternalNode internal = (InternalNode) current;
            current = internal.childFor(key);
        }

        return ((LeafNode) current).find(key);
    }

    /**
     * Removes the record associated with an exact key.
     *
     * <p>Deletion is reserved for the B+ tree deletion milestone. This
     * scaffold currently performs no operation.</p>
     *
     * @param key the key to remove
     */
    @Override
    public void delete(int key) {
        // TODO: implement leaf deletion and underflow handling.

        //search for the key
        Deque<InternalNode> path = new ArrayDeque<>();
        LeafNode leaf = findLeafFor(key, path);

        if (leaf.size() + 1 > minChildren()){
            leaf.deleteKey(key);
        }

        while (!path.isEmpty()){
            InternalNode currNode = path.pop();

        }


        //
    }

    /**
     * Scans the tree for keys in an inclusive range.
     *
     * <p>The search begins at the leaf that could contain the lower bound and
     * then follows the linked leaves from left to right. This avoids scanning
     * unrelated leaves while preserving key order.</p>
     *
     * @param lowerBound the smallest key to include
     * @param upperBound the largest key to include
     * @return matching record identifiers in ascending key order
     */
    @Override
    public List<RecordId> rangeScan(int lowerBound, int upperBound) {
        if (lowerBound > upperBound) {
            return List.of();
        }

        LeafNode leaf = findLeafFor(lowerBound, new ArrayDeque<>());
        List<RecordId> result = new ArrayList<>();

        while (leaf != null) {
            leaf.appendRange(lowerBound, upperBound, result);

            // Once the last key in this leaf exceeds the requested range,
            // all later leaves will also be outside the range.
            if (!leaf.keys.isEmpty() && leaf.keyAt(leaf.keyCount() - 1) >= upperBound) {
                break;
            }

            leaf = leaf.nextLeaf();
        }

        return result;
    }

    /**
     * Determines whether an exact key exists in the tree.
     *
     * @param key the key to check
     * @return {@code true} when the key exists; otherwise {@code false}
     */
    @Override
    public boolean containsKey(int key) {
        return search(key) != null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        root = new LeafNode(maxKeysPerNode);
        size = 0;
    }

    public int minChildren(){
        return Math.ceilDiv(maxKeysPerNode, 2);
    }

    public int minKeys(){
        return minChildren() - 1;
    }
}
