package org.pentaho.di.cachefile.test;

import java.math.BigDecimal;
import java.util.Date;

import org.pentaho.di.cachefile.Constants;
import org.pentaho.di.cachefile.ResultSet;
import org.pentaho.di.cachefile.Table;
import org.pentaho.di.cachefile.index.Index;
import org.pentaho.di.cachefile.index.btree.BPTreeIndex;
import org.pentaho.di.cachefile.index.hash.BasicHashBucketNode;
import org.pentaho.di.cachefile.index.hash.HashBucketNode;
import org.pentaho.di.cachefile.index.hash.HashBucketOptimized;
import org.pentaho.di.cachefile.index.hash.LinearHashIndex;
import org.pentaho.di.cachefile.meta.IndexMeta;
import org.pentaho.di.cachefile.meta.RecordMeta;
import org.pentaho.di.cachefile.storage.StorageManager;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;
import org.pentaho.di.cachefile.storage.buffer.SimpleBufferManager;
import org.pentaho.di.cachefile.storage.pageWriter.DataPage;
import org.pentaho.di.cachefile.type.FieldType;
import org.pentaho.di.cachefile.util.ByteUtil;


/**
 *  Test class.
 * 
 * 
 * */
public class Main
{
    static DataPage dpw = DataPage.getInstance() ;
    static HashBucketNode hbo = LinearHashIndex.hashBucket ;
    static int pageSize = 16*1024 ;
    static int bufferPageCount = 10000 ;
    static String storageFile = "lookup.tbs" ;
    public static long rand = 1 ;
    public static long recordCount = 10000000;
    
    public class Record
    {
        RecordMeta tm ;
        Object[] values ;
        public Record(Object[] values, RecordMeta tm)
        {
            this.values = values ;
            this.tm = tm ;
        }
        
        /**
         *  Generate random row.
         * 
         * */
        public void random()
        {
            if (values == null)
                values = new Object[tm.num_filed] ;
          
            values[0] = rand++ ;
            values[1] = String.valueOf(rand-1).getBytes() ;
            values[2] = (rand-1)%2==0?true:false ;
            values[3] = new Date(System.currentTimeMillis()) ;
            values[4] = (rand-1)*1.0 ;
            values[5] = new BigDecimal((Double)values[4]) ;
        }
    }

    /**
     *  Create table and insert data.
     * 
     * */
    public static void createTable() throws Exception
    {
        /* Initialize storage manager */
        StorageManager sm = StorageManager.createStorage(storageFile, pageSize) ;
        
        /* Initialize buffer manager for storage manager  */
        SimpleBufferManager sbm = new SimpleBufferManager() ;
        sbm.init(sm, pageSize*bufferPageCount, null) ;
        
        /*  Row meta    */
        int num_field = 6 ;
        FieldType[] field_formats = 
        {
            FieldType.LongType,
            FieldType.StringType,
            FieldType.BooleanType,
            FieldType.DateType,
            FieldType.DoubleType,
            FieldType.BigDecimalType
            } ;
        int[] field_sizes = {-1,30,-1,-1,-1,-1} ;
        String[] field_names = {"ID","NAME","SEX","AGE","SCORE","SALES"} ;
        RecordMeta rm = new RecordMeta( num_field, field_formats,  field_sizes, field_names) ;
        
        /*  Create table    */
        Table table = Table.createTable(sbm, rm) ;
        sm.setTableInfoPageNo(table.getPage_no()) ;
        
        Main main = new Main() ;
        Record data = main.new Record(null,rm) ;
        
//        /*  Create index    */
//        int [] field_index = {0} ;
//        FieldType[] index_field_formats = new FieldType[field_index.length] ;
//        boolean[] asc = new boolean[field_index.length] ;
//        for (int i = 0 ; i< field_index.length ; i ++)
//        {
//            index_field_formats[i] = table.rm.field_formats[field_index[i]] ;
//            asc[i] = true ;
//        }
//        IndexMeta im = new IndexMeta(index_field_formats,field_index,asc,true) ;
//        Index lhi = table.createIndex(im,0) ;
        
        long start = System.currentTimeMillis() ;
        for (int i = 0 ; i < recordCount ; i ++)
        {
            
            /*  Generate random data    */
            data.random() ;

            /*  Insert record   */
            long address = table.insert(data.values) ;
            

//            /*  Insert to index */
//            lhi.insertRecord(rm, data.values, address) ;
            
            if (i % 100000 == 99999)
                System.out.println("Insert "+ i );
        }
        
        System.out.println(table) ;
        
        table.close() ;
        sbm.close() ;
        sm.close() ;  
        
        System.out.println("Create table time cost: " + (System.currentTimeMillis() - start)/1000) ;
//        ((LinearHashIndex)lhi).print() ;
//        System.out.println("Total insert time cost:" + ((LinearHashIndex)lhi).insertTotalTime);
//        System.out.println("Insert time cost:" + ((LinearHashIndex)lhi).insertTime);
//        System.out.println("Write data time cost:" + ((LinearHashIndex)lhi).writeDataTime);
//        System.out.println("Split time cost:" + ((LinearHashIndex)lhi).splitTime);
        
    }
    
    public static void main(String [] args) throws Exception
    {
        System.out.println(hbo) ;
        System.out.println("Page size:" + pageSize+", page count:"+bufferPageCount) ;
//        createTable() ;
//        bptree_index_test() ;
        hash_index_test() ;
    }
    public static void bptree_index_test() throws Exception
    {
        createTable() ;
        create_bptree_index() ;
    }
    public static void hash_index_test() throws Exception
    {
        createTable() ;
        create_hash_index() ;
//        scan() ;
//        hash_index_scan() ;
        hash_index_search() ;
        System.exit(0);
    }
    public static ResultSet hash_index_search() throws Exception
    {
        StorageManager sm = new StorageManager(storageFile) ;
        
        // Init buffer manager for storage manager
        SimpleBufferManager sbm = new SimpleBufferManager() ;
        sbm.init(sm, pageSize*bufferPageCount, null) ;
        
        Table table = Table.loadTable(sbm, sm.getTableMetaPageNo()) ;
        System.out.println(table) ;
        

        LinearHashIndex lhi = (LinearHashIndex)table.indexes.get(0) ;
        
        long start = System.currentTimeMillis() ;
        long extraTime = 0 ;
        int[] pre_index = new int[]{5,4,3,2,1,0} ;
        for (int i = 1 ; i < table.record_count ; i ++)
        {
            long start_extra = System.currentTimeMillis() ;
            long keyVal = i ;
            Object[] keyValues = new Object[]{keyVal} ;
            extraTime += System.currentTimeMillis() - start_extra ;
            ResultSet rs = lhi.searchRecord(table.rm, keyValues, pre_index) ;
            start_extra = System.currentTimeMillis() ;
//            if ( i % 10000 == 9999)
//                rs.print() ;
            if(rs.record_count != 1)
            {
                System.out.println("Search failed : " + i) ;
                rs.close() ;
                break ;
            }
            rs.close() ;
            extraTime += System.currentTimeMillis() - start_extra ;
        }
        System.out.println("Hash index search time cost: " + (System.currentTimeMillis() - start)) ;  
        System.out.println("Hash index search extra time cost:" + extraTime) ;
        table.close() ;
        sbm.close() ;
        sm.close() ;     
        System.out.println("Find real key time cost:" + lhi.findRealKeyTime) ;
        System.out.println("Pre-search time cost:" + lhi.preSearchTime) ;
        System.out.println("Conflict times while hash search: " + lhi.conflictTimes) ;
        System.out.println("Total hash index search time cost: " + (System.currentTimeMillis() - start)) ;        
        System.out.println("Result set close time cose:"+ ResultSet.closeTime);
        return null ;
    }

    /**
     *  Create b-plus-tree index.
     * 
     * */
    public static void create_bptree_index() throws Exception
    {
        
        StorageManager sm = new StorageManager(storageFile) ;
        
        /* Initialize buffer manager for storage manager  */
        SimpleBufferManager sbm = new SimpleBufferManager() ;
        sbm.init(sm, pageSize*bufferPageCount, null) ;
        
        Table table = Table.loadTable(sbm, sm.getTableMetaPageNo()) ;
        
        /*  Create index    */
        int [] field_index = {0,1} ;
        FieldType[] index_field_formats = new FieldType[field_index.length] ;
        boolean[] asc = new boolean[field_index.length] ;
        for (int i = 0 ; i< field_index.length ; i ++)
        {
            index_field_formats[i] = table.rm.field_formats[field_index[i]] ;
            asc[i] = true ;
        }
        IndexMeta im = new IndexMeta(index_field_formats,field_index,asc,true) ;

        long start = System.currentTimeMillis() ;
        Index lhi = table.createIndex(im,1) ;
        
//        ((BPTreeIndex)lhi).printData() ;
        System.out.println("Status OK:"+((BPTreeIndex)lhi).isStatusOK()) ;
        System.out.println(lhi.toString()) ;
        System.out.println("Time cost: " + (System.currentTimeMillis() - start)/1000) ;
        
        table.close() ;
        sbm.close() ;
        sm.close() ;       
    }
    
    public static void create_hash_index() throws Exception
    {
        
        StorageManager sm = new StorageManager(storageFile) ;
        
        /* Initialize buffer manager for storage manager  */
        SimpleBufferManager sbm = new SimpleBufferManager() ;
        sbm.init(sm, pageSize*bufferPageCount, null) ;
        
        Table table = Table.loadTable(sbm, sm.getTableMetaPageNo()) ;
        
        /*  Create index    */
        int [] field_index = {0} ;
        FieldType[] index_field_formats = new FieldType[field_index.length] ;
        boolean[] asc = new boolean[field_index.length] ;
        for (int i = 0 ; i< field_index.length ; i ++)
        {
            index_field_formats[i] = table.rm.field_formats[field_index[i]] ;
            asc[i] = true ;
        }
        IndexMeta im = new IndexMeta(index_field_formats,field_index,asc,true) ;

        long start = System.currentTimeMillis() ;
        Index lhi = table.createIndex(im,0) ;
        ((LinearHashIndex)lhi).print() ;
        System.out.println("Total insert time cost:" + ((LinearHashIndex)lhi).insertTotalTime);
        System.out.println("Insert time cost:" + ((LinearHashIndex)lhi).insertTime);
        System.out.println("Write data time cost:" + ((LinearHashIndex)lhi).writeDataTime);
        System.out.println("Split time cost:" + ((LinearHashIndex)lhi).splitTime);
        
        table.close() ;
        sbm.close() ;
        sm.close() ;
        System.out.println("Create hash index time cost: " + (System.currentTimeMillis() - start)) ;
    }
    
    public static void hash_index_scan() throws Exception 
    {
        StorageManager sm = new StorageManager(storageFile) ;
        
        // Init buffer manager for storage manager
        SimpleBufferManager sbm = new SimpleBufferManager() ;
        sbm.init(sm, pageSize*bufferPageCount, null) ;
        
        Table table = Table.loadTable(sbm, sm.getTableMetaPageNo()) ;
        int count = 0 ;
        
        Object[] values = new Object[table.rm.num_filed] ;
 
        System.out.println(table) ;
        
        LinearHashIndex lhi = (LinearHashIndex)table.indexes.get(0) ;
        
        long start = System.currentTimeMillis() ;
        long logicalAddress = -1L ;
        int hashKey = 0 ;
        int max_list_length = 0 ;
        int list_length = 0 ;
        BufferedPageHeader bph = null ;
        for (int i = 0 ; i < lhi.num_buckets; i++)
        {
            list_length = 0 ;
            int indexItemCount = 0 ;
            /* Get page for each bucket */
            bph = sbm.getPage(lhi.getPageNoByBucketNo(i)) ;
            while(true)
            {
                list_length ++ ;
                if (list_length > max_list_length)
                    max_list_length = list_length ;
                int recordCount = bph.getItemCount() ;
                indexItemCount += recordCount ;
                for (int j = 0 ; j < recordCount ; j ++)
                { 
                    logicalAddress = hbo.getLogicalAddress(bph, j) ;
                    hashKey = hbo.getHashKey(bph, j) ;
                    int offset = (int)(logicalAddress & ((-1<<16)^-1)) ;
                    long page_no = logicalAddress>>16 ;
                    BufferedPageHeader dataBph = sbm.getPage(page_no) ;
                    dpw.readData(dataBph,offset,table.rm,values) ;
                    dataBph.unlock() ;
                    
                }
                long nextBucketPage = bph.getNextPageNo() ;
                bph.unlock() ;
                if (nextBucketPage <= 0)
                    break ;
                else
                    bph = sbm.getPage(nextBucketPage) ;
                
            }
            count += indexItemCount ;
            
        }
        assert(count == lhi.num_record) ;
        
        System.out.println("Record count:"+ count);
        table.close() ;
        sbm.close() ;
        sm.close() ;       
        System.out.println("Max list length: " + max_list_length) ;
        System.out.println("Hash index scan time cost: " + (System.currentTimeMillis() - start)/1000) ;        
        return ;
    }
    

    
    /**
     *  Scan the table.
     *  
     * 
     * */
    public static void scan() throws Exception
    {

        StorageManager sm = new StorageManager(storageFile) ;
        
        /*  Initialize buffer manager for storage manager  */
        SimpleBufferManager sbm = new SimpleBufferManager() ;
        sbm.init(sm, pageSize*bufferPageCount, null) ;
        
        
        Table table = Table.loadTable(sbm, sm.getTableMetaPageNo()) ;
        
        int count = 0 ;
        BufferedPageHeader bph = sbm.getPage(table.first_data_page) ;
//        bph.lock() ;
        RecordMeta rm = table.rm ;
        Object[] values = new Object[rm.num_filed] ;
        while(true)
        {
            int recordCount = bph.getItemCount() ;
            int offset = 0 ;

            for (int i = 0 ; i < recordCount ; i++)
            {
                offset = dpw.getRecordOffset(bph, i) ;
                dpw.readData(bph, offset, rm, values) ;
                count++ ;
            }
            
            long nextPage = bph.getNextPageNo() ;
            bph.unlock() ;
            
            if (nextPage<= 0)
                break ;
            else
            {
                bph = sbm.getPage(nextPage) ;
            }
            
        }
        
        System.out.println("Record count:"+ count);
        table.close() ;
        sbm.close() ;
        System.out.println(sm);
        sm.close() ;
    }
}
