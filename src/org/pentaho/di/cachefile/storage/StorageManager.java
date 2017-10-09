package org.pentaho.di.cachefile.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;


import org.pentaho.di.cachefile.storage.buffer.BufferManager;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;
import org.pentaho.di.cachefile.util.ByteUtil;

public final class StorageManager
{
    public static int default_page_size = 32*1024 ;
    /**
     * Number of allocated blank pages per getBlankPage()
     *  
     * */
    public static int number_batch_page = 4 ;
    public static String default_open_mode = "rwd" ;
    
    /**
     *   Page size of this storage, 16K by default.
     *      
     **/
    public int pageSize = default_page_size ;
    /** 
     *  Page count in this storage, start from 1:
     *  because the storage information is stored in page NO.0. 
     **/
    public long pageCount = 1;
    
    /**
     *  Page NO of table meta of this storage
     *  (Need dictionary page if multi-tables).
     *  
     * */
    public long tableMetaPageNo = 1L ;
    

    /**
     *  First blank page, updated while allocating a new page.
     * 
     * */
    public long first_blank_page = -1 ;
    
//    /**
//     *  Reserved information.
//     * 
//     * */
//    public long next_blank_page = -1 ;
    /**
     *  For optimization purpose
     *  
     * */
    public long readTime = 0 ;
    public long writeTime = 0 ;
    
    
    /**
     *  Storage file for writing page images in buffer to physical pages,
     *  <br> or reading verse.
     * 
     * */
    public RandomAccessFile raf ; 
    public byte[] blankPageContent ;
    
    
    
    public StorageManager()
    {
        
    }
    
    public StorageManager(String storagePath) throws IOException
    {
        init(storagePath) ;
    }
    
    
    /**
     *  Create a new storage file. 
     *  If the file exists already, it will be deleted at first.
     * 
     * */
    public static StorageManager createStorage(String storagePath, int pageSize) throws IOException
    {
        File storageFile = new File(storagePath) ;
        if (storageFile.exists())
            storageFile.delete() ;
        
        StorageManager sm = new StorageManager() ;
        sm.pageSize = pageSize ;
        sm.pageCount = 1 ;
        sm.initBlankPageContent(pageSize) ;

        sm.raf = new RandomAccessFile(storagePath, default_open_mode) ;
        sm.writeStroageInformation() ;
        return sm ;
    }
    
    private void initBlankPageContent(int pageSize)
    {
        blankPageContent = new byte[pageSize] ;
    }
    
    private void init(String storagePath) throws IOException
    {
        try
        {
            if (!(new File(storagePath)).exists())
                throw new IOException("File "+storagePath+" not found!") ;
            raf = new RandomAccessFile(storagePath, default_open_mode) ;
            if (raf.length() > 0)
            {
                readStorageInformation() ;
            }
            else
            {
                this.pageCount = 1 ;
                this.pageSize = default_page_size ;
            }
            initBlankPageContent(pageSize) ;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     *  Read storage information from the storage start.
     * 
     * */
    private void readStorageInformation() throws IOException
    {
        raf.seek(0) ;
        pageSize = raf.readInt() ;
        pageCount = raf.readLong() ;
        tableMetaPageNo = raf.readLong();
        first_blank_page = raf.readLong() ;   
    }
    
    /**
     *  Write storage information to the storage start.
     * 
     * 
     * */
    private void writeStroageInformation() throws IOException
    {
        raf.seek(0) ;
        raf.writeInt(pageSize) ;
        raf.writeLong(pageCount) ;
        raf.writeLong(tableMetaPageNo) ;
        raf.writeLong(first_blank_page) ;
    }
    
    /**
     *  Read a physical page into one buffer page, and reset buffer page flags.
     *  
     *  @param bph buffer page header, which wrap the buffer page
     *  @param physicalPageNo physical page no in storage 
     * 
     * */
    public synchronized void readPage(BufferedPageHeader bph, long physicalPageNo) throws IOException
    {
        long start = System.currentTimeMillis() ;
        assert(physicalPageNo > 0 && physicalPageNo < pageCount) ;

        raf.seek(physicalPageNo*pageSize) ;
        raf.read(bph.pageContext, bph.pageStartPosition, pageSize) ;
        readTime += System.currentTimeMillis() - start ;
        bph.resetFlags() ;
        bph.setPageNo(physicalPageNo) ;
    }
    
    /**
     *  Write a buffer page back to physical page.
     * 
     * */
    public synchronized void writePage(BufferedPageHeader bph) throws Exception
    {
        long start = System.currentTimeMillis() ;
        raf.seek(bph.getPageNo()*pageSize) ;
        raf.write(bph.pageContext, bph.pageStartPosition, pageSize) ;
        writeTime = System.currentTimeMillis() - start ;
    }
    
    /**
     *  Allocate a blank physical page.
     *  
     * @throws IOException 
     * 
     * */
    public synchronized long getBlankPageNo(BufferManager bm) throws Exception
    {
        assert(bm.getStorage() == this) ;
        
        /* If there is blank page left  */
        if (first_blank_page > 0)
        {            
            long blank_page = first_blank_page ;
            BufferedPageHeader bph = bm.getPage(first_blank_page) ;
            first_blank_page = bph.getNextPageNo() ;
            bph.unlock() ;
            
            return blank_page ;
        }
        else
        {
            
            if (blankPageContent == null)
                initBlankPageContent(pageSize) ;
            
            long temp = pageCount  ;
            for (int i = 0 ; i < number_batch_page ; i++)
            {
                /*  For the new first and last page, set the next page no to 0L */
                if (i == number_batch_page-1 || i == 0)
                    ByteUtil.writeLong(blankPageContent, BufferedPageHeader.offset_next_page, 0L) ;
                else
                    ByteUtil.writeLong(blankPageContent, BufferedPageHeader.offset_next_page, pageCount+1) ;
                raf.seek(pageCount*pageSize) ;
                raf.write(blankPageContent) ;
                pageCount ++ ;
            }
            first_blank_page = temp+1 ;
            return temp ;
        }
    }
    
    /**
     *  Recover and put back a storage page to use. Write the blank page to disk.
     *  @throws IOException 
     * 
     * */
    public synchronized void recyclePage(BufferedPageHeader bph) throws IOException
    {
        long older_first_blank_page = first_blank_page ;
        first_blank_page = bph.getPageNo() ;
        
        bph.resetPage() ;
        bph.setNextPageNo(older_first_blank_page) ;

        bph.setDirty() ;
    }
    
    
    /**
     *  Write the storage information back 
     *  and close the storage file.
     * 
     * */
    public void close() throws IOException
    {
        if (this.raf != null)
        {
            writeStroageInformation() ;
            this.raf.close() ;
            this.raf = null ;
        }
        System.out.println("Write page time cost:" + writeTime) ;
        System.out.println("Read page time cost:" + readTime) ;
    }
    
    /**
     *  Write the storage information back.
     * 
     * */
    public void flush() throws IOException
    {
        writeStroageInformation() ;
    }    
    
    public long getTableMetaPageNo()
    {
        return tableMetaPageNo;
    }

    public void setTableInfoPageNo(long tableMetaPageNo)
    {
        this.tableMetaPageNo = tableMetaPageNo;
    }    
    
    public String toString()
    {
        return "Page size:"+pageSize +", page count:" + pageCount + ", first blank page:" + first_blank_page ;
    }
    
    public static void main(String[] args) throws IOException
    {
        RandomAccessFile raf = new RandomAccessFile("aa",default_open_mode) ;
        raf.seek(16) ;
        raf.writeInt(1) ;
        raf.close() ;
    }
}
