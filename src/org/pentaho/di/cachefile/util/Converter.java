package org.pentaho.di.cachefile.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.cachefile.meta.IndexMeta;
import org.pentaho.di.cachefile.meta.RecordMeta;
import org.pentaho.di.cachefile.type.FieldType;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;

public class Converter
{
    /**
     *  Row meta to record meta.
     * 
     * */
    public static RecordMeta rowMetaConvert(RowMetaInterface rm) throws Exception
    {
        List<ValueMetaInterface> vmList = rm.getValueMetaList() ;
        int num_fields = vmList.size() ;
        FieldType[] field_formats = new FieldType[num_fields] ;
        for (int i = 0 ; i < num_fields ; i ++)
            field_formats[i] = getType(vmList.get(i)) ;
        String[] field_names = new String[num_fields] ;
        for (int i = 0 ; i < num_fields ; i ++)
            field_names[i] = vmList.get(i).getName() ;
        int [] field_sizes = new int[num_fields] ;
        for (int i = 0 ; i < num_fields ; i++)
            field_sizes[i] = vmList.get(i).getLength() ;
        
        return new RecordMeta(num_fields, field_formats, field_sizes ,  field_names) ;
    }
    
    /**
     *  Record meta to row meta.
     * 
     * */
    public static RowMetaInterface rowMetaConvert(RecordMeta rm) throws Exception
    {
        List<ValueMetaInterface> valueMetaList = new ArrayList<ValueMetaInterface>() ;
        ValueMeta vm = null ;
        for (int i = 0 ; i < rm.num_filed ; i ++)
        {
            vm = new ValueMeta(rm.field_names[i],getType(rm.field_formats[i]),rm.field_max_sizes[i],-1) ;
            
            valueMetaList.add(vm) ;
        }
        
        RowMeta rowMeta = new RowMeta() ;
        rowMeta.setValueMetaList(valueMetaList) ;
        
        return rowMeta ;
    }
    /**
     *  Record meta to index row meta.
     * 
     * */
    public static RowMetaInterface rowMetaConvert(RecordMeta rm, IndexMeta im ) throws Exception
    {
        List<ValueMetaInterface> valueMetaList = new ArrayList<ValueMetaInterface>() ;
        ValueMeta vm = null ;
        for (int i = 0 ; i < im.numKeyFields ; i ++)
        {
            vm = new ValueMeta(rm.field_names[im.field_index[i]],getType(rm.field_formats[im.field_index[i]]),
                rm.field_max_sizes[im.field_index[i]],-1) ;
            valueMetaList.add(vm) ;
        }
        
        RowMeta rowMeta = new RowMeta() ;
        rowMeta.setValueMetaList(valueMetaList) ;

        return rowMeta ;
    }    
    
    /**
     *  Value convert.
     * 
     * */
    public static Object[] valueConvert(RowMetaInterface srcRm, Object[] originalValues) throws UnsupportedEncodingException
    {
        List<ValueMetaInterface> vmList = srcRm.getValueMetaList() ;
        Object[] newValues = new Object[vmList.size()] ;
        for (int i = 0 ; i < newValues.length ; i++)
        {
            newValues[i] = originalValues[i] ;
            
            if (originalValues[i] == null)
                continue ;
            
            /*  Convert for StringType and */
            if (srcRm.getValueMeta(i).getType() == ValueMetaInterface.TYPE_STRING)
            {
                newValues[i] = ((String)originalValues[i]).getBytes("UTF-8") ;
                continue ;
            }
        }
        return newValues ;
    }
    
    /**
     *  Field type to value meta type.
     * 
     * */
    public static int getType(FieldType fieldType) throws Exception
    {

        if (fieldType == FieldType.DoubleType)
            return ValueMetaInterface.TYPE_NUMBER ;
        if (fieldType == FieldType.StringType)
            return ValueMetaInterface.TYPE_STRING ;
        if (fieldType == FieldType.DateType)
            return ValueMetaInterface.TYPE_DATE ;
        if (fieldType == FieldType.BooleanType)
            return ValueMetaInterface.TYPE_BOOLEAN ;
        if (fieldType == FieldType.LongType)
            return ValueMetaInterface.TYPE_INTEGER ;
        if (fieldType == FieldType.BigDecimalType)
            return ValueMetaInterface.TYPE_BIGNUMBER ;        
        
        throw new Exception("Unsupported type") ;
    }
    
    /**
     *  Value meta type to field type.
     * 
     * */
    public static FieldType getType(ValueMetaInterface vm) throws Exception
    {
        int vmType = vm.getType() ;
        
        /** Value type indicating that the value has no type set */
        if (vmType == ValueMetaInterface.TYPE_NONE )
            throw new Exception("Unsupported type!") ;
        
        /** Value type indicating that the value contains a floating point double precision number. */
        if (vmType == ValueMetaInterface.TYPE_NUMBER)
            return FieldType.DoubleType ;
        
        /** Value type indicating that the value contains a text String. */
        if (vmType == ValueMetaInterface.TYPE_STRING)
            return FieldType.StringType ;
        
        /** Value type indicating that the value contains a Date. */
        if (vmType == ValueMetaInterface.TYPE_DATE)
            return FieldType.DateType ;
        
        /** Value type indicating that the value contains a boolean. */
        if (vmType == ValueMetaInterface.TYPE_BOOLEAN)
            return FieldType.BooleanType ;
        
        /** Value type indicating that the value contains a long integer. */
        if (vmType == ValueMetaInterface.TYPE_INTEGER)
            return FieldType.LongType ;
        
        /** Value type indicating that the value contains a floating point precision number with arbitrary precision. */
        if (vmType == ValueMetaInterface.TYPE_BIGNUMBER)
            return FieldType.BigDecimalType ;
        
        /** Value type indicating that the value contains an Object. */
        if (vmType == ValueMetaInterface.TYPE_SERIALIZABLE)
            throw new Exception("Unsupported type!") ;
        
        /** Value type indicating that the value contains binary data: BLOB, CLOB, ... */
        if (vmType == ValueMetaInterface.TYPE_BINARY)
            throw new Exception("Unsupported type!") ;
        
        throw new Exception("Unsupported type!") ;
    }

    /**
     *  Row meta to index meta.
     * 
     * */
    public static IndexMeta indexMeta(RowMetaInterface oriRm,
            RowMetaInterface indexMeta) throws Exception
    {
        List<ValueMetaInterface> vmList = indexMeta.getValueMetaList() ;

        int num_index_field = vmList.size();
        FieldType[] field_formats = new FieldType[num_index_field];
        int[] field_index = new int[num_index_field];
        boolean[] field_asc = new boolean[num_index_field];
        boolean allowDuplicates = true;

        for (int i = 0 ; i < num_index_field ; i++)
        {
            field_formats[i] = getType(vmList.get(i)) ;
            field_index[i] = oriRm.indexOfValue(vmList.get(i).getName()) ;
            assert(field_index[i] != -1) ;
            field_asc[i] = true ;
        }
        
        return new IndexMeta(field_formats, field_index,field_asc, allowDuplicates) ;
        
    }

    /**
     *  Get field indexes from row meta.
     * 
     * */
    public static int[] fieldIndex(RowMetaInterface oriRm,
            RowMetaInterface destRm)
    {
        List<ValueMetaInterface> vmList = destRm.getValueMetaList() ; 
        int num_fields = vmList.size() ;
        int [] field_index = new int[num_fields] ;
        for (int i = 0 ; i < num_fields ; i ++)
        {
            field_index[i] = oriRm.indexOfValue(vmList.get(i).getName()) ;
            assert(field_index[i] != -1) ;
        }
        return field_index;
    }
}
