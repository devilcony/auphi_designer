package org.pentaho.di.repository.kdr.delegates;


import org.pentaho.di.cachefile.CacheFile;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleDependencyException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleObjectExistsException;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.kdr.KettleDatabaseRepository;

public class KettleDatabaseRepositoryCacheFileDelegate extends KettleDatabaseRepositoryBaseDelegate  {

	public KettleDatabaseRepositoryCacheFileDelegate(
			KettleDatabaseRepository repository) {
		super(repository);
		// TODO Auto-generated constructor stub
	}
    public RowMetaAndData getCacheFile(ObjectId id_cache_file) throws KettleException
    {
        return repository.connectionDelegate.getOneRow(quoteTable(KettleDatabaseRepository.TABLE_R_CACHE_FILE), quote(KettleDatabaseRepository.FIELD_ID_CACHE_FILE), id_cache_file);
    }
    
    
	public synchronized ObjectId getCacheFileID(String name) throws KettleException
    {
        return repository.connectionDelegate.getIDWithValue(quoteTable(KettleDatabaseRepository.TABLE_R_CACHE_FILE), quote(KettleDatabaseRepository.FIELD_ID_CACHE_FILE), quote(KettleDatabaseRepository.FIELD_CACHE_FILE_NAME), name);
    }	
	public void saveCacheFile(CacheFile cacheFile, ObjectId id_transformation, boolean isUsedByTransformation, boolean overwrite) throws KettleException
	{
	  if (cacheFile.getObjectId() == null){   //add new cache file
		  ObjectId existingCacheFileId = getCacheFileID(cacheFile.getName());
		  
		  //no duplicate cache file name
		  if(existingCacheFileId==null)
			  cacheFile.setObjectId(insertCacheFile(cacheFile));	
		  
		// duplicate repository cache file name. 
		//if overwrite is true,delete old one and add new one
		  if(overwrite&&existingCacheFileId!=null){
			  repository.deleteCacheFile(existingCacheFileId);
			  cacheFile.setObjectId(insertCacheFile(cacheFile));
		  }
       }else {//update cache file
    	  updateCacheFile(cacheFile);
		}                
	}	
	public CacheFile loadCacheFile(ObjectId id_cache_file) throws KettleException
	{
		CacheFile cacheFile = new CacheFile();
        
		cacheFile.setObjectId(id_cache_file);
        
		RowMetaAndData row = getCacheFile(id_cache_file);
        
		cacheFile.setName( row.getString("NAME", null) );
        cacheFile.setFilePath(row.getString(KettleDatabaseRepository.FIELD_CACHE_FILE_FILEPATH, null));
        cacheFile.setMemorySize(Integer.parseInt(row.getString(KettleDatabaseRepository.FIELD_CACHE_FILE_MEMORYSIZE,null)));
        cacheFile.setIndexType(Integer.parseInt(row.getString(KettleDatabaseRepository.FIELD_CACHE_FILE_INDEXTYPE,null)));        	
		
        return cacheFile;
	}


    public synchronized ObjectId insertCacheFile(CacheFile cacheFile) throws KettleException
    {
      if(getCacheFileID(cacheFile.getName()) != null) { //override original cache file
        cacheFile.setObjectId(getCacheFileID(cacheFile.getName()));
        updateCacheFile(cacheFile);   
        return cacheFile.getObjectId();
      }
      
    	ObjectId id = repository.connectionDelegate.getNextCacheFileID();

        RowMetaAndData table = new RowMetaAndData();

        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_ID_CACHE_FILE, ValueMetaInterface.TYPE_INTEGER), id);
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CACHE_FILE_NAME, ValueMetaInterface.TYPE_STRING), cacheFile.getName());
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CACHE_FILE_FILEPATH, ValueMetaInterface.TYPE_STRING), cacheFile.getFilePath());
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CACHE_FILE_MEMORYSIZE, ValueMetaInterface.TYPE_INTEGER), Long.parseLong(String.valueOf(cacheFile.getMemorySize())));
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CACHE_FILE_INDEXTYPE, ValueMetaInterface.TYPE_INTEGER), Long.parseLong(String.valueOf(cacheFile.getIndexType())));
        
        repository.connectionDelegate.getDatabase().prepareInsert(table.getRowMeta(), KettleDatabaseRepository.TABLE_R_CACHE_FILE);
        repository.connectionDelegate.getDatabase().setValuesInsert(table);
        repository.connectionDelegate.getDatabase().insertRow();
        repository.connectionDelegate.getDatabase().closeInsert();
      
        return id;
      
    }
    
    public synchronized void updateCacheFile(CacheFile cacheFile) throws KettleException
    {
        RowMetaAndData table = new RowMetaAndData();
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CACHE_FILE_NAME, ValueMetaInterface.TYPE_STRING), cacheFile.getName());
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CACHE_FILE_FILEPATH, ValueMetaInterface.TYPE_STRING), cacheFile.getFilePath());
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CACHE_FILE_MEMORYSIZE, ValueMetaInterface.TYPE_INTEGER),Long.parseLong(String.valueOf(cacheFile.getMemorySize())));
        table.addValue(new ValueMeta(KettleDatabaseRepository.FIELD_CACHE_FILE_INDEXTYPE, ValueMetaInterface.TYPE_INTEGER), Long.parseLong(String.valueOf(cacheFile.getIndexType())));
        
        repository.connectionDelegate.updateTableRow(KettleDatabaseRepository.TABLE_R_CACHE_FILE, KettleDatabaseRepository.FIELD_ID_CACHE_FILE, table, cacheFile.getObjectId());
    }

    public synchronized void delCacheFile(ObjectId id_cache_file) throws KettleException
    {
        repository.connectionDelegate.performDelete("DELETE FROM "+quoteTable(KettleDatabaseRepository.TABLE_R_CACHE_FILE)+" WHERE "+quote(KettleDatabaseRepository.FIELD_ID_CACHE_FILE)+" = ? ", id_cache_file);
    }
 }
