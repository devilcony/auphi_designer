package org.pentaho.di.cachefile;

import org.pentaho.di.cachefile.index.Index;
import org.pentaho.di.cachefile.meta.IndexMeta;
import org.pentaho.di.cachefile.meta.RecordMeta;
import org.pentaho.di.cachefile.storage.StorageManager;
import org.pentaho.di.cachefile.storage.buffer.BufferManager;
import org.pentaho.di.cachefile.storage.buffer.SimpleBufferManager;
import org.pentaho.di.cachefile.util.Converter;
import org.pentaho.di.core.changed.ChangedFlag;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.ObjectRevision;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryElementInterface;
import org.pentaho.di.repository.RepositoryObjectType;

public class CacheFile extends ChangedFlag  implements RepositoryElementInterface, ICache
{   
	public static final int defualt_page_size = 16*1024 ; 
	public static final int default_memory_size = 192*1024*1024 ;
    public static final int USE_HASH_INDEX=0;
    public static final int USE_BTREE_INDEX=1;
	public static final RepositoryObjectType REPOSITORY_ELEMENT_TYPE =RepositoryObjectType.CACHE_FILE;
	private String name;
	private String filePath;
	private int memorySize = default_memory_size;
	private int pageSize = defualt_page_size;
	private int indexType;
	private boolean share; 
	private ObjectId id;
	
	private RowMetaInterface oriRm  ;
	private RowMetaInterface keyRm ;
	private StorageManager sm  ;
	private BufferManager bm ;
	private Table table ;
	private Index index ;
	private boolean isClosed = false ;
	
	public CacheFile()
	{
		id=null;
		indexType=0;
	}
	
	public String getName() 
	{
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public int getMemorySize() {
		return memorySize/1024/1024;
	}
	public void setMemorySize(int memorySize) {
		this.memorySize = memorySize*1024*1024;
	}
	public int getIndexType() {
		return indexType;
	}
	public void setIndexType(int indexType) {
		this.indexType = indexType;
	}
	public boolean isShare() {
		return share;
	}
	public void setShare(boolean share) {
		this.share = share;
	}
	public ObjectId getObjectId() {
		return id;
	}
	public void setObjectId(ObjectId id) {
		this.id = id;
	}
	public RepositoryObjectType getRepositoryElementType() 
	{
		return REPOSITORY_ELEMENT_TYPE;
	}
	public void  replaceMeta(CacheFile cacheFile)
	{
		this.filePath=cacheFile.filePath;
		this.id=cacheFile.id;
		this.indexType=cacheFile.indexType;
		this.memorySize=cacheFile.memorySize;
		this.share=cacheFile.share;
		this.name=cacheFile.name;
		this.setChanged(true);
	}
	
	@Override
	public RepositoryDirectoryInterface getRepositoryDirectory() 
	{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setRepositoryDirectory(
			RepositoryDirectoryInterface repositoryDirectory) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setDescription(String description) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public ObjectRevision getObjectRevision() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setObjectRevision(ObjectRevision objectRevision) {
		// TODO Auto-generated method stub
		
	}
	
	
	
    @Override
    public void close() throws Exception
    {
        if (table != null)
        {
            table.close() ;
            table = null ;
        }
        if (bm != null)
        {
            bm.close() ;
            bm = null ;
        }
        if (sm != null)
        {
            sm.close() ;
            sm = null ;
        }
        filePath = null ;
        name = null ;
        sm = null ;
        bm = null ;
    	table = null ;
    	index = null ;
        isClosed = true ;
    }
    public void flush() throws Exception
    {
        if (table != null)
        {
            table.flush() ;
        }
        if (bm != null)
        {
            bm.flush() ;
        }
        if (sm != null)
        {
            sm.flush() ;
        }       
    }
    @Override
    public ICache create(RowMetaInterface meta)
            throws KettleException
    {
        try
        {
            oriRm = meta.clone() ;
            RecordMeta rm = Converter.rowMetaConvert(meta);
            sm = StorageManager.createStorage(filePath, pageSize) ;
            bm = new SimpleBufferManager() ;
            bm.init(sm, memorySize, null) ;
            table = Table.createTable(bm, rm) ;
            sm.setTableInfoPageNo(table.getPage_no()) ;
            return this ;
        }
        catch (Exception e)
        {
            throw new KettleException(e) ;
        }        

    }
    @Override
    public void createIndex(RowMetaInterface meta, int indexType)
            throws KettleException
    {
        try
        {
            IndexMeta im = Converter.indexMeta(oriRm,meta) ;
            index = table.createIndex(im, indexType) ;
            keyRm = meta.clone() ;
        }
        catch (Exception e)
        {
            e.printStackTrace() ;
            throw new KettleException(e) ;
        }
        
    }
    @Override
    public RowMetaInterface getInfoFields()
    {
        return this.oriRm;
    }
    @Override
    public RowMetaInterface getKeyFields()
    {
        return this.keyRm;
    }
    @Override
    public boolean insert(Object[] row) throws KettleException
    {
        try
        {
            Object[] newRow = Converter.valueConvert(oriRm, row) ;
            table.insert(newRow) ;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new KettleException(e) ;
        }
        return false;
    }
    @Override
    public ResultSet search(Object[] key, RowMetaInterface returnFields)
            throws KettleException
    {
        int[] result_field_index = Converter.fieldIndex(oriRm,returnFields) ;
        Object[] newKeys;
        ResultSet rs = null ;
        try
        {
            newKeys = Converter.valueConvert(keyRm, key);
            rs = index.searchRecord(table.rm, newKeys, result_field_index);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new KettleException(e) ;
        }
        return rs ;
    }
    @Override
    public void setMemSize(long size)
    {
        this.memorySize = (int)size ;
    }
    @Override
    public void setPageSize(long size)
    {
        this.pageSize = (int)size ;
    }
    
    public ICache load(String filePath) throws KettleException
    {
        try
        {
            sm = new StorageManager(filePath) ;
            bm = new SimpleBufferManager() ;
            bm.init(sm, memorySize, null) ;        
            table = Table.loadTable(bm,sm.getTableMetaPageNo()) ;
            index = table.getIndex(0) ;
            
            this.filePath = filePath ;
            
            oriRm = Converter.rowMetaConvert(table.rm) ;
            keyRm = Converter.rowMetaConvert(table.rm, index.meta);
            
            return this;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new KettleException(e) ;
        }    	
    }

    public boolean hasClosed()
    {
        return isClosed;
    }

}
