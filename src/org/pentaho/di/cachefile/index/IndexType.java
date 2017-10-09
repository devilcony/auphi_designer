package org.pentaho.di.cachefile.index;

import org.pentaho.di.cachefile.index.hash.LinearHashIndex;
import org.pentaho.di.cachefile.storage.buffer.BufferManager;

public enum IndexType {
	LinearHashIndex(0),
	ExtensibleHashIndex(1),
	BTreeIndex(2) ;
	
	public final int indexType ;
	private IndexType(int indexType)
	{
		this.indexType = indexType ;
	}
	
	public static Index getIndex(int indexType, BufferManager bm) throws Exception
	{
		switch (indexType)
		{
		case 0:
				return new LinearHashIndex(bm) ;
		default:
			throw new Exception("Uncomplement index!") ;
		}
	}
}
