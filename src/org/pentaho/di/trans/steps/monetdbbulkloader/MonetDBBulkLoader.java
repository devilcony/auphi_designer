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

package org.pentaho.di.trans.steps.monetdbbulkloader;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.vfs.FileObject;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.SQLStatement;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.StreamLogger;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;


/**
 * Performs a bulk load to a MonetDB table.
 *
 * Based on (copied from) Sven Boden's Oracle Bulk Loader step
 * 
 * @author matt
 * @since  22-aug-2008
 */
public class MonetDBBulkLoader extends BaseStep implements StepInterface
{
	private static Class<?> PKG = MonetDBBulkLoaderMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private MonetDBBulkLoaderMeta meta;
	private MonetDBBulkLoaderData data;
	
	public MonetDBBulkLoader(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	/**
	 * Create the command line for a psql process depending on the meta
	 * information supplied.
	 * 
	 * @param meta The meta data to create the command line from
	 * 
	 * @return The string to execute.
	 * 
	 * @throws KettleException Upon any exception
	 */
	public String createCommandLine(MonetDBBulkLoaderMeta meta, boolean lSql) throws KettleException
	{
	   StringBuffer sb = new StringBuffer(300);
	   
	   if ( !Const.isEmpty(meta.getMClientPath()) )
	   {
		   try
		   {
	           FileObject fileObject = KettleVFS.getFileObject(environmentSubstitute(meta.getMClientPath()), getTransMeta());
  	      	   String psqlexec = KettleVFS.getFilename(fileObject);
		       sb.append(psqlexec);
  	       }
	       catch ( KettleFileException ex )
	       {
	           throw new KettleException("Error retrieving mclient application string", ex);
	       }		       
	   }
	   else
	   {
		   throw new KettleException("No mclient application specified");
	   }

	   // Add standard options to the mclient command:
	   //
	   if( lSql ) {
		   sb.append(" -lsql");
	   }
	   // See if the encoding is set...
	   //
	   if ( !Const.isEmpty(meta.getEncoding()))
	   {
		   sb.append(" --encoding=");
		   sb.append(environmentSubstitute(meta.getEncoding()));
	   }
	   
	   if ( !Const.isEmpty(meta.getLogFile()))
	   {
		   try 
		   {
		       FileObject fileObject = KettleVFS.getFileObject(environmentSubstitute(meta.getLogFile()), getTransMeta());   
	   
		       sb.append(" --log=");
		       sb.append('\'').append(KettleVFS.getFilename(fileObject)).append('\'');
		   }
		   catch ( KettleFileException ex )
		   {
		       throw new KettleException("Error retrieving logfile string", ex);
		   }
	   }
	   
       DatabaseMeta dm = meta.getDatabaseMeta();
       if ( dm != null )
       {
           String hostname = environmentSubstitute(Const.NVL(dm.getHostname(), ""));
           String portnum  = environmentSubstitute(Const.NVL(dm.getDatabasePortNumberString(), ""));
           String dbname   = environmentSubstitute(Const.NVL(dm.getDatabaseName(), ""));

           if (!Const.isEmpty(hostname)) {
        	   sb.append(" --host=").append(hostname);
           }
           if (!Const.isEmpty(portnum) && Const.toInt(portnum, -1)>0) {
        	   sb.append(" --port=").append(portnum);
           }
           if (!Const.isEmpty(dbname)) {
        	   sb.append(" --database=").append(dbname);
           }
       }
	   else
	   {
		   throw new KettleException("No connection specified");
	   }

	   return sb.toString(); 
	}
	
	public boolean execute(MonetDBBulkLoaderMeta meta, boolean wait) throws KettleException
	{
		Runtime rt = Runtime.getRuntime();
		if (log.isDetailed()) logDetailed("Started execute" );

    	String cmd = null;
    	String cmdLSql = null;
		if (log.isDetailed()) logDetailed("Creating commands" );
        try 
        {
        	cmdLSql = createCommandLine(meta, true);
        	cmd = createCommandLine(meta, false);
        }
        catch ( Exception ex )
        {
           	throw new KettleException("Error while generating MonetDB commands", ex);
        }
		if (log.isDetailed()) logDetailed("Created command: "+cmdLSql );
		if (log.isDetailed()) logDetailed("Created command: "+cmd );

            try  
        {

       		if (log.isDetailed()) logDetailed("Truncate flag: "+meta.isTruncate() );
       		if (log.isDetailed()) logDetailed("Auto Adjust Schema flag: "+meta.isAutoSchema() );
       		if (log.isDetailed()) logDetailed("Auto String Length flag: "+meta.isAutoStringWidths() );
            if( meta.isTruncate() && meta.isAutoSchema() ) {
            	dropTable( rt, cmd );
            } else {
	            if( meta.isTruncate() ) {
	            	truncateTable( rt, cmd );
	            }
	            if( meta.isAutoSchema() ) {
	            	autoAdjustSchema( rt, cmd );
	            }
            }
        	
        	logBasic("Executing command: "+cmdLSql);
            data.mClientlProcess = rt.exec(cmdLSql);
            
            // any error message?
            //
            data.errorLogger = new StreamLogger(log, data.mClientlProcess.getErrorStream(), "ERROR");
        
            // any output?
            data.outputLogger = new StreamLogger(log, data.mClientlProcess.getInputStream(), "OUTPUT");
            
            // Where do we send the data to?  --> To STDIN of the mclient process
            //
            data.monetOutputStream = data.mClientlProcess.getOutputStream();
            
            // kick them off
            new Thread(data.errorLogger).start();
            new Thread(data.outputLogger).start();                              

            // OK, from here on, we need to feed the COPY INTO command followed by the data into the monetOutputStream
            //
        }
        catch ( Exception ex )
        {
        	throw new KettleException("Error while executing mclient : " + cmdLSql, ex);
        }
        
        return true;
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(MonetDBBulkLoaderMeta)smi;
		data=(MonetDBBulkLoaderData)sdi;

		try
		{
			Object[] r=getRow();  // Get row from input rowset & set row busy!
			if (r==null)          // no more input to be expected...
			{
				setOutputDone();

	    		writeBufferToMonetDB();
				// Close the output stream...
				//
	    		if( data.monetOutputStream != null ) {
	    			data.monetOutputStream.flush();
	    			data.monetOutputStream.close();
	                // wait for the mclient process to finish and check for any error...
					//
	            	int exitVal = data.mClientlProcess.waitFor();
					logBasic(BaseMessages.getString(PKG, "MonetDBBulkLoader.Log.ExitValuePsqlPath", "" + exitVal)); //$NON-NLS-1$
	    		}
	            
				return false;
			}
			
			if (first)
			{
				first=false;
				
				// Cache field indexes.
				//
				data.keynrs = new int[meta.getFieldStream().length];
				for (int i=0;i<data.keynrs.length;i++) {
					data.keynrs[i] = getInputRowMeta().indexOfValue(meta.getFieldStream()[i]);
				}

				// execute the psql statement...
				//
				execute(meta, true);
			}
			
			writeRowToMonetDB(getInputRowMeta(), r);
			putRow(getInputRowMeta(), r);
			incrementLinesOutput();

			return true;
		}
		catch(Exception e)
		{
			logError(BaseMessages.getString(PKG, "MonetDBBulkLoader.Log.ErrorInStep"), e); //$NON-NLS-1$
			setErrors(1);
			stopAll();
			setOutputDone();  // signal end to receiver(s)
			return false;
		} 
	}

    private void writeRowToMonetDB(RowMetaInterface rowMeta, Object[] r) throws KettleException {
    	if (data.bufferIndex==data.bufferSize) {
    		writeBufferToMonetDB();
    	}
		addRowToBuffer(rowMeta, r);
    }

	private void addRowToBuffer(RowMetaInterface rowMeta, Object[] r) throws KettleException {

    	ByteArrayOutputStream line = new ByteArrayOutputStream(25000);
    	
    	try {
	    	// So, we have this output stream to which we can write CSV data to.
	    	// Basically, what we need to do is write the binary data (from strings to it as part of this proof of concept)
	    	//
    		// The data format required is essentially:
    		//
    		for (int i=0;i<data.keynrs.length;i++) {
				if (i>0) {
		    		// Write a separator 
		    		//
		    		line.write(data.separator);
				}
				
	    		int index = data.keynrs[i];
	    		ValueMetaInterface valueMeta = rowMeta.getValueMeta(index);
	    		Object valueData = r[index];
	    		
	    		if (valueData!=null) {
		    		switch(valueMeta.getType()) {
		    		case ValueMetaInterface.TYPE_STRING :
		    			line.write(data.quote);
		    			if(meta.isAutoStringWidths()) {
		    				// this is slower, but safer
		    				String str = valueMeta.getString(valueData);
		    				if( str.length() > valueMeta.getLength()) {
		    					str = str.substring(0, valueMeta.getLength());
		    				}
		    				line.write(str.getBytes(meta.getEncoding()));
		    			}
		    			else if (valueMeta.isStorageBinaryString() && meta.getFieldFormatOk()[i]) {
		    				// We had a string, just dump it back.
		    				line.write((byte[])valueData);
		    			} else {
		    				line.write(valueMeta.getString(valueData).getBytes(meta.getEncoding()));
		    			}
		    			line.write(data.quote);
		    			break;
		    		case ValueMetaInterface.TYPE_INTEGER:
		    			if (valueMeta.isStorageBinaryString() && meta.getFieldFormatOk()[i]) {
		    				line.write((byte[])valueData);
		    			} else {
		    				line.write(Long.toString(valueMeta.getInteger(valueData)).getBytes());
		    			}
		    			break;
		    		case ValueMetaInterface.TYPE_DATE:
		    			// Keep the data format as indicated.
		    			//
		    			if (valueMeta.isStorageBinaryString() && meta.getFieldFormatOk()[i]) {
		    				line.write((byte[])valueData);
		    			} else {
		    				Date date = valueMeta.getDate(valueData);
		    				// Convert it to the MonetDB date format "yyyy/MM/dd HH:mm:ss"
		    				//
		    				line.write(data.monetDateMeta.getString(date).getBytes());
		    			}
		    			break;
		    		case ValueMetaInterface.TYPE_BOOLEAN:
		    			if (valueMeta.isStorageBinaryString() && meta.getFieldFormatOk()[i]) {
		    				line.write((byte[])valueData);
		    			} else {
		    				line.write(Boolean.toString(valueMeta.getBoolean(valueData)).getBytes());
		    			}
		    			break;
		    		case ValueMetaInterface.TYPE_NUMBER:
		    			if (valueMeta.isStorageBinaryString() && meta.getFieldFormatOk()[i]) {
		    				line.write((byte[])valueData);
		    			} else {
		    				line.write(Double.toString(valueMeta.getNumber(valueData)).getBytes());
		    			}
		    			break;
		    		case ValueMetaInterface.TYPE_BIGNUMBER:
		    			if (valueMeta.isStorageBinaryString() && meta.getFieldFormatOk()[i]) {
		    				line.write((byte[])valueData);
		    			} else {
		    				line.write(valueMeta.getString(valueData).getBytes());
		    			}
		    			break;
		    		}
	    		}
	    	}
			
			// finally write a newline
			//
			line.write(data.newline);
			
			// Now that we have the line, grab the content and store it in the buffer...
			//
			data.rowBuffer[data.bufferIndex] = line.toByteArray();
			data.bufferIndex++;
    	}
    	catch(Exception e)
    	{
    		throw new KettleException("Error serializing rows of data to the psql command", e);
    	}
		
	}

	public void truncateTable( Runtime rt, String mClientCmd ) throws KettleException {
		
    	try {
		   if (log.isDetailed()) logDetailed("attempting to truncate table" );
		
		  	Process p = rt.exec(mClientCmd);
		  	OutputStream stdIn = p.getOutputStream();
		   
		  	String cmd;
		  	cmd = meta.getDatabaseMeta().getTruncateTableStatement(null, data.schemaTable)+";";		  	
		  	
		  	if (log.isDetailed()) logDetailed("Trying: "+cmd);
		  	stdIn.write(cmd.getBytes());
		  	if (log.isDetailed()) logDetailed("Successfull: "+cmd);
		   
		   stdIn.flush();
		   stdIn.close();
		    // wait for the process to finish and check for any error...

		   int exitVal = p.waitFor();
		   logBasic(BaseMessages.getString(PKG, "MonetDBBulkLoader.Log.ExitValuePsqlPath", "" + exitVal)); //$NON-NLS-1$
	
    	}
    	catch(Exception e) {
    		throw new KettleException("An error occurred writing data to the mclient process", e);
    	}		
	}


	public void dropTable( Runtime rt, String mClientCmd ) throws KettleException {
		
    	try {
		   if (log.isDetailed()) logDetailed("attempting to truncate table" );
		
		  	Process p = rt.exec(mClientCmd);
		  	OutputStream stdIn = p.getOutputStream();
		   
		  	String cmd;
		  	cmd = "drop table " + data.schemaTable+";";		  	
		  	
		  	if (log.isDetailed()) logDetailed("Trying: "+cmd);
		  	stdIn.write(cmd.getBytes());
		  	if (log.isDetailed()) logDetailed("Successfull: "+cmd);
		   
		   stdIn.flush();
		   stdIn.close();
		    // wait for the process to finish and check for any error...

		   int exitVal = p.waitFor();
		   logBasic(BaseMessages.getString(PKG, "MonetDBBulkLoader.Log.ExitValuePsqlPath", "" + exitVal)); //$NON-NLS-1$
	
		   autoAdjustSchema(rt, mClientCmd);
		   
    	}
    	catch(Exception e) {
    		throw new KettleException("An error occurred writing data to the mclient process", e);
    	}		
	}	
	
	public void autoAdjustSchema( Runtime rt, String mClientCmd )  throws KettleException {
		
    	try {
 		   if (log.isDetailed()) logDetailed("Attempting to auto adjust table structure" );
 		
		   Process p = rt.exec(mClientCmd);
		   OutputStream stdIn = p.getOutputStream();
 		  	
		   if (log.isDetailed()) logDetailed("getTransMeta: "+getTransMeta() );
   		   if (log.isDetailed()) logDetailed("getStepname: "+getStepname() );
   		   SQLStatement statement = meta.getTableDdl(getTransMeta(), getStepname(), true, data);
   		   if (log.isDetailed()) logDetailed("Statement: "+statement );
  		   if (log.isDetailed() && statement != null) logDetailed("Statement has SQL: "+statement.hasSQL() );
    		
  		   if(statement != null && statement.hasSQL()) {
    			String cmd = statement.getSQL();
     		  	if (log.isDetailed()) logDetailed("Trying: "+cmd);
     		  	stdIn.write(cmd.getBytes());
     		  	if (log.isDetailed()) logDetailed("Successfull: "+cmd);
    		}	    	
 		  	 		   
 		   stdIn.flush();
 		   stdIn.close();
 		    // wait for the process to finish and check for any error...

 		   int exitVal = p.waitFor();
 		   logBasic(BaseMessages.getString(PKG, "MonetDBBulkLoader.Log.ExitValuePsqlPath", "" + exitVal)); //$NON-NLS-1$
 	
     	}
     	catch(Exception e) {
     		throw new KettleException("An error occurred writing data to the mclient process", e);
     	}		
     	
	}
		
    private void writeBufferToMonetDB() throws KettleException {
    	if (data.bufferIndex==0) return;
    	
    	try {
	    	// first write the COPY INTO command...
	    	//
    		StringBuffer cmdBuff = new StringBuffer();
    		cmdBuff.append( "COPY " )
    		.append(data.bufferIndex)
    		.append(" RECORDS INTO ")
    		.append(data.schemaTable)
    		.append(" FROM STDIN USING DELIMITERS '")
    		.append(new String(data.separator))
    		.append("','\\n','")
    		.append(new String(data.quote))
    		.append("';");
    		String cmd = cmdBuff.toString();
	    	if (log.isDetailed()) logDetailed(cmd);
	    	data.monetOutputStream.write(cmd.getBytes());
	    	
	    	for (int i=0;i<data.bufferIndex;i++) {
	    		data.monetOutputStream.write(data.rowBuffer[i]);
		    	if (log.isRowLevel()) logRowlevel(new String(data.rowBuffer[i]));
	    	}
	    	
	    	// Also write an empty row
	    	//
//	    	data.monetOutputStream.write(Const.CR.getBytes());
	    	if (log.isRowLevel()) logRowlevel(Const.CR);
	    	
	    	// reset the buffer pointer...
	    	//
	    	data.bufferIndex=0;
    	}
    	catch(Exception e) {
    		throw new KettleException("An error occurred writing data to the mclient process", e);
    	}
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(MonetDBBulkLoaderMeta)smi;
		data=(MonetDBBulkLoaderData)sdi;

		if (super.init(smi, sdi))
		{			
			data.quote = "\"".getBytes();
			data.separator = "|".getBytes();
			data.newline = Const.CR.getBytes();

			data.monetDateMeta = new ValueMeta("dateMeta", ValueMetaInterface.TYPE_DATE);
			data.monetDateMeta.setConversionMask("yyyy/MM/dd HH:mm:ss");
			data.monetDateMeta.setStringEncoding(meta.getEncoding());

			data.monetNumberMeta = new ValueMeta("numberMeta", ValueMetaInterface.TYPE_NUMBER);
			data.monetNumberMeta.setConversionMask("#.#");
			data.monetNumberMeta.setGroupingSymbol(",");
			data.monetNumberMeta.setDecimalSymbol(".");
			data.monetNumberMeta.setStringEncoding(meta.getEncoding());

			data.bufferSize = Const.toInt(environmentSubstitute(meta.getBufferSize()), 100000);
			
			// Allocate the buffer
			// 
			data.rowBuffer = new byte[data.bufferSize][];
			data.bufferIndex = 0;
			
			// Schema-table combination...
			data.schemaTable = meta.getDatabaseMeta().getQuotedSchemaTableCombination(
			    environmentSubstitute(meta.getSchemaName()), 
			    environmentSubstitute(meta.getTableName())
			  );
			
			return true;
		}
		return false;
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
	    meta = (MonetDBBulkLoaderMeta)smi;
	    data = (MonetDBBulkLoaderData)sdi;

	    // Close the mclient output stream
	    //
	    try {
	    	if( data.monetOutputStream != null ) {
	    		data.monetOutputStream.close();
		    	int exitValue = data.mClientlProcess.waitFor();
		    	logDetailed("Exit value for the mclient process was : "+exitValue);
	    	}
	    }
	    catch(Exception e) {
	    	setErrors(1L);
	    	logError("Unexpected error encountered while finishing the mclient process", e);
	    }
	    
	    super.dispose(smi, sdi);
	}

}