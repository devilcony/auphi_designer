package org.pentaho.di.cachefile.index.btree;


import org.pentaho.di.cachefile.Constants;
import org.pentaho.di.cachefile.meta.IndexMeta;
import org.pentaho.di.cachefile.meta.Meta;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;
import org.pentaho.di.cachefile.storage.pageWriter.PageWriterReader;
import org.pentaho.di.cachefile.util.ByteUtil;


/**
 *  Page writer & reader for b-plus-tree node.
 *  The node contains &lt;key,pointer&gt; pairs.
 *  
 *  <br>
 *  Page  layout  sketch:
 *  <br>
 *  <br> -------------------------------------------------------------
 *  <br>|Physical page header|Node flag|Parent node|Pointer|Key offset|
 *  <br>|Pointer|Key offset|..........................................|
 *  <br>|.............................................................|
 *  <br>|.............................................|Key|Key|Key|Key|
 *  <br> -----------------------------------------------------------
 *  
 *  <br>There are K keys and K+1 pointers: pointer0,key1,pointer1,...,keyK,pointerK.
 *  The (i-1)'th pointer points to the sub-tree whose maximum value is less
 *  than the i'th key and the i'th pointers points to the sub-tree whose minimum key is no less than
 *  than the i'th key.
 *  <br>The 0'th pointer is maintained by the next page pointer in physical page header.
 *   
 *   
 * */
public final class BPTreeNode implements PageWriterReader
{
    /*  Extra size for key value, including key offset and pointer */
    private static int indexItemSize = BufferedPageHeader.offsetSize + Constants.size_logical_addr ;
    
    public static int offset_node_flag = BufferedPageHeader.pageHeaderSize ;
    public static int size_node_flag = Byte.SIZE/8 ;
    public static int offset_parent_node = offset_node_flag + size_node_flag ;
    public static int size_parent_node = Constants.size_page_no ;
    public static int pageHeaderSize = BufferedPageHeader.pageHeaderSize + size_node_flag + size_parent_node ;
    
    public static byte init_flag = 0x0 ;
    public static byte leaf_flag = 0x1 ;
    public static byte root_flag = 0x2 ;
    
    public static BPTreeNode bpTreeNpw = new BPTreeNode() ;
    
    private BPTreeNode()
    {
        
    }
    public static BPTreeNode getInstance()
    {
        return bpTreeNpw ;
    }
    

    @Override
    public int writeData(BufferedPageHeader bph, Meta valueMeta, Object value)
            throws Exception
    {
        throw new Exception("Unimplemented!") ;
    }

    /**
     *  Get offset of the i'th key value.
     *  
     *  @param i start from 0
     * 
     * */
    private int getOffset(BufferedPageHeader bph, int i)
    {
        return ByteUtil.readInt(bph.pageContext, bph.pageStartPosition+pageHeaderSize+size_node_flag+i*indexItemSize + Constants.size_logical_addr) ;
    }
    
    /**
     *  Get the i'th pointer.
     *  
     *  @param i start from 0, if index == -1, return the K+1 pointer
     *  
     * */
    public long getPointer(BufferedPageHeader bph, int i)
    {
        if (i == -1)
            return bph.getNextPageNo() ;
        
        return ByteUtil.readLong(bph.pageContext, bph.pageStartPosition+pageHeaderSize+i*indexItemSize) ;
    }
    
    /**
     *  Compute the offset of the new key.
     *  
     *  @param keySize size of the new key
     * 
     * */
    private int computeKeyOffset(BufferedPageHeader bph, int keySize)
    {
        int recordCount = bph.getItemCount() ;
        int byteUsed = bph.getByteUsed() ;
        
        return bph.pageSize - (byteUsed-recordCount*indexItemSize) - keySize ;
    }
    
    /**
     *  Find the first key equal to the given key.
     *  Return the key's index; return -1 if no such key. 
     *  
     *  @param node the b-plus-tree node
     *  @param meta meta information
     *  @param keyValue key value
     *  
     *  @return the index of found key, start from 0; -1 if no found.
     * 
     * */
    public int binarySearchEqual(BufferedPageHeader node, IndexMeta meta, Object[] keyValue) throws Exception
    {
        int recordCount = node.getItemCount() ;
        
        /*  Binary search for the position to insert */
        int low = 0 ; 
        int high = recordCount-1 ;
        int mid = 0;
        int com = 0 ;
        Object[] mid_key = null ;
        while(low <= high)
        {
            mid = (low+high)/2 ; 
            mid_key = getKey(node,mid,meta) ;
            com = meta.compare(mid_key, keyValue) ;
            if ( com == 0)
            {
                /*  Move forward to find the first key that equals  */
                int i = 1 ;
                while(i<=mid)
                {
//                    offset = getOffset(node,mid-i) ;
//                    com = compare(node.pageContext,offset,meta,keyValue) ;
                    mid_key = getKey(node,mid-i,meta) ;
                    com = meta.compare(mid_key, keyValue) ;
                    if (com != 0)
                        break ;
                    i++ ;
                }
                return mid-i+1 ;
            }
            
            if ( com == -1)
                low = mid + 1;
            else if (com == 1)
                high = mid -1 ;
            else if (com == -2)
                throw new Exception("null value") ;
        }
        
        return -1 ;
        
    }

    public Object[] getKey(BufferedPageHeader node, int index, IndexMeta meta) throws Exception
    {
        Object[] key = new Object[meta.numKeyFields] ;
        int offset = getOffset(node,index) ;
        for (int i = 0 ; i < meta.numKeyFields ; i++)
        {
            offset += meta.field_formats[i].readValue(node.pageContext, node.pageStartPosition+offset, key, i) ;
        }
        return key ;
    }
    /**
     *  Find the first key no less than the given key.
     *   
     *  @param node b-plus-tree node
     *  @param meta meta information
     *  @param keyValue key value
     *  @return index of the found key, start from 0; return -1 if the given key is greater than the maximum key in the node. 
     * 
     * */
    public int binarySearchNoLess(BufferedPageHeader node, IndexMeta meta, Object[] keyValue) throws Exception
    {
        int recordCount = node.getItemCount() ;
        
        /*  Binary search for the position to insert */
        int low = 0 ; 
        int high = recordCount-1 ;
        int mid = 0;
//        int mid_offset = -1 ;
        int com = 0 ;
        while(low <= high)
        {
            mid = (low+high)/2 ;
//            mid_offset = getOffset(node,mid) ;
            Object[] mid_key = getKey(node,mid, meta) ;
//            com = compare(node.pageContext,mid_offset,meta,keyValue) ;
            com = meta.compare(mid_key, keyValue) ;
            if ( com == 0)
                break ;
            
            if ( com < 0)
                low = mid + 1;
            else
                high = mid -1 ;
        }
        /*  If the last key compared is less than, move one position forward    */
        if (com < 0)
            mid++ ;
        
        /*  If exceed the key count, then all keys is less than the given key  */
        if (mid >= recordCount)
            return -1 ;
        else
            return mid ;
    }
    
    /**
     *  Write the index item.
     *  
     *  @param bph
     *  @param index
     *  @param keyOffset
     *  @param logicalAddr
     * 
     * */
    private void writeIndexItem(BufferedPageHeader bph, int index, int keyOffset, long logicalAddr)
    {
        /*  Write index item: pointer    */ 
        ByteUtil.writeLong(bph.pageContext, bph.pageStartPosition+pageHeaderSize + index*indexItemSize, logicalAddr) ;
        /*  Write index item: key offset    */
        ByteUtil.writeInt(bph.pageContext, bph.pageStartPosition+pageHeaderSize + index*indexItemSize + Constants.size_logical_addr, keyOffset) ;
    }
    
    /**
     *  Write the key value and the pointer to the page. Return the key value offset starting from
     *  the page start position. 
     * 
     * */
    public int writeKey(BufferedPageHeader bph, IndexMeta keyMeta, Object[] keyValue, long pointer)
    throws Exception
    {
        int recordCount = bph.getItemCount() ;

        int actualSize = keyMeta.getStoreSize(keyValue);
        int dataOffset = computeKeyOffset(bph,actualSize) ;
        
//        if ((Long)keyValue[0] == 68)
//            System.out.println();
        /*  Binary search for the position to insert */
        int insertPosition = binarySearchNoLess(bph,keyMeta,keyValue) ;
        /*  If no key greater than the given key, insert it to the end  */
        if (insertPosition == -1)
            insertPosition = recordCount ;
        
        /*  Move to vacate space for new index item */
        for (int i = (recordCount)*indexItemSize - 1 ; i >= insertPosition*indexItemSize; i --)
            bph.pageContext[bph.pageStartPosition+pageHeaderSize+i+indexItemSize] = bph.pageContext[bph.pageStartPosition+pageHeaderSize+i] ;
        
        /*  Write index item    */
        writeIndexItem(bph,insertPosition,dataOffset,pointer) ;
        /*  Write key value   */
        int offset = dataOffset ;
        for (int i = 0 ; i < keyMeta.numKeyFields ; i ++)
            offset += keyMeta.field_formats[i].writeValue(bph.pageContext, bph.pageStartPosition+offset, keyValue[i]) ;
        
        /*  Update physical page header */
        bph.updatePhysicalPageHeader(1, actualSize+indexItemSize) ;
        bph.setDirty() ;
        
        return dataOffset;
    }

//    /**
//     *  Return 0 if equal; -1 if less than; 1 if greater than; -2 if unknown(null value involved).
//     * 
//     * */
//    private int compare(byte[] context, int start, IndexMeta meta, Object[] value) throws Exception
//    {
//        int com = 0 ;
//        int offset = start ;
//        for (int i = 0 ; i < meta.numKeyFields ; i ++)
//        {
//            com = meta.field_formats[i].compare(context, offset, value[i]) ;
//            if (com != 0)
//                break ;
//            offset += meta.field_formats[i].getLength(context, offset) ;
//        }
//
//        return com ;
//    }

    
    
    
    
    @Override
    public boolean isFull(BufferedPageHeader bph, Meta valueMeta, Object value)
            throws Exception
    {
        int storeSize = ((IndexMeta)valueMeta).getStoreSize((Object[])value) ;
        
        if ( storeSize > Constants.MAX_KEY_SIZE ||
                storeSize + BufferedPageHeader.offsetSize + Constants.size_logical_addr > bph.pageSize ) 
            throw new Exception("Too large key!") ;
        
        return storeSize + indexItemSize > bph.getByteLeft() ;
    }

    public void readData(BufferedPageHeader bph, int index, IndexMeta meta, Object[] value) throws Exception
    {
        int offset = getOffset(bph,index) ;

        for (int i = 0 ; i < meta.numKeyFields ; i ++)
            offset += meta.field_formats[i].readValue(bph.pageContext, bph.pageStartPosition+offset, value, i) ;

    }
    
    /**
     *  Insert &lt;key,pointer&gt; to the specified position. If the position equals to -1, insert to the end.
     * 
     * */
    public int writeData(BufferedPageHeader bph, int insertPosition, IndexMeta keyMeta, Object[] keyValue, long pointer) throws Exception
    {
        int recordCount = bph.getItemCount() ;

        int actualSize = keyMeta.getStoreSize(keyValue);
        int dataOffset = computeKeyOffset(bph,actualSize) ;
        
        if (insertPosition == -1)
            insertPosition = recordCount ;
        
        /*  Move to vacate space for new index item */
        for (int i = (recordCount)*indexItemSize - 1 ; i >= insertPosition*indexItemSize; i --)
            bph.pageContext[bph.pageStartPosition+pageHeaderSize+i+indexItemSize] = bph.pageContext[bph.pageStartPosition+pageHeaderSize+i] ;
        
        /*  Write index item    */
        writeIndexItem(bph,insertPosition,dataOffset,pointer) ;
        /*  Write key value   */
        int offset = dataOffset ;
        for (int i = 0 ; i < keyMeta.numKeyFields ; i ++)
            offset += keyMeta.field_formats[i].writeValue(bph.pageContext, bph.pageStartPosition+offset, keyValue[i]) ;
        
        /*  Update physical page header */
        bph.updatePhysicalPageHeader(1, actualSize+indexItemSize) ;
        bph.setDirty() ;
        
        if (!isNodeStatusOK(bph,keyMeta))
            printNode(bph,keyMeta) ;
        
        return dataOffset;
        
    }
    
    /**
     * Split item from node A to node B.
     * 
     * @param nodeA
     * @param nodeB
     * @param start
     * 
     * */
    public int split(BufferedPageHeader nodeA, BufferedPageHeader nodeB, int start)
    {
        int offset = -1 ;getOffset(nodeA,start) ;     
        int length = -1 ;
        
        int itemCount = nodeA.getItemCount() ;
        /* copy pointers and offsets    */
        for (int i = start ; i < itemCount ; i ++)
            
        
        /* copy keys */

        /*  The K+1 pointer */
        nodeB.setNextPageNo(nodeA.getNextPageNo()) ;
        
        /*  update physical page header  */
        
        nodeA.setDirty() ;
        nodeB.setDirty() ;
        return 1;
    }
    
    public void printNode(BufferedPageHeader bph, IndexMeta meta) throws Exception
    {
        Object[] value = new Object[meta.numKeyFields] ;
        long pointer = -1L ;
        int itemCount = bph.getItemCount() ;
        System.out.println("Page no:" + bph.getPageNo()+",Item count: " + itemCount ) ;
        for (int i = 0 ; i < itemCount ; i++)
        {
            readData(bph,i,meta,value) ;
            pointer = getPointer(bph,i) ;
            meta.printKey(value) ;
            System.out.println(":"+pointer) ;
        }
        System.out.println(bph.getNextPageNo()) ;
    }
    
    /**
     * Page content copy.
     * 
     * */
    public void copy(BufferedPageHeader src, BufferedPageHeader dest)
    {
        int srcOffset,destOffset ;
        int length ;
        
        /*  copy page header*/
        srcOffset = src.pageStartPosition ;
        destOffset = dest.pageStartPosition ;
        length = pageHeaderSize ;
        System.arraycopy(src.pageContext, srcOffset, dest.pageContext, destOffset,length) ;
        
        /*  copy extra information such as pointers,offsets */
        srcOffset = src.pageStartPosition+pageHeaderSize ;
        destOffset = dest.pageStartPosition + pageHeaderSize ;
        length = src.getItemCount()*indexItemSize ;
        System.arraycopy(src.pageContext, srcOffset, dest.pageContext, destOffset,length) ;
        
        /*  copy key items  */
        length = src.getByteUsed()-length ;
        srcOffset = src.pageStartPosition + dest.pageSize - length ;
        destOffset = dest.pageStartPosition + dest.pageSize - length ;
        System.arraycopy(src.pageContext, srcOffset, dest.pageContext, destOffset,length) ;
        
        dest.setDirty() ;
    }
    public boolean isNodeStatusOK(BufferedPageHeader node, IndexMeta meta) throws Exception
    {
        int itemCount = node.getItemCount() ;
        Object[] A = null ;
        Object[] B = null ;
        long valueA = 0 ;
        long valueB = 0 ;
        for (int i = 0 ; i < itemCount-1 ; i ++)
        {
            A = getKey(node,i,meta) ;
            B = getKey(node,i+1,meta) ;
            valueA = getPointer(node,i) ;
            valueB = getPointer(node,i+1) ;
            if (valueA == valueB)
                return false ;
            if (meta.compare(A, B) == 0 || meta.compare(A, B) == 1)
                return false ;
        }
        return true ;
    }
    public long getParent(BufferedPageHeader bph)
    {
        return 0 ;
    }
    public void setParent(BufferedPageHeader bph)
    {
        return ;
    }
    public boolean isLeafNode(BufferedPageHeader bph)
    {
        return true ;
    }
    public boolean isRootNode(BufferedPageHeader bph)
    {
        return true ;
    }
    private void setNodeFlag(BufferedPageHeader bph, byte flag)
    {
        ByteUtil.writeByte(bph.pageContext, bph.pageStartPosition+offset_node_flag, flag) ;
    }
    private byte getNodeFlag(BufferedPageHeader bph)
    {
        return ByteUtil.readByte(bph.pageContext, bph.pageStartPosition+offset_node_flag) ;
    }
    public void setRootNodeFlag(BufferedPageHeader bph)
    {
        byte oriFlag = getNodeFlag(bph) ;
        setNodeFlag(bph,(byte)(oriFlag|root_flag)) ;
    }
    public void setLeafNodeFlag(BufferedPageHeader bph)
    {
        byte oriFlag = getNodeFlag(bph) ;
        setNodeFlag(bph,(byte)(oriFlag|leaf_flag)) ;   
    }
    public void resetNodeFlag(BufferedPageHeader bph)
    {
        setNodeFlag(bph,init_flag) ;
    }
    public void initBptreeNode(BufferedPageHeader bph)
    {
        bph.resetPage() ;
        resetNodeFlag(bph) ;
        bph.updatePhysicalPageHeader(0, size_node_flag) ;
        bph.setDirty() ;
    }
    
}
