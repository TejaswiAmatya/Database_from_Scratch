package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;

public class InMemoryBPlusTree implements BPlusTree{
	
	private final int maxKeysPerNode;
	private final BPlusTreeNode root;
	
	public InMemoryBPlusTree(int maxKeysPerNode) {
		this.maxKeysPerNode = maxKeysPerNode;
		root = new LeafNode(maxKeysPerNode);
	}

	@Override
	public void insert(int key, RecordId recordId) {
		BPlusTreeNode current = root;

		while (!current.isLeaf()){
			InternalNode internal = (InternalNode) current;
			current = internal.childFor(key);
		}

		LeafNode leaf = (LeafNode) current;
		leaf.insert(key, recordId);
	}

	@Override
	public RecordId find(int key) {
		BPlusTreeNode current = root;

		while (!current.isLeaf()) {
			InternalNode internal = (InternalNode) current;
			current = internal.childFor(key);
		}

		return ((LeafNode) current).find(key);
	}
	
}
