package org.pentaho.di.cachefile.index.btree;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.pentaho.di.cachefile.ResultSet;
import org.pentaho.di.cachefile.index.Index;
import org.pentaho.di.cachefile.index.IndexType;
import org.pentaho.di.cachefile.meta.IndexMeta;
import org.pentaho.di.cachefile.meta.RecordMeta;
import org.pentaho.di.cachefile.storage.buffer.BufferManager;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;


/**
 *  B plus tree index
 * 
 * */
public class BPTreeIndex extends Index
{
    static BPTreeNode nodePw = BPTreeNode.getInstance() ;
    
    /*  Node count of the whole index, including  leaf nodes and non-leaf nodes */
    public int num_nodes = 0 ;
    
    /*  Record count of the whole index */
    public long num_records = 0;
    
    /*  Page No of root node  */
    public long root_node_page_no = -1L ;
    
    /*  Page No of first leaf node  */
    public long first_leaf_page_no = -1L ;
    
    /*  Tree depth  */
    public int tree_depth = 0 ;
    
    /*  The buffer manager the index bound up with */
    public final BufferManager bm  ;

    public BPTreeIndex(BufferManager bm)
    {
        indexType = IndexType.BTreeIndex ;
        this.bm = bm ;
    }
    
    public BPTreeIndex(BufferManager bm, IndexMeta indexMeta) throws Exception
    {
        indexType = IndexType.BTreeIndex ;
        this.bm = bm ;
        meta = indexMeta ;
        init() ;
    }
    
    /**
     * Initialize a empty root node.
     *  
     * @throws Exception 
     * 
     * */
    private void init() throws Exception
    {
        BufferedPageHeader bph = bm.getNewPage() ;
        this.num_nodes = 1 ;
        this.tree_depth = 1 ;
        this.num_records = 0 ;
        this.root_node_page_no = bph.getPageNo() ;
        this.first_leaf_page_no = this.root_node_page_no ;
        bph.unlock() ;
    }
    
	@Override
	public ResultSet searchRecord(RecordMeta row, Object[] keyValue, int[] pre_index) 
	{
		// TODO Auto-generated method stub
		return null;
	}

	
	/**
	 * Extract key value from the whole record.
	 * 
	 * @param rm record meta
	 * @param value record value
	 * @return key value
	 * 
	 * */
	private Object[] toKeyValue(RecordMeta rm, Object[] value)
	{
	    Object[] keyValue = new Object[meta.numKeyFields] ;
	    for (int i = 0 ; i < meta.numKeyFields ; i ++)
	        keyValue[i] = value[meta.field_index[i]] ;
	    return keyValue ;
	}
	
	@Override
	public void insertRecord(RecordMeta rm, Object[] value, long logicalAddr) throws Exception 
	{
	    Object[] keyValue = toKeyValue(rm,value) ;
	    insert(keyValue,logicalAddr) ;
	    num_records ++ ;
	}
	

	@Override
	public int read(byte[] content, int start, int length) throws Exception 
	{
        /*  Read index meta information */
        meta = new IndexMeta() ;
        int readByteCount = meta.read(content, start, length) ;
        
        ByteBuffer bb = ByteBuffer.wrap(content, start+readByteCount, length-readByteCount) ;

        /*  Read index information  */
        num_nodes = bb.getInt() ;
        num_records = bb.getLong() ;
        root_node_page_no = bb.getLong() ;
        first_leaf_page_no = bb.getLong() ;
        tree_depth = bb.getInt() ;
        
        return bb.position() - start ;
	}

	@Override
	public int write(byte[] content, int start, int length) throws Exception 
	{
        /*  Read index meta information */
        meta = new IndexMeta() ;
        int writeByteCount = meta.write(content, start, length) ;
        
        ByteBuffer bb = ByteBuffer.wrap(content, start+writeByteCount, length-writeByteCount) ;

        /*  Read index information  */
        bb.putInt(num_nodes) ;
        bb.putLong(num_records) ;
        bb.putLong(root_node_page_no) ;
        bb.putLong(first_leaf_page_no) ;
        bb.putInt(tree_depth) ;
        
        return bb.position() - start ;
	}

	/**
	 * Find the path to the destination item and return the corresponding pointer if found;<BR>
	 * return <=0 if not found. The array list keep the search path, from the root node to the leaf.<BR>
	 * If need to insert, the search path is the context path for inserting(from the leaf to the root).<BR>
	 * The pages(contain b-tree-node) in the path are <B>locked</B>, do remember to release them.
	 * 
	 * @param keyValue key value of the destination item
	 * @param searchPath a empty array list for b-tree-node
	 * @throws Exception 
	 * 
	 * */
	public long search(Object[] keyValue, ArrayList<BufferedPageHeader> searchPath) throws Exception
	{
	    int index = -1 ;
	    long pointer = -1L ;
	    
	    if ((Long)keyValue[0] == 28)
	    {
	        printData() ;
	    }
	    
	    /* Search from root node to leaf node*/
	    int current_depth = 1 ;
        BufferedPageHeader currentNode = bm.getPage(root_node_page_no) ;
	    while(current_depth <= tree_depth)
	    {
	      searchPath.add(currentNode) ;
	      
	      if ((Long)keyValue[0] == 28)
	            nodePw.printNode(currentNode, meta) ;
	      
	      if (current_depth == tree_depth)
	      {
	          index = nodePw.binarySearchEqual(currentNode, meta, keyValue) ;
	          if (index >= 0)
	              pointer = nodePw.getPointer(currentNode, index) ;
	          else
	              pointer = -1 ;
	          break ;
	      }
	      else
	      {
	          index = nodePw.binarySearchNoLess(currentNode, meta, keyValue) ;
	          pointer = nodePw.getPointer(currentNode, index) ;
	      }
	      
	      currentNode = bm.getPage(pointer) ;
	      current_depth ++ ;
	    }
	    
	    return pointer ;
	}
	
	
	/**
	 *  Insert &lt;key, pointer&gt; to index a record. 
	 * 
     *  Insert steps:<BR>
     *     
     *  1.do a search to determine what node the new record should go in<BR>
     *  
     *  2.if the node is not full, add the record.<BR>
     *  
     *  3.otherwise, split the .<BR>
     *  
     *  4.allocate new leaf and move half the node's elements to the new bucket<BR>
     *  
     *  5.insert the new leaf's smallest key and address into the parent.<BR>
     *  
     *  6.if the parent is full, split it also<BR>
     *  
     *  7.now add the middle key to the parent node<BR>
     *  
     *  8.repeat until a parent is found that need not split<BR>
     *  
     *  9.if the root splits, create a new root which has one key and two pointers.<BR> 
     *  
	 * 
	 *  @param keyValue key value of the record, <b>not</b> the whole value
	 *  @param logicalAddr pointer, usually a logical address point to the record in the storage
	 * 
	 * 
	 * */
	public void insert(Object[] keyValue,long logicalAddr) throws Exception
	{
	    BTreeEntry btr = null ;
	    BufferedPageHeader bph = null ;
	    ArrayList<BufferedPageHeader> searchPath = new ArrayList<BufferedPageHeader>(3) ;
	    long pointer = search(keyValue,searchPath) ;
	    
	    /* If found and no duplicate allowed */
	    if (pointer > 0 && !meta.allowDuplicates)
	        return ;
	    
	    if ((Long)keyValue[0] == 28)
	    {
	        for (BufferedPageHeader pathNode: searchPath)
	            nodePw.printNode(pathNode, meta) ;
	    }
	    
	    /* From leaf to root node   */
	    int depth = searchPath.size()-1 ; 
	    while(depth >= 0)
	    {
	        bph = searchPath.get(depth) ;
	        
	        /* If current node is vacant enough for the new key, insert then stop */
	        if (!nodePw.isFull(bph, meta, keyValue))
	        {
	            if ((Long)keyValue[0] == 28)
                    nodePw.printNode(bph, meta) ;
	            nodePw.writeKey(bph, meta, keyValue, logicalAddr) ;
	            if ((Long)keyValue[0] == 28)
	                nodePw.printNode(bph, meta) ;
	            break ;
	        }
	        else
	        {/* else split current node, and insert to its parent node recursively  */
	            if (depth == 0)
	                btr = root_node_split(bph,keyValue,logicalAddr) ;
	            else if (depth == (searchPath.size()-1))
	                btr = leaf_node_split(bph,keyValue,logicalAddr) ;
	            else
	                btr = internal_node_split(bph,keyValue,logicalAddr) ;

	            keyValue = btr.keyValue ;
	            logicalAddr = btr.pointer ;
	        }
	        
	        depth -- ;
	    }
	    
	    /* Release the search path */
	    for (BufferedPageHeader bphItem: searchPath)
	    {
	        bphItem.unlock() ;
	    }
	    searchPath.clear() ;
	    if (!isStatusOK())
	    {
	        printData() ;
	        System.out.println("Status illegal!") ;
	    }
	}
	
	
	/**
	 * Leaf node split. 
	 * Move second-half of the keys and pointers in the split node to the new node.
	 * Return the smallest key and page no(pointer) of the new node. 
	 * @throws Exception 
	 * 
	 * */
	private BTreeEntry leaf_node_split(BufferedPageHeader splitNode, Object[] newKey, long newPointer) throws Exception
	{
        BufferedPageHeader newNode = bm.getNewPage() ;
 
        BTreeEntry middle_entry = node_split_include_middle(splitNode,newNode,newKey,newPointer) ;
       
        newNode.unlock() ;
        
        /* Update index information */
        num_nodes++ ;
        
        return middle_entry ;
	}
	
	/**
	 * Whether the node is the first node: the initial root node and also the first leaf node.
	 * 
	 * */
	private boolean isRootLeafNode(BufferedPageHeader node)
	{
	    return first_leaf_page_no == root_node_page_no && root_node_page_no == node.getPageNo();
	}
	
	/**
	 * 
	 * Root node split.
	 * Create a new root node, and update index meta information.
	 * Return the key and pointer of the new root node.
	 * 
	 * @throws Exception 
	 * 
	 * */
	private BTreeEntry root_node_split(BufferedPageHeader splitNode, Object[] newKey, long newPointer) throws Exception
	{
        BufferedPageHeader newNode = bm.getNewPage() ;
 
        BTreeEntry middle_entry  ;
        if (isRootLeafNode(splitNode))
            middle_entry = node_split_include_middle(splitNode,newNode,newKey,newPointer) ;
        else
            middle_entry = node_split_exclude_middle(splitNode,newNode,newKey,newPointer) ;
       
        /*  
         * Create a new root node, insert the middle key and pointer.
         * The pointer is the page no of the split node;   
         * and the K+1 pointer is the page no of the new node.
         * 
         * */
        BufferedPageHeader newRootNode = bm.getNewPage() ;
        nodePw.writeData(newRootNode, -1, meta, middle_entry.keyValue, splitNode.getPageNo()) ;
        newRootNode.setNextPageNo(newNode.getPageNo()) ;
        
        /* Update index information */
        root_node_page_no = newRootNode.getPageNo() ;
        num_nodes = num_nodes+2 ;// new node and new root node
        tree_depth ++ ;
        
        System.out.println("Tree upper:"+tree_depth) ;
        System.out.println(this) ;
        nodePw.printNode(newRootNode, meta) ;
        nodePw.printNode(splitNode, meta) ;
        nodePw.printNode(newNode, meta) ;

        
        newRootNode.unlock() ;
        splitNode.setDirty() ;
        newNode.unlock() ;
        
        return middle_entry ;
	}
	
	/**
	 * Internal node split(neither root node nor leaf node).
	 * Find the middle key in the split node, including the new key while process.
	 * Move second-half of the keys, greater than the middle key, and pointers in the split node to the new node;
	 * Keep first-half of the keys, less than the middle key, and pointers in the split node.
	 * Return the middle key before split(including the new key) and the corresponding pointer. 
	 * 
	 * @throws Exception
	 * 
	 * */
	private BTreeEntry internal_node_split(BufferedPageHeader splitNode, Object[] newKey, long newPointer) throws Exception
	{
        BufferedPageHeader newNode = bm.getNewPage() ;
 
        BTreeEntry middle_entry = node_split_exclude_middle(splitNode,newNode,newKey,newPointer) ;
       
        newNode.unlock() ;
       
        /* Update index information */
        num_nodes++ ;
        
        return middle_entry ;
	}

	/**
	 * Split node, the middle key is excluded after split.
	 * 
	 * @return the middle key and the new node's page no 
	 * 
	 * 
	 * */
	private BTreeEntry node_split_exclude_middle(BufferedPageHeader splitNode, BufferedPageHeader newNode, Object[] newKey, long newPointer) throws Exception
	{
        Object[] value = new Object[meta.numKeyFields] ;
        long pointer = -1L ;
        Object[] mid_value = new Object[meta.numKeyFields] ;
        long mid_pointer = -1L ;
        BufferedPageHeader tmpNode = bm.getNewPage() ;
        
        if (!nodePw.isNodeStatusOK(splitNode, meta))
        {
            System.out.println("Before split") ;
            nodePw.printNode(splitNode,meta) ;
        }
        if (!nodePw.isNodeStatusOK(newNode, meta))
        {
            System.out.println("Before split") ;
            nodePw.printNode(newNode, meta) ;
        }

        int itemCount = splitNode.getItemCount() ;
        
        /* The expected position of middle key  */
        int mid_key_position = ((itemCount+1)+1)/2 - 1 ;
        
        /*  The insert position of the new key  */
        int new_key_position = nodePw.binarySearchNoLess(splitNode, meta, newKey) ;
        

        int i = 0 ;
        
        /* For key <= middle key, excluding the middle key itself  */
        for (i = 0 ; i < mid_key_position ; i ++)
        {
            if (i == new_key_position)
                nodePw.writeData(tmpNode, -1, meta, newKey, newPointer);
            
            nodePw.readData(splitNode, i, meta, value) ;
            pointer = nodePw.getPointer(splitNode, i) ;
            nodePw.writeData(tmpNode, -1, meta, value, pointer) ;
        }
        tmpNode.setNextPageNo(nodePw.getPointer(splitNode, i)) ;

        /* For key >=  middle key, excluding the middle key itself*/
        if (mid_key_position == new_key_position)
        {
            nodePw.readData(splitNode, mid_key_position, meta, value) ;
            pointer = newPointer ;
            nodePw.writeData(newNode,-1, meta, value,pointer) ;
        }
        for ( i = mid_key_position + 1; i < itemCount  ; i ++)
        {
            if ( i == new_key_position && mid_key_position != new_key_position)
                nodePw.writeData(newNode, -1, meta, newKey, newPointer) ;
            
            nodePw.readData(splitNode, i, meta, value) ;
            pointer = nodePw.getPointer(splitNode, i) ;
            nodePw.writeData(newNode, -1, meta, value, pointer) ;
        }
        /*  If the new key is insert at the end   */
        if (new_key_position == -1)
            nodePw.writeData(newNode, -1, meta, newKey, newPointer) ;
        newNode.setNextPageNo(splitNode.getNextPageNo()) ;
        
        /*  Page copy, update split node   */
        nodePw.copy(tmpNode, splitNode) ;
//        if (!nodePw.isNodeStatusOK(splitNode, meta))
//        {
//            if (!nodePw.isNodeStatusOK(tmpNode, meta))
//                System.out.println("Split wrong") ;
//            else
//                System.out.println("Copy wrong");
//        }
        
        /* get the middle key and the pointer to be inserted to parent node */
        if (mid_key_position == new_key_position)
            mid_value = newKey ;
        else
            nodePw.readData(splitNode, mid_key_position, meta, mid_value) ;
        mid_pointer = newNode.getPageNo() ;  
        
        bm.recyclePage(tmpNode) ;
        tmpNode.unlock() ;
        
//        if (!nodePw.isNodeStatusOK(splitNode, meta))
//        {
//            System.out.println("After split") ;
//            nodePw.printNode(splitNode,meta) ;
//        }
//        if (!nodePw.isNodeStatusOK(newNode, meta))
//        {
//            System.out.println("After split") ;
//            nodePw.printNode(newNode, meta) ;
//        }
        
        return new BTreeEntry(mid_value,mid_pointer) ;
	}

	/**
	 * Split node, the middle key is contained after split.
	 * 
	 * 
	 * @return the middle key and the page no of the new node
	 * @throws Exception 
	 * 
	 * 
	 * */
    private BTreeEntry node_split_include_middle(BufferedPageHeader splitNode, BufferedPageHeader newNode, Object[] newKey, long newPointer) throws Exception
    {
        Object[] value = new Object[meta.numKeyFields] ;
        long pointer = -1L ;
        Object[] mid_value = new Object[meta.numKeyFields] ;
        long mid_pointer = -1L ;
        BufferedPageHeader tmpNode = bm.getNewPage() ;
        
//        if (!nodePw.isNodeStatusOK(splitNode, meta))
//        {
//            System.out.println("Before split") ;
//            nodePw.printNode(splitNode,meta) ;
//        }
//        if (!nodePw.isNodeStatusOK(newNode, meta))
//        {
//            System.out.println("Before split") ;
//            nodePw.printNode(newNode, meta) ;
//        }
        
        int itemCount = splitNode.getItemCount() ;
        
        /* The expected position of middle key */
        int mid_key_position = ((itemCount+1)+1)/2 - 1 ;
        
        /* The insert position of the new key */
        int new_key_position = nodePw.binarySearchNoLess(splitNode, meta, newKey) ;
       
        
        int i = 0 ;
        /*
         * For key <= middle key, excluding the middle key itself.   
         * First half of the split node.
         * 
         * */
        for (i = 0 ; i < mid_key_position ; i ++)
        {
            if (i == new_key_position)
                nodePw.writeData(tmpNode, -1, meta, newKey, newPointer);
            
            nodePw.readData(splitNode, i, meta, value) ;
            pointer = nodePw.getPointer(splitNode, i) ;
            nodePw.writeData(tmpNode, -1, meta, value, pointer) ;
        }
        tmpNode.setNextPageNo(newNode.getPageNo()) ;

        /* 
         * For key >=  middle key, including the middle key itself.
         * Second half of the split node
         * 
         * */
        for (i = mid_key_position ; i < itemCount  ; i ++)
        {
            if ( i == new_key_position)
                nodePw.writeData(newNode, -1, meta, newKey, newPointer) ;
            
            nodePw.readData(splitNode, i, meta, value) ;
            pointer = nodePw.getPointer(splitNode, i) ;
            nodePw.writeData(newNode, -1, meta, value, pointer) ;
        }
        /*  If the new key is insert at the end   */
        if (new_key_position == -1)
            nodePw.writeData(newNode, -1, meta, newKey, newPointer) ;
        newNode.setNextPageNo(splitNode.getNextPageNo()) ;
        
        /*  Page copy, update split node   */
        nodePw.copy(tmpNode, splitNode) ;
//        if (!nodePw.isNodeStatusOK(splitNode, meta))
//        {
//            if (!nodePw.isNodeStatusOK(tmpNode, meta))
//                System.out.println("Split wrong") ;
//            else
//                System.out.println("Copy wrong");
//        }
        
        /* get the middle key and the pointer to be inserted to parent node */
        nodePw.readData(newNode, 0, meta, mid_value) ;
        mid_pointer = newNode.getPageNo() ;
        
//        if (!nodePw.isNodeStatusOK(splitNode, meta))
//        {
//            System.out.println("After split") ;
//            nodePw.printNode(splitNode,meta) ;
//        }
//        if (!nodePw.isNodeStatusOK(newNode, meta))
//        {
//            System.out.println("After split") ;
//            nodePw.printNode(newNode, meta) ;
//        }
        
        bm.recyclePage(tmpNode) ;
        tmpNode.unlock() ;
        
        return new BTreeEntry(mid_value,mid_pointer) ;
    }
	
    public String toString()
    {
        StringBuffer sb = new StringBuffer(1024) ;
        sb.append("Node count:").append(num_nodes);
        sb.append(", record count:").append(num_records) ;
        sb.append(", depth:").append(tree_depth) ;
        sb.append(", root node:").append(root_node_page_no) ;
        sb.append(", first leaf node:").append(first_leaf_page_no) ;
        
        return sb.toString() ;
    }
	
    public void printTree()
    {
        
    }
    public void printData() throws Exception
    {
        long page_no = first_leaf_page_no ;
        System.out.println("Start to print data:");
        BufferedPageHeader bph = bm.getPage(page_no) ;
        while(true)
        {
            nodePw.printNode(bph, meta) ;
            page_no = bph.getNextPageNo() ;
            bph.unlock() ;
            
            if (page_no <= 0)
                break ;
            bph = bm.getPage(page_no) ;
        }
        
    }
    public boolean isStatusOK() throws Exception
    {
        long page_no = first_leaf_page_no ;
        long next_page_no = -1L ;
        BufferedPageHeader bph = bm.getPage(page_no) ;
        Object[] keyMax, keyMin ;
        while(true)
        {
            keyMax = nodePw.getKey(bph, bph.getItemCount()-1, meta) ;
            next_page_no = bph.getNextPageNo() ;
            bph.unlock() ;
            
            if (next_page_no < 0)
                break ;
            
            bph = bm.getPage(next_page_no) ;
            keyMin = nodePw.getKey(bph, 0, meta) ;
            if (meta.compare(keyMax, keyMin) >= 0)
            {
                bph.unlock() ;
                return false ;
            }
            
        }
        return true ;
    }
    private boolean isStatusOK(long root_node_page_no, int depth) throws Exception 
    {
        BufferedPageHeader bph = bm.getPage(root_node_page_no) ;
        if (!nodePw.isNodeStatusOK(bph, meta))
        {
            bph.unlock() ;
            return false ;
        }
        if (depth == tree_depth)
        {
            bph.unlock() ;
            return true ;
        }
        int itemCount = bph.getItemCount() ;
        for (int i = 0 ; i < itemCount ; i++)
        {
            
        }
        bph.unlock() ;
        return true ;
    }
	
    /**
     * Create a new b-p-tree node.
     * 
     * */
    private BufferedPageHeader newBPTreeNode() throws Exception
    {
        BufferedPageHeader bph = bm.getNewPage() ;
        nodePw.initBptreeNode(bph) ;
        return bph ;
    }
}
