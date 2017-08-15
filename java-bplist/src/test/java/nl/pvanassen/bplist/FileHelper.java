package nl.pvanassen.bplist;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import org.apache.commons.io.IOUtils;

class FileHelper {
    private FileHelper() {

    }

    /**
     * Get a random access file, one way or the other. If the url resolves to a file, use this. Otherwise create a copy in temp and use that
     * 
     * @param resource Resouce url normally used to getResourceAsStream
     * @return Random access file
     */
    static File getFile(String resource) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        try {
            if (url.getProtocol().equals("file")) {
                return new File(url.toURI());
            }
        } catch (URISyntaxException e) {
            // Unhandled. Just copy
        }
        try {
            File file = File.createTempFile("raf", "tmp");
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            ReadableByteChannel src = Channels.newChannel(inputStream);
            WritableByteChannel dest = raf.getChannel();
            final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
            while (src.read(buffer) != -1) {
                // prepare the buffer to be drained
                buffer.flip();
                // write to the channel, may block
                dest.write(buffer);
                // If partial transfer, shift remainder down
                // If buffer is empty, same as doing clear()
                buffer.compact();
            }
            // EOF will leave buffer in fill state
            buffer.flip();
            // make sure the buffer is fully drained.
            while (buffer.hasRemaining()) {
                dest.write(buffer);
            }
            raf.close();
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Error getting RAF", e);
        }
    }

    static String getContent(String resource) throws IOException {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            IOUtils.copy(input, output);
            return new String(output.toByteArray());
        }
    }
}
