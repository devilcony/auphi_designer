package org.pentaho.di.cachefile.index.hash;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.pentaho.di.cachefile.Constants;
import org.pentaho.di.cachefile.ResultSet;
import org.pentaho.di.cachefile.index.Index;
import org.pentaho.di.cachefile.index.IndexType;
import org.pentaho.di.cachefile.meta.IndexMeta;
import org.pentaho.di.cachefile.meta.Serialize;
import org.pentaho.di.cachefile.meta.RecordMeta;
import org.pentaho.di.cachefile.storage.buffer.BufferManager;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;
import org.pentaho.di.cachefile.storage.buffer.SimpleBufferManager;
import org.pentaho.di.cachefile.util.ByteUtil;

/**
 *  Linear hashing index.
 * 
 * 
 * */
public class LinearHashIndex extends Index implements HashIndex
{
    public static HashBucketNode hashBucket = BasicHashBucketNode.getInstance() ;
//    public  static HashBucketNode hashBucket = HashBucketOptimized.getInstance() ;

    
    public static int indexItemSize = hashBucket.indexItemSize ;
    
    /*  Bit length used of hash key   */
    public int hash_key_length = 0 ;
    
    /*  Bucket count of the whole index, including  header buckets and overflow buckets */
    public int num_buckets = 0 ;
    public int num_buckets_total = 0;
    
    /*  Record count of the whole index */
    public long num_record = 0;
    
    /*  Byte count of index data, used to compute fill-in ratio    */
    public long num_bytes = 0 ;
    
    /*  Bucket fill-in ratio  */
    public double ratio = 1.0 ;
    
    /*  Page No of bucket pointer array  */
    public long bucket_pointer_page_no = -1 ;
    
    /*  The buffer manager the index bound up with */
    public final BufferManager bm  ;
    
    /**
     * For optimization purpose
     * 
     * */
    public long findRealKeyTime = 0 ;
    public long conflictTimes = 0 ;
    public long splitTime = 0 ;
    public long insertTime = 0 ;
    public long insertTotalTime = 0 ;
    public long writeDataTime = 0 ;
    public long preSearchTime = 0 ;
    
    /**
     * 
     * 	Initialize a index, given the index meta data and the buffer manager.
     * 
     * */
    public LinearHashIndex(BufferManager bm, IndexMeta indexMeta, long recordCount) throws Exception
    {
    	indexType = IndexType.LinearHashIndex ;
        meta = indexMeta ;
        this.bm = bm ;
        
        int hash_key_length = computeInitHashKeyLen(recordCount) ;
        initHashBucket(hash_key_length) ;
    }

    /**
     *  
     * 
     * */
    public LinearHashIndex(BufferManager bm) throws Exception
    {
    	indexType = IndexType.LinearHashIndex ;
        this.bm = bm ;
        this.bucket_pointer_page_no = -1 ;
        this.hash_key_length = 0 ;
        this.num_buckets = 0 ; 
        this.num_buckets_total = 0 ;
        this.num_bytes = 0 ;
        this.num_record = 0 ;
        this.meta = null ;
    }

    int computeInitHashKeyLen(long recordCount)
    {
        if (recordCount <= 0)
            return init_hash_key_length ;
        
        /* log2(expectedPage/ratio) */
        int hashKeyLen = (int)(Math.log(recordCount*1.0*(indexItemSize)/ratio/bm.getPageSize())/Math.log(2)) ;
        
        /* log2(maxBufferPage)    */
        hashKeyLen =  Math.min((int)(Math.log(bm.getPageCount())/Math.log(2)), hashKeyLen);
        
        if (hashKeyLen <= 0)
            return init_hash_key_length ;
        else
            return hashKeyLen ;
    }    
    
    /** 
     *  Get hash key of record value.
     *
     **/
    public int hash(RecordMeta rm,Object[] value)
    {
        int hashKey = -1 ;
        int[] hashField = meta.getField_index() ;
        for (int i = 0 ; i < hashField.length ; i ++)
        {
            hashKey = hashKey ^ meta.getField_formats()[i].hashKey(value[hashField[i]]) ;
        }
        return hashKey ;
    }
    
    /**
     *  Compute hash key.
     * @throws Exception 
     * 
     * */
    public int hash(RecordMeta rm, byte[] content ,int start, int length) throws Exception
    {
        Object[] value = new Object[rm.num_filed] ;
        int offset = 0 ;
        for (int i = 0 ; i < rm.num_filed ; i++)
            offset += rm.getField_formats()[i].readValue(content, start+offset, value, i) ;
        
        return hash(rm,value) ;
    }
    
    /**
     *  Get hash key of key value.
     * 
     * */
    private int hash(Object[] keyValue)
    {
        assert(keyValue.length == meta.numKeyFields) ;
        int hashKey = -1 ;
        int[] hashField = meta.getField_index() ;
        for (int i = 0 ; i < hashField.length ; i ++)
        {
            hashKey = hashKey ^ meta.getField_formats()[i].hashKey(keyValue[i]) ;
        }
        return hashKey ;
    }
    
    
//    private static int hash(byte[] value)
//    {
//        int hashKey = 1 ;
//        for (int i = 0 ; i < value.length ; i ++)
//        {
//            hashKey = hashKey ^ (value[i]<<(8*i%64)) ;
//        }
//        return hashKey ;
//    }
//    
    public static void main(String []args)
    {
        
    }


    @Override
    public int read(byte[] content,int start, int length) throws Exception
    {
        /*  Read index meta information */
    	meta = new IndexMeta() ;
        int readByteCount = meta.read(content, start, length) ;
        
        ByteBuffer bb = ByteBuffer.wrap(content, start+readByteCount, length-readByteCount) ;

        /*  Read index information  */
        bucket_pointer_page_no = bb.getLong() ;
        
        hash_key_length = bb.getInt() ;
        
        num_buckets = bb.getInt() ;
        
        num_record = bb.getLong() ;
        
        num_bytes = bb.getLong() ;
        
        ratio = bb.getDouble() ;
        
        return bb.position() - start ;
    }


    @Override
    public int write(byte[] content, int start, int length) throws Exception
    {
        /*  Write index meta information    */
    	int byteWriteCount = meta.write(content, start, length) ;
    	
        ByteBuffer bb = ByteBuffer.wrap(content, start+byteWriteCount, length-byteWriteCount) ;
        
        /*  Write index information */
        bb.putLong(bucket_pointer_page_no) ;
        
        bb.putInt(hash_key_length) ;
        
        bb.putInt(num_buckets) ;
        
        bb.putLong(num_record) ;
        
        bb.putLong(num_bytes) ;
        
        bb.putDouble(ratio) ;
        
        return bb.position() - start;
    }
    
    private boolean fillinRatio()
    {
        if (num_record == 0)
            return false ;
        
        return num_bytes*1.0/(num_buckets*bm.getPageSize()) >= ratio ;
    }
    
    
    /**
     * 	Insert a index item(hash key + logical address) to a bucket.
     * 
     * */
    public void insertIndex(int bucketNo, int hashKey, long logicalAddr) throws Exception
    {
        long start = System.currentTimeMillis() ;
        long pageNo = getPageNoByBucketNo(bucketNo) ;
        BufferedPageHeader bph = bm.getPage(pageNo) ;
        
        if (hashBucket.isFull(bph, null, null))
        {
            /*  Insert a new bucket into the first place  */
            long next_page = bph.getPageNo() ;
            bph.hint() ;
            bph.unlock() ;
            bph = bm.getNewPage() ;
            bph.setNextPageNo(next_page) ;
            updateBucketPageNo(bucketNo,bph.getPageNo()) ;
            num_buckets_total++ ;
        }
        long start_writeData = System.currentTimeMillis() ; 
        hashBucket.writeData(bph,meta, new Object[]{hashKey,logicalAddr}) ;
        writeDataTime += System.currentTimeMillis() - start_writeData ;
        num_bytes += indexItemSize ;
        num_record ++ ;
        bph.unlock() ;
        insertTime += System.currentTimeMillis() - start ;
        return ;
    }

    
    /**
     *  Get bucket NO given the hash key.
     * 
     * */
    public int getBucketNoByHashKey(int hashKey)
    {
        int bucketNo = hashKey & ((-1<<(hash_key_length))^-1) ;
        if (bucketNo >= num_buckets)
            bucketNo = bucketNo - (1<<(hash_key_length-1)) ;
        
        return bucketNo ;
    }
    
    /**
     *  Initialize the hash bucket.
     *  @param hash_key_length initial bit length for hash
     * 
     * */
    public void initHashBucket(int hash_key_length) throws Exception
    {
        assert(hash_key_length > 0) ;
        BufferedPageHeader bucketPointerBph = bm.getNewPage() ;
//        bucketPointerBph.page_type = "Hash bucket pointer page" ;
//        bucketPointerBph.lock() ;
        bucket_pointer_page_no = bucketPointerBph.getPageNo() ;
        bucketPointerBph.unlock() ;
        
        System.out.println("Init hash key length:" + hash_key_length);
        this.hash_key_length = hash_key_length ;
        num_buckets = 1<<hash_key_length ;
        num_buckets_total = num_buckets ;
        
        for (int i = 0 ; i < num_buckets ; i ++)
        {
            BufferedPageHeader bucketHeader = bm.getNewPage() ; ;
            insertBucketPageNo(i,bucketHeader.getPageNo()) ;
            bucketHeader.unlock() ;
        }
        System.out.println("Hash index initial over!");
        return ;
    }
    
    /**
     *  Get bucket's page NO given the bucket NO.
     *  
     *  @param bucketNo start from 0
     *  @return return -1 if no page found for the bucket
     *  
     *  @throws Exception 
     * 
     * */
    public long getPageNoByBucketNo(int bucketNo) throws Exception
    {
        BufferedPageHeader bph = bm.getPage(bucket_pointer_page_no) ;
     
        while(bph.getItemCount() < (bucketNo+1))
        {
            bucketNo = bucketNo - bph.getItemCount() ;
            long nextBucketPointerPage = bph.getNextPageNo() ;
            bph.unlock() ;
            
            if (nextBucketPointerPage <= 0)
                return -1 ;
            
            bph = bm.getPage(nextBucketPointerPage) ;
        }

        long pageNo = ByteUtil.readLong(bph.pageContext, bph.pageStartPosition+BufferedPageHeader.pageHeaderSize+bucketNo*Constants.size_page_no) ;
        bph.unlock() ;
        
        return pageNo ;
        
    }
    
    public boolean isFullBucketPointer(BufferedPageHeader bph)
    {
        return bph.getByteLeft() < Constants.size_page_no ;
    }
    
    /**
     *  Update page no for exist bucket.
     *   
     * */
    public void updateBucketPageNo(int bucketNo, long pageNo) throws Exception
    {
        assert(bucketNo < num_buckets) ;
        
        BufferedPageHeader bph = bm.getPage(bucket_pointer_page_no) ;
//        bph.lock() ;

        int index = bucketNo ;
        while(true)
        {
            /*  Find the pointer page which the bucket pointer store    */
            int recordCount = bph.getItemCount() ;
            if (index < recordCount)
            {
                updateBucketPageNo(bph,index,pageNo) ;
                bph.unlock() ;
                break ;
            }
            else
            {
                index -= recordCount ;
                
//                /* Move to next bucket pointer page    */
                long next_bucket_pointer_page_no = bph.getNextPageNo() ;
//                if (next_bucket_pointer_page_no < 0)
//                {/*  If no next bucket pointer page, create a new one    */
//                    BufferedPageHeader newBph = bm.getNewPage() ;
//                    bph.page_type = "Hash bucket pointer page" ;
//                    newBph.lock() ;
//                    
//                    bph.setNextPageNo(newBph.getPageNo()) ;
//                    bph.unlock() ;
//                    
//                    updateBucketPageNo(newBph, index,pageNo) ;
//                    newBph.unlock() ;
//                    break ;
//                }
//                else
//                {/* Move to  next bucket pointer page    */
                
                    bph.unlock() ;
                    bph = bm.getPage(next_bucket_pointer_page_no) ;
//                    bph.lock() ;
//                }
            }
        }        
    }
    /**
     *  Add page no for a new bucket.
     *  
     * @throws Exception 
     * 
     * */
    public void insertBucketPageNo(int bucketNo, long pageNo) throws Exception
    {
        assert(bucketNo < num_buckets) ;
        
        BufferedPageHeader bph = bm.getPage(bucket_pointer_page_no) ;

        int index = bucketNo ;
        while(true)
        {
            /*  Find the pointer page which the bucket pointer store    */
            int recordCount = bph.getItemCount() ;
            if (!isFullBucketPointer(bph))
            {
                insertBucketPageNo(bph,pageNo) ;
                bph.unlock() ;
                break ;
            }
            else
            {
                index -= recordCount ;
                
                /* Move to next bucket pointer page    */
                long next_bucket_pointer_page_no = bph.getNextPageNo() ;
                if (next_bucket_pointer_page_no < 0)
                {/*  If no next bucket pointer page, create a new one    */
                    BufferedPageHeader newBph = bm.getNewPage() ;
//                    newBph.lock() ;
                    
                    bph.setNextPageNo(newBph.getPageNo()) ;
                    bph.unlock() ;
                    
//                    updateBucketPageNo(newBph, index,pageNo) ;
                    insertBucketPageNo(newBph,pageNo) ;
                    newBph.unlock() ;
                    break ;
                }
                else
                {/* Move to  next bucket pointer page    */
                    bph.unlock() ;
                    bph = bm.getPage(next_bucket_pointer_page_no) ;
//                    bph.lock() ;
                }
            }
        }
        
    }

    
    /**
     *  Add new page no in bucket pointer page.
     * 
     * */
    private void insertBucketPageNo(BufferedPageHeader bph, long pageNo)
    {
        assert(Constants.size_page_no <= bph.getByteLeft());
        ByteUtil.writeLong(bph.pageContext, 
            bph.pageStartPosition+BufferedPageHeader.pageHeaderSize+bph.getItemCount()*Constants.size_page_no, 
            pageNo) ;
        bph.updatePhysicalPageHeader(1, Constants.size_page_no) ;
        bph.setDirty() ;
    }
    
    /**
     *  Update exist page no in bucket pointer page.
     * 
     * */
    private void updateBucketPageNo(BufferedPageHeader bph, int index, long pageNo)
    {
        assert(index < bph.getItemCount()):"Bucket index:"+index+", pageNo:" + pageNo ;
        
        ByteUtil.writeLong(bph.pageContext, 
            bph.pageStartPosition+BufferedPageHeader.pageHeaderSize+index*Constants.size_page_no, 
            pageNo) ;
        bph.setDirty() ;
    }
    
    /**
     *  Add a new bucket, and split the items in old bucket to the new bucket.
     *  Return the new bucket NO.
     *  
     * */
    public int addNewBucket() throws Exception
    {
        int newBucketNo = num_buckets ++ ;
  
        if (num_buckets > (1<<(hash_key_length)))
        {
            hash_key_length ++ ;        
        }
        
        /* split bucket */
        splitBucket(newBucketNo, hash_key_length) ;
        
        return newBucketNo ;
    }
    
    
    
    /**
     *  Split index items in bucket 0xxx to bucket 0xxx and bucket 1xxx. 
     *  
     *  @param newBucketNo NO of the new bucket: 1xxx 
     *  @param hash_key_length length of the binary bucket NO   
     *  
     *  @throws Exception 
     * 
     * */
    public void splitBucket(int newBucketNo, int hash_key_length) throws Exception
    {
        long start = System.currentTimeMillis() ;
        
        int splitBucketNo = newBucketNo - (1<<(hash_key_length-1)) ;

        /*  Create two new bucket */
        BufferedPageHeader newBucket1Bph = bm.getNewPage() ;
        BufferedPageHeader newBucket0Bph = bm.getNewPage() ;
        num_buckets_total++ ;
        num_buckets_total++ ;
        
        BufferedPageHeader splitBucketBph = bm.getPage(getPageNoByBucketNo(splitBucketNo)) ;
        
        int hash_key ;
        long logicalAddr ;
       
        int count0 = 0 ;
        int count1 = 0 ;
        int checkBit = 1<<(hash_key_length-1) ;
        while(true)
        {
        	/*	for each item in the bucket to be split	*/
            int splitCount0 = 0 ;
            int splitCount1 = 0 ;
            int recordCount = splitBucketBph.getItemCount() ;
            for (int i = 0 ; i < recordCount ; i++)
            {
                hash_key = hashBucket.getHashKey(splitBucketBph, i) ;
                logicalAddr = hashBucket.getLogicalAddress(splitBucketBph, i) ;
                if ( (hash_key&checkBit) == 0)
                {/* Move to bucket 0xxx */
                    count0 ++ ;
                    splitCount0 ++ ;
                    if (hashBucket.isFull(newBucket0Bph,null,null))
                    {
                        BufferedPageHeader temp = bm.getNewPage() ;
                        temp.setNextPageNo(newBucket0Bph.getPageNo()) ;
                        newBucket0Bph.hint() ;
                        newBucket0Bph.unlock() ;
                        num_buckets_total++ ;
                        newBucket0Bph = temp ;
                    }
                    hashBucket.writeData(newBucket0Bph, meta, new Object[]{hash_key, logicalAddr}) ;
                }
                else
                {/* Move to bucket 1xxx */
                    count1 ++ ;
                    splitCount1 ++ ;
                    if (hashBucket.isFull(newBucket1Bph,null,null))
                    {
                        BufferedPageHeader temp = bm.getNewPage() ;
                        temp.setNextPageNo(newBucket1Bph.getPageNo()) ;
                        newBucket1Bph.hint() ;
                        newBucket1Bph.unlock() ;
                        num_buckets_total++ ;
                        newBucket1Bph = temp ;
                    }
                    
                    hashBucket.writeData(newBucket1Bph, meta, new Object[]{hash_key,logicalAddr}) ;

                }

            }
            assert(splitCount0+splitCount1 == recordCount):"Split error" ;         
            
            long nextOverFlowBucketPageNo = splitBucketBph.getNextPageNo() ;
            
            /*	Recycle the split bucket	*/
            bm.recyclePage(splitBucketBph) ;
            splitBucketBph.unlock() ;
            num_buckets_total -- ;
            
            /*  If next over flow bucket exists, go on  */
            if (nextOverFlowBucketPageNo <= 0)
                break ;
            else
            {
                splitBucketBph = bm.getPage(nextOverFlowBucketPageNo) ;
            }
        }
        
        updateBucketPageNo(splitBucketNo, newBucket0Bph.getPageNo()) ;
        insertBucketPageNo(newBucketNo,newBucket1Bph.getPageNo()) ;
        
        newBucket0Bph.unlock() ;
        newBucket1Bph.unlock() ;
        
        splitTime += System.currentTimeMillis() - start ;
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer(1024) ;
        sb.append("Bucket pointer page no:" + bucket_pointer_page_no);
        sb.append(",Hash key length:" + hash_key_length);
        sb.append(",Number of buckets:" + num_buckets);
        sb.append(",Number of bytes:" + num_bytes);
        sb.append(",Number of record:" + num_record);
        sb.append("\n") ;
        return sb.toString() ;
    }
    
    public boolean equal(RecordMeta rm, byte[] content, int start, int length, Object[] keyValue) throws Exception
    {
        int offset ;
        int[] orderedIndex = Arrays.copyOf(meta.field_index,meta.field_index.length) ;
        Arrays.sort(orderedIndex) ;
        for (int i = 0 ; i < meta.numKeyFields ; i ++)
        {
            offset = rm.getFieldOffset(content, start, meta.field_index[i]) ;
            if ( (meta.getField_formats()[i]).compare(content, start+offset, keyValue[i]) != 0)
                return false ;
        }
        return true ;
    }
    

	@Override
	public ResultSet searchRecord(RecordMeta tableMeta, Object[] keyValue, int[] pre_index) throws Exception 
	{
	    long start = System.currentTimeMillis() ;
	    assert(keyValue != null && this.meta.numKeyFields == keyValue.length) ;
	    int hashKey = hash(keyValue) ;
	    int bucketNo = getBucketNoByHashKey(hashKey) ;
	    long page_no = getPageNoByBucketNo(bucketNo) ;
	    ResultSet rs = new ResultSet(bm,tableMeta,pre_index) ;
	    BufferedPageHeader bph = bm.getPage(page_no) ;
	    preSearchTime += System.currentTimeMillis() - start ;
//        start = System.currentTimeMillis() ;
	    while(true)
		{
		    int recordCount = bph.getItemCount() ; 
		    int offsetIndex = hashBucket.searchHashKey(bph,hashKey) ;
		    if (offsetIndex >= 0)
		    {
		        for (int i = offsetIndex ; i < recordCount && hashBucket.getHashKey(bph,i) == hashKey ; i++)
    		    {
    		        long logicalAddress = hashBucket.getLogicalAddress(bph, i) ;
    		        long record_pageNo = logicalAddress >> 16 ;
    		        int record_offset = (int)((logicalAddress) & ((-1L<<16)^-1L)) ;
    		        BufferedPageHeader dataBph = bm.getPage(record_pageNo) ;
    		        if (equal(tableMeta,dataBph.pageContext,dataBph.pageStartPosition+record_offset,dataBph.pageSize-record_offset,keyValue))
    		        {
    		            Object[] vals = new Object[pre_index.length] ;
    		            int field_offset = 0 ;
    		            for (int index = 0 ; index < vals.length ; index++)
    		            {
    		                field_offset = tableMeta.getFieldOffset(dataBph.pageContext, dataBph.pageStartPosition+record_offset, pre_index[index]) ;
    		                (tableMeta.getField_formats()[pre_index[index]]).readValue(dataBph.pageContext,dataBph.pageStartPosition+record_offset+field_offset, vals, index) ;
    		            }

    		            rs.insertRecord(vals) ;
    		        }
    		        else
	                    conflictTimes ++ ;
    		        
    		        dataBph.unlock() ;
    		        
    		    }
		        
		    }

		    long nextPageNo = bph.getNextPageNo() ;
		    bph.unlock() ;
		    
		    if (nextPageNo <= 0)
		        break ;
		    bph = bm.getPage(nextPageNo) ;
		}
	    findRealKeyTime += System.currentTimeMillis() - start ;
	    
	    return rs;
	}

	@Override
	public void insertRecord(RecordMeta rm, Object[] value, long offset) throws Exception 
	{
	    long start = System.currentTimeMillis() ;
        int hashKey = hash(rm,value) ;
        int bucketNo = getBucketNoByHashKey(hashKey) ;
        
        /*  Insert the hash key and logical address */
        insertIndex(bucketNo,hashKey,offset) ;
        
        /*  If the fill-ratio touch the threshold line  */
        if (fillinRatio())
            addNewBucket() ;
        insertTotalTime += System.currentTimeMillis() - start ;
	}
	
	public void print()
	{
	    System.out.println("Bucket pointer page no:" + bucket_pointer_page_no);
	    System.out.println("Hash key length:" + hash_key_length);
	    System.out.println("Number of buckets:" + num_buckets);
	    System.out.println("Number of total buckets:" + num_buckets_total);
	    System.out.println("Number of bytes:" + num_bytes);
	    System.out.println("Number of record:" + num_record);
	}
	
	public void scan() throws Exception
	{
	    long[] page_nos = new long[num_buckets] ;
	    for (int i = 0 ; i < this.num_buckets ; i++)
	    {
	        page_nos[i] = getPageNoByBucketNo(i) ;
	    }
	    Arrays.sort(page_nos) ;
	    for (int i = 0 ; i < num_buckets-1 ; i ++)
	    {
	        if (page_nos[i] == page_nos[i+1])
	            System.out.println("Duplicate page pointer") ;
	    }
	    System.out.println("Min page no:" + page_nos[0] + ", max page no: " + page_nos[num_buckets-1]);
	    System.out.println("Number buckets: " + num_buckets) ;
	    
	}
	public void printBucketPageNo() throws Exception
	{
	    BufferedPageHeader bph = bm.getPage(bucket_pointer_page_no) ;
        System.out.println(num_buckets) ;
        for (int i = 0 ; i < bph.getItemCount()&&i<num_buckets ; i ++)
            System.out.println(i+":"+ByteUtil.readLong(bph.pageContext, bph.pageStartPosition+BufferedPageHeader.pageHeaderSize+i*Constants.size_page_no)) ;
        bph.unlock() ;
	}
	public boolean check() throws Exception
	{
        BufferedPageHeader bph = bm.getPage(bucket_pointer_page_no) ;
        long[] addres = new long[num_buckets] ;
        for (int i = 0 ; i < bph.getItemCount() && i < num_buckets ; i ++)
        {
            long page_no = ByteUtil.readLong(bph.pageContext, bph.pageStartPosition+BufferedPageHeader.pageHeaderSize+i*Constants.size_page_no) ;
            if (page_no < 0 || page_no > 1000000)
            {
                bph.unlock() ;
                return false ;
            }
            addres[i] = page_no ;
        }
        bph.unlock() ;
        Arrays.sort(addres) ;
        if (addres[0] < 0 || addres[num_buckets-1] > 100000)
            return false;
        for (int i = 0 ; i < num_buckets-1 ; i ++)
        {
            if (addres[i] == addres[i+1])
            {
                System.out.println("Duplicate page : " + addres[i]) ;
                return false ;
            }
        }
        return true ;
	}
}
