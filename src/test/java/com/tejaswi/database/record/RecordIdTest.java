package com.tejaswi.database.record;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
