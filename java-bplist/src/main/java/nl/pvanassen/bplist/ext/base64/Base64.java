package nl.pvanassen.bplist.ext.base64;

import static nl.pvanassen.bplist.ext.base64.Constants.*;

import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;

/**
 * Encodes and decodes to and from Base64 notation.
 * <p>
 * Change Log:
 * </p>
 * <ul>
 * <li>v2.1 - Cleaned up javadoc comments and unused variables and methods. Added some convenience methods for reading and writing to and from files.</li>
 * <li>v2.0.2 - Now specifies UTF-8 encoding in places where the code fails on systems with other encodings (like EBCDIC).</li>
 * <li>v2.0.1 - Fixed an error when decoding a single byte, that is, when the encoded data was a single byte.</li>
 * <li>v2.0 - I got rid of methods that used booleans to set options. Now everything is more consolidated and cleaner. The code now detects when data that's being decoded is
 * gzip-compressed and will decompress it automatically. Generally things are cleaner. You'll probably have to change some method calls that you were making to support the new
 * options format (<tt>int</tt>s that you "OR" together).</li>
 * <li>v1.5.1 - Fixed bug when decompressing and decoding to a byte[] using <tt>decode( String s, boolean gzipCompressed )</tt>. Added the ability to "suspend" encoding in the
 * Output Stream so you can turn on and off the encoding if you need to embed base64 data in an otherwise "normal" stream (like an XML file).</li>
 * <li>v1.5 - Output stream pases on flush() command but doesn't do anything itself. This helps when using GZIP streams. Added the ability to GZip-compress objects before encoding
 * them.</li>
 * <li>v1.4 - Added helper methods to read/write files.</li>
 * <li>v1.3.6 - Fixed OutputStream.flush() so that 'position' is reset.</li>
 * <li>v1.3.5 - Added flag to turn on and off line breaks. Fixed bug in input stream where last buffer being read, if not completely full, was not returned.</li>
 * <li>v1.3.4 - Fixed when "improperly padded stream" error was thrown at the wrong time.</li>
 * <li>v1.3.3 - Fixed I/O streams which were totally messed up.</li>
 * </ul>
 * <p>
 * I am placing this code in the Public Domain. Do with it as you will. This software comes with no guarantees or warranties but with plenty of well-wishing instead! Please visit
 * <a href="http://iharder.net/base64">http://iharder.net/base64</a> periodically to check for updates or to contribute improvements.
 * </p>
 *
 * @author Robert Harder
 * @author rob@iharder.net
 * @version 2.1
 */
public class Base64 {

    /* ******** P U B L I C F I E L D S ******** */
    /** No options specified. Value is zero. */
    public final static int NO_OPTIONS = 0;
    /** Specify encoding. */
    public final static int ENCODE = 1;
    /** Specify decoding. */
    public final static int DECODE = 0;
    /** Specify that data should be gzip-compressed. */
    public final static int GZIP = 2;
    /** Don't break lines when encoding (violates strict Base64 specification) */
    public final static int DONT_BREAK_LINES = 8;

    // encoding

    /** Defeats instantiation. */
    private Base64() {
    }

    /**
     * Encodes a byte array into Base64 notation.
     * <p>
     * Valid options:
     *
     * <pre>
     *   GZIP: gzip-compresses object before encoding it.
     *   DONT_BREAK_LINES: don't break lines at 76 characters
     *     <i>Note: Technically, this makes your encoding non-compliant.</i>
     * </pre>
     * <p>
     * Example: <code>encodeBytes( myData, Base64.GZIP )</code> or
     * <p>
     * Example: <code>encodeBytes( myData, Base64.GZIP | Base64.DONT_BREAK_LINES )</code>
     *
     * @param source
     *            The data to convert
     * @param options
     *            Specified options
     * @see Base64#GZIP
     * @see Base64#DONT_BREAK_LINES
     * @since 2.0
     * @return Base64 encoded string from source
     */
    public static String encodeBytes(byte[] source, int options) {
        return encodeBytes(source, 0, source.length, options);
    } // end encodeBytes

    /**
     * Encodes a byte array into Base64 notation.
     * <p>
     * Valid options:
     *
     * <pre>
     *   GZIP: gzip-compresses object before encoding it.
     *   DONT_BREAK_LINES: don't break lines at 76 characters
     *     <i>Note: Technically, this makes your encoding non-compliant.</i>
     * </pre>
     * <p>
     * Example: <code>encodeBytes( myData, Base64.GZIP )</code> or
     * <p>
     * Example: <code>encodeBytes( myData, Base64.GZIP | Base64.DONT_BREAK_LINES )</code>
     *
     * @param source
     *            The data to convert
     * @param off
     *            Offset in array where conversion should begin
     * @param len
     *            Length of data to convert
     * @param options
     *            Specified options
     * @see Base64#GZIP
     * @see Base64#DONT_BREAK_LINES
     * @since 2.0
     */
    private static String encodeBytes(byte[] source, int off, int len, int options) {
        // Isolate options
        int dontBreakLines = (options & DONT_BREAK_LINES);
        int gzip = (options & GZIP);

        // Compress?
        if (gzip == GZIP) {
            java.io.ByteArrayOutputStream baos = null;
            java.util.zip.GZIPOutputStream gzos = null;
            Base64OutputStream b64os = null;

            try {
                // GZip -> Base64 -> ByteArray
                baos = new java.io.ByteArrayOutputStream();
                b64os = new Base64OutputStream(baos, ENCODE | dontBreakLines);
                gzos = new java.util.zip.GZIPOutputStream(b64os);

                gzos.write(source, off, len);
                gzos.close();
            } // end try
            catch (java.io.IOException e) {
                e.printStackTrace();
                return null;
            } // end catch
            finally {
                IOUtils.closeQuietly(gzos);
                IOUtils.closeQuietly(b64os);
                IOUtils.closeQuietly(baos);
            } // end finally

            // Return value according to relevant encoding.
            try {
                return new String(baos.toByteArray(), PREFERRED_ENCODING);
            } // end try
            catch (UnsupportedEncodingException uue) {
                return new String(baos.toByteArray());
            } // end catch
        } // end if: compress
          // Else, don't compress. Better not to use streams at all then.
        else {
            // Convert option to boolean in way that code likes it.
            boolean breakLines = dontBreakLines == 0;

            int len43 = (len * 4) / 3;
            byte[] outBuff = new byte[(len43) // Main 4:3
                    + ((len % 3) > 0 ? 4 : 0) // Account for padding
                    + (breakLines ? (len43 / MAX_LINE_LENGTH) : 0)]; // New
            // lines
            int d = 0;
            int e = 0;
            int len2 = len - 2;
            int lineLength = 0;
            for (; d < len2; d += 3, e += 4) {
                Encode3to4.encode3to4(source, d + off, 3, outBuff, e);

                lineLength += 4;
                if (breakLines && (lineLength == MAX_LINE_LENGTH)) {
                    outBuff[e + 4] = NEW_LINE;
                    e++;
                    lineLength = 0;
                } // end if: end of line
            } // en dfor: each piece of array

            if (d < len) {
                Encode3to4.encode3to4(source, d + off, len - d, outBuff, e);
                e += 4;
            } // end if: some padding needed

            // Return value according to relevant encoding.
            try {
                return new String(outBuff, 0, e, PREFERRED_ENCODING);
            } // end try
            catch (UnsupportedEncodingException uue) {
                return new String(outBuff, 0, e);
            } // end catch

        } // end else: don't compress

    } // end encodeBytes

}

