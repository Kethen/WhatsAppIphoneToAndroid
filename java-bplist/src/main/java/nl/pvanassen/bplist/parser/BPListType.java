package nl.pvanassen.bplist.parser;

/**
 * Types of binary data
 * @author Paul van Assen
 *
 */
public enum BPListType {
    BOOLEAN, 
    LONG, 
    FLOAT, 
    DOUBLE, 
    DATE, 
    DATA, 
    ASCII_STRING,
    UNICODE_STRING,
    UID, 
    SHORT_DICT, BYTE_DICT, 
    SHORT_ARRAY, BYTE_ARRAY
}
