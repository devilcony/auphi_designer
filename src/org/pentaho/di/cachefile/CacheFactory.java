package org.pentaho.di.cachefile;

import java.util.HashMap;
import java.util.Map;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;

public class CacheFactory {  
	
	/**
	 * all new and existing cache files and cached in the map object.
	 */
	static Map<String,ICache> caches = new HashMap<String,ICache>(); 
	
	/** 
	 * create a CacheFile object , by create a new file.
	 * @param rowMeta
	 * @param fileName
	 * @return
	 * @throws KettleException
	 *
	 */
	public static ICache create(RowMetaInterface rowMeta, String fileName,int memorySize) throws KettleException
	{
		if (caches.get(fileName) == null)
		{
			CacheFile cacheFile = new CacheFile();
			//edit by cli   2012/08/10
			//add a new parameter "memorySize" in this function
			//when create a new cache file,we need to set the memory size ,
			//otherwise it will always be the default value 192M
			cacheFile.setMemorySize(memorySize);
			
			cacheFile.setIndexType(CacheFile.USE_HASH_INDEX);
			cacheFile.setFilePath(fileName);
			cacheFile.create(rowMeta);
			caches.put(fileName, cacheFile);			
			return cacheFile;
		}
			
		else
			 return caches.get(fileName);
	}

	/**
	 * create a CacheFile object , by load an existing file.
	 * @param rowMeta 
	 * @param fileName
	 * @return
	 * @throws KettleException
	 */
	public static ICache load(String fileName) throws KettleException
	{
		ICache cacheFile = caches.get(fileName) ;
		if (cacheFile == null)
		{
			cacheFile = new CacheFile();
			cacheFile.load(fileName);
			caches.put(fileName, cacheFile);			
			return cacheFile;
		}	
		else
		{

			if (((CacheFile)cacheFile).getFilePath() == null)
				cacheFile.load(fileName) ;
			return cacheFile;
		}
	}
}
