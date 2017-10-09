package org.pentaho.di.cachefile.type;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Date;

import org.pentaho.di.cachefile.util.ByteUtil;


/**
 *  Field type in cache file. The type defined<BR>
 *  how the field value of this type is serialized to byte array,<BR> 
 *  and how the byte array is de-serialized to field value.
 * 
 * 
 * */
public enum FieldType implements FieldTypeInterface 
{   
    
    /**
     *  For Long type, the Long object is a Long object in java.
     * 
     * */
    LongType(0,Long.SIZE/8)
    {
        @Override
        public int compare(byte[] value, int start, Object obj)
        {
            byte status = value[start+0] ;
            if (obj == null || status == STATUS_NULL)
                return -2 ;
            
            long diff = ByteUtil.readLong(value, start+status_size) - (Long)obj ;
            if (diff > 0)
                return 1 ;
            else if (diff < 0)
                return -1 ;
            else
                return 0 ;
        }

        @Override
        public  int readValue(byte[] value, int start, Object[] objs, int index)
        {
            /*  Check whether the value is null */
            byte status = value[start+0] ;
            if (status == STATUS_NULL )
            {
                objs[index] = null ;
                return status_size ;
            }
            
            objs[index] = ByteUtil.readLong(value, start+status_size) ;
            return format_size + status_size;
        }

        @Override
        public int writeValue(byte[] value, int start, Object obj)
        {
            /*  Write status */
            if (obj == null)
            {
                value[start+0] = STATUS_NULL ;
                return status_size ;
            }
            value[start+0] = STATUS_INITIAL ;
            
            /*  Write value */
            ByteUtil.writeLong(value, start+status_size, (Long)obj) ;
            return format_size + status_size;
        }

//        @Override
//        public boolean equal(byte[] value1, int start1, byte[] value2, int start2)
//        {
//            byte status1 = value1[start1] ;
//            byte status2 = value2[start2] ;
//            
//            /*  NULL value compare  */
//            if (status1 == STATUS_NULL)
//                return status2 == STATUS_NULL ;
//            if (status2 == STATUS_NULL)
//                return false ;
//                   
//            /*  Content compare byte by byte    */
//            for (int i = 0 ; i < format_size ; i ++)
//            {
//                if (value1[start1+status_size+i] != value2[start2+status_size+i])
//                    return false ;
//            }
//            return true;
//        }

        @Override
        public int hashKey(Object obj)
        {
            if (obj == null)
                return NULL_HASH_KEY ;
            Long val = (Long)obj ;
            
            /*  Refer to Long.hashCode() */
            return (int)(val^(val>>>32)) ;
        }

        @Override
        public int hashKey(byte[] content, int start, int length)
        {
            byte status = content[start] ;
            if (status == STATUS_NULL)
                return NULL_HASH_KEY ;
            long val = ByteUtil.readLong(content,start+status_size) ;
            return (int)(val^(val>>>32)) ;
        }

        @Override
        public int compare(Object A, Object B) throws Exception
        {
            if (A == null || B == null)
                return -2 ;
            long diff = (Long)A - (Long)B ;
            if (diff < 0)
                return -1 ;
            else if (diff > 0)
                return 1 ;
            
            return 0;
        }

    },
    
    /**
     *  For String type, the string object is a UTF-8 encoded byte arrays.
     *  <BR>Keep that in mind: it's not the String object in java.
     * 
     *  
     * */    
    StringType(LongType.format_id+1,variable_length)
    {
        /**
         *  
         *  @param value byte array context
         *  @start start position of value in byte array
         *  @param obj UTF-8 encoded byte array
         *  
         * */
        @Override
        public int compare(byte[] value, int start, final Object obj) throws UnsupportedEncodingException
        {            
            /*  Read status */
            byte status = value[start+0] ;
            if (obj == null || status == STATUS_NULL)
                return -2 ;
            
            /*  Read length */
            int length = ByteUtil.readInt(value, start+status_size) ;
            
            /*  Compare byte value  */
            byte[] byteValue = (byte[])obj ;
            int i = 0 ;
            int diff = 0 ;
            while( i < length && i < byteValue.length)
            {
                diff = value[start+status_size+length_size+i] - byteValue[i] ;
                if (diff == 0)
                    i ++ ;
                else
                    return diff > 0 ? 1:-1 ;
            }
            if (length - byteValue.length == 0)
                return 0 ;
            return length - byteValue.length > 0 ? -1:1 ;
        }

        @Override
        public int readValue(byte[] value, int start, Object[] objs, int index) throws Exception
        {
            /*  Read status */
            byte status = value[start] ;
            if (status == STATUS_NULL)
            {
                objs[index] = null ;
                return status_size ;
            }
            /*  Read length */
            int length = ByteUtil.readInt(value, start+status_size) ;
            
            /*  Read value  */
            objs[index]  = new byte[length] ;
            System.arraycopy(value, start+status_size+ length_size, objs[index], 0, length) ;

            
            return status_size + length_size + length;
        }

        @Override
        public int writeValue(byte[] value, int start, Object obj) throws UnsupportedEncodingException
        {
            /*  Write status */
            if (obj == null)
            {
                value[start+0] = STATUS_NULL ;
                return status_size ;
            }
            value[start] = STATUS_INITIAL ;
            
            /*  Write length    */
            byte[] bytes = (byte[])obj ;
            int length = bytes.length ;
            ByteUtil.writeInt(value, start+status_size, length) ;
            
            /*  Write value    */
            System.arraycopy(bytes, 0, value, start+status_size+length_size,length) ;
            
            return status_size + length_size + length;
        }
        
        @Override
        public int getLength(Object obj)
        {
            if (obj == null)
                return status_size ;
            return status_size + length_size + ((byte[])obj).length ;
        }

        @Override
        public int hashKey(Object obj)
        {
            if (obj == null)
                return NULL_HASH_KEY ;
            return ByteUtil.hashKey((byte[])obj, 0, ((byte[])obj).length) ;
        }

        @Override
        public int hashKey(byte[] content, int start, int length)
        {
            byte status = content[start] ;
            if (status == STATUS_NULL)
                return NULL_HASH_KEY ;
            int size = ByteUtil.readInt(content, start+status_size) ;
            return ByteUtil.hashKey(content,start+status_size+length_size,size);
        }

        @Override
        public int equal(byte[] value1, int start1, byte[] value2,
                int start2)
        {
            byte status1 = value1[start1] ;
            byte status2 = value2[start2] ;
            
            /*  NULL value compare  */
            if (status1 == STATUS_NULL || status2 == STATUS_NULL)
                return -2 ;
            
            /*  Length compare  */
            int length1 = ByteUtil.readInt(value1, start1+status_size) ;
            int length2 = ByteUtil.readInt(value2, start2+status_size) ;
            if (length1 != length2)
                return 0 ;
            
            /*  Content compare byte by byte    */
            for (int i = 0 ; i < length1 ; i ++)
            {
                if (value1[start1+status_size+length_size+i] != value2[start2+status_size+length_size+i])
                    return 0 ;
            }
            return 1;
        }

        @Override
        public int compare(Object A, Object B) throws Exception
        {
            if (A == null || B == null)
                return -2 ;
            byte[] byteA = (byte[])A ;
            byte[] byteB = (byte[])B ;
            
            /*  Compare byte value  */
            int i = 0 ;
            int diff = 0 ;
            while( i < byteA.length && i < byteB.length)
            {
                diff = byteA[i] - byteB[i] ;
                if (diff == 0)
                    i ++ ;
                else
                    return diff > 0 ? 1:-1 ;
            }
            if (byteA.length - byteB.length == 0)
                return 0 ;
            return byteB.length - byteB.length > 0 ? -1:1 ;
        }

    },
    DoubleType(StringType.format_id + 1,Double.SIZE/8 )
    {

        @Override
        public int readValue(byte[] value, int start, Object[] objs, int index)
                throws Exception
        {
            byte status = value[start] ;
            if (status == STATUS_NULL)
            {
                objs[index] = null ;
                return status_size ;
            }
            objs[index] = ByteUtil.readDouble(value, start+status_size) ;;
            return status_size + format_size;
        }

        @Override
        public int writeValue(byte[] value, int start, Object obj)
                throws Exception
        {
            if (obj == null)
            {
                value[start] = STATUS_NULL ;
                return status_size ;
            }
            value[start] = STATUS_INITIAL ;
            ByteUtil.writeDouble(value, start+status_size, (Double)obj) ;
            
            return status_size + format_size;
        }

        @Override
        public int hashKey(Object obj)
        {
            if (obj == null)
                return NULL_HASH_KEY ;
            return ((Double)obj).hashCode();
        }

        @Override
        public int hashKey(byte[] content, int start, int length)
        {
            byte status = content[start] ;
            if (status == STATUS_NULL)
                return NULL_HASH_KEY ;

            long val = ByteUtil.readLong(content, start+status_size) ;
            /*  hashCode() of Double object */
            return (int)(val^(val>>>32));
        }

        @Override
        public int compare(byte[] value, int start, Object obj)
                throws Exception
        {
            byte status = value[start+0] ;
            if (obj == null || status == STATUS_NULL)
                return -2 ;
            
            double diff = ByteUtil.readDouble(value, start+status_size) - (Double)obj ;
            if (diff > 0.0)
                return 1 ;
            else if (diff < 0.0)
                return -1 ;
            else
                return 0 ;
        }

        @Override
        public int compare(Object A, Object B) throws Exception
        {
            if (A == null || B == null)
                return -2 ;
            double diff = (Double)A - (Double)B ;
            if (diff > 0)
                return 1 ;
            else if (diff < 0) 
                return -1 ;
            return 0;
        }
         
    },
    /**
     *  Big decimal value is stored as string byte.
     * 
     * */
    BigDecimalType(DoubleType.format_id+1,variable_length)
    {

        @Override
        public int readValue(byte[] value, int start, Object[] objs, int index)
                throws Exception
        {
            if (value[start] == STATUS_NULL)
            {
                objs[index] = null ;
                return status_size ;
            }
            int len = ByteUtil.readInt(value, start+status_size) ;
            objs[index] = new BigDecimal(new String(value,start+status_size+length_size,len)) ;
            return status_size+length_size+len;
        }

        @Override
        public int writeValue(byte[] value, int start, Object obj)
                throws Exception
        {
            if (obj == null)
            {
                value[start] = STATUS_NULL ;
                return status_size ;
            }
            value[start] = STATUS_INITIAL ;
            byte[] byteVal = ((BigDecimal)obj).toString().getBytes() ;
            int len = byteVal.length ;
            ByteUtil.writeInt(value, start+status_size, len) ;
            System.arraycopy(byteVal, 0, value, start+status_size+length_size, len) ;
            return status_size + length_size + len;
        }

        @Override
        public int hashKey(Object obj)
        {
            if (obj == null)
                return NULL_HASH_KEY ;
            return ((BigDecimal)obj).hashCode();
        }

        @Override
        public int hashKey(byte[] content, int start, int length)
        {
            if (content[start] == STATUS_NULL)
                return NULL_HASH_KEY ;
            
            int len = ByteUtil.readInt(content, start+status_size) ;
            return new BigDecimal(new String(content,start+status_size+length_size,len)).hashCode() ;
            
        }

        @Override
        public int compare(byte[] value, int start, Object obj)
                throws Exception
        {
            byte status = value[start] ;
            if (obj == null || status == STATUS_NULL)
                return -2 ;
            
            int len = ByteUtil.readInt(value, start+status_size);
            return new BigDecimal(new String(value,start+status_size+length_size,len)).compareTo((BigDecimal)obj) ;
        }
        
        @Override
        public int equal(byte[] value1, int start1, byte[] value2, int start2)
        {
            byte status1 = value1[start1] ;
            byte status2 = value2[start2] ;
            
            /*  NULL value compare  */
            if (status1 == STATUS_NULL || status2 == STATUS_NULL)
                return -2 ;
            
            int length1 = value1[start1+status_size] ;
            int length2 = value2[start2+status_size] ;
            
            BigDecimal val1 = new BigDecimal(new String(value1,start1+status_size+length_size,length1)) ;
            BigDecimal val2 = new BigDecimal(new String(value2,start2+status_size+length_size,length2)) ;
            return val1.equals(val2)?1:0 ;
        }
        
        
        @Override
        public int getLength(Object obj)
        {
            if (obj == null)
                return status_size ;
            byte[] byteVal = ((BigDecimal)obj).toString().getBytes() ;
            return status_size + length_size + byteVal.length ;
        }

        @Override
        public int compare(Object A, Object B) throws Exception
        {
            if (A == null || B == null)
                return -2 ;
            int com =  ((BigDecimal)A).compareTo((BigDecimal)B) ;
            if (com < 0)
                return -1 ;
            else if (com > 0)
                return 1 ;
            return 0 ;
        }
        
    }, 
    BooleanType(BigDecimalType.format_id+1,Byte.SIZE/8)
    {

        @Override
        public int readValue(byte[] value, int start, Object[] objs, int index)
                throws Exception
        {
            byte status = value[start] ;
            if (status == STATUS_NULL)
            {
                objs[index] = null ;
                return status_size ;
            }
            objs[index] = value[start+status_size] > 0?true:false ;
            return status_size + format_size;
        }

        @Override
        public int writeValue(byte[] value, int start, Object obj)
                throws Exception
        {
            if (obj == null)
            {
                value[start] = STATUS_NULL ;
                return status_size ;
            }
            else
                value[start] = STATUS_INITIAL ;
            
            value[start+status_size] = ((Boolean)obj)?(byte)1:0 ;
            
            return status_size + format_size ;
        }

        @Override
        public int hashKey(Object obj)
        {
            if (obj == null)
                return NULL_HASH_KEY ;
            return (Boolean)obj?1:0;
        }

        @Override
        public int hashKey(byte[] content, int start, int length)
        {
            if (content[start] == STATUS_NULL)
                return NULL_HASH_KEY ;
            
            return content[start+status_size];
        }

        @Override
        public int compare(byte[] value, int start, Object obj)
                throws Exception
        {
            byte status = value[start+0] ;
            if (obj == null || status == STATUS_NULL)
                return -2 ;
            
            return (int) (value[start+status_size] - ((Boolean)obj?1:0)) ;
        }

        @Override
        public int compare(Object A, Object B) throws Exception
        {
            if (A == null || B == null)
                return -2 ;
            int com = ((Boolean)A).compareTo((Boolean)B) ;
            if (com == 0)
                return 0 ;
            return com > 0?1:-1;
        }
        
    }, 
    /**
     *  Date type is stored as long type. The long value of date type is equal to 
     *  'java.util.Date.getTime()'.
     * 
     * */
    DateType(BooleanType.format_id+1,Long.SIZE/8)
    {
        @Override
        public int compare(byte[] value, int start, Object obj)
        {
            byte status = value[start+0] ;
            if (obj == null || status == STATUS_NULL)
                return -2 ;
            
            long diff = ByteUtil.readLong(value, start+status_size) - ((Date)obj).getTime() ;

            if (diff > 0)
                return 1 ;
            else if (diff < 0)
                return -1 ;
            else
                return 0 ;
        }

        @Override
        public int readValue(byte[] value, int start, Object[] objs, int index)
        {
            /*  Check whether the value is null */
            byte status = value[start+0] ;
            if (status == STATUS_NULL )
            {
                objs[index] = null ;
                return status_size ;
            }
            
            objs[index] = new Date(ByteUtil.readLong(value, start+status_size)) ;
            return format_size + status_size;
        }

        @Override
        public int writeValue(byte[] value, int start, Object obj)
        {
            /*  Write status */
            if (obj == null)
            {
                value[start+0] = STATUS_NULL ;
                return status_size ;
            }
            value[start+0] = STATUS_INITIAL ;
            
            /*  Write value */
            ByteUtil.writeLong(value, start+status_size, ((Date)obj).getTime()) ;
            return format_size + status_size;
        }


        @Override
        public int hashKey(Object obj)
        {
            if (obj == null)
                return NULL_HASH_KEY ;
            long val = ((Date)obj).getTime() ;
            return (int)(val^(val>>>32)) ;
        }

        @Override
        public int hashKey(byte[] content, int start, int length)
        {
            byte status = content[start] ;
            if (status == STATUS_NULL)
                return NULL_HASH_KEY ;
            long val = ByteUtil.readLong(content,start+status_size) ;
            return (int)(val^(val>>>32)) ;
        }

        @Override
        public int compare(Object A, Object B) throws Exception
        {
            if (A == null || B == null)
                return -2 ;
            int com = ((Date)A).compareTo((Date)B) ;
            if (com == 0)
                return 0 ;
            return com > 0?1:-1;
        }
    }
    
    /** TODO:  Types to be continued 
     *  
     *  
     * 
     * */
    ;



    /*  Unique format id for each field type   */
    public int format_id ;
    /*  If variable length type, format_size = -1   */
    public int format_size ;
    
    
    private FieldType(int format_id,int format_size)
    {
        this.format_id = format_id ;
        this.format_size = format_size ;
    }
    
    
    @Override
    public boolean isFixedLength()
    {
        return this.format_size != variable_length;
    }

    
    @Override
    public int equal(byte[] value1, int start1, byte[] value2, int start2)
    {
        byte status1 = value1[start1] ;
        byte status2 = value2[start2] ;
        
        /*  NULL value compare  */
        if (status1 == STATUS_NULL || status2 == STATUS_NULL)
            return -2 ;
        
        /*  Compare length */
        int length = 0;
        int offset = 0 ;
        if (format_size == -1)
        {
            int length1 = ByteUtil.readInt(value1, start1+status_size) ;
            int length2 = ByteUtil.readInt(value2, start2+status_size) ;
            if (length1 != length2)
                return 0 ;
            length = length1 ;
            offset = status_size+length_size ;
        }
        else
        {
            length = format_size ;
            offset = status_size ;
        }
        
        /*  Content compare byte by byte    */
        for (int i = 0 ; i < length ; i ++)
        {
            if (value1[start1+offset+i] != value2[start2+offset+i])
                return 0 ;
        }
        return 1;
    }    
    
//    @Override
//    public int getLength(Object obj) throws Exception
//    {
//        if (obj == null)
//            return status_size ;
//        if (isFixedLength())
//            return status_size + this.format_size ;
//        return format_size ;
//    }
    /**
     *  Get the field storage length by bytes.
     * 
     * */
    @Override
    public int getLength(byte[] content, int start)
    {
        byte status = content[start] ;
        if (status == STATUS_NULL)
            return status_size ;
        if (isFixedLength())
            return status_size + format_size ;
        int len = ByteUtil.readInt(content, start+status_size) ;
        return status_size + length_size + len ;
    }

    @Override
    public int getLength(Object obj) throws Exception
    {
        if (obj == null)
            return status_size ;
        if (isFixedLength())
            return status_size + format_size ;
        
        throw new Exception("Unimplemented getLength(obj)!") ;
    }    
    
    public static FieldType getFormat(int format_id)
    {
        return FieldType.values()[format_id] ;
    }
    
    public static void main(String[] args)
    {

    }

    @Override
    public boolean isNull(byte[] value, int start)
    {
        return value[start] == STATUS_NULL ;
    }





}
