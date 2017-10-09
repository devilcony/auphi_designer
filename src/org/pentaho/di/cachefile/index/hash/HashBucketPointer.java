package org.pentaho.di.cachefile.index.hash;


import org.pentaho.di.cachefile.Constants;
import org.pentaho.di.cachefile.meta.Meta;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;
import org.pentaho.di.cachefile.storage.pageWriter.PageWriterReader;
import org.pentaho.di.cachefile.util.ByteUtil;


/**
 *  Structure of hash bucket pointer page.
 * 
 *  <br>
 *  Page layout sketch:
 *  <br>
 *  <br>--------------------------------------------------------------
 *  <br>|Page header|Page No|Page No|Page No|Page No|.................|             
 *  <br>|.............................................................|
 *  <br>|.............................................................|
 *  <br>|.............................................................|
 *  <br>--------------------------------------------------------------
 * 
 * 
 * */
public class HashBucketPointer implements PageWriterReader
{
    final int itemSize = Constants.size_page_no ;

    static HashBucketPointer hbppw = new HashBucketPointer() ;
    
    public static HashBucketPointer getInstance()
    {
        return hbppw ;
    }

    @Override
    public int writeData(BufferedPageHeader bph, Meta valueMeta, Object value)
            throws Exception
    {
        int index = (Integer) ((Object[])value)[0] ;
        long pageNo = (Long)(((Object[])value)[1]) ;
        ByteUtil.writeLong(bph.pageContext, 
            bph.pageStartPosition+BufferedPageHeader.pageHeaderSize+index*Constants.size_page_no, 
            pageNo) ;
        bph.updatePhysicalPageHeader(1, itemSize) ;
        bph.setDirty() ;
        return 0;
    }

    @Override
    public boolean isFull(BufferedPageHeader bph, Meta valueMeta, Object value)
            throws Exception
    {
        return bph.getByteLeft() <= itemSize;
    }


}
