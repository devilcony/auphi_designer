package org.pentaho.di.cachefile.meta;

import java.nio.ByteBuffer;

import org.pentaho.di.cachefile.type.FieldType;

public class IndexMeta extends Meta
{
    /*  Maximum size of field(column)   */
    public final static int MAX_NUM_FIELDS = 512 ;
    /*  Maximum size of field(column) name  */
    public final static int MAX_COLUMN_NAME = 64 ;
    /*  Maximum size of record  */
    public final static int MAX_RECORD_SIZE = 8*1024 ;
    
    public int numKeyFields  ;   
    
    /**
     * The array of indexed columns' format & index & order
     * 
     * */
    public FieldType[] field_formats ;
    public int[] field_index ;
    public boolean[] field_asc ;
    
    
    public boolean allowDuplicates ;
    
    public IndexMeta()
    {
    	
    }
    
    public IndexMeta(FieldType[] field_formats, int[] field_index, boolean[] field_asc, boolean allowDuplicates)
    {
        assert(field_formats != null && field_index != null && field_asc != null) ;
        assert(field_formats.length == field_index.length && field_index.length == field_asc.length) ;
        this.numKeyFields = field_formats.length ;
    	this.field_asc = field_asc ;
    	this.field_formats = field_formats ;
    	this.field_index = field_index ;
    	this.allowDuplicates = allowDuplicates ;
    }
    
    public static void main(String[] args)
    {

    }


    /**
     *  Index meta information to byte array.
     * 
     * */
    public int write(byte[] context, int start, int length)
    {
        ByteBuffer bb = ByteBuffer.wrap(context,start,length) ;
        
        bb.putInt(numKeyFields) ;
        
        for (int i = 0 ; i < numKeyFields ; i ++)
            bb.putInt(field_formats[i].format_id) ;
        
        for (int i = 0 ; i < numKeyFields ; i ++)
            bb.putInt(field_index[i]) ;
        
        for (int i = 0 ; i < numKeyFields ; i ++)
            bb.put((byte)(field_asc[i]?1:0)) ;
        
        bb.put((byte)(allowDuplicates?1:0));
        
        return bb.position()-start ;
    }


    /**
     *  Byte array to index meta information.
     * 
     * 
     * */
    public int read(byte[] context, int start, int length) throws Exception
    {
        ByteBuffer bb = ByteBuffer.wrap(context,start,length) ;
        
        numKeyFields = bb.getInt() ;
        assert(numKeyFields > 0) ;
        
        field_formats = new FieldType[numKeyFields] ;
        for (int i = 0 ; i < numKeyFields ; i ++)
            field_formats[i] = FieldType.values()[bb.getInt()] ;
        
        field_index = new int[numKeyFields] ;
        for (int i = 0 ; i < numKeyFields ; i ++)
            field_index[i] = bb.getInt() ;
        
        field_asc = new boolean[numKeyFields] ;
        for (int i = 0 ; i < numKeyFields ; i ++)
            field_asc[i]= (bb.get()==1?true:false) ;
        
        allowDuplicates = (bb.get()==1?true:false) ;
        
        return bb.position() - start;
    }
    
    public int getNumKeyFields()
    {
        return numKeyFields;
    }


    public void setNumKeyFields(int numKeyFields)
    {
        this.numKeyFields = numKeyFields;
    }


    public FieldType[] getField_formats()
    {
        return field_formats;
    }


    public void setField_formats(FieldType[] field_formats)
    {
        this.field_formats = field_formats;
    }


    public int[] getField_index()
    {
        return field_index;
    }


    public void setField_index(int[] field_index)
    {
        this.field_index = field_index;
    }


    public boolean[] getField_asc()
    {
        return field_asc;
    }


    public void setField_asc(boolean[] field_asc)
    {
        this.field_asc = field_asc;
    }


    public boolean isAllowDuplicates()
    {
        return allowDuplicates;
    }


    public void setAllowDuplicates(boolean allowDuplicates)
    {
        this.allowDuplicates = allowDuplicates;
    }    

    
    /**
     *  Compute the actual storage size of the value.
     *  
     * @throws Exception 
     * 
     * */
    public int getStoreSize(Object[] value) throws Exception
    {
        int size = 0 ;
        
        for (int i = 0 ; i < numKeyFields ; i++)
        {
            size += field_formats[i].getLength(value[i]) ;
        }
        return size ;
    }
    
    public void printKey(Object[] value)
    {
        for (int i = 0 ; i < this.numKeyFields ; i ++)
        {
            if (i>0)
                System.out.print(" , ");
            
            if (this.field_formats[i] == FieldType.StringType)
                System.out.print(new String((byte[])value[i])) ;
            else
                System.out.print(value[i]);

        }
    }
    
    public int compare(Object[] A, Object[] B) throws Exception
    {
        int com ;
        for (int i = 0 ; i < numKeyFields ; i++)
        {
            com = field_formats[i].compare(A[i], B[i]) ;
            if (com != 0)
                return com ;
        }
        return 0;
    }
}
