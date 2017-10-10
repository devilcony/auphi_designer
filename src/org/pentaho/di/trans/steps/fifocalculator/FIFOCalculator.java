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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleRowException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;

/**
 * 
 */
public class FIFOCalculator extends BaseStep implements StepInterface
{
	private static Class<?> PKG = FIFOCalculatorMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private FIFOCalculatorMeta meta;
	private FIFOCalculatorData data;

	
	public FIFOCalculator(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(FIFOCalculatorMeta)smi;
		data=(FIFOCalculatorData)sdi;
		
	    if (data.readLookupValues)
	    {
	        data.readLookupValues = false;	        
			if (! readLookupValues()) // Read values in lookup table (look)
			{
				logError("Unable To Read Data From Journal Stream"); //$NON-NLS-1$
				setErrors(1);
				stopAll();
				return false;
			}
			
			// At this point, all the values in the cache are of normal storage data type...
			// We should reflect this in the metadata...
			//
//			if(data.keyMeta!=null) { //null when no rows coming from lookup stream
//				for (ValueMetaInterface valueMeta : data.keyMeta.getValueMetaList()) {
//					valueMeta.setStorageType(ValueMetaInterface.STORAGE_TYPE_NORMAL);
//				}
//			}
//			if(data.valueMeta!=null) { //null when no rows coming from lookup stream
//				for (ValueMetaInterface valueMeta : data.valueMeta.getValueMetaList()) {
//					valueMeta.setStorageType(ValueMetaInterface.STORAGE_TYPE_NORMAL);
//				}
//			}
			
			return true;
	    }
	    
		Object[] r=getRow();      // Get row from input rowset & set row busy!
		
		
		if (r==null)         // no more input to be expected...
		{
			if(data.previousKey!=null)
        	{
        		Object previousValue = data.getHashTable().get(data.previousKey);
        		if(previousValue!=null)
	        	{
        			Double remainValue = new Double(previousValue.toString()).doubleValue();
        			if(remainValue>0)
        			{
        				data.previousRow[data.ValueNrsInMainStream[0]]=new BigDecimal(0-remainValue);
        				data.getHashTable().remove(data.previousKey);
        			}
	        	}
	        	putRow(data.outputRowMeta, RowDataUtil.addValueData(data.previousRow, data.outputIndex, 0));
        	}			
			if (log.isDetailed()) logDetailed(BaseMessages.getString(PKG, "StreamLookup.Log.StoppedProcessingWithEmpty",getLinesRead()+"")); //$NON-NLS-1$ //$NON-NLS-2$
			setOutputDone();
			return false;
		}
        
        if (first)
        {
        	data.outputIndex = r.length;
            first = false;            
            // Find the appropriate RowSet 
            //
            List<StreamInterface> infoStreams = meta.getStepIOMeta().getInfoStreams();
            
//            try
//            {
//                checkInputLayoutValid(data.oneRowSet.getRowMeta(), data.twoRowSet.getRowMeta());
//            }
//            catch(KettleRowException e)
//            {
//            	throw new KettleException(BaseMessages.getString(PKG, "MergeRows.Exception.InvalidLayoutDetected"), e);
//            }       
            // Find the key indexes in main stream:
            data.keyNrsInMainStream = new int[meta.getKeyFields().length];
            for (int i=0;i<meta.getKeyFields().length;i++)
            {
                data.keyNrsInMainStream[i] = this.getInputRowMeta().indexOfValue(meta.getKeyFields()[i]);
                if (data.keyNrsInMainStream[i]<0)
                {
                    String message = BaseMessages.getString(PKG, "MergeRows.Exception.UnableToFindKeyFieldInMainStream",meta.getKeyFields()[i]);  //$NON-NLS-1$ //$NON-NLS-2$
                    logError(message);
                    throw new KettleStepException(message);
                }
            }
            
            data.ValueNrsInMainStream = new int[meta.getValueFields().length];
            for (int i=0;i<meta.getValueFields().length;i++)
            {
                data.ValueNrsInMainStream[i] = this.getInputRowMeta().indexOfValue(meta.getValueFields()[i]);
                if (data.ValueNrsInMainStream[i]<0)
                {
                    String message = BaseMessages.getString(PKG, "MergeRows.Exception.UnableToFindValueFieldInMainStream",meta.getValueFields()[i]);  //$NON-NLS-1$ //$NON-NLS-2$
                    logError(message);
                    throw new KettleStepException(message);
                }
            }
            
            data.DateNrInMainStream = this.getInputRowMeta().indexOfValue(meta.getDateField());
            data.MultiplyNrInMainStream = this.getInputRowMeta().indexOfValue(meta.getMultiplyField());
            
            if(data.DateNrInMainStream<0)
            {
                String message = BaseMessages.getString(PKG, "MergeRows.Exception.UnableToFindDateFieldInMainStream",meta.getDateField());  //$NON-NLS-1$ //$NON-NLS-2$
                logError(message);
                throw new KettleStepException(message);
            }

	        if (log.isRowLevel()) logRowlevel(BaseMessages.getString(PKG, "MergeRows.Log.DataInfo",data.one+"")+data.two); //$NON-NLS-1$ //$NON-NLS-2$
	
	
	        if (data.outputRowMeta==null)
	        {
	            data.outputRowMeta = this.getInputRowMeta().clone();
	            int index  =data.ValueNrsInMainStream[0];
	            ValueMetaInterface infoStreamValueFieldValueMeta = this.getInputRowMeta().getValueMeta(index);
	            
	            ValueMetaInterface infoStreamValueFieldValue = new ValueMeta(meta.getValueFields()[0]+"_1",infoStreamValueFieldValueMeta.getType());
	            infoStreamValueFieldValue.setOrigin(getStepname());
	            data.outputRowMeta.addValueMeta(infoStreamValueFieldValue);
	        }
        }
        
        String groupField = null;
        String key="";
        for (int i=0;i<meta.getKeyFields().length;i++)
        {
            key+=r[data.keyNrsInMainStream[i]];// VALUE_OF_KEY1+VALUE_OF_KEY2+... AS A COMBINED KEY IN HASH TABLE
        }
        
        //double valueInMainStream =new Double(r[data.ValueNrsInMainStream[0]].toString()).doubleValue();
        BigDecimal valueInMainStream  = this.getInputRowMeta().getBigNumber(r, data.ValueNrsInMainStream[0]);
        
        double multiplyValueInMainStream=1;
        
        if(data.MultiplyNrInMainStream>0)
        {
        	if(r[data.MultiplyNrInMainStream]!=null)
        		multiplyValueInMainStream = new Double(r[data.MultiplyNrInMainStream].toString()).doubleValue();
        	else 
        		multiplyValueInMainStream = 0;
        }
        Double valueInLookupStream = null;
        Object infoStreamObj = data.getHashTable().get(key);
        while(infoStreamObj!=null)//VALUE IS FOUND IN INFO STREAM
        {
        	valueInLookupStream = new Double(infoStreamObj.toString()).doubleValue();
//        	if(valueInMainStream.doubleValue()>0)
//        	{
	        	if(valueInMainStream.doubleValue() >valueInLookupStream* multiplyValueInMainStream)
	        	{
	        		valueInMainStream=new BigDecimal(valueInMainStream.doubleValue() - valueInLookupStream* multiplyValueInMainStream);
	        		data.getHashTable().remove(key);
	        	}
	        	else
	        	{
	        		data.getHashTable().put(key, valueInLookupStream - valueInMainStream.doubleValue());
	        		valueInMainStream=new BigDecimal(0);
	        		break;
	        		//data.getHashTable().remove(key);
	        	}	        	 
//        	}
//        	else// if valueInMainStream<=0, just skip
//        	{
//        		data.getHashTable().remove(key);
//        		//putRow(data.outputRowMeta, RowDataUtil.addValueData(r, data.outputIndex, valueInLookupStream));
//        	}
        	infoStreamObj = data.getHashTable().get(key);
        }
        r[data.ValueNrsInMainStream[0]]=valueInMainStream;
        
        if(data.previousKey!=null && key.toString().equals(data.previousKey.toString()))// in the same group
        	  //putRow(data.outputRowMeta, RowDataUtil.addValueData(r, data.outputIndex, valueInLookupStream));  //original
        	putRow(data.outputRowMeta, RowDataUtil.addValueData(data.previousRow, data.outputIndex, valueInLookupStream));
        else //the first row in the next group
        {
        	if(data.previousKey!=null)
        	{
        		Object previousRemainValue = data.getHashTable().get(data.previousKey);
        		if(previousRemainValue!=null)
	        	{
        			Double remainValue = new Double(previousRemainValue.toString()).doubleValue();
        			if(remainValue>0)
        			{
        				data.previousRow[data.ValueNrsInMainStream[0]]=new BigDecimal(0-remainValue);
        				data.getHashTable().remove(data.previousKey);
        			}
	        	}
	        	putRow(data.outputRowMeta, RowDataUtil.addValueData(data.previousRow, data.outputIndex, valueInLookupStream));
        	}
        }
        data.previousRow = r;
        data.previousKey = key;
        
        if (checkFeedback(getLinesRead())) 
        {
        	if(log.isBasic()) logBasic(BaseMessages.getString(PKG, "MergeRows.LineNumber")+getLinesRead()); //$NON-NLS-1$
        }

		return true;
	}

	/***
	 * load info stream data into cache.
	 * @return
	 * @throws KettleException
	 */
	private boolean readLookupValues() throws KettleException
	{
		data.infoStream = meta.getStepIOMeta().getInfoStreams().get(0);
		if (data.infoStream.getStepMeta()==null)
		{
			logError(BaseMessages.getString(PKG, "StreamLookup.Log.NoLookupStepSpecified")); //$NON-NLS-1$
			return false;
		}
		
		if (log.isDetailed()) logDetailed(BaseMessages.getString(PKG, "StreamLookup.Log.ReadingFromStream")+data.infoStream.getStepname()+"]"); //$NON-NLS-1$ //$NON-NLS-2$

		data.keyNrs = new int[meta.getKeyFields().length];
		data.valueNrs = new int[meta.getValueFields().length];
        boolean firstRun = true;
        
        // Which row set do we read from?
        //
        RowSet rowSet = findInputRowSet(data.infoStream.getStepname());
        Object[] rowData=getRowFrom(rowSet); // rows are originating from "lookup_from"
		while (rowData!=null)
		{
            if (log.isRowLevel()) logRowlevel(BaseMessages.getString(PKG, "StreamLookup.Log.ReadLookupRow")+rowSet.getRowMeta().getString(rowData)); //$NON-NLS-1$

            if (firstRun)
            {
                firstRun=false;
                data.hasLookupRows=true;                
                data.infoMeta = rowSet.getRowMeta().clone();
                data.keyMeta = new RowMeta();
                data.valueMeta = new RowMeta();
            
                // Look up the keys in the source rows
                for (int i=0;i<meta.getKeyFields().length;i++)
                {
                	data.keyNrs[i] = rowSet.getRowMeta().indexOfValue(meta.getKeyFields()[i]);
                    if (data.keyNrs[i]<0)
                    {
                        throw new KettleStepException(BaseMessages.getString(PKG, "StreamLookup.Exception.UnableToFindField",meta.getKeyFields()[i])); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    data.keyMeta.addValueMeta( rowSet.getRowMeta().getValueMeta( data.keyNrs[i] ));
                }
                // Save the data types of the keys to optionally convert input rows later on...
                if (data.keyTypes==null)
                {
                    data.keyTypes=data.keyMeta.clone();
                }
                
    			// 
                for (int i=0;i<data.keyNrs.length;i++)
                {
                	data.keyMeta.getValueMeta(i).setStorageType(ValueMetaInterface.STORAGE_TYPE_NORMAL);
                }                
            			
    			for (int v=0;v<meta.getValueFields().length;v++)
    			{
    			    data.valueNrs[v] = rowSet.getRowMeta().indexOfValue( meta.getValueFields()[v] );
                    if (data.valueNrs[v]<0)
    				{
                        throw new KettleStepException(BaseMessages.getString(PKG, "StreamLookup.Exception.UnableToFindField",meta.getValueFields()[v])); //$NON-NLS-1$ //$NON-NLS-2$
    				}
                    data.valueMeta.addValueMeta( rowSet.getRowMeta().getValueMeta(data.valueNrs[v]) );
    			}
            }
            

                
            Object[] keyData = new Object[data.keyNrs.length];
            for (int i=0;i<data.keyNrs.length;i++)
            {
                ValueMetaInterface keyMeta = data.keyTypes.getValueMeta(i);
                if(rowData[ data.keyNrs[i] ]!=null)
                {
                	keyData[i] = keyMeta.convertToNormalStorageType( rowData[ data.keyNrs[i] ] ); // Make sure only normal storage goes in
                	keyMeta.setStorageType(ValueMetaInterface.STORAGE_TYPE_NORMAL); // now we need to change keyMeta/keyTypes also to normal
                }                	
                else
                {
                	String message = BaseMessages.getString(PKG, "MergeRows.Exception.OneKeyIsNull",data.keyMeta.getValueMeta(i).getName());  //$NON-NLS-1$ //$NON-NLS-2$
                    logError(message);
                    throw new KettleStepException(message);
                }
                
            }

            Object[] valueData = new Object[data.valueNrs.length];
            for (int i=0;i<data.valueNrs.length;i++)
            {
                ValueMetaInterface valueMeta = data.valueMeta.getValueMeta(i);
                valueData[i] = valueMeta.convertToNormalStorageType( rowData[ data.valueNrs[i] ] ); // make sure only normal storage goes in
            }
            addToCache(data.keyMeta, keyData, data.valueMeta, valueData);
			rowData=getRowFrom(rowSet);
		}

		return true;
	}
	

	private void addToCache(RowMeta keyMeta, Object[] keyData,
			RowMeta valueMeta, Object[] valueData) throws KettleValueException {
		String key="";
		for(int i=0;i<keyData.length;i++)
		{
			key+=keyData[i].toString();
		}		
		if(valueData[0]!=null)
		{
			Object obj = data.getHashTable().get(key);
			if(obj!=null)
			{
				BigDecimal oldValue= valueMeta.getValueMeta(0).getBigNumber(obj);
				BigDecimal addValue= valueMeta.getValueMeta(0).getBigNumber(valueData[0]);
				BigDecimal newValue = new BigDecimal(oldValue.doubleValue() + addValue.doubleValue());
				data.getHashTable().put(key, newValue);
			}    
			else
				data.getHashTable().put(key, valueData[0]);
		}
			
		else
			data.getHashTable().put(key, 0);
	}

	/**
     * @see StepInterface#init( org.pentaho.di.trans.step.StepMetaInterface , org.pentaho.di.trans.step.StepDataInterface)
     */
    public boolean init(StepMetaInterface smi, StepDataInterface sdi)
    {
		meta=(FIFOCalculatorMeta)smi;
		data=(FIFOCalculatorData)sdi;
		data.readLookupValues=true;
		//data.resultRowSet=new RowSet();
        if (super.init(smi, sdi))
        {
            List<StreamInterface> infoStreams = meta.getStepIOMeta().getInfoStreams();

            if (infoStreams.get(0).getStepMeta()!=null ^ infoStreams.get(0).getStepMeta()!=null)
            {
                logError(BaseMessages.getString(PKG, "MergeRows.Log.BothTrueAndFalseNeeded")); //$NON-NLS-1$
            }
            else
            {
                return true;
            }            
        }
        return false;
    }

    /**
     * Checks whether 2 template rows are compatible for the mergestep. 
     * 
     * @param referenceRow Reference row
     * @param compareRow Row to compare to
     * 
     * @return true when templates are compatible.
     * @throws KettleRowException in case there is a compatibility error.
     */
//    protected void checkInputLayoutValid(RowMetaInterface referenceRowMeta, RowMetaInterface compareRowMeta) throws KettleRowException
//    {
//        if (referenceRowMeta!=null && compareRowMeta!=null)
//        {
//            BaseStep.safeModeChecking(referenceRowMeta, compareRowMeta);
//        }
//    }
    
    public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
	    // Recover memory immediately, allow in-memory data to be garbage collected
	    //
	    data.valueNrs = null; 
	    data.keyNrs = null;
	    data.keyMeta = null;
	    data.valueMeta = null;
	    if (data.getHashTable() != null)
	    {
	        try
            {
	        	data.getHashTable().clear();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
	    }
	    
		super.dispose(smi, sdi);
	}
}