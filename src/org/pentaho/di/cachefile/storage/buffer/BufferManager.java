package org.pentaho.di.cachefile.storage.buffer;

import org.pentaho.di.cachefile.storage.StorageManager;

public interface BufferManager
{

    /**
     * Initialize the buffer manager. <br>
     * The size of buffer page is equal to that of physical page in the storage. 
     * 
     * @param storage the storage under this buffer
     * @param maxBufferSize max size of buffer by byte
     * @param cachePolicy policy for swap buffer page out, using LRU by default
     * 
     * */
    public boolean init(StorageManager storage, int maxBufferSize , CachePolicy cachePolicy) ;
    
    
    /**
     *  Get the buffer page mapped to the physical page in the storage.
     *  If not found in the buffer, swap one old page out(with cache-policy:LRU,etc.), 
     *  and swap the physical page in from the storage.
     *  
     *  @param pageNo page no of physical page in the storage
     *  
     *  @return The page is <b>locked</b> before return, remember to unlock it after use.
     * 
     * */
    public BufferedPageHeader getPage(long pageNo) throws Exception ;
    
    
    /**
     *  Get a new blank physical page from the storage, and map it to buffer.
     *  
     *  @return the buffer page header wrap the buffer page, which is mapped to the new blank physical page
     *          The page is <b>locked</b> before return.
     *  
     * */
    public BufferedPageHeader getNewPage() throws Exception ;
    
    
//    /**
//     * Get a temporary blank buffer page, no physical page to the blank page.
//     * The returned buffered page is usually used for temporary memory region.
//     * 
//     * @return the buffer page is <b>locked</b> before return, remember to unlock it after use. 
//     * 
//     * */
//    public BufferedPageHeader getTempPage() throws Exception ;
//    
    
    /**
     *  Recycle a page to the storage.
     * 
     * */
    public void recyclePage(BufferedPageHeader bph) throws Exception;
    
    
    /**
     *  Get page size.
     * 
     * */
    public int getPageSize() ;
    
    
    /**
     *  Get page count.
     * 
     * */
    public int getPageCount() ;
    
    
    /**
     *  Flush all dirty pages to the storage.
     * 
     * */
    public void flush() throws Exception ;
    
    
    /**
     *  Flush all pages to the storage and release the buffer.
     * 
     * */
    public void close() throws Exception;
    
    /**
     * Get count of locked pages.
     * 
     * */
    public int getLockedPageCount() throws Exception ;
    
    /**
     * Get storage to which the buffer is mapped.
     * 
     * */
    public StorageManager getStorage() ;
}
