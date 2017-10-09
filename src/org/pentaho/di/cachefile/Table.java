package org.pentaho.di.cachefile;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.cachefile.index.Index;
import org.pentaho.di.cachefile.index.IndexType;
import org.pentaho.di.cachefile.index.btree.BPTreeIndex;
import org.pentaho.di.cachefile.index.hash.LinearHashIndex;
import org.pentaho.di.cachefile.meta.IndexMeta;
import org.pentaho.di.cachefile.meta.RecordMeta;
import org.pentaho.di.cachefile.meta.Serialize;
import org.pentaho.di.cachefile.storage.buffer.BufferManager;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;
import org.pentaho.di.cachefile.storage.buffer.SimpleBufferManager;
import org.pentaho.di.cachefile.storage.pageWriter.DataPage;

public class Table implements Serialize
{

    static DataPage dpw = DataPage.getInstance() ;
    
    /*  Record count    */
    public long record_count = 0;   
    
    /*  Physical page NO of this table meta information */
    public long page_no = -1 ;

    /*  Physical page NO of first data page */
    public long first_data_page = -1;
    
    /*  Physical page NO of last data page */
    public long last_data_page = -1;    
    
    /*  Physical page NO of  information about indexes created on this table  */
    public long indexPageNo = -1;
    
    public RecordMeta rm ;
    public final BufferManager bm ;
    public List<Index> indexes ;
    
    public Table(BufferManager bm, RecordMeta rm) throws Exception
    {
        this.rm = rm ;
        this.bm = bm ;
        indexes = new ArrayList<Index>() ;
        record_count = 0 ;
        indexPageNo = -1 ;

        
        first_data_page = -1 ;
        last_data_page = -1 ;

        BufferedPageHeader bph = bm.getNewPage() ;
        page_no = bph.getPageNo() ;
        bph.unlock() ;
    }
    
    public Table(BufferManager bm) throws Exception
    {
        this.bm = bm ;
        indexes = new ArrayList<Index>() ;
        record_count = 0 ;
        indexPageNo = -1 ;

        
        first_data_page = -1 ;
        last_data_page = -1 ;
    }    
    
    @Override
    public int read(byte[] content, int start, int length) throws Exception
    {
        rm = new RecordMeta() ;
        int readCount = rm.read(content, start, length) ;
        
        ByteBuffer bb = ByteBuffer.wrap(content,start+readCount,length -readCount) ;
        
        /*  Read field number  */
        page_no = bb.getLong() ;
        /*  Write first data page NO.    */
        first_data_page = bb.getLong() ;
        /*  Write last data page NO.    */
        last_data_page = bb.getLong() ;
        /*  Write record count	*/
        record_count = bb.getLong() ;
        
        /*	Read index count	*/
        int indexCount = bb.getInt() ;
        readCount += (bb.position()-start) ;
        for (int i = 0 ; i < indexCount ; i ++)
        {
        	int indexType = bb.getInt(start+readCount) ;
        	readCount += Integer.SIZE/8 ;
        	Index index = IndexType.getIndex(indexType, bm) ;
        	readCount += index.read(content, start+readCount, length-readCount) ;
        	indexes.add(index) ;
        }
//        System.out.println("Read count:"+readCount);
        return readCount ;
    }
    
    @Override
    public int write(byte[] content, int start, int length) throws Exception
    {
    	int writeByteCount = rm.write(content, start, length) ;
        ByteBuffer bb = ByteBuffer.wrap(content,start+writeByteCount,length-writeByteCount) ;
        
        /*  Write page no   */
        bb.putLong(page_no) ;        
        /*  Write first data page NO.   */
        bb.putLong(first_data_page) ;
        /*  Write last data page NO.    */
        bb.putLong(last_data_page) ;
        /*  Write record count  */
        bb.putLong(record_count) ;
        
        /*	Write index count	*/
        bb.putInt(indexes.size()) ;
        writeByteCount += (bb.position()-start) ;
        for (Index index: indexes)
        {
            /* Write index type */
        	bb.putInt(start+writeByteCount, index.getIndexType()) ;
        	writeByteCount += Integer.SIZE/8 ;
        	/* Write index information */
        	writeByteCount += index.write(content, start+writeByteCount, length-writeByteCount) ;
        }

        return writeByteCount ;
    }
    
    
    public static Table createTable(BufferManager bm, RecordMeta tm) throws Exception
    {
    	Table table = new Table(bm,tm) ;
        return table ;
    }
    
    public static Table loadTable(BufferManager bm, long pageNo) throws Exception
    {
    	Table table = new Table(bm) ;
    	table.page_no = pageNo ;
    	BufferedPageHeader bph = bm.getPage(pageNo) ;
//    	bph.lock() ;
    	table.read(bph.pageContext, bph.pageStartPosition, bph.pageSize) ;
    	bph.unlock() ;
    	return table ;
    }
    
    
    /**
     *  Create index on this table, given the index meta information.
     *  
     *  @param im index meta
     *  @param indexType index type, 0 for hash index, 1 for b-tree index
     *  @return the new created index
     * 
     * */
    public Index createIndex(IndexMeta im, int indexType) throws Exception
    {
    	Index index = null ;
    	if (indexType == 0)
    	    index = new LinearHashIndex(bm,im,record_count) ;
    	else if (indexType == 1)
    	    index = new BPTreeIndex(bm, im) ;
    	else
    	    throw new Exception("Illegal index type!") ;

        long logicalAddress = -1L ;
        BufferedPageHeader bph = bm.getPage(first_data_page) ;
        Object[] value = new Object[rm.num_filed] ;
        int count = 0 ;
        while(true)
        {
            int recordCount = bph.getItemCount() ;
            int recordStartOffset = 0 ;

            if ((count ++) % 1000 == 999)
                System.out.println("Page count: " + count) ;
            
            for (int i = 0 ; i < recordCount ; i++)
            {
                recordStartOffset = dpw.getRecordOffset(bph, i)  ;
                dpw.readData(bph,recordStartOffset,rm,value) ;
                
                logicalAddress = (bph.getPageNo()<<16) | recordStartOffset ;

                index.insertRecord(rm, value, logicalAddress) ;
//                if (!((SimpleBufferManager)bm).statusOK())
//                {
//                    System.out.println("Wrong:\n"+((SimpleBufferManager)bm).toString());
//                }
            }
            
            long nextPage = bph.getNextPageNo() ;
            bph.hint() ;
            bph.unlock() ;
            
            if (bm.getLockedPageCount() >= 9)
                return null;
            
            if (nextPage<= 0)
                break ;
            else
            {
                bph = bm.getPage(nextPage) ;
            }
        }    
        
        /*  Add the new created index to table's index list */
        indexes.add(index) ;
    	
        return index ;
    }
    public Index getIndex(int i)
    {
        return indexes.get(i) ;
    }
    
    /**
     * 	Insert a new record. Return the logical address of this record.
     * @throws Exception 
     * 
     * 
     * */
    public long insert(Object[] value) throws Exception
    {
    	long logicalAddress = -1L ;
    	BufferedPageHeader bph = null ;
        
        /*	If empty table, create the first data page*/
        if (first_data_page <= 0)
        {
        	bph = bm.getNewPage() ;
//        	bph.page_type = "Data page" ;
//        	bph.lock() ;
        	first_data_page = bph.getPageNo() ;
        	last_data_page = bph.getPageNo() ;
        	bph.unlock() ;
        }
        
        /*	Get the last data page to insert record	*/
        bph = bm.getPage(last_data_page) ;
//        bph.lock() ;


        /*  If the page is full, get a new page */
        if (dpw.isFull(bph,rm, value))
        {   
            BufferedPageHeader temp = bph ;
            bph = bm.getNewPage() ;
            temp.setNextPageNo(bph.getPageNo()) ;
            temp.hint() ;
            temp.unlock() ;
            last_data_page = bph.getPageNo() ;
        }
        
        /*	Write data	to page	*/
        int offset = dpw.writeData(bph,rm, value) ;
        
        /*	Compute logical address	*/
        logicalAddress = (bph.getPageNo() << 16) | offset ;
        
        /*  Unlock the page after writing   */
        bph.unlock() ;
        
        record_count ++ ;
    	return logicalAddress ;
    }
    
    /**
     * 	Close the table, flush the table information to buffer.
     * @throws Exception 
     * 
     * */
    public void close() throws Exception
    {
        BufferedPageHeader bph = bm.getPage(page_no) ;
//        bph.lock() ;
        write(bph.pageContext,bph.pageStartPosition,bph.pageSize) ;
        bph.setDirty() ;
        bph.unlock() ;
    }
    
    public long getLast_data_page()
    {
        return last_data_page;
    }

    public void setLast_data_page(long last_data_page)
    {
        this.last_data_page = last_data_page;
    }
    
    public long getRecord_count()
    {
        return record_count;
    }

    public void setRecord_count(long record_count)
    {
        this.record_count = record_count;
    }    
    
    public long getPage_no()
    {
        return page_no;
    }

    public void setPage_no(long page_no)
    {
        this.page_no = page_no;
    }
    public String toString()
    {
        StringBuffer sb = new StringBuffer(1024) ;
        sb.append("Page NO:" + page_no) ;
        sb.append(",First data page:" + first_data_page);
        sb.append(",Last data page:" + last_data_page);
        sb.append(",Record count:" + record_count);
        sb.append(",Index count:" + indexes.size()).append("\n");
        for (int i = 0 ; i < indexes.size() ; i ++)
            sb.append(indexes.get(i)) ;
        return sb.toString() ;
    }

    public void flush() throws Exception
    {
        BufferedPageHeader bph = bm.getPage(page_no) ;
//        bph.lock() ;
        write(bph.pageContext,bph.pageStartPosition,bph.pageSize) ;
        bph.setDirty() ;
        bph.unlock() ;
    }
    
}
