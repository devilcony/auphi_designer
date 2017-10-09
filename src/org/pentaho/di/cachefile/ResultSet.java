package org.pentaho.di.cachefile;

import org.pentaho.di.cachefile.meta.RecordMeta;
import org.pentaho.di.cachefile.meta.ResultSetMeta;
import org.pentaho.di.cachefile.storage.buffer.BufferManager;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;
import org.pentaho.di.cachefile.storage.pageWriter.DataPage;
import org.pentaho.di.cachefile.type.FieldType;

public class ResultSet 
{
    
//    static ResultSetPage rspw = ResultSetPage.getInstance() ;
    static DataPage dpw = DataPage.getInstance() ;
    
//	RecordMeta tm ;
    ResultSetMeta rsm ;
	
	private long first_page_no ;
	private long last_page_no ;
	
	public long record_count ;
	
	private long current_page_no ;
	private int current_record_position ;
	private final BufferManager bm ;
	
	/**
	 * For optimization purpose
	 * 
	 * */
	public static long closeTime = 0 ;
	
	
	public ResultSet(BufferManager bm, RecordMeta tableMeta, int[] pre_index) throws Exception
	{
		this.bm = bm ;
		this.first_page_no = -1 ;
		this.current_page_no = -1 ;
		this.current_record_position = -1 ;
		this.rsm = new ResultSetMeta(tableMeta,pre_index) ;
	}
	
	
	public boolean next() throws Exception
	{
		if (first_page_no <= 0)
			return false ;
		
		/*	If record in the current page */
		BufferedPageHeader bph = bm.getPage(current_page_no) ;
//		bph.lock() ;

		if ((bph.getItemCount()-1)  > current_record_position)
		{
			current_record_position ++ ;
			bph.unlock() ;
			return true ;
		}
		
		/*	Move to next page	*/
		long next_page = bph.getNextPageNo() ;
		if (next_page <= 0)
		{
			bph.unlock() ;
			return false ;
		}
		else
		{
			bph.unlock() ;
			current_record_position = 0 ;
			current_page_no = next_page ;
			return true ;
		}
	}
	
	/**
	 * 	Get the index'th filed value of the current record.
	 *  @param index the index of the field
	 *  @throws Exception 
	 *  
	 * */
	public Object get(int index) throws Exception
	{
	    BufferedPageHeader bph = bm.getPage(current_page_no) ;
//	    bph.lock() ;
	    int record_offset = dpw.getRecordOffset(bph, current_record_position)  ;
//	    int field_offset = rsm.rm.getFieldOffset(bph.pageContext, bph.pageStartPosition+record_offset, index) ;
	    int field_offset = dpw.getFieldOffset(bph,record_offset,rsm.rm, index) ;
	    Object[] value = new Object[1] ;
	    rsm.rm.field_formats[index].readValue(bph.pageContext, bph.pageStartPosition+record_offset+field_offset, value,0) ;
	    bph.unlock() ;  
        if (rsm.rm.field_formats[index] == FieldType.StringType)
            return new String((byte[])(value[0])) ;	    
		return value[0] ;
	}
	
	/**
	 * 	Get the specific field value of the current record.
	 * 
	 * 	@param fieldName name of the field
	 * @throws Exception 
	 * 
	 * */
	public Object get(String fieldName) throws Exception
	{
	    int index = rsm.rm.indexOf(fieldName) ;
		return get(index) ;
	}
	
	/**
	 * 	Get all fields value of the current record.
	 * 
	 * 	@param fieldName name of the field
	 * @throws Exception 
	 * 
	 * */
	public Object[] getAll() throws Exception
	{
        BufferedPageHeader bph = bm.getPage(current_page_no) ;
//        bph.lock() ;
        int recordOffset = dpw.getRecordOffset(bph, current_record_position)  ;
        

        Object[] value = new Object[rsm.rm.num_filed] ;
        for (int i = 0 ; i < value.length ; i++)
        {
            recordOffset += rsm.rm.field_formats[i].readValue(bph.pageContext, bph.pageStartPosition+recordOffset, value, i) ;
            if (rsm.rm.field_formats[i] == FieldType.StringType)
                value[i] = new String((byte[])value[i]) ;
        }
        
        bph.unlock() ;
        return value ;	    
		
	}
	
	/**
	 * 	Recycle pages of this result.	
	 * 
	 * 	@throws Exception 
	 * 
	 * */
	public void close() throws Exception
	{
	    long start = System.currentTimeMillis() ;
		if (first_page_no <= 0)
			return  ;
		
		BufferedPageHeader bph = bm.getPage(first_page_no) ;
		long nextPage = -1L ;
		while(true)
		{
			nextPage = bph.getNextPageNo();
			bm.recyclePage(bph) ;
            bph.unlock() ;
			if(nextPage <= 0)
				break ;
			bph = bm.getPage(nextPage) ;
		}
		
		this.first_page_no = -1L ;
		this.last_page_no = -1L ;
		this.current_page_no = -1L ;
		this.current_record_position = -1 ;
		this.record_count = 0 ;
		this.rsm = null ;
		closeTime += System.currentTimeMillis() - start ;
	}
	
	public void insertRecord(Object[] value) throws Exception
	{
	     BufferedPageHeader bph = null ;
	     
	     /*    Find the last data page */
	     if (last_page_no <= 0)
         {
	         bph = bm.getNewPage() ;
//             bph.lock() ;
             first_page_no = bph.getPageNo() ;
             last_page_no = bph.getPageNo() ;
             current_page_no = first_page_no ;
             current_record_position = -1 ;
         }
         else
         {
            bph = bm.getPage(last_page_no) ;
//            bph.lock() ;
         }
	     
	     /*    Whether the last page is full   */
	     if(dpw.isFull(bph,rsm.rm, value))
	     {
	        BufferedPageHeader nextBph = bm.getNewPage() ;
//	        nextBph.lock();
	        
	        last_page_no = nextBph.getPageNo() ;
	        bph.setNextPageNo(last_page_no) ;
	        bph.unlock() ;
	        
	        bph = nextBph ;
	     }
	     dpw.writeData(bph,rsm.rm, value) ;
	     record_count ++ ;
      
	     bph.unlock() ;
	     return ;
	}
	
	public void print() throws Exception
	{
	    System.out.println("Record count: " + record_count) ;
	    while(next())
	    {
	        Object[] value = getAll() ;
	        for (int i = 0 ; i < this.rsm.rm.num_filed ; i ++)
	        {
	            System.out.print(value[i] + " ") ;
	        }
	        System.out.println();
	    }
	}
	
	public long getRecordCount()
	{
	    return record_count ;
	}

}
