package org.pentaho.di.cachefile.index.btree;


/**
 *  B-plus-tree entry, including the key and pointer.
 * 
 * */
public final class BTreeEntry
{
    public final Object[] keyValue ;
    public final long pointer ;
    
    public BTreeEntry(Object[] keyValue, long pointer)
    {
        this.keyValue = keyValue.clone() ;
        this.pointer = pointer ;
    }
    
}
