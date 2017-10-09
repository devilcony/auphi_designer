package org.pentaho.di.cachefile.index.hash;

import org.pentaho.di.cachefile.meta.RecordMeta;

public interface HashIndex
{
    public final static int init_hash_key_length = 8 ;
    
    /**
     *  Get hash key of value.
     *   
     *    
     * */
    public int hash(RecordMeta tm,Object[] value) ;

}
