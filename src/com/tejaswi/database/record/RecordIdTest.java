package com.tejaswi.database.record;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RecordIdTest {

	@Test
	void storesItsLongValue() {
		RecordId recordId = new RecordId(42L);
		assertEquals(42L, recordId.value());
	}
	
	@Test
	void rejectNegativeValue() {
		assertThrows(IllegalArgumentException.class, () -> new RecordId(-1L));
	}

}
