package org.pentaho.di.cachefile.meta;


public interface Serialize
{
    /**
     * Reconstruct object from byte array.
     *  
     * @return the byte count read
     * 
     * */
    public int read(byte[] content, int start, int length) throws Exception;
    
    /**
     * Serialize object to storage page.
     * 
     * @return the byte count wrote
     * 
     * */
    public int write(byte[] content , int start, int length) throws Exception;
    
    
    
}
