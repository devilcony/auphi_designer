package org.pentaho.di.cachefile.storage.buffer;


import org.pentaho.di.cachefile.util.ByteUtil;


/**
 *  Memory page header, including mapping information from a physical page to a memory region.
 * 
 * */
public class BufferedPageHeader extends PhysicalPageHeader
{
    /* instance counter */
    public static int headerCounter = 0 ;
    /*  Initialize hit count    */
    public final static int init_hit_count = 256 ;  
    
    /*  information of memory region    */
    public byte[] pageContext ;
    public int pageStartPosition ;
    public int pageSize ;

    /*  page flags for buffer page  */
    private boolean dirty_page_flag = false ;
    private boolean locked = false ;
    public long hitCount = 0 ;
    
    /*  physical page NO in the storage */
    private long pageNo = 0 ;
    
    
    /**
     *  Create a new buffer page header.
     *  
     *  @param pageContext memory region of the buffer page
     *  @param pageStartPosition start position of the buffer page
     *  @param pageSize page size of the buffer page
     *  @param pageNo physical page NO mapped in the storage. 
     *          While pageNo == -1, it means that the buffered page is mapped to no physical page. 
     * 
     * */
    public BufferedPageHeader(byte[] pageContext, int pageStartPosition, int pageSize,long pageNo)
    {
        assert(pageStartPosition + pageSize <= pageContext.length && pageStartPosition >= 0 && pageSize > 0) ;
        this.pageContext = pageContext ;
        this.pageStartPosition = pageStartPosition ;
        this.pageSize = pageSize ;
        this.pageNo = pageNo ;
        headerCounter ++ ;
    }
    
    public long getPageNo()
    {
        return pageNo ;
    }
    
    public void setPageNo(long pageNo)
    {
        this.pageNo = pageNo ;
    }
    
    /**
     *  Reset page header flags: including dirty_page_flag, lock_page_flag, hitCount.
     * 
     * */
    public void resetFlags()
    {
        dirty_page_flag = false ;
        locked = false ;
        hitCount = 0 ;
//        pageNo = -1 ;
    }
    
    /**
     *  Hint that this page could be swapped out.
     * 
     * */
    public void hint()
    {
        hitCount = -1 ;
    }
    
    /**
     *  If the buffered page has been modified.
     * 
     * */
    public boolean isDirty()
    {
        return dirty_page_flag ;
    }
    
    /**
     *  Lock the page in case of swapping out.
     * 
     * */
    protected void lock()
    {
        if (locked)
        {
            System.out.println("Lock a locked page!") ;
        }
        locked = true ;
    }
    
    
    /**
     *  Get next page NO.
     *  Return 0 if no next page.
     * 
     * */
    public long getNextPageNo()
    {
        return ByteUtil.readLong(pageContext, pageStartPosition+offset_next_page) ;
    }
    
    public void setNextPageNo(long nextPage)
    {
        assert (nextPage != this.pageNo) ;
        ByteUtil.writeLong(pageContext, pageStartPosition+offset_next_page, nextPage) ;
        setDirty() ;
    }
    
    /**
     *  Indicate the page to write to the storage while swap out or flushed.
     * 
     * */
    public void setDirty()
    {
        dirty_page_flag = true ;
    }
    
    /**
     *  Get activeness of the page. Usually, the more the page is
     *  visited, the more active the page is.
     * 
     * */
    public long getActiveness()
    {
        return hitCount ;
    }
    
    /**
     *  Whether the page is locked.
     *  If locked, the page won't be swapped out or modified. 
     * 
     * */
    public boolean isLocked()
    {
        return locked ;
    }
    
    /**
     *  Unlock the page after u have operated(read/writing) on this page.
     * 
     * */
    public void unlock()
    {
        if (!locked)
            System.out.println("Unlock a unlocked page!");
        locked = false ;
    }

    /**
     *  Clear this buffer page's content(just reset the physical page header content).
     *  
     * */
    public void resetPage()
    {
        /*  Set record count to 0   */
        ByteUtil.writeLong(pageContext, pageStartPosition+offset_item_count, 0) ;
        
        /*  Set byte used to 0  */
        ByteUtil.writeLong(pageContext, pageStartPosition+offset_bytes_used, 0) ;

        /*  Set next page to -1  */
        ByteUtil.writeLong(pageContext, pageStartPosition+offset_next_page, -1L) ;
    }
    
    
    /**
     *  Increase the hit count of this page.
     * 
     * */
    protected void hit()
    {
        hitCount++ ;
    }
    
    /**
     *  Decrease the hit count of this page.
     * 
     * */
    protected void miss()
    {
        hitCount -- ;
    }    
    
    /**
     *  Get current item count in this page.
     * 
     * */
    public int getItemCount()
    {
        return ByteUtil.readInt(pageContext, pageStartPosition+offset_item_count) ;
    }
    
    /**
     *  Get bytes used, excluding the page header
     * 
     * */
    public int getByteUsed()
    {
        return ByteUtil.readInt(pageContext, pageStartPosition+offset_bytes_used) ;
    }
    
    /**
     *  Return blank space of the page by bytes.
     *  The formula: 'page size' - 'page header size' - 'used byte count'
     * 
     * */
    public int getByteLeft()
    {
        return pageSize - pageHeaderSize - getByteUsed();
    }
    
    public void increamentByteUsed(int increment)
    {
        int byteUsed = getByteUsed();
        byteUsed += increment ;
        ByteUtil.writeInt(pageContext, pageStartPosition+offset_bytes_used, byteUsed) ;
    }
    
    /**
     *  Update physical page header's record count and byte used count.
     * 
     * */
    public void updatePhysicalPageHeader(int recordCountIncre, int byteUsedIncre)
    {
        /*  Update record count */
        int recordCount = getItemCount() ;
        recordCount += recordCountIncre ;          
        ByteUtil.writeInt(pageContext, pageStartPosition+offset_item_count, recordCount) ;
        
        /*  Update byte used    */
        int byteUsed = getByteUsed() ;
        byteUsed += byteUsedIncre ;  
        ByteUtil.writeInt(pageContext, pageStartPosition+offset_bytes_used, byteUsed) ;
        
        assert(byteUsed <= (pageSize - pageHeaderSize)):"byteUsed:"+byteUsed+"pageSize-pageHeaderSize:"+(pageSize - pageHeaderSize) ;
    }
    

    public void print()
    {
        System.out.println("Page NO. :" + this.pageNo) ;
        System.out.println("Page hit count :" + this.hitCount) ;
        System.out.println("Record count:"+this.getItemCount()) ;
        System.out.println("Byte used:" + this.getByteUsed()) ;
    }

    
    public String toString()
    {
        return "PageNO:" + pageNo + ",start position:" + pageStartPosition
                +",hitcount:"+hitCount+",locked:"+locked+",isDirty:"+dirty_page_flag
                +",RecordCount:"+getItemCount()+",byteUsed:"+getByteUsed()+",nextPage:"+getNextPageNo();
    }

    protected void release()
    {
        this.pageContext = null ;
    }

}
