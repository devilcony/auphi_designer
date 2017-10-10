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

package org.pentaho.di.trans.steps.fieldschangeaccumulator;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * @author Samatar
 * @since 16-06-2008
 *
 */
public class FieldsChangeAccumulatorData extends BaseStepData implements StepDataInterface
{

	/**
	 * 
	 */
	public ValueMetaInterface fieldnrsMeta[];
	public RowMetaInterface previousMeta;
    public RowMetaInterface outputRowMeta;
    
	public int     fieldnrs[]; 
	public Object   previousValues[]; 
	public int     fieldnr;
//	public long     start;
//	public long     incrementBy;

	public int nextIndexField;
	public int valuenrs; //Field index for accumlate value
	public Double previousValue=0d; //accumlate value in previous row
	public Double     newValue;
	
	public int continueValuenrs; //Field index for continue value
	public Object previousContinueValue= null;//continue value in previous row
	public Object newContinueValue;
	
//	boolean isFillContinue = true;
	public FieldsChangeAccumulatorData()
	{
		super();
	}

}
