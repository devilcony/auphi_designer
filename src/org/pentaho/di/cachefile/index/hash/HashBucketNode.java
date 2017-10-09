package org.pentaho.di.cachefile.index.hash;

import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;
import org.pentaho.di.cachefile.storage.pageWriter.PageWriterReader;


/**
 *  Hash bucket page.
 * 
 * */
public abstract class HashBucketNode implements PageWriterReader
{
    public int indexItemSize ;
    
    public abstract int getHashKey(BufferedPageHeader bph, int index) ;
    
    public abstract long getLogicalAddress(BufferedPageHeader bph, int index) ;
    
    public abstract int searchHashKey(BufferedPageHeader bph, int hashKey) ;
    
    
}
