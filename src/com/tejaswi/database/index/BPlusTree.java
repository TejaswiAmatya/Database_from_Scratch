package com.tejaswi.database.index;
import com.tejaswi.database.record.RecordId;

public interface BPlusTree {
	
	
	void insert(int key, RecordId recordId);

    RecordId find(int key);

}

