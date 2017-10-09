package org.pentaho.di.cachefile.index;

import org.pentaho.di.cachefile.ResultSet;
import org.pentaho.di.cachefile.meta.IndexMeta;
import org.pentaho.di.cachefile.meta.RecordMeta;
import org.pentaho.di.cachefile.meta.Serialize;


/**
 * Index super class. 
 * 
 * 
 * */
public abstract class Index implements Serialize
{
	public IndexType indexType ;
    public IndexMeta meta ; 
	
    /**
     * Insert a record into this index.
     * Return the logical position of the inserted record in storage.
     * 
     * */
    public abstract void insertRecord(RecordMeta rm, Object[] value, long offset) throws Exception;
    
    
    /**
     * Find a record.
     * Return the logical position of the record in the storage.
     * 
     * @param rm record meta information of the table
     * @param keyValue values of the indexed fields
     * @param result_field_index field index for the result record
     * 
     * */
    public abstract ResultSet searchRecord(RecordMeta rm, Object[] keyValue, int[] result_field_index) throws Exception;
    
    public int getIndexType()
    {
        return indexType.indexType ;
    }
    
    
}
