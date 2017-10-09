package org.pentaho.di.cachefile.meta;

import java.nio.ByteBuffer;

import org.pentaho.di.cachefile.Constants;
import org.pentaho.di.cachefile.type.FieldType;

public class RecordMeta extends Meta
{    
    public int num_filed  = 0;
    public FieldType[] field_formats = null;
    /*  Max size for variable-length type, such as String, Number */
    public int[] field_max_sizes = null ;
    public String[] field_names = null;    

    

    public RecordMeta()
    {
        
    }
    
    public RecordMeta(int num_field, FieldType [] field_formats, int [] field_sizes, String[] field_names) throws Exception
    {
        if(num_field > Constants.MAX_NUM_FIELDS)
            throw new Exception("Too many fields!") ;
        
        int max_size = 0 ;
        for(int i = 0; i < num_field ; i ++)
        {
            if(field_names[i].length() > Constants.MAX_COLUMN_NAME)
                throw new Exception("Field name is too long!") ;
            max_size += field_sizes[i] ;
        }
        if (max_size > Constants.MAX_RECORD_SIZE)
            throw new Exception("Record is too long!") ;
        
        this.num_filed = num_field ;
        this.field_formats = field_formats ;
        this.field_names = field_names ;
        this.field_max_sizes = field_sizes ;

    }
    
    /**
     *  Reconstruct table meta from byte array.
     *  Return the byte count read.
     * 
     * */
    @Override
    public  int read(byte[] content, int start, int length)
    {
        ByteBuffer bb = ByteBuffer.wrap(content,start, length) ;
        
        /*  Read field number  */
        num_filed = bb.getInt() ;
        field_formats = new FieldType[num_filed] ;
        field_max_sizes = new int[num_filed] ;
        field_names = new String[num_filed] ;
        
        /*  Read field format type  */
        for(int i = 0; i< num_filed; i++)
        {
            field_formats[i] = FieldType.values()[bb.getInt()] ;
        }
        
        /*  Read field max size */
        for(int i = 0; i< num_filed; i++)
        {
            field_max_sizes[i] = bb.getInt() ;
        }
        
        /*  Read field name */
        for(int i = 0; i< num_filed; i++)
        {
            int name_size = bb.getInt() ;
            byte[] filed_name = new byte[name_size] ;
            bb.get(filed_name) ;
            field_names[i] = new String(filed_name) ;
        }
        
        return bb.position()-start ;
    }
    
    /**
     *  Storage table meta to byte array.
     * 
     * 
     * */
    @Override
    public int write(byte[] content, int start,int length)
    {
        ByteBuffer bb = ByteBuffer.wrap(content,start,length) ;
        
        /*  Write field number  */
        bb.putInt(num_filed) ;
        
        /*  Write field format type */
        for (int i = 0 ; i < num_filed ; i ++)
            bb.putInt(field_formats[i].format_id) ;
        
        /*  Write field max size    */
        for(int i = 0; i< num_filed; i++)
            bb.putInt(field_max_sizes[i]) ;
        
        /*  Write field name    */
        for(int i = 0; i< num_filed; i++)
        {
            bb.putInt(field_names[i].length()) ;
            bb.put(field_names[i].getBytes()) ;
        }
        
        return bb.position()-start ;
    }
    

    public int getNum_filed()
    {
        return num_filed;
    }

    public void setNum_filed(int num_filed)
    {
        this.num_filed = num_filed;
    }

    public FieldType[] getField_formats()
    {
        return field_formats;
    }

    public void setField_formats(FieldType[] field_formats)
    {
        this.field_formats = field_formats;
    }

    public String[] getField_names()
    {
        return field_names;
    }

    public void setField_names(String[] field_names)
    {
        this.field_names = field_names;
    }

    public int[] getField_sizes()
    {
        return field_max_sizes;
    }

    public void setField_sizes(int[] field_sizes)
    {
        this.field_max_sizes = field_sizes;
    }
    
    /**
     * 
     * 
     * */
    public int getFieldSize(int fieldIndex)
    {
        return this.field_max_sizes[fieldIndex] ;
    }
    
//    /**
//     *  Get the actual size of the filed value.
//     * @throws Exception 
//     * 
//     * 
//     * */
//    public int getFieldSize(int fieldIndex, Object fieldObj) throws Exception
//    {
//        if (isFixedLength(fieldIndex))
//            return field_max_sizes[fieldIndex] ;
//        
//        return field_formats[fieldIndex].getLength(fieldObj) ;
//    }
    
    /**
     *  Whether the field is of fixed length.
     * 
     * */
    public boolean isFixedLength(int fieldIndex)
    {
        return field_formats[fieldIndex].isFixedLength() ;
    }
    
    public static void main(String[] args)
    {
        byte[] test = {0,65,0,65,0,65,0,0,0} ;
        String str = new String(test) ;
        System.out.println(str+" : " + str.length()) ; 
//        Object ss =  null ;
//        System.out.println(((byte[])ss).length) ;
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
        
        for (int i = 0 ; i < num_filed ; i++)
        {
            size += field_formats[i].getLength(value[i]) ;
        }
        
        return size ;
    }    

    /**
    *   Return the offset of the i'th field value.
    *   @param content : byte context of the field value
    *   @param start : start position of the field value
    *   @param index : index number of the field value, start from 0
    *    
    *    
    * */
    public int getFieldOffset(byte[] content, int start , int index)
    {
        int offset = 0 ;
        for (int i = 0 ; i < index ; i++)
        {
            offset += field_formats[i].getLength(content, start+offset);
        }
        return offset ;
    }
    
    /**
     *  Find the index of the field specific by field name(case-sensitive).
     *  <BR>Return -1 if not found.
     * 
     * */
    public int indexOf(String fieldName)
    {
        for (int i = 0 ; i < this.num_filed ; i ++)
            if (this.field_names[i].equals(fieldName))
                return i ;
        return -1 ;
    }
    
    public void print()
    {
        System.out.println("Number of field:" + this.num_filed) ;
    }


}
