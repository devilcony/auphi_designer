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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.pentaho.di.compatibility.Value;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Add Accumulator value to each input row.
 * 
 */

public class FieldsChangeAccumulator extends BaseStep implements StepInterface 
{
	private static Class<?> PKG = FieldsChangeAccumulatorMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private FieldsChangeAccumulatorMeta meta;
	private FieldsChangeAccumulatorData data; 
	public boolean test;
	
	public FieldsChangeAccumulator()
	{
		super(null, null, 0, null, null);
	}
	
	public FieldsChangeAccumulator(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(FieldsChangeAccumulatorMeta)smi;
		data=(FieldsChangeAccumulatorData)sdi;

		Object[] r=getRow();    // get row, set busy!
		if (r==null)  // no more input to be expected...
		{
			setOutputDone();
			return false;
		}
		
		if(first)
		{
			// get the RowMeta
			data.previousMeta = getInputRowMeta().clone();
			data.nextIndexField = data.previousMeta .size();
            data.outputRowMeta = getInputRowMeta().clone();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
            data.valuenrs= data.previousMeta.indexOfValue(meta.getValueField());
            if(meta.getContinueValueField()!=null)
            	data.continueValuenrs = data.previousMeta.indexOfValue(meta.getContinueValueField());
            else
            	data.continueValuenrs = -1;
            
			if(meta.getFieldName()!=null && meta.getFieldName().length>0)
			{
				data.fieldnr=meta.getFieldName().length;
				data.fieldnrs=new int[data.fieldnr];
				data.previousValues=new Object[data.fieldnr];
				data.fieldnrsMeta = new ValueMeta[data.fieldnr];
				for (int i=0;i<data.fieldnr;i++)
				{
					data.fieldnrs[i]=data.previousMeta.indexOfValue(meta.getFieldName()[i] );
					if (data.fieldnrs[i]<0)
					{
						logError(BaseMessages.getString(PKG, "FieldsChangeSequence.Log.CanNotFindField",meta.getFieldName()[i]));
						throw new KettleException(BaseMessages.getString(PKG, "FieldsChangeSequence.Log.CanNotFindField",meta.getFieldName()[i]));
					}
					data.fieldnrsMeta[i]=data.previousMeta.getValueMeta(data.fieldnrs[i]);
	 			}
			}else
			{
				data.fieldnr=data.previousMeta.size();
				data.fieldnrs=new int[data.fieldnr];
				data.previousValues=new Object[data.fieldnr];
				data.fieldnrsMeta = new ValueMeta[data.fieldnr];
				for(int i=0;i<data.previousMeta.size();i++)
				{
					data.fieldnrs[i]=i;
					data.fieldnrsMeta[i]=data.previousMeta.getValueMeta(i);
				}
			}
			
//			data.startAt=Const.toInt(environmentSubstitute(meta.getValueField()), 1);
//			data.incrementBy=Const.toInt(environmentSubstitute(meta.getIncrement()), 1);
			if(r[data.valuenrs]!=null)
				data.newValue=new Double(r[data.valuenrs].toString());
			else
				data.newValue=new Double(0);
			
			if(data.continueValuenrs>-1 && r[data.continueValuenrs]!=null)
			{
				data.newContinueValue=r[data.continueValuenrs];
				data.previousContinueValue=r[data.continueValuenrs];
			}            
			else
			{
				data.newContinueValue=null;	
				data.previousContinueValue=null;
			}			
		} // end if first
		

		try
		{
			boolean change=false;
			
		   	// Loop through fields
			for(int i=0;i<data.fieldnr;i++)
			{
				if(!first)
				{
					if(data.fieldnrsMeta[i].compare(data.previousValues[i], r[data.fieldnrs[i]])!=0) change=true;
				}
				data.previousValues[i]=r[data.fieldnrs[i]];
			}
			if(first) first=false;
			
			if(change) 
			{
				data.previousValue=0d;
				if(data.continueValuenrs>-1)
					data.previousContinueValue=r[data.continueValuenrs];
				else
					data.previousContinueValue=null;
			}
			
		    if (log.isRowLevel()) logRowlevel(BaseMessages.getString(PKG, "FieldsChangeSequence.Log.ReadRow")+getLinesRead()+" : "+getInputRowMeta().getString(r)); //$NON-NLS-1$ //$NON-NLS-2$
		    
		    if(r[data.valuenrs]!=null)
		    	data.newValue=new Double(r[data.valuenrs].toString()) + data.previousValue;
		    else
		    	data.newValue=new Double(0) + data.previousValue;
			
			if(data.continueValuenrs>-1)
			{
				if(r[data.continueValuenrs]!=null)
					data.newContinueValue=r[data.continueValuenrs];
				else
					throw new KettleException(BaseMessages.getString(PKG, "FieldsChangeSequence.Log.ValueInContinueFieldIsNull",meta.getContinueValueField()));
			}
			
			if(meta.getContinueValueField()!=null && data.continueValuenrs>-1)
			{
				while(!isSame(data.previousContinueValue, data.newContinueValue))
				{
					if(!isContinue(data.previousContinueValue, data.newContinueValue)) //continue filed is not same and is not continual, fill with 0
					{
						Object[] addedRow = new Object[r.length];
						addedRow[data.continueValuenrs]=addOne(data.previousContinueValue); 
						ValueMeta valueMeata = (ValueMeta)getInputRowMeta().getValueMeta(data.valuenrs);
						//Object obj= valueMeata.convertDataFromString("0", valueMeata, null, null, 0);
						Object obj= valueMeata.convertData( valueMeata, new Double(0));
						addedRow[data.valuenrs]=obj;							
						for(int i=0;i< meta.getFieldName().length;i++)
						{
							addedRow[data.fieldnrs[i]]=r[data.fieldnrs[i]];
						}
						Object[] outputRowData=RowDataUtil.addValueData(addedRow,data.nextIndexField, data.previousValue);//previous accumulate value in result field
						putRow(data.outputRowMeta, outputRowData);     // copy row to possible alternate rowset(s).
					}
					data.previousContinueValue=addOne(data.previousContinueValue);// add one in continue value field	
				}
				Object[] outputRowData=RowDataUtil.addValueData(r,data.nextIndexField, data.newValue);
				putRow(data.outputRowMeta, outputRowData);     // copy row to possible alternate rowset(s).	
				
			}else
			{
				Object[] outputRowData=RowDataUtil.addValueData(r,data.nextIndexField, data.newValue);
				putRow(data.outputRowMeta, outputRowData);     // copy row to possible alternate rowset(s).
			}
			
			data.previousValue = data.newValue;
	        if (log.isRowLevel()) logRowlevel(BaseMessages.getString(PKG, "FieldsChangeSequence.Log.WriteRow")+getLinesWritten()+" : "+getInputRowMeta().getString(r)); //$NON-NLS-1$ //$NON-NLS-2$
			
	        if (checkFeedback(getLinesRead())) 
			{
				if(log.isBasic()) logBasic(BaseMessages.getString(PKG, "FieldsChangeSequence.Log.LineNumber")+getLinesRead()); //$NON-NLS-1$
			}
			
		}catch(Exception e) {
	        boolean sendToErrorRow=false;
	        String errorMessage = null;
        	if (getStepMeta().isDoingErrorHandling())
        	{
                sendToErrorRow = true;
                errorMessage = e.toString();
        	}
        	else
        	{
	            logError(BaseMessages.getString(PKG, "FieldsChangeSequence.ErrorInStepRunning")+e.getMessage()); //$NON-NLS-1$
	            logError(Const.getStackTracker(e));
	            setErrors(1);
	            stopAll();
	            setOutputDone();  // signal end to receiver(s)
	            return false;
        	}
        	if (sendToErrorRow)
        	{
        	   // Simply add this row to the error row
        	   putError(getInputRowMeta(),r, 1, errorMessage, meta.getResultFieldName(), "FieldsChangeSequence001");
        	}
        }
		return true;
	}
	
	private boolean isContinue(Object previousContinueValue,
			Object newContinueValue) {
		if(previousContinueValue instanceof Date && newContinueValue instanceof Date )
		{
				return daysBetween((Date)previousContinueValue,(Date)newContinueValue) == 1;
		}
		else if(previousContinueValue instanceof Integer && newContinueValue instanceof Integer)
		{
			return (Integer)newContinueValue - (Integer)previousContinueValue == 1;
		}
		else 
			return false;
	}

	private Object addOne(Object previousContinueValue) {
		if(previousContinueValue instanceof Date )
		{
				return addOneDay((Date)previousContinueValue);
		}
		else if(previousContinueValue instanceof Integer)
		{
			return  (Integer)previousContinueValue + 1;
		}
		else 
			return null;
	}

	/**
	 * add one day on previousContinueValue
	 * @param previousContinueValue
	 * @return
	 */
	private Object addOneDay(Date previousContinueValue) {
        Calendar cal = Calendar.getInstance();    
        cal.setTime(previousContinueValue);    
        long time1 = cal.getTimeInMillis();    
        time1 +=1000*3600*24;       
        cal.setTimeInMillis(time1);  
        return new Date(time1);
	}

	/**
	 * check two object 
	 * @param previousContinueValue
	 * @param newContinueValue
	 * @return
	 */
	private boolean isSame(Object previousContinueValue,
			Object newContinueValue) {
		if(previousContinueValue instanceof Date && newContinueValue instanceof Date )
		{
				return daysBetween((Date)previousContinueValue,(Date)newContinueValue) == 0;
		}
		else if(previousContinueValue instanceof Integer && newContinueValue instanceof Integer)
		{
			return (Integer)newContinueValue - (Integer)previousContinueValue == 0;
		}
		else 
			return false;
	}

    public int daysBetween(Date prevDate,Date newDate) 
    {
        Calendar cal = Calendar.getInstance();    
        cal.setTime(prevDate);    
        long time1 = cal.getTimeInMillis();                 
        cal.setTime(newDate);    
        long time2 = cal.getTimeInMillis();         
        long between_days=(time2-time1)/(1000*3600*24);              
       return Integer.parseInt(String.valueOf(between_days));           
    }    
    
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(FieldsChangeAccumulatorMeta)smi;
		data=(FieldsChangeAccumulatorData)sdi;
		
		if (super.init(smi, sdi))
		{
		    // Add init code here.
		    return true;
		}
		return false;
	}
	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(FieldsChangeAccumulatorMeta)smi;
		data=(FieldsChangeAccumulatorData)sdi;

		data.previousValues=null;
		data.fieldnrs=null;
		super.dispose(smi, sdi);
	}	
}
