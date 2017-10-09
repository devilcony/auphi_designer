package org.pentaho.di.cachefile;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;

public interface ICache {

	/**
	 * create a cache file
	 * @param meta: cache fields
	 * @param fileName: storage file name
	 * @return
	 */
	abstract public ICache create(RowMetaInterface meta) throws KettleException;

	/**
	 * load an existing cache file
	 * @param meta: cache fields
	 * @param fileName: storage file name
	 * @return
	 */
	abstract public ICache load(String fileName) throws KettleException;
	
	/**
	 * create an index on the cache file
	 * @param meta: key fields meta
	 * @param indexType 0 hash, 1 btree
	 * @return
	 */
	abstract void createIndex(RowMetaInterface meta,int indexType) throws KettleException;
	
	/**
	 * insert a row to the cache file
	 * @param row
	 * @return true,false
	 * @throws Exception
	 */
	abstract boolean insert(Object[] row) throws KettleException;
	
	/**
	 * 
	 * @param key: search keys
	 * @param returnFields: meta of return field
	 * @return null if not found
	 * @throws Exception
	 */
	abstract ResultSet search(Object[] key,RowMetaInterface returnFields) throws KettleException;
	
	/**
	 * get all key fields meta
	 * @return
	 */
	abstract RowMetaInterface getKeyFields();
	
	/**
	 * get info field meta
	 * @return
	 */
	abstract RowMetaInterface getInfoFields();
	
	/**
	 * set memory size for search, by bytes
	 * @param size
	 */
	abstract void setMemSize(long size);
	
	/**
	 * set a page size
	 * @param size
	 */
	abstract void setPageSize(long size);
	
	/**
	 * close the cache file
	 * @param row
	 * @throws Exception
	 */
	abstract void close() throws Exception;
	
	/**
	 *  Flush information to the cache file(but keep the file open, which is different from close()).
	 * 
	 * */
	abstract void flush() throws Exception ;
	
	/**
	 * Whether the cache file is closed.
	 * 
	 * */
	abstract boolean hasClosed() ;
}
