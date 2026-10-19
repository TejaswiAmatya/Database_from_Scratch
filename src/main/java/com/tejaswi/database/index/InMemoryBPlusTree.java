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
     * <p>This currently supports deletion when the leaf remains above its
     * minimum occupancy. Underflow rebalancing is still a later milestone.</p>
     *
     * @param key the key to remove
     */
    @Override
    public void delete(int key) {
        /*
         * B+ tree deletion roadmap
         * ------------------------
         *
         * Step 1: Find the target leaf.
         *
         *   Traverse from the root to the leaf that should contain `key`.
         *   Keep every internal node on the path because those parents are
         *   needed later if the leaf underflows.
         *
         * Step 2: Stop if the key does not exist.
         *
         *   Deleting a missing key must not change the tree or its `size`.
         *
         * Step 3: Remove the key and its RecordId from the leaf.
         *
         *   The key and value lists must remain aligned. Remove both entries
         *   at the same index, then decrement the tree size exactly once.
         *
         * Step 4: Handle the root-leaf case.
         *
         *   The root is allowed to contain fewer keys than ordinary leaves,
         *   including zero keys. If the target leaf is the root, deletion is
         *   complete after Step 3.
         *
         * Step 5: Update separator keys in internal nodes.
         *
         *   Internal keys are routing boundaries, not separate records. If
         *   the deleted key was the first key of a right-hand child, replace
         *   that separator with the child's new first key. Do not remove the
         *   separator unless two children are merged later.
         *
         * Step 6: Check for leaf underflow.
         *
         *   If the leaf still has its minimum number of keys, stop. Otherwise
         *   try to repair it before changing the parent:
         *
         *   a. Borrow the smallest entry from the right sibling, if that sibling
         *      has more than its minimum occupancy. Update the parent
         *      separator to the leaf's new first key.
         *
         *   b. Otherwise borrow the smallest entry from the right sibling, if
         *      possible. Update the parent separator to the right sibling's
         *      new first key.
         *
         *   c. If neither sibling can lend an entry, merge the leaf with one
         *      sibling. Preserve the left-to-right leaf links, remove the
         *      merged-away child from the parent, and remove the separator
         *      between those children.
         *
         * Step 7: Repair an underflowing internal parent.
         *
         *   After a merge, the parent may have too few children. Move upward
         *   through the saved path and repeat the same borrow-or-merge idea,
         *   this time moving child pointers and separator keys together.
         *
         * Step 8: Shrink the root when necessary.
         *
         *   If the root becomes an internal node with zero keys and one child,
         *   make that child the new root. This reduces the tree height by one.
         *
         * Current implementation status:
         *   - Steps 1-3 and separator updates from Step 5 are implemented
         *     below.
         *   - The explicit root-leaf case from Step 4 still needs to be added
         *     before the underflow check.
         *   - Leaf underflow borrowing and merging are still TODO.
         *   - Internal-node repair and root shrinking are still TODO.
         */

        Deque<InternalNode> path = new ArrayDeque<>();
        LeafNode leaf = findLeafFor(key, path);

        if (leaf.find(key) == null) {
            return;
        }

        leaf.deleteKey(key);
        size--;

        if (leaf == root){
            return;
        }

        InternalNode parent = path.peek();
        // Internal keys are separators, not independent records. If the
        // deleted key was a separator, replace it with the first key in the
        // right-hand subtree so searches continue to route correctly.
        while (!path.isEmpty()){
            InternalNode currNode = path.pop();
            if (currNode.contains(key) && !leaf.isEmpty()) {
                int separatorIndex = currNode.findInsertionIndex(key);
                currNode.keys.set(separatorIndex, leaf.keyAt(0));
            }
        }

        if (leaf.size() < minKeys()){
            assert parent != null;
            int childIndex = parent.children().indexOf(leaf);

            boolean repaired = false;

            // Borrow from the right sibling when it has an extra key.
            int rightSiblingIndex = childIndex + 1;
            if (rightSiblingIndex < parent.children().size()) {
                LeafNode rightSibling = (LeafNode) parent.children().get(rightSiblingIndex);

                if (rightSibling.size() > minKeys()) {
                    LeafNode.LeafEntry entry = rightSibling.removeFirstEntry();
                    leaf.insert(entry.key(), entry.recordId());
                    parent.keys.set(childIndex, rightSibling.keyAt(0));
                    repaired = true;
                }
            }

            // Otherwise, borrow from the left sibling when possible.
            if (!repaired && childIndex - 1 >= 0){
                int leftSiblingIndex = childIndex - 1;
                LeafNode leftSibling = (LeafNode) parent.children().get(leftSiblingIndex);

                if(leftSibling.size() > minKeys()){
                    LeafNode.LeafEntry entry = leftSibling.removeLastEntry();
                    leaf.insert(entry.key(), entry.recordId());
                    parent.keys.set(childIndex - 1, leaf.keyAt(0));
                    repaired = true;
                }
            }

            if (!repaired) {
                /*
                 * Neither sibling could lend an entry. Merge with the right
                 * sibling when one exists; otherwise merge this leaf into the
                 * left sibling. In both cases, remove the merged-away child
                 * and the separator between the two children from the parent.
                 */
                if (rightSiblingIndex < parent.children().size()) {
                    LeafNode rightSibling =
                            (LeafNode) parent.children().get(rightSiblingIndex);

                    leaf.appendEntriesFrom(rightSibling);
                    parent.children().remove(rightSiblingIndex);
                    parent.keys.remove(childIndex);
                } else if (childIndex > 0) {
                    LeafNode leftSibling =
                            (LeafNode) parent.children().get(childIndex - 1);

                    leftSibling.appendEntriesFrom(leaf);
                    parent.children().remove(childIndex);
                    parent.keys.remove(childIndex - 1);
                }
            }
        }
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
