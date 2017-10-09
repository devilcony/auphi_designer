package org.pentaho.di.cachefile.meta;

import org.pentaho.di.cachefile.type.FieldType;


public class ResultSetMeta
{
    public RecordMeta rm ;
    public final int [] pre_index  ;
    
    
    public ResultSetMeta(RecordMeta tableMeta, int[] pre_index) throws Exception
    {
        this.pre_index = pre_index.clone() ;
        int num_fields = pre_index.length ;
        String[] field_names = new String[num_fields] ;
        FieldType[] field_formats = new FieldType[num_fields] ;
        int [] field_sizes = new int[num_fields] ;
        
        for (int i = 0 ; i < num_fields ; i ++)
        {
            field_names[i] = tableMeta.field_names[pre_index[i]] ;
            field_formats[i] = tableMeta.field_formats[pre_index[i]] ;
            field_sizes[i] = tableMeta.field_max_sizes[pre_index[i]] ;
        }
        
        rm = new RecordMeta(num_fields, field_formats, field_sizes, field_names) ;
    }
}
