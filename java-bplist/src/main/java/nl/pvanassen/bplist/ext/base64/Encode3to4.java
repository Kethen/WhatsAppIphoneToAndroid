package nl.pvanassen.bplist.ext.base64;
import static nl.pvanassen.bplist.ext.base64.Constants.*;

class Encode3to4 {
    /** The 64 valid Base64 values. */
    private final static byte[] ALPHABET;
    /** May be something funny like EBCDIC */
    private final static byte[] _NATIVE_ALPHABET = { (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J',
            (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X',
            (byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l',
            (byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z',
            (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) '+', (byte) '/' };

    /** Determine which ALPHABET to use. */
    static {
        byte[] __bytes;
        try {
            __bytes = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(PREFERRED_ENCODING);
        } // end try
        catch (java.io.UnsupportedEncodingException use) {
            __bytes = _NATIVE_ALPHABET; // Fall back to native encoding
        } // end catch
        ALPHABET = __bytes;
    } // end static
    private Encode3to4() {
        
    }
    

    /* ******** E N C O D I N G M E T H O D S ******** */
    /**
     * Encodes up to the first three bytes of array <var>threeBytes</var> and
     * returns a four-byte array in Base64 notation. The actual number of
     * significant bytes in your array is given by <var>numSigBytes</var>. The
     * array <var>threeBytes</var> needs only be as big as
     * <var>numSigBytes</var>. Code can reuse a byte array by passing a
     * four-byte array as <var>b4</var>.
     *
     * @param b4
     *            A reusable byte array to reduce array instantiation
     * @param threeBytes
     *            the array to convert
     * @param numSigBytes
     *            the number of significant bytes in your array
     * @return four byte array in Base64 notation.
     * @since 1.5.1
     */
    static byte[] encode3to4(byte[] b4, byte[] threeBytes, int numSigBytes) {
        encode3to4(threeBytes, 0, numSigBytes, b4, 0);
        return b4;
    } // end encode3to4

    /**
     * Encodes up to three bytes of the array <var>source</var> and writes the
     * resulting four Base64 bytes to <var>destination</var>. The source and
     * destination arrays can be manipulated anywhere along their length by
     * specifying <var>srcOffset</var> and <var>destOffset</var>. This method
     * does not check to make sure your arrays are large enough to accomodate
     * <var>srcOffset</var> + 3 for the <var>source</var> array or
     * <var>destOffset</var> + 4 for the <var>destination</var> array. The
     * actual number of significant bytes in your array is given by
     * <var>numSigBytes</var>.
     *
     * @param source
     *            the array to convert
     * @param srcOffset
     *            the index where conversion begins
     * @param numSigBytes
     *            the number of significant bytes in your array
     * @param destination
     *            the array to hold the conversion
     * @param destOffset
     *            the index where output will be put
     * @return the <var>destination</var> array
     * @since 1.3
     */
    static byte[] encode3to4(byte[] source, int srcOffset, int numSigBytes, byte[] destination, int destOffset) {
        // 1 2 3
        // 01234567890123456789012345678901 Bit position
        // --------000000001111111122222222 Array position from threeBytes
        // --------| || || || | Six bit groups to index ALPHABET
        // >>18 >>12 >> 6 >> 0 Right shift necessary
        // 0x3f 0x3f 0x3f Additional AND

        // Create buffer with zero-padding if there are only one or two
        // significant bytes passed in the array.
        // We have to shift left 24 in order to flush out the 1's that appear
        // when Java treats a value as negative that is cast from a byte to an
        // int.
        int inBuff = (numSigBytes > 0 ? ((source[srcOffset] << 24) >>> 8) : 0) | (numSigBytes > 1 ? ((source[srcOffset + 1] << 24) >>> 16) : 0)
                | (numSigBytes > 2 ? ((source[srcOffset + 2] << 24) >>> 24) : 0);

        switch (numSigBytes) {
            case 3:
                destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
                destination[destOffset + 3] = ALPHABET[(inBuff) & 0x3f];
                return destination;

            case 2:
                destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
                destination[destOffset + 3] = EQUALS_SIGN;
                return destination;

            case 1:
                destination[destOffset] = ALPHABET[(inBuff >>> 18)];
                destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
                destination[destOffset + 2] = EQUALS_SIGN;
                destination[destOffset + 3] = EQUALS_SIGN;
                return destination;

            default:
                return destination;
        } // end switch
    } // end encode3to4

}
