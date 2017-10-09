package org.pentaho.di.cachefile.meta;


public abstract class Meta implements Serialize
{    
    /**
     * Compute the actual storage size of the value.
     *  
     * @throws Exception 
     * 
     * */
    public abstract int getStoreSize(Object[] value) throws Exception ;
}
