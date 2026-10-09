package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;

import java.util.*;

/**
 * Provides an in-memory B+ tree for integer keys and record identifiers.
 *
 * <p>The tree currently starts with one leaf node and supports insertion and
 * exact-key lookup. The node-splitting behavior required to grow the tree
 * beyond that initial leaf is intentionally left for a later milestone.</p>
 *
 * <p>Keys are unique. Attempting to insert an existing key is delegated to
 * the leaf node and results in an {@link IllegalArgumentException}.</p>
 */
public class InMemoryBPlusTree implements BPlusTree {
    private final int maxKeysPerNode;

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
     * TODO: move half the entries to a new right leaf, preserve leaf links,
     * and use the first key in the right leaf as the separator.
     */
    private LeafSplit splitLeaf(LeafNode leaf) {
        //create a new leaf
        int seperatorKeyIndex = (leaf.size()/2) + 1;
        Integer seperatorKey = leaf.keyAt(seperatorKeyIndex);
        List<RecordId> rightValues = leaf.remove(seperatorKey, leaf.size());
        LeafNode rightLeaf = new LeafNode(rightValues, maxKeysPerNode);

         return new LeafSplit(seperatorKey, rightLeaf);
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
//            throw new IllegalStateException(
//                    "Cannot insert into a parent because the path is empty."
//            );
            return;
        }

        InternalNode parent = path.pop();
        parent.insertSeparator(separatorKey, rightChild);

        if (!parent.hasOverflow()) {
            return;
        }

        // Internal-node splitting and propagation will be added next.
        InternalSplit split = internalNodeSplit(parent);
        insertIntoParent(parent, split.separatorKey, split.rightNode(), path);

    }

    private record InternalSplit(
            int separatorKey,
            InternalNode rightNode
    ) {
    }
    private InternalSplit internalNodeSplit(InternalNode node){

        //create a new leaf
        int seperatorKeyIndex = (node.size()/2) + 1;
        Integer seperatorKey = node.keyAt(seperatorKeyIndex);
        List<Integer> rightKeys = node.remove(seperatorKeyIndex, node.size());
        InternalNode rightNode = new InternalNode(rightKeys, maxKeysPerNode);

        return new InternalSplit(seperatorKey, rightNode);
    }

    /** Result returned by a leaf split. */
    private record LeafSplit(int separatorKey, LeafNode rightLeaf) {
    }

    @Override
    public RecordId search(int key) {
        return null;
    }

    /**
     * Finds the record identifier associated with an exact key.
     *
     * @param key the integer key to find
     * @return the associated record identifier, or {@code null} when the key
     *         is not present
     */
    public RecordId find(int key) {
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
    }

    /**
     * Scans the tree for keys in an inclusive range.
     *
     * <p>Range scanning is reserved for the leaf-link milestone. This
     * scaffold currently returns an empty result.</p>
     *
     * @param lowerBound the smallest key to include
     * @param upperBound the largest key to include
     * @return an empty result until range scanning is implemented
     */
    @Override
    public List<RecordId> rangeScan(int lowerBound, int upperBound) {
        return List.of();
    }

    /**
     * Determines whether an exact key exists in the tree.
     *
     * @param key the key to check
     * @return {@code true} when the key exists; otherwise {@code false}
     */
    @Override
    public boolean containsKey(int key) {
        return find(key) != null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void clear() {

    }
}
