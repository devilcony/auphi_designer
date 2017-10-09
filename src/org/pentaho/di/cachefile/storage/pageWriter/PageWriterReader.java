package org.pentaho.di.cachefile.storage.pageWriter;

import org.pentaho.di.cachefile.meta.Meta;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;


/**
 *  Page writer and reader interface.
 * 
 * */
public interface PageWriterReader
{
    /**
     *  Write data value to a page, given the meta and the value.
     *  Return the offset from where the written data value starts.
     * 
     * */
    public int writeData(BufferedPageHeader bph,Meta valueMeta, Object value) throws Exception ;
    
    
    /**
     *  Whether the page is too full to add a new item.
     * 
     * */
    public boolean isFull(BufferedPageHeader bph,Meta valueMeta, Object value) throws Exception;
}
