package nl.pvanassen.bplist.ext.base64;

import static nl.pvanassen.bplist.ext.base64.Constants.*;

class Decode4to3 {
    private Decode4to3() {
        
    }
    

    /* ******** D E C O D I N G M E T H O D S ******** */
    /**
     * Decodes four bytes from array <var>source</var> and writes the resulting
     * bytes (up to three of them) to <var>destination</var>. The source and
     * destination arrays can be manipulated anywhere along their length by
     * specifying <var>srcOffset</var> and <var>destOffset</var>. This method
     * does not check to make sure your arrays are large enough to accomodate
     * <var>srcOffset</var> + 4 for the <var>source</var> array or
     * <var>destOffset</var> + 3 for the <var>destination</var> array. This
     * method returns the actual number of bytes that were converted from the
     * Base64 encoding.
     *
     * @param source
     *            the array to convert
     * @param srcOffset
     *            the index where conversion begins
     * @param destination
     *            the array to hold the conversion
     * @param destOffset
     *            the index where output will be put
     * @return the number of decoded bytes converted
     * @since 1.3
     */
    static int decode4to3(byte[] source, int srcOffset, byte[] destination, int destOffset) {
        // Example: Dk==
        if (source[srcOffset + 2] == EQUALS_SIGN) {
            // Two ways to do the same thing. Don't know which way I like best.
            // int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6
            // )
            // | ( ( DECODABET[ source[ srcOffset + 1] ] << 24 ) >>> 12 );
            int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) | ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12);

            destination[destOffset] = (byte) (outBuff >>> 16);
            return 1;
        } // Example: DkL=
        else if (source[srcOffset + 3] == EQUALS_SIGN) {
            // Two ways to do the same thing. Don't know which way I like best.
            // int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 ) >>> 6
            // )
            // | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
            // | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 );
            int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) | ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12) | ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6);

            destination[destOffset] = (byte) (outBuff >>> 16);
            destination[destOffset + 1] = (byte) (outBuff >>> 8);
            return 2;
        } // Example: DkLE
        else {
            try {
                // Two ways to do the same thing. Don't know which way I like
                // best.
                // int outBuff = ( ( DECODABET[ source[ srcOffset ] ] << 24 )
                // >>> 6 )
                // | ( ( DECODABET[ source[ srcOffset + 1 ] ] << 24 ) >>> 12 )
                // | ( ( DECODABET[ source[ srcOffset + 2 ] ] << 24 ) >>> 18 )
                // | ( ( DECODABET[ source[ srcOffset + 3 ] ] << 24 ) >>> 24 );
                int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) | ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12) | ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6)
                        | ((DECODABET[source[srcOffset + 3]] & 0xFF));

                destination[destOffset] = (byte) (outBuff >> 16);
                destination[destOffset + 1] = (byte) (outBuff >> 8);
                destination[destOffset + 2] = (byte) (outBuff);

                return 3;
            } catch (Exception e) {
                System.out.println("" + source[srcOffset] + ": " + (DECODABET[source[srcOffset]]));
                System.out.println("" + source[srcOffset + 1] + ": " + (DECODABET[source[srcOffset + 1]]));
                System.out.println("" + source[srcOffset + 2] + ": " + (DECODABET[source[srcOffset + 2]]));
                System.out.println("" + source[srcOffset + 3] + ": " + (DECODABET[source[srcOffset + 3]]));
                return -1;
            } // e nd catch
        }
    } // end decodeToBytes
}
