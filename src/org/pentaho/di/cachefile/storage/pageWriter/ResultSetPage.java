package org.pentaho.di.cachefile.storage.pageWriter;

import org.pentaho.di.cachefile.meta.Meta;
import org.pentaho.di.cachefile.meta.RecordMeta;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;

public class ResultSetPage implements PageWriterReader
{

    static ResultSetPage rspw = new ResultSetPage() ;
    
    public static ResultSetPage getInstance()
    {
        return rspw ;
    }
    
    @Override
    public int writeData(BufferedPageHeader bph, Meta valueMeta, Object value)
            throws Exception
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isFull(BufferedPageHeader bph, Meta valueMeta, Object value)
            throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    public int writeData(RecordMeta rm , byte[] rowContent, int rowStart, int length, RecordMeta resultMeta, 
                        byte[] rsContent, int rsStart) throws Exception 
    {
        return 0 ;
    }
    
}
