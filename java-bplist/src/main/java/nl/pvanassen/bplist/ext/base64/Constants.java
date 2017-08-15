package nl.pvanassen.bplist.ext.base64;

class Constants {
    /* ******** P R I V A T E F I E L D S ******** */
    /** Maximum line length (76) of Base64 output. */
    final static int MAX_LINE_LENGTH = 76;
    /** The new line character (\n) as a byte. */
    final static byte NEW_LINE = (byte) '\n';
    /** The equals sign (=) as a byte. */
    final static byte EQUALS_SIGN = (byte) '=';
    /** Preferred encoding. */
    final static String PREFERRED_ENCODING = "UTF-8";
    /** Indicates white space in encoding */
    final static byte WHITE_SPACE_ENC = -5; 


    /**
     * Translates a Base64 value to either its 6-bit reconstruction value or a
     * negative number indicating some other meaning.
     **/
    final static byte[] DECODABET = { -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 0 - 8
            -5, -5, // Whitespace: Tab and Linefeed
            -9, -9, // Decimal 11 - 12
            -5, // Whitespace: Carriage Return
            -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 -
            // 26
            -9, -9, -9, -9, -9, // Decimal 27 - 31
            -5, // Whitespace: Space
            -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
            62, // Plus sign at decimal 43
            -9, -9, -9, // Decimal 44 - 46
            63, // Slash at decimal 47
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine
            -9, -9, -9, // Decimal 58 - 60
            -1, // Equals sign at decimal 61
            -9, -9, -9, // Decimal 62 - 64
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A' through
            // 'N'
            14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'O'
            // through 'Z'
            -9, -9, -9, -9, -9, -9, // Decimal 91 - 96
            26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters 'a'
            // through 'm'
            39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters 'n'
            // through 'z'
            -9, -9, -9, -9 // Decimal 123 - 126
    /*
     * ,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9, // Decimal 127 - 139
     * -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9, // Decimal 140 - 152
     * -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9, // Decimal 153 - 165
     * -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9, // Decimal 166 - 178
     * -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9, // Decimal 179 - 191
     * -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9, // Decimal 192 - 204
     * -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9, // Decimal 205 - 217
     * -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9, // Decimal 218 - 230
     * -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9, // Decimal 231 - 243
     * -9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9,-9 // Decimal 244 - 255
     */};
    
    private Constants() {
        
    }
    
    
}
