/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2012 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.trans.steps.fifocalculator;

import java.util.Date;
import java.util.Hashtable;

import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;


/**
 * @author Matt
 * @since 24-jan-2005
 *
 */
public class FIFOCalculatorData extends BaseStepData implements StepDataInterface
{
    public RowMetaInterface outputRowMeta;
    
    public Object[] one, two;
    public int[] keyNrs;
    public int[] valueNrs;
    public Object[] previousRow;
    public String previousKey;
//	public RowSet oneRowSet;
//	public RowSet twoRowSet;
	
	public RowSet resultRowSet;
	public Date lastRowDate;
	
	private Hashtable hashTable;

	public boolean readLookupValues;

	public StreamInterface infoStream;

	public boolean hasLookupRows;

	public RowMetaInterface infoMeta;

	public RowMeta keyMeta;

	public RowMeta valueMeta;

	public RowMeta keyTypes;

	public int[] keyNrsInMainStream;

	public int[] ValueNrsInMainStream;

	public int DateNrInMainStream;
	public int MultiplyNrInMainStream;

	public int outputIndex;
    
	/**
	 * 
	 */
	public FIFOCalculatorData()
	{
		super();
	}

	public Hashtable getHashTable() {
		if(hashTable==null)
			hashTable = new Hashtable();
		return hashTable;
	}

	public void setHashTable(Hashtable hashTable) {
		this.hashTable = hashTable;
	}

}
