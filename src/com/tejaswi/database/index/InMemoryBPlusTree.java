package com.tejaswi.database.index;

import com.tejaswi.database.record.RecordId;

public class InMemoryBPlusTree implements BPlusTree{
	
	private final int maxKeysPerNode;
	private final LeafNode root;
	
	public InMemoryBPlusTree(int maxKeysPerNode) {
		this.maxKeysPerNode = maxKeysPerNode;
		root = null;
	}

	@Override
	public void insert(int key, RecordId recordId) {

		
	}

	@Override
	public RecordId find(int key) {

	}
	
}
