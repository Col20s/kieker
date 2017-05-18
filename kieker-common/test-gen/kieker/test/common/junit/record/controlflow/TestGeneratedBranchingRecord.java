/***************************************************************************
 * Copyright 2017 Kieker Project (http://kieker-monitoring.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package kieker.test.common.junit.record.controlflow;

import org.junit.Assert;
import org.junit.Test;

import kieker.common.record.controlflow.BranchingRecord;
import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.test.common.junit.AbstractGeneratedKiekerTest;

/**
 * Creates {@link OperationExecutionRecord}s via the available constructors and
 * checks the values passed values via getters.
 * 
 * @author Andre van Hoorn, Jan Waller
 * 
 * @since 1.2
 */
public class TestGeneratedBranchingRecord extends AbstractGeneratedKiekerTest {

	public TestGeneratedBranchingRecord() {
		// empty default constructor
	}

	/**
	 * Tests {@link BranchingRecord#TestBranchingRecord(long, int, int)}.
	 */
	@Test
	public void testToArray() { // NOPMD (assert missing)
		for (int i=0;i<ARRAY_LENGTH;i++) {
			// initialize
			BranchingRecord record = new BranchingRecord(LONG_VALUES.get(i % LONG_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()));
			
			// check values
			Assert.assertEquals("BranchingRecord.timestamp values are not equal.", (long) LONG_VALUES.get(i % LONG_VALUES.size()), record.getTimestamp());
			Assert.assertEquals("BranchingRecord.branchID values are not equal.", (int) INT_VALUES.get(i % INT_VALUES.size()), record.getBranchID());
			Assert.assertEquals("BranchingRecord.branchingOutcome values are not equal.", (int) INT_VALUES.get(i % INT_VALUES.size()), record.getBranchingOutcome());
			
			Object[] values = record.toArray();
			
			Assert.assertNotNull("Record array serialization failed. No values array returned.", values);
			Assert.assertEquals("Record array size does not match expected number of properties 3.", 3, values.length);
			
			// check all object values exist
			Assert.assertNotNull("Array value [0] of type Long must be not null.", values[0]); 
			Assert.assertNotNull("Array value [1] of type Integer must be not null.", values[1]); 
			Assert.assertNotNull("Array value [2] of type Integer must be not null.", values[2]); 
			
			// check all types
			Assert.assertTrue("Type of array value [0] " + values[0].getClass().getCanonicalName() + " does not match the desired type Long", values[0] instanceof Long);
			Assert.assertTrue("Type of array value [1] " + values[1].getClass().getCanonicalName() + " does not match the desired type Integer", values[1] instanceof Integer);
			Assert.assertTrue("Type of array value [2] " + values[2].getClass().getCanonicalName() + " does not match the desired type Integer", values[2] instanceof Integer);
								
			// check all object values 
			Assert.assertEquals("Array value [0] " + values[0] + " does not match the desired value " + LONG_VALUES.get(i % LONG_VALUES.size()),
				LONG_VALUES.get(i % LONG_VALUES.size()), values[0]
					);
			Assert.assertEquals("Array value [1] " + values[1] + " does not match the desired value " + INT_VALUES.get(i % INT_VALUES.size()),
				INT_VALUES.get(i % INT_VALUES.size()), values[1]
					);
			Assert.assertEquals("Array value [2] " + values[2] + " does not match the desired value " + INT_VALUES.get(i % INT_VALUES.size()),
				INT_VALUES.get(i % INT_VALUES.size()), values[2]
					);
		}
	}
	
	/**
	 * Tests {@link BranchingRecord#TestBranchingRecord(long, int, int)}.
	 */
	@Test
	public void testBuffer() { // NOPMD (assert missing)
		for (int i=0;i<ARRAY_LENGTH;i++) {
			// initialize
			BranchingRecord record = new BranchingRecord(LONG_VALUES.get(i % LONG_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()));
			
			// check values
			Assert.assertEquals("BranchingRecord.timestamp values are not equal.", (long) LONG_VALUES.get(i % LONG_VALUES.size()), record.getTimestamp());
			Assert.assertEquals("BranchingRecord.branchID values are not equal.", (int) INT_VALUES.get(i % INT_VALUES.size()), record.getBranchID());
			Assert.assertEquals("BranchingRecord.branchingOutcome values are not equal.", (int) INT_VALUES.get(i % INT_VALUES.size()), record.getBranchingOutcome());
		}
	}
	
	/**
	 * Tests {@link BranchingRecord#TestBranchingRecord(long, int, int)}.
	 */
	@Test
	public void testParameterConstruction() { // NOPMD (assert missing)
		for (int i=0;i<ARRAY_LENGTH;i++) {
			// initialize
			BranchingRecord record = new BranchingRecord(LONG_VALUES.get(i % LONG_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()));
			
			// check values
			Assert.assertEquals("BranchingRecord.timestamp values are not equal.", (long) LONG_VALUES.get(i % LONG_VALUES.size()), record.getTimestamp());
			Assert.assertEquals("BranchingRecord.branchID values are not equal.", (int) INT_VALUES.get(i % INT_VALUES.size()), record.getBranchID());
			Assert.assertEquals("BranchingRecord.branchingOutcome values are not equal.", (int) INT_VALUES.get(i % INT_VALUES.size()), record.getBranchingOutcome());
		}
	}
	
	@Test
	public void testEquality() {
		int i = 0;
		BranchingRecord oneRecord = new BranchingRecord(LONG_VALUES.get(i % LONG_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()));
		i = 0;
		BranchingRecord copiedRecord = new BranchingRecord(LONG_VALUES.get(i % LONG_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()));
		
		Assert.assertEquals(oneRecord, copiedRecord);
	}	
	
	@Test
	public void testUnequality() {
		int i = 0;
		BranchingRecord oneRecord = new BranchingRecord(LONG_VALUES.get(i % LONG_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()));
		i = 2;
		BranchingRecord anotherRecord = new BranchingRecord(LONG_VALUES.get(i % LONG_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()), INT_VALUES.get(i % INT_VALUES.size()));
		
		Assert.assertNotEquals(oneRecord, anotherRecord);
	}
}
