package org.pentaho.di.cachefile;

public class Constants
{
//	public final static long invalid_page_no = -1L ;
    
    /*  Maximum size of field(column)   */
    public final static int MAX_NUM_FIELDS = 512 ;
    /*  Maximum size of field(column) name  */
    public final static int MAX_COLUMN_NAME = 64 ;
    /*  Maximum size of record  */
    public final static int MAX_RECORD_SIZE = 8*1024 ;
    /*  Maximum size of key */
    public final static int MAX_KEY_SIZE = 2*1024;
	
    
    public final static int size_logical_addr = Long.SIZE/8 ;
    public final static int size_page_no = Long.SIZE/8 ;
    public final static int size_hash_key = Integer.SIZE/8 ;
    public final static int size_page_offset = Integer.SIZE/8 ;
}
