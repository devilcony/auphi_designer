package org.pentaho.di.cachefile.storage.buffer;


/**
 *  Physical page header, locate at header region of most physical
 *  pages, such as: data page, index page, etc.
 *  The physical page header contains informations about the page, including
 *  count of used bytes in this page, count of items stored at this page, 
 *  and the next page no.
 *  <br>
 *  <br>Physical page header layout sketch:
 *  <br> -------------------------------------------
 *  <br>| byte used | item count | next page no |...|
 *  <br>|...........................................|
 *  <br>|...........................................|
 *  <br>|...........................................|
 *  <br>|...........................................|
 *  <br> -------------------------------------------
 * 
 * */
public abstract class PhysicalPageHeader
{
    /**
     * Offsets of physical page information.
     * Those information is called 'physical page header', 
     * which is different from buffered page header.
     * The physical page header(part of physical page) will be written to the storage.
     * 
     **/
    
    /*  offset of byte used of the physical page    */
    public final static int offset_bytes_used = 0;
    /*  offset of item count of the physical page */
    public final static int offset_item_count = 4;
    /*  offset of next page of the physical page    */
    public final static int offset_next_page = 8 ;
    
    /*  size of physical page header, total size of page information */
    public final static int pageHeaderSize = 16 ;
    /*  size of internal offset in page    */
    public final static int offsetSize = 4 ;
}
