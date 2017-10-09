package org.pentaho.di.cachefile.storage.buffer;


import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.pentaho.di.cachefile.storage.StorageManager;

public class SimpleBufferManager implements BufferManager
{
    public float ratio = 0.8f ;
    public int pageSize ;
    public int maxPageCount ;
    public int nextPosition = 0 ;
    
    /*  For optimization purpose    */
    public long swapTimes = 0 ;
    public long getPageTime = 0;

    
    CachePolicy DefaultCachePolicy =  null ;
    byte[] buffer ;

    private StorageManager sm ;
    
    /**
     * Physical page NO  to buffer page mapping.
     * 
     * */
    HashMap<Long,BufferedPageHeader> pageHeadersMap ;
    BufferedPageHeader[] pageHeaderList ;
    
    
    public SimpleBufferManager()
    {
        
    }
    
    public static void main(String[] args)
    {
    }
    
    @Override
    public boolean init(StorageManager storage, int maxBufferSize, CachePolicy cp)
    {
        assert(maxBufferSize>storage.pageSize && storage.pageSize > 0) ;
        System.out.println("Init buffer size:" + maxBufferSize/1024/1024 + " MB!") ;
        buffer = new byte[maxBufferSize] ;
        this.maxPageCount = maxBufferSize/storage.pageSize ;
        this.pageSize = storage.pageSize ;
        pageHeaderList = new BufferedPageHeader[maxPageCount] ;
        pageHeadersMap = new HashMap<Long,BufferedPageHeader>((int)(maxPageCount*ratio)) ;
        sm = storage ;
        
        for (int i = 0 ; i < maxPageCount ; i ++)
        {
            pageHeaderList[i] = new BufferedPageHeader(buffer,i*pageSize,pageSize,-1) ;
        }
        
        return true ;
        
    }
    
    /**
     *  Find the swap out page by LRU(Least Recently Used).
     *  If the buffer is non-saturated, return a new buffer page header with
     *  physical page no  = -1. 
     *  
     * 
     * */
    public BufferedPageHeader getSwapOutPage() throws Exception
    {
        /*  If buffer is non-saturated  */
        if (pageHeadersMap.size() < maxPageCount)
        {
            return pageHeaderList[pageHeadersMap.size()] ;
        }
        
        /*  Find the least active page  */
        long minHitCount = Long.MAX_VALUE ;
        long hitCount = 0 ;
        BufferedPageHeader swapOutBph = null ;
        BufferedPageHeader bph = null ;
        for (int i = 0 ; i < pageHeaderList.length ; i++)
        {
            bph = pageHeaderList[nextPosition] ;
//            bph = pageHeaderList[(int)(Math.random()*pageHeaderList.length)];
//            if (!bph.isLocked())
//                return bph ;
            if (!bph.isLocked())
            {
                hitCount = bph.getActiveness() ;
                if (hitCount < minHitCount)
                {
                    swapOutBph = bph ;
                    minHitCount = hitCount ;
                    if (hitCount <= 0)
                        return swapOutBph ;
                }
                bph.miss() ;
            }
            
            nextPosition = (nextPosition+1)%pageHeaderList.length ;
        }
        swapTimes ++ ;

        assert(swapOutBph!=null) ;
        return swapOutBph ;
    }

    @Override
    public BufferedPageHeader getPage(long pageNo) throws Exception
    {
//        long start = System.currentTimeMillis() ;
        assert(pageNo>=0) ;
        
        BufferedPageHeader bph = pageHeadersMap.get(pageNo) ;
        
        /*  If found in buffer, add hit count and return    */
        if (bph!=null)
        {
            bph.hit() ;
            bph.lock() ;
//            getPageTime += System.currentTimeMillis() - start ;
            return bph ;
        }
        
        /*
         *  If not found, swap the wanted page into
         *   the buffer(swap one page out if necessary).
         *   
         * */
        bph = getSwapOutPage() ;
        bph = swapPage(bph,pageNo) ;
        bph.lock() ;
//        getPageTime += System.currentTimeMillis() - start ;
        return bph ;
        
    }
    
    /**
     * Swap one page out and one page in.
     * Return the page header of the new-in page.
     * 
     * @param outPage page to be swap out
     * @param pageNoIn page no to be swap in
     * 
     * @throws IOException 
     * 
     * */
    public BufferedPageHeader swapPage(BufferedPageHeader outPage, long pageNoIn) throws Exception
    {
        assert(pageHeadersMap.get(pageNoIn) == null) ;
        
        /*  If dirty, write buffer page to the storage   */
        if (outPage.isDirty())
            sm.writePage(outPage) ;
        pageHeadersMap.remove(outPage.getPageNo()) ;

        /*  swap the new page into the buffer from the storage   */
        sm.readPage(outPage,pageNoIn) ;
        pageHeadersMap.put(pageNoIn, outPage) ;
        /*  Give the page a initial hit count  */
        outPage.hitCount = BufferedPageHeader.init_hit_count ;

        return outPage ;
    }

    @Override
    public BufferedPageHeader getNewPage() throws Exception
    {
        long newPage = sm.getBlankPageNo(this) ;
        BufferedPageHeader newBph = getPage(newPage) ;
        newBph.resetPage() ;
        return newBph ;
    }
    


    @Override
    public void flush() throws Exception
    {
        Collection<BufferedPageHeader> headers = pageHeadersMap.values() ;
        
        Iterator<BufferedPageHeader> headerItr = headers.iterator() ;
        BufferedPageHeader bph = null ;
        while(headerItr.hasNext())
        {
            bph = headerItr.next() ; 
            if (bph.isDirty())
            {
                sm.writePage(bph) ;
                bph.resetFlags() ;
            }
        }
    }

    @Override
    public void close() throws Exception
    {
        flush() ;
        this.buffer = null ;
        for (int i = 0 ; i < pageHeaderList.length ; i++)
            pageHeaderList[i].release() ;
        this.pageHeaderList = null ;
        this.pageHeadersMap.clear() ;
        this.pageHeadersMap = null ;
        this.DefaultCachePolicy = null ;
        System.out.println("Swap times:" + swapTimes) ;
        System.out.println("Get page time: " + getPageTime);
    }

    @Override
    public void recyclePage(BufferedPageHeader bph) throws IOException
    {
        sm.recyclePage(bph) ;
    }

    @Override
    public int getPageSize()
    {
        return pageSize ;
    }

    public int getPageCount()
    {
        return maxPageCount ;
    }
    
    @Override
    public int getLockedPageCount() throws Exception
    {
        Collection<BufferedPageHeader> headers = pageHeadersMap.values() ;
        
        int lockedPageCount = 0 ;
        Iterator<BufferedPageHeader> headerItr = headers.iterator() ;

        while(headerItr.hasNext())
        {
            if (headerItr.next().isLocked())
                lockedPageCount ++ ;
        }
        
        return lockedPageCount ;
    }
    
    public boolean statusOK()
    {
        if (pageHeadersMap.size() > maxPageCount)
            return false ;
        
        Collection<BufferedPageHeader> headers = pageHeadersMap.values() ;
        
        int[] starts = new int[pageHeadersMap.size()] ;
        int i = 0 ;
        
        Iterator<BufferedPageHeader> headerItr = headers.iterator() ;

        while(headerItr.hasNext())
        {
            BufferedPageHeader bph = headerItr.next() ; 
            if (bph.getPageNo() >= sm.pageCount)
                return false ;
            if (bph.getNextPageNo() >= sm.pageCount && bph.getPageNo() > 1)
                return false ;
            starts[i++] = bph.pageStartPosition ;
        }
        
        Arrays.sort(starts) ;
        for (i = 0 ; i < starts.length -1 ; i++)
        {
            if (starts[i] == starts[i+1])
                return false ;
        }

        return true ;
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer(1024) ;
        sb.append("Current buffer header count: " + pageHeadersMap.size()).append("\n");
        int lockCount = 0 ;
        Collection<BufferedPageHeader> headers = pageHeadersMap.values() ;
        Iterator<BufferedPageHeader> headerItr = headers.iterator() ;
        while(headerItr.hasNext())
        {
            BufferedPageHeader bph = headerItr.next() ;
            if (bph.isLocked())
                lockCount ++ ;
            sb.append(bph).append("\n") ;
        }     
        sb.append("Lock count:").append(lockCount) ;
        return sb.toString();
    }

//    @Override
//    public BufferedPageHeader getTempPage() throws Exception
//    {
//        BufferedPageHeader swapOut = getSwapOutPage() ;
//        if (swapOut.isDirty())
//            sm.writePage(swapOut) ;
//        swapOut.resetPage() ;
//        swapOut.resetFlags() ;
//        swapOut.lock() ;
//        return swapOut ;
//    }
    
    @Override
    public final StorageManager getStorage()
    {
        return sm ;
    }

}
