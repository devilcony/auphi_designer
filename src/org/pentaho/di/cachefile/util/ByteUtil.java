package org.pentaho.di.cachefile.util;

//import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;

import java.sql.Date;

//import sun.misc.Unsafe;

public abstract class ByteUtil
{
    
    public static long readTime = 0 ;
    public static long writeTime = 0 ;
    
//    public static sun.misc.Unsafe unsafe ; 
//    static
//    {
//        try
//        {
//            unsafe = getUnsafeInstance() ;
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace() ;
//        }
//    }
    
    
//    private static Unsafe getUnsafeInstance() throws SecurityException,
//    NoSuchFieldException, IllegalArgumentException,
//    IllegalAccessException 
//    {
//        Field theUnsafeInstance = Unsafe.class.getDeclaredField("theUnsafe");
//        theUnsafeInstance.setAccessible(true);
//        return (Unsafe) theUnsafeInstance.get(Unsafe.class);
//    }
    
    /**
     *  Copy byte array from source to destination. 
     *  
     * */
    public static void copy(byte[] srcBytes, int offset, int len,
            byte[] descBytes, int offset_desc)
    {
        System.arraycopy(srcBytes, offset, descBytes, offset_desc,len) ;
    }

    /**
     *  Bytes to integer.
     * 
     * */
    public static int readInt(byte[] content, int start)
    {
//        return unsafe.getInt(content,(long)start) ;
        return (content[start]&0xff)<<24|
                (content[start+1]&0xff)<<16|
                (content[start+2]&0xff)<<8|
                (content[start+3]&0xff) ;
    }
    
    /**
     *  Bytes to long number.
     * 
     * */
    public static long readLong(byte[] content, int start)
    {
        return  (content[start]&0xffL) << 56|
                (content[start+1]&0xffL) << 48|
                (content[start+2]&0xffL) << 40|
                (content[start+3]&0xffL) << 32| 
                (content[start+4]&0xffL) << 24|
                (content[start+5]&0xffL) << 16|
                (content[start+6]&0xffL) << 8|
                (content[start+7]&0xffL);
//        return unsafe.getLong(content,(long)start) ;
    }
    public static int writeByte(byte[] content, int start, byte value)
    {
        content[start] = value ;
        return 1 ;
    }
    public static byte readByte(byte[] content, int start)
    {
        return content[start] ;
    }
    /**
     *  Integer to bytes.
     *  
     *  Return the written bytes length.
     *  
     * */
    public static int writeInt(byte[] content, int start, int value)
    {
        content[start] = (byte)(value>>24 & 0xff);
        content[start+1] = (byte)(value>>16 & 0xff);
        content[start+2] = (byte)(value>>8 & 0xff);
        content[start+3] = (byte)(value & 0xff);
//        unsafe.putInt(content, (long)start,value) ;
        return 4 ;
    }
    
    /**
     *  Long number to bytes.
     *  
     *  Return the written bytes length.
     * 
     * */
    public static int writeLong(byte[] content, int start, long value)
    {
        content[start] = (byte)(value>>56 & 0xffL) ;
        content[start+1] = (byte)(value>>48 & 0xffL) ;
        content[start+2] = (byte)(value>>40 & 0xffL) ;
        content[start+3] = (byte)(value>>32 & 0xffL) ;
        content[start+4] = (byte)(value>>24 & 0xffL) ;
        content[start+5] = (byte)(value>>16 & 0xffL) ;
        content[start+6] = (byte)(value>>8 & 0xffL) ;
        content[start+7] = (byte)(value & 0xffL) ;
//        unsafe.putLong(content,(long)start, value) ;
        return 8 ;
    }
    
    
    /**
     *  Hash function for byte array.
     * 
     * */
    public static int hashKey(byte[] val, int start, int keylen)
    {
        int a, b, c, len; 
        
        /* Set up the internal state */ 
        len = keylen; 
        a = b = 0x9e3779b9; 
        /* the golden ratio; an arbitrary value */ 
        c = 3923095; 
        
        /* initialize with an arbitrary value */
        /* handle most of the key */ 
        int offset = 0 ;
        while (len >= 12) 
        { 
            a += ((val[offset+0]&0xff) + ( (val[offset+1]&0xff) << 8) + ( (val[offset+2]&0xff) << 16) + ( (val[offset+3]&0xff) << 24)); 
            b += ((val[offset+4]&0xff) + ( (val[offset+5]&0xff) << 8) + ( (val[offset+6]&0xff) << 16) + ( (val[offset+7]&0xff) << 24)); 
            c += ((val[offset+8]&0xff) + ( (val[offset+9]&0xff) << 8) + ( (val[offset+10]&0xff) << 16) + ( (val[offset+11]&0xff) << 24)); 
            
            /*  mix */
            {
                a -= b; a -= c; a ^= ((c)>>13); 
                b -= c; b -= a; b ^= ((a)<<8); 
                c -= a; c -= b; c ^= ((b)>>13); 
                a -= b; a -= c; a ^= ((c)>>12);  
                b -= c; b -= a; b ^= ((a)<<16); 
                c -= a; c -= b; c ^= ((b)>>5); 
                a -= b; a -= c; a ^= ((c)>>3);    
                b -= c; b -= a; b ^= ((a)<<10); 
                c -= a; c -= b; c ^= ((b)>>15); 
            }
            offset += 12;
            len -= 12; 
        } 
        /* handle the last 11 bytes */ 
        c += keylen; 
        switch (len) 
        /* all the case statements fall through */ 
        { 
            case 11: 
                c += ( (val[10]&0xff) << 24); 
            case 10: c += ( (val[9]&0xff) << 16);
            case 9: c += ( (val[8]&0xff) << 8); 
            /* the first byte of c is reserved for the length */ 
            case 8: b += ( (val[7]&0xff) << 24); 
            case 7: b += ( (val[6]&0xff) << 16); 
            case 6: b += ( (val[5]&0xff) << 8); 
            case 5: b += val[4]; 
            case 4: a += ( (val[3]&0xff) << 24); 
            case 3: a += ( (val[2]&0xff) << 16); 
            case 2: a += ( (val[1]&0xff) << 8); 
            case 1: a += val[0]&0xff; 
            /* case 0: nothing left to add */ 
        } 
        /*  mix */
        {
            a -= b; a -= c; a ^= ((c)>>13); 
            b -= c; b -= a; b ^= ((a)<<8); 
            c -= a; c -= b; c ^= ((b)>>13); 
            a -= b; a -= c; a ^= ((c)>>12);  
            b -= c; b -= a; b ^= ((a)<<16); 
            c -= a; c -= b; c ^= ((b)>>5); 
            a -= b; a -= c; a ^= ((c)>>3);    
            b -= c; b -= a; b ^= ((a)<<10); 
            c -= a; c -= b; c ^= ((b)>>15); 
        }
        /* report the result */ 
        return c;         
    }
    
    /**
     *  Double to bytes.
     *  Get a representation of the specified floating-point value <BR>
     *  according to the IEEE 754 floating-point "double format" bit layout.<BR>
     *  Refer to Double.doubleToLongBits(). <BR>
     *  
     *  Return the written bytes length.
     * 
     * */
    public static int writeDouble(byte[] content, int start, double value)
    {
        long bitVal = Double.doubleToLongBits(value) ;
        return writeLong(content, start, bitVal) ;
    }
    
    /**
     *  Bytes to double.
     *  <BR>Refer to Double.longBitsToDouble().
     * 
     * */
    public static double readDouble(byte[] content, int start)
    {
        long bitVal = readLong(content,start) ;
        return Double.longBitsToDouble(bitVal) ;
    } 
    
    public static Date readDate(byte[] content, int start)
    {
        long val = readLong(content,start) ;
        return new Date(val) ;
    }
    
    public static int writeDate(byte[] content, int start, Date date)
    {
        long val = date.getTime() ;
        return writeLong(content,start, val) ;
    }
    /**
     *  Bytes to boolean.
     * 
     * */
    public static boolean readBool(byte[] content, int start)
    {
        return content[start]==0?false:true;
    }
    /**
     *  Boolean to bytes.
     *  Return the bytes length.
     * 
     * */
    public static int writeBool(byte[] content, int start, boolean value)
    {
        content[start] = value?(byte)1:0 ;
        return 1;
    }
    
    /**
     *  Bytes to big integer.
     * 
     * */
    public static BigInteger readBigInteger(byte[] content, int start, int len)
    {
        byte[] byteVal = new byte[len] ;
        System.arraycopy(content, start, byteVal, 0, len) ;
        return new BigInteger(byteVal) ;
    }
    /**
     *  Big integer to bytes.
     *  
     *  Return the bytes length.
     * 
     * */
    public static int writeBiginteger(byte[] content, int start, BigInteger value)
    {
        byte[] byteValue = value.toByteArray() ;
        System.arraycopy(byteValue, 0, content, start, byteValue.length) ;
        return byteValue.length ;
    }
    /**
     *  Big decimal to bytes.
     *  
     *  Return the bytes length.
     * 
     * */
    public static int writeBigDecimal(byte[] content, int start , BigDecimal value)
    {
       String str = value.toString() ;
       byte[] byteVal = str.getBytes() ;
       System.arraycopy(byteVal, 0, content, start, byteVal.length) ;
       return byteVal.length ;
    }
    
    public static BigDecimal readBigDecimal(byte[] content, int start, int len)
    {
        return new BigDecimal(new String(content,start,len)) ;
    }
    
    public static void main(String[] args)
    {
        byte[] buf = new byte[1024] ;
        
        writeLong(buf,0,123) ;
        System.out.println(readLong(buf,0)) ;
        
        writeInt(buf,0,123) ;
        System.out.println(readInt(buf,0)) ;
        
    }
}
