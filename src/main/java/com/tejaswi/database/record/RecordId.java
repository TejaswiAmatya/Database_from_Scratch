package com.tejaswi.database.record;

public record RecordId(long value) {
	
	public RecordId {
		if (value < 0) {
			throw new IllegalArgumentException("RecordId must be non-negative");
		}
	}

}
