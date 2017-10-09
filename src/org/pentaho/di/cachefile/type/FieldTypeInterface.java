package org.pentaho.di.cachefile.type;


/**
 *  Field type interface.
 * 
 * */
public interface FieldTypeInterface
{
    public static final int variable_length = -1 ;
    public static String default_encoding = "UTF-8" ;
    
    /*  Filed status    */
    final static int status_size = 1 ;
    final static int length_size = 4 ;
    final static byte STATUS_INITIAL = 0x00 ;
    final static byte STATUS_NULL = 0x01 ;
    final static int NULL_HASH_KEY = -1 ;
    
    
    /**
     *  Whether the type is of fixed length.
     * 
     * */
    public abstract boolean isFixedLength()  ;
    
    /**
     * Get object's value from bytes.
     * Return the byte count read.
     * 
     * @param value byte context
     * @param start position where the value start
     * @param objs result object array
     * @param index index of result object, start from 0
     *  
     * */
    public abstract int readValue(byte[] value, int start, Object[] objs, int index) throws Exception ;
    
    /**
     * Write object's value to bytes.<BR>
     * For variable-length type, with format: |status|length|content|<BR>
     * For fixed-length type, with format:|status|content|<BR>
     * Return the byte count wrote.
     * 
     * */
    public abstract int writeValue(byte[] value, int start, Object obj) throws Exception ;       
    
    /**
     *  Compute the object's storage length of this type by bytes.
     *  
     * 
     * */
    public abstract int getLength(Object obj) throws Exception;
    
    /**
     *  Get storage length of this byte by bytes.
     *  For variable-length type, return 'status size + length size + actual length';
     *  For fixed-length type, return 'status size + fixed length' 
     * 
     * */
    public abstract int getLength(byte[] content, int start) ;
    
    /**
     *  Compute hash key with object value.
     * 
     * */
    public abstract int hashKey(Object obj) ;
    
    /**
     *  Compute hash key with bytes value.
     * 
     * */
    public abstract int hashKey(byte[] content, int start, int length) ;
    
    
    /**
     *  Compare the value in byte array and the object value.
     *  Return 0 if byte value equal to object value,
     *  return -1 if byte value less than object value,
     *  else 1 if byte value greater than object value.
     *  Return -2 if null value is compared .
     * 
     * */
    int compare(byte[] value,int start, Object obj) throws Exception;
    
    /**
     *  Compare the two object values.
     *  Return 0 if A equal to B,
     *  return -1 if A less than B,
     *  return 1 if A greater than B.
     *  Return -2 if null value is compared .
     * 
     * */    
    int compare(Object A, Object B) throws Exception ;
    
    /**
     *  Whether two values in the byte array are equal. 
     *  Return 1 if equal; return 0 if not; return -2 if null value involved.
     * 
     * */
    int equal(byte[] value1, int start1, byte[] value2, int start2) ;    

    /**
     *  Whether the value is null.
     * 
     * */
    boolean isNull(byte[] value, int start) ;
}
