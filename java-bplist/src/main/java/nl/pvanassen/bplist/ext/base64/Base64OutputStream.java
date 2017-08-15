package nl.pvanassen.bplist.ext.base64;

import java.io.*;
import static nl.pvanassen.bplist.ext.base64.Constants.*;

/* ******** I N N E R C L A S S O U T P U T S T R E A M ******** */
/**
 * A {@link Base64OutputStream} will write data to another <tt>java.io.OutputStream</tt>, given in the constructor, and
 * encode/decode to/from Base64 notation on the fly.
 *
 * @see Base64
 * @since 1.3
 */
class Base64OutputStream extends FilterOutputStream {

    private boolean encode;
    private int position;
    private byte[] buffer;
    private int bufferLength;
    private int lineLength;
    private boolean breakLines;
    private byte[] b4; // Scratch used in a few places

    /**
     * Constructs a {@link Base64OutputStream} in either ENCODE or DECODE
     * mode.
     * <p>
     * Valid options:
     * 
     * <pre>
     *   ENCODE or DECODE: Encode or Decode as data is read.
     *   DONT_BREAK_LINES: don't break lines at 76 characters
     *     (only meaningful when encoding)
     *     <i>Note: Technically, this makes your encoding non-compliant.</i>
     * </pre>
     * <p>
     * Example: <code>new Base64.OutputStream( out, Base64.ENCODE )</code>
     * 
     * @param out
     *            the <tt>java.io.OutputStream</tt> to which data will be
     *            written.
     * @param options
     *            Specified options.
     * @see Base64#ENCODE
     * @see Base64#DECODE
     * @see Base64#DONT_BREAK_LINES
     * @since 1.3
     */
    Base64OutputStream(java.io.OutputStream out, int options) {
        super(out);
        breakLines = (options & Base64.DONT_BREAK_LINES) != Base64.DONT_BREAK_LINES;
        encode = (options & Base64.ENCODE) == Base64.ENCODE;
        bufferLength = encode ? 3 : 4;
        buffer = new byte[bufferLength];
        position = 0;
        lineLength = 0;
        b4 = new byte[4];
    } // end constructor

    /**
     * Writes the byte to the output stream after converting to/from Base64
     * notation. When encoding, bytes are buffered three at a time before
     * the output stream actually gets a write() call. When decoding, bytes
     * are buffered four at a time.
     * 
     * @param theByte
     *            the byte to write
     * @since 1.3
     */
    @Override
    public void write(int theByte) throws IOException {
        // Encode?
        if (encode) {
            buffer[position++] = (byte) theByte;
            if (position >= bufferLength) // Enough to encode.
            {
                out.write(Encode3to4.encode3to4(b4, buffer, bufferLength));

                lineLength += 4;
                if (breakLines && (lineLength >= MAX_LINE_LENGTH)) {
                    out.write(NEW_LINE);
                    lineLength = 0;
                } // end if: end of line

                position = 0;
            } // end if: enough to output
        } // end if: encoding
          // Else, Decoding
        else {
            // Meaningful Base64 character?
            if (DECODABET[theByte & 0x7f] > WHITE_SPACE_ENC) {
                buffer[position++] = (byte) theByte;
                if (position >= bufferLength) // Enough to output.
                {
                    int len = Decode4to3.decode4to3(buffer, 0, b4, 0);
                    out.write(b4, 0, len);
                    // out.write( Base64.decode4to3( buffer ) );
                    position = 0;
                } // end if: enough to output
            } // end if: meaningful base64 character
            else if (DECODABET[theByte & 0x7f] != WHITE_SPACE_ENC) {
                throw new IOException("Invalid character in Base64 data.");
            } // end else: not white space either
        } // end else: decoding
    } // end write

    /**
     * Calls {@link #write(int)} repeatedly until <var>len</var> bytes are
     * written.
     * 
     * @param theBytes
     *            array from which to read bytes
     * @param off
     *            offset for array
     * @param len
     *            max number of bytes to read into array
     * @since 1.3
     */
    @Override
    public void write(byte[] theBytes, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(theBytes[off + i]);
        } // end for: each byte written

    } // end write

    /**
     * Method added by PHIL. [Thanks, PHIL. -Rob] This pads the buffer
     * without closing the stream.
     */
    private void flushBase64() throws IOException {
        if (position > 0) {
            if (encode) {
                out.write(Encode3to4.encode3to4(b4, buffer, position));
                position = 0;
            } // end if: encoding
            else {
                throw new IOException("Base64 input not properly padded.");
            } // end else: decoding
        } // end if: buffer partially full

    } // end flush

    /**
     * Flushes and closes (I think, in the superclass) the stream.
     * 
     * @since 1.3
     */
    @Override
    public void close() throws IOException {
        // 1. Ensure that pending characters are written
        flushBase64();

        // 2. Actually close the stream
        // Base class both flushes and closes.
        super.close();

        buffer = null;
        out = null;
    }
}