package nl.pvanassen.bplist.parser;

import java.io.*;
import java.math.BigInteger;
import java.util.*;


import org.apache.commons.io.IOUtils;
import org.slf4j.*;

/**
 * Parser for reading the bplist
 * 
 * @author Paul van Assen
 */
public class ElementParser {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Parse object table with a random access file. This method will not close
     * the file for you.
     *
     * @param file File object
     * @return List of objects parsed
     * @throws IOException
     *             In case of an error
     */
    public List<BPListElement<?>> parseObjectTable(File file) throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            return parseObjectTable(raf);
        } finally {
            IOUtils.closeQuietly(raf);
        }
    }

    /**
     * Parse object table with a random access file. This method will not close
     * the file for you.
     *
     * @param raf
     *            Random access file
     * @return List of objects parsed
     * @throws IOException
     *             In case of an error
     */
    private List<BPListElement<?>> parseObjectTable(RandomAccessFile raf) throws IOException {

        // Parse the HEADER
        // ----------------
        // magic number ("bplist")
        // file format version ("00")
        int bpli = raf.readInt();
        int st00 = raf.readInt();
        if ((bpli != 0x62706c69) || (st00 != 0x73743030)) {
            throw new IOException("parseHeader: File does not start with 'bplist00' magic.");
        }

        // Parse the TRAILER
        // ----------------
        // byte size of offset ints in offset table
        // byte size of object refs in arrays and dicts
        // number of offsets in offset table (also is number of objects)
        // element # in offset table which is top level object
        raf.seek(raf.length() - 32);
        raf.readLong();
        // count of object refs in arrays and dicts
        int refCount = (int) raf.readLong();
        raf.readLong();
        // element # in offset table which is top level object
        int topLevelOffset = (int) raf.readLong();
        raf.seek(8);

        // Read everything in memory hmmmm
        byte[] buf = new byte[topLevelOffset - 8];
        raf.readFully(buf);
        ByteArrayInputStream stream = new ByteArrayInputStream(buf);

        return parseObjectTable(new DataInputStream(stream), refCount);
    }

    /**
     * Parse object table with an input stream. This method will not close
     * the input stream for you.
     *
     * @param is
     *            Input stream
     * @return List of objects parsed
     * @throws IOException
     *             In case of an error
     */
    public List<BPListElement<?>> parseObjectTable(InputStream is) throws IOException{
        // mimic using a raf but with a InputStream so that we can read from memory if wanted
        // supports only input streams that allows reset()
        //byte[] intBuffer = new byte[4];
        //byte[] longBuffer = new byte[8];
        //was going to extend RandomAccessFile but realise I can't initialize it without a real file
        //assume that everything happens here were in memory or some other InputStream that has no File
        byte[] readBuffer = new byte[1024];
        int length = 0;
        int readSize;
        is.reset();
        while((readSize = is.read(readBuffer, 0, 1024)) != -1){
            length += readSize;
        }
        // read bpli and st00 from header
        is.reset();
        readSize = is.read(readBuffer, 0, 8);
        if(readSize != 8){
            throw new IOException("parseHeader: File too small to be a bplist.");
        }
        int bpli = ((readBuffer[0] & 0xFF) << 24) | ((readBuffer[1] & 0xFF) << 16) | ((readBuffer[2] & 0xFF) << 8) | (readBuffer[3] & 0xFF);
        int st00 = ((readBuffer[4] & 0xFF) << 24) | ((readBuffer[5] & 0xFF) << 16) | ((readBuffer[6] & 0xFF) << 8) | (readBuffer[7] & 0xFF);
        if ((bpli != 0x62706c69) || (st00 != 0x73743030)) {
            throw new IOException("parseHeader: File does not start with 'bplist00' magic.");
        }
        // read refCount and topLevelOffset from trailer
        is.reset();
        is.skip(length - 32);
        readSize = is.read(readBuffer, 0, 16);
        if(readSize != 16){
            throw new IOException("parseHeader: File too small to be a bplist.");
        }
        int refCount = ((readBuffer[12] & 0xFF) << 24) | ((readBuffer[13] & 0xFF) << 16) | ((readBuffer[14] & 0xFF) << 8) | (readBuffer[15] & 0xFF);
        readSize = is.read(readBuffer, 0, 16);
        if(readSize != 16){
            throw new IOException("parseHeader: File too small to be a bplist.");
        }
        int topLevelOffset = ((readBuffer[12] & 0xFF) << 24) | ((readBuffer[13] & 0xFF) << 16) | ((readBuffer[14] & 0xFF) << 8) | (readBuffer[15] & 0xFF);
        
        is.reset();
        is.skip(8);
        // read all to memory
        byte[] buf = new byte[topLevelOffset - 8];
        readSize = is.read(buf, 0, topLevelOffset - 8);
        if(readSize != (topLevelOffset - 8)){
            throw new IOException("parseHeader: File too small to be a bplist.");
        }
        ByteArrayInputStream stream = new ByteArrayInputStream(buf);
        
        return parseObjectTable(new DataInputStream(stream), refCount);

    }

    /**
     * Object Formats (marker byte followed by additional info in some cases)
     * <ul>
     * <li>null 0000 0000</li>
     * <li>bool 0000 1000 // false</li>
     * <li>bool 0000 1001 // true</li>
     * <li>fill 0000 1111 // fill byte</li>
     * <li>int 0001 nnnn ... // # of bytes is 2^nnnn, big-endian bytes</li>
     * <li>real 0010 nnnn ... // # of bytes is 2^nnnn, big-endian bytes</li>
     * <li>date 0011 0011 ... // 8 byte float follows, big-endian bytes</li>
     * <li>data 0100 nnnn [int] ... // nnnn is number of bytes unless 1111 then int count follows, followed by bytes</li>
     * <li>string 0101 nnnn [int] ... // ASCII string, nnnn is # of chars, if 1111 then int count, else bytes</li>
     * <li>string 0110 nnnn [int] ... // Unicode string, nnnn is # of chars, else 1111 then int count, then big-endian 2-byte shorts</li>
     * <li>0111 xxxx // unused</li>
     * <li>uid 1000 nnnn ... // nnnn+1 is # of bytes</li>
     * <li>1001 xxxx // unused</li>
     * <li>array 1010 nnnn [int] objref* // nnnn is count, unless '1111', then int count follows</li>
     * <li>1011 xxxx // unused</li>
     * <li>1100 xxxx // unused</li>
     * <li>dict 1101 nnnn [int] keyref* objref* // nnnn is count, unless '1111', then int count follows</li>
     * <li>1110 xxxx // unused</li>
     * <li>1111 xxxx // unused</li>
     * </ul>
     */
    private List<BPListElement<?>> parseObjectTable(DataInputStream in, int refCount) throws IOException {
        List<BPListElement<?>> objectTable = new LinkedList<BPListElement<?>>();
        int marker;
        while ((marker = in.read()) != -1) {
            // System.err.println("parseObjectTable marker=" +
            // Integer.toBinaryString(marker)+" 0x"+Integer.toHexString(marker)+" @0x"+Long.toHexString(getPosition()));
            switch ((marker & 0xf0) >> 4) {
                case 0: {
                    parseBoolean(marker & 0xf, objectTable);
                    break;
                }
                case 1: {
                    int count = 1 << (marker & 0xf);
                    parseInt(in, count, objectTable);
                    break;
                }
                case 2: {
                    int count = 1 << (marker & 0xf);
                    parseReal(in, count, objectTable);
                    break;
                }
                case 3: {
                    switch (marker & 0xf) {
                        case 3:
                            parseDate(in, objectTable);
                            break;
                        default:
                            throw new IOException("parseObjectTable: illegal marker " + Integer.toBinaryString(marker));
                    }
                    break;
                }
                case 4: {
                    int count = marker & 0xf;
                    if (count == 15) {
                        count = readCount(in);
                    }
                    parseData(in, count, objectTable);
                    break;
                }
                case 5: {
                    int count = marker & 0xf;
                    if (count == 15) {
                        count = readCount(in);
                    }
                    parseAsciiString(in, count, objectTable);
                    break;
                }
                case 6: {
                    int count = marker & 0xf;
                    if (count == 15) {
                        count = readCount(in);
                    }
                    parseUnicodeString(in, count, objectTable);
                    break;
                }
                case 7: {
                    if (logger.isDebugEnabled()) {
                        logger.debug("parseObjectTable: illegal marker " + Integer.toBinaryString(marker));
                    }
                    return objectTable;
                    // throw new
                    // IOException("parseObjectTable: illegal marker "+Integer.toBinaryString(marker));
                    // break;
                }
                case 8: {
                    int count = (marker & 0xf) + 1;
                    if (logger.isDebugEnabled()) {
                        logger.debug("uid " + count);
                    }
                    parseUID(in, count, objectTable);
                    break;
                }
                case 9: {
                    throw new IOException("parseObjectTable: illegal marker " + Integer.toBinaryString(marker));
                    // break;
                }
                case 10: {
                    int count = marker & 0xf;
                    if (count == 15) {
                        count = readCount(in);
                    }
                    if (refCount > 255) {
                        parseShortArray(in, count, objectTable);
                    } else {
                        parseByteArray(in, count, objectTable);
                    }
                    break;
                }
                case 11: {
                    throw new IOException("parseObjectTable: illegal marker " + Integer.toBinaryString(marker));
                    // break;
                }
                case 12: {
                    throw new IOException("parseObjectTable: illegal marker " + Integer.toBinaryString(marker));
                    // break;
                }
                case 13: {
                    int count = marker & 0xf;
                    if (count == 15) {
                        count = readCount(in);
                    }
                    if (refCount > 256) {
                        parseShortDict(in, count, objectTable);
                    } else {
                        parseByteDict(in, count, objectTable);
                    }
                    break;
                }
                case 14: {
                    throw new IOException("parseObjectTable: illegal marker " + Integer.toBinaryString(marker));
                    // break;
                }
                case 15: {
                    throw new IOException("parseObjectTable: illegal marker " + Integer.toBinaryString(marker));
                    // break;
                }
            }
        }
        return objectTable;
    }

    /**
     * Reads a count value from the object table. Count values are encoded using
     * the following scheme:
     * int 0001 nnnn ... // # of bytes is 2^nnnn, big-endian bytes
     */
    private int readCount(DataInputStream in) throws IOException {
        int marker = in.read();
        if (marker == -1) {
            throw new IOException("variableLengthInt: Illegal EOF in marker");
        }
        if (((marker & 0xf0) >> 4) != 1) {
            throw new IOException("variableLengthInt: Illegal marker " + Integer.toBinaryString(marker));
        }
        int count = 1 << (marker & 0xf);
        int value = 0;
        for (int i = 0; i < count; i++) {
            int b = in.read();
            if (b == -1) {
                throw new IOException("variableLengthInt: Illegal EOF in value");
            }
            value = (value << 8) | b;
        }
        return value;
    }

    /**
     * null 0000 0000 bool 0000 1000 // false bool 0000 1001 // true fill 0000
     * 1111 // fill byte
     */
    private void parseBoolean(int primitive, List<BPListElement<?>> objectTable) throws IOException {
        switch (primitive) {
            case 0:
                objectTable.add(null);
                break;
            case 8:
                objectTable.add(BPListBoolean.FALSE);
                break;
            case 9:
                objectTable.add(BPListBoolean.TRUE);
                break;
            case 15:
                // fill byte: don't add to object table
                break;
            default:
                throw new IOException("parsePrimitive: illegal primitive " + Integer.toBinaryString(primitive));
        }
    }

    /**
     * array 1010 nnnn [int] objref* // nnnn is count, unless '1111', then int
     * count follows
     */
    private void parseByteArray(DataInputStream in, int count, List<BPListElement<?>> objectTable) throws IOException {
        int[] objref = new int[count];

        for (int i = 0; i < count; i++) {
            objref[i] = in.readByte() & 0xff;
            if (objref[i] == -1) {
                throw new IOException("parseByteArray: illegal EOF in objref*");
            }
        }

        objectTable.add(new BPLArray(objectTable, objref, BPListType.BYTE_ARRAY));
    }

    /**
     * array 1010 nnnn [int] objref* // nnnn is count, unless '1111', then int
     * count follows
     */
    private void parseShortArray(DataInputStream in, int count, List<BPListElement<?>> objectTable) throws IOException {
        int[] objref = new int[count];

        for (int i = 0; i < count; i++) {
            objref[i] = in.readShort() & 0xffff;
            if (objref[i] == -1) {
                throw new IOException("parseShortArray: illegal EOF in objref*");
            }
        }

        objectTable.add(new BPLArray(objectTable, objref, BPListType.SHORT_ARRAY));
    }

    /*
     * data 0100 nnnn [int] ... // nnnn is number of bytes unless 1111 then int
     * count follows, followed by bytes
     */

    private void parseData(DataInputStream in, int count, List<BPListElement<?>> objectTable) throws IOException {
        byte[] data = new byte[count];
        in.readFully(data);
        objectTable.add(new BPListData(data));
    }

    /**
     * byte dict 1101 nnnn keyref* objref* // nnnn is less than '1111'
     */
    private void parseByteDict(DataInputStream in, int count, List<BPListElement<?>> objectTable) throws IOException {
        int[]keyref = new int[count];
        int[]objref = new int[count];

        for (int i = 0; i < count; i++) {
            keyref[i] = in.readByte() & 0xff;
        }
        for (int i = 0; i < count; i++) {
            objref[i] = in.readByte() & 0xff;
        }
        objectTable.add(new BPLDict(objectTable, keyref, objref, BPListType.BYTE_DICT));
    }

    /**
     * short dict 1101 ffff int keyref* objref* // int is count
     */
    private void parseShortDict(DataInputStream in, int count, List<BPListElement<?>> objectTable) throws IOException {
        int[]keyref = new int[count];
        int[]objref = new int[count];

        for (int i = 0; i < count; i++) {
            keyref[i] = in.readShort() & 0xffff;
        }
        for (int i = 0; i < count; i++) {
            objref[i] = in.readShort() & 0xffff;
        }
        objectTable.add(new BPLDict(objectTable, keyref, objref, BPListType.SHORT_DICT));
    }

    /**
     * string 0101 nnnn [int] ... // ASCII string, nnnn is # of chars, else 1111
     * then int count, then bytes
     */
    private void parseAsciiString(DataInputStream in, int count, List<BPListElement<?>> objectTable) throws IOException {
        byte[] buf = new byte[count];
        in.readFully(buf);
        objectTable.add(new BPListString(buf));
    }

    private void parseUID(DataInputStream in, int count, List<BPListElement<?>> objectTable) throws IOException {
        if (count > 4) {
            throw new IOException("parseUID: unsupported byte count: " + count);
        }
        byte[] uid = new byte[count];
        in.readFully(uid);
        objectTable.add(new BPLUid(new BigInteger(uid).intValue()));
    }

    /**
     * int 0001 nnnn ... // # of bytes is 2^nnnn, big-endian bytes
     */
    private void parseInt(DataInputStream in, int count, List<BPListElement<?>> objectTable) throws IOException {
        if (count > 8) {
            throw new IOException("parseInt: unsupported byte count: " + count);
        }
        long value = 0;
        for (int i = 0; i < count; i++) {
            int b = in.read();
            if (b == -1) {
                throw new IOException("parseInt: Illegal EOF in value");
            }
            value = (value << 8) | b;
        }
        objectTable.add(new BPListLong(value));
    }

    /**
     * real 0010 nnnn ... // # of bytes is 2^nnnn, big-endian bytes
     */
    private void parseReal(DataInputStream in, int count, List<BPListElement<?>> objectTable) throws IOException {
        switch (count) {
            case 4:
                objectTable.add(new BPListFloat(in.readFloat()));
                break;
            case 8:
                objectTable.add(new BPListDouble(in.readDouble()));
                break;
            default:
                throw new IOException("parseReal: unsupported byte count:" + count);
        }
    }

    /**
     * unknown 0011 0000 ... // 8 byte float follows, big-endian bytes
     */
    /*
     * private void parseUnknown(DataInputStream in) throws IOException {
     * in.skipBytes(1); objectTable.add("unknown"); }
     */

    /**
     * date 0011 0011 ... // 8 byte float follows, big-endian bytes
     */
    private void parseDate(DataInputStream in, List<BPListElement<?>> objectTable) throws IOException {
        objectTable.add(new BPListDate(in.readDouble()));
    }

    /**
     * string 0110 nnnn [int] ... // Unicode string, nnnn is # of chars, else
     * 1111 then int count, then big-endian 2-byte shorts
     */
    private void parseUnicodeString(DataInputStream in, int count, List<BPListElement<?>> objectTable) throws IOException {
        char[] buf = new char[count];
        for (int i = 0; i < count; i++) {
            buf[i] = in.readChar();
        }
        objectTable.add(new BPListString(buf));
    }

}
