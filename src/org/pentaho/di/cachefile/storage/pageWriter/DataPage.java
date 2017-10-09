package org.pentaho.di.cachefile.storage.pageWriter;

import org.pentaho.di.cachefile.meta.Meta;
import org.pentaho.di.cachefile.meta.RecordMeta;
import org.pentaho.di.cachefile.storage.buffer.BufferedPageHeader;
import org.pentaho.di.cachefile.util.ByteUtil;


/**
 *  Data page.
 * 
 *  
 *  <br>Page layout sketch:
 *  <br>
 *  <br> -------------------------------------------------------------
 *  <br>|Page header|Record offset|Record offset|.....................|             
 *  <br>|.............................................................|
 *  <br>|.............................................................|
 *  <br>|...............................................|Record|Record|
 *  <br> -------------------------------------------------------------
 * 
 * 
 * */
public class DataPage implements PageWriterReader
{

    static DataPage dataPage = new DataPage() ;
    
    public static DataPage getInstance()
    {
        return dataPage ;
    }
    
    
    /**
     *  Whether the page can keep the new data.
     * 
     * */
    public boolean isFull(BufferedPageHeader bph, Meta valueMeta, Object value) throws Exception
    {
        int actualSize = ((RecordMeta)valueMeta).getStoreSize((Object[])value) ;
        assert(actualSize + BufferedPageHeader.offsetSize <= (bph.pageSize-BufferedPageHeader.pageHeaderSize)) ;
        if (actualSize + BufferedPageHeader.offsetSize > (bph.pageSize - BufferedPageHeader.pageHeaderSize)) 
            throw new Exception("Too large record!") ;
        return actualSize + BufferedPageHeader.offsetSize > bph.getByteLeft() ;
    }
    
    @Override
    public int writeData(BufferedPageHeader bph, Meta valueMeta, Object value) throws Exception
    {
        RecordMeta tm = (RecordMeta)valueMeta ;
        Object[] val = (Object[]) value ;
        
        int actualSize = tm.getStoreSize(val) ;
        int dataOffset = getDataOffset(bph,actualSize) ;
        int offset = dataOffset ;
        
        /*  Write data pointer  */
        writeDataOffset(bph,dataOffset) ;
        
        /* Write field value    */
        for (int i = 0 ; i < tm.num_filed ; i ++)
        {
            dataOffset += tm.field_formats[i].writeValue(bph.pageContext, bph.pageStartPosition+dataOffset, val[i]) ;
        }
        
        /*  update page header  */
        bph.updatePhysicalPageHeader(1, actualSize+BufferedPageHeader.offsetSize) ;
        
        bph.setDirty() ;
        return offset;
    }
    
    /**
     *  Get offset, from where the data been written start.
     * 
     * 
     * */
    private int getDataOffset(BufferedPageHeader bph, int dataSize)
    {
        int offset = 0 ;
        int byteLeft = bph.getByteLeft() ;
        int recordCount = bph.getItemCount() ;
        
        offset = BufferedPageHeader.pageHeaderSize + recordCount*BufferedPageHeader.offsetSize+ byteLeft - dataSize ;
        
        return offset ;
    }
    
    /**
     *  Get offset of the i'th record, distance from the page start position.
     *  @param bph buffered page header
     *  @param i index of record
     * 
     * */
    public int getRecordOffset(BufferedPageHeader bph, int i)
    {
        return ByteUtil.readInt(bph.pageContext, bph.pageStartPosition + BufferedPageHeader.pageHeaderSize + i*BufferedPageHeader.offsetSize) ;
    }
    
    /**
     *  Get filed offset of the i'th field, distance from the record start position.
     *  @param bph buffered page header
     *  @param recordOffset record start position, distance from the page start position
     *  @param fieldIndex field index
     * 
     * */
    public int getFieldOffset(BufferedPageHeader bph, int recordOffset, RecordMeta rm ,int fieldIndex)
    {
        int fieldOffset = 0 ;
        for (int i = 0 ; i < fieldIndex ; i++)
        {
            fieldOffset += rm.field_formats[i].getLength(bph.pageContext, recordOffset+fieldOffset);
        }
        return fieldOffset ;
    }    
    
    private void writeDataOffset(BufferedPageHeader bph, int startPosition)
    {
        int recordCount = bph.getItemCount() ;
        int pointerPosition = bph.pageStartPosition + BufferedPageHeader.pageHeaderSize + recordCount*BufferedPageHeader.offsetSize ;
        ByteUtil.writeInt(bph.pageContext, pointerPosition, startPosition) ;
    }
    
    /**
     *  Read record from page's byte content.
     *  Return the offset where the reading ends, distance from the page beginning.
     * @param bph buffer page header
     * @param offset offset from where the record starts, distance from the page beginning
     * @param rm record meta
     * @param value result value objects
     *  
     *  @return offset at where the record ends, distance from the page beginning
     * 
     * */
    public int readData(BufferedPageHeader bph, int offset, RecordMeta rm, Object[] value) throws Exception
    {
        for (int i = 0 ; i < rm.num_filed ; i ++)
        {
            offset += rm.field_formats[i].readValue(bph.pageContext, bph.pageStartPosition+offset, value, i) ;
        }
        return offset ;
    }    
}
