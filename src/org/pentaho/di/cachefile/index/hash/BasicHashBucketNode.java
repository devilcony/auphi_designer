package org.pentaho.di.cachefile.index.hash;

import java.nio.ByteBuffer;

import org.pentaho.di.cachefile.Constants;
import org.pentaho.di.cachefile.meta.Meta;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;
import org.pentaho.di.cachefile.storage.pageWriter.PageWriterReader;
import org.pentaho.di.cachefile.util.ByteUtil;


/**
 *  Hash bucket page.
 * 
 *  <br>
 *  Page layout sketch:
 *  <br>
 *  <br>--------------------------------------------------------------
 *  <br>|Page header|Hash key|Logical Address|Hash key|Logical Address|             
 *  <br>|Hash key|Logical Address|....................................|
 *  <br>|.............................................................|
 *  <br>|.............................................................|
 *  <br>--------------------------------------------------------------
 * 
 *  <br>
 *  The hash key is in <b>ascending</b> order.
 * 
 * */
public class BasicHashBucketNode extends HashBucketNode
{
    /**
     * For optimization purpose.
     * 
     * */
    public long searchTime = 0 ;

    static BasicHashBucketNode ipw = new BasicHashBucketNode() ;
    
    private BasicHashBucketNode()
    {
        indexItemSize = Constants.size_hash_key+Constants.size_logical_addr ;
    }
    
    public static BasicHashBucketNode getInstance()
    {
        return ipw ;
    }

    @Override
    public int writeData(BufferedPageHeader bph, Meta meta, Object value)
    {
        
        int hashKey = (Integer)(((Object[])value)[0]) ;
        long logicalAddr = (Long)(((Object[])value)[1]) ;
        int itemCount = bph.getItemCount() ;
              
        /*  Binary search for the insert position */
        int low = 0 ; 
        int high = itemCount-1 ;
        int mid = 0;
        int midHashKey = Integer.MAX_VALUE ;
        while(low <= high)
        {
            mid = (low+high)/2 ;
            midHashKey = getHashKey(bph,mid) ;
            if (midHashKey == hashKey)
                break ;
            
            if (midHashKey < hashKey)
                low = mid + 1;
            else
                high = mid -1 ;
        }
        if (midHashKey < hashKey && mid < itemCount)
        {/* bug fixed: add 'mid < itemCount', if itemCount == 0 and mid == 0  */
            mid ++ ;
        }
        
        /*  Move to vacate space for new index item */
        int srcStart,destStart,length ;
        srcStart = bph.pageStartPosition+BufferedPageHeader.pageHeaderSize+mid*indexItemSize ;
        destStart = srcStart + indexItemSize ;
        length = (itemCount-mid)*indexItemSize ;
        System.arraycopy(bph.pageContext, srcStart, bph.pageContext, destStart, length) ;
        
        int writeOffset = BufferedPageHeader.pageHeaderSize + mid*indexItemSize ;
        ByteUtil.writeInt(bph.pageContext, bph.pageStartPosition+writeOffset, hashKey) ;
        ByteUtil.writeLong(bph.pageContext, bph.pageStartPosition+writeOffset+Constants.size_hash_key, logicalAddr) ;

        
        bph.updatePhysicalPageHeader(1, indexItemSize) ;
        
        bph.setDirty() ;
        return writeOffset ;
    }
    
    public boolean isFull(BufferedPageHeader bph, Meta meta, Object obj)
    {
        return bph.getByteLeft() <= (indexItemSize) ;
    }
    
    void print(BufferedPageHeader bph)
    {
        int recordCount = bph.getItemCount() ;
        System.out.println("Record count for this bucket:" + recordCount);
        ByteBuffer bb = ByteBuffer.wrap(bph.pageContext,bph.pageStartPosition,bph.pageSize) ;
        bb.position(BufferedPageHeader.pageHeaderSize) ;
        for (int i = 0 ; i < recordCount ; i++)
        {
            System.out.println("Hash key:" + getHashKey(bph,i) + ", logical address: " + getLogicalAddress(bph,i));
        }
    }
    
    /**
     *  Get logical address of the i'th item.
     * 
     *  @param index start from 0 
     *  
     * */
    public long getLogicalAddress(BufferedPageHeader bph, int index)
    {
       assert (index >= 0 && index < bph.getItemCount()) ;
       return ByteUtil.readLong(bph.pageContext, bph.pageStartPosition+BufferedPageHeader.pageHeaderSize+index*indexItemSize+Constants.size_hash_key) ; 
    }
    
    /**
     *  Get hash key of the i'th index item.
     * 
     * */
    public int getHashKey(BufferedPageHeader bph, int index)
    {
        assert (index >= 0 && index < bph.getItemCount()) ;
        return ByteUtil.readInt(bph.pageContext, bph.pageStartPosition+BufferedPageHeader.pageHeaderSize+index*indexItemSize) ;
    }
    
    /**
     *  Find the first index item whose hash key equals to the given value.
     *  Return the index of the item , return -1 if not found.
     * 
     * */
    public int searchHashKey(BufferedPageHeader bph, int hashKey)
    {
//        long start = System.currentTimeMillis() ;
        int recordCount = bph.getItemCount() ;
        
        /*  Binary search for the first item */
        int low = 0 ; 
        int high = recordCount - 1 ;
        int mid = 0;
        int midHashKey = 0 ;
        while(low <= high)
        {
            mid = (low+high)/2 ;
            midHashKey = getHashKey(bph,mid) ; 
            
            /*  If found a item, push the position forward as possible*/
            if ( midHashKey == hashKey)
            {
                int i = 1 ;
                while( (mid-i)>=0 && (getHashKey(bph,mid-i) == hashKey) )
                    i++ ;
//                searchTime += System.currentTimeMillis() - start ;
                return mid-i+1 ;
            }
            if (midHashKey < hashKey)
                low = mid+1 ;
            else
                high = mid -1 ;
            
        }        
//        searchTime += System.currentTimeMillis() - start ;
        return -1 ;
    }
    
    private static int randomHash(int i)
    {
        return i*1123411%79 ;
    }
    public static void testCase(String[] args)
    {
        int pageSize = 1024 ;
        byte[] pageContext = new byte[pageSize] ;
        BufferedPageHeader bph = new BufferedPageHeader(pageContext, 0, pageSize, -1) ;
        bph.resetFlags() ;
        bph.resetPage() ;
        BasicHashBucketNode hipw = BasicHashBucketNode.getInstance() ;
        
        int hashKey = 0 ;
        long logicalAddr = 0 ;
        for (int i = 0 ; i < 100 ; i ++)
        {
            hashKey = randomHash(i) ;
            logicalAddr = (long)(i*1123123%137) ;
            if (hipw.isFull(bph, null, null))
                break ;
            hipw.writeData(bph, null, new Object[]{hashKey,logicalAddr}) ;
        }
//        hipw.print(bph) ;
        hipw.checkOrder(bph) ;
        
        for (int i = 0 ; i < 100 ; i ++)
        {
            hashKey = randomHash(i) ;
            int index = hipw.searchHashKey(bph, hashKey) ; 
            if ( index != -1)
            {
                System.out.println("Logical address for " + hashKey + ":") ;
                for (int j = index ; ;j ++)
                {
                    logicalAddr = hipw.getLogicalAddress(bph,j) ;
                    if (hashKey != hipw.getHashKey(bph, j))
                        break ;
                    System.out.print(" " + logicalAddr) ;
                }
                System.out.println() ;
            }
        }
        
    }

    /**
     *  Check the order of the index items.
     * 
     * */
    private void checkOrder(BufferedPageHeader bph)
    {
        int recordCount = bph.getItemCount() ;
        for (int i = 0 ; i < recordCount-1 ; i ++)
        {
            if (getHashKey(bph,i) > getHashKey(bph,i+1))
                System.err.println("Something wrong!");
        }
    }
    
    
    public static void main(String[] args)
    {
        testCase(args) ;
    }
}
