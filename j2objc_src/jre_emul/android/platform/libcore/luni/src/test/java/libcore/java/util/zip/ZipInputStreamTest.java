/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.java.util.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import tests.support.resource.Support_Resources;

public final class ZipInputStreamTest extends TestCase {

    public void testShortMessage() throws IOException {
        byte[] data = "Hello World".getBytes("UTF-8");
        byte[] zipped = ZipOutputStreamTest.zip("short", data);
        assertEquals(Arrays.toString(data), Arrays.toString(unzip("short", zipped)));
    }

    public void testLongMessage() throws IOException {
        byte[] data = new byte[1024 * 1024];
        new Random().nextBytes(data);
        assertTrue(Arrays.equals(data, unzip("r", ZipOutputStreamTest.zip("r", data))));
    }

    public static byte[] unzip(String name, byte[] bytes) throws IOException {
        ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ZipEntry entry = in.getNextEntry();
        assertEquals(name, entry.getName());

        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }

        assertNull(in.getNextEntry()); // There's only one entry in the Zip files we create.

        in.close();
        return out.toByteArray();
    }

    /**
     * Reference implementation allows reading of empty zip using a {@link ZipInputStream}.
     */
    public void testReadEmpty() throws IOException {
        InputStream emptyZipIn = Support_Resources.getStream("java/util/zip/EmptyArchive.zip");
        ZipInputStream in = new ZipInputStream(emptyZipIn);
        try {
            ZipEntry entry = in.getNextEntry();
            assertNull("An empty zip has no entries", entry);
        } finally {
            in.close();
        }
    }

    // NOTE: Using octal because it's easiest to use "hexdump -b" to dump file contents.
    private static final byte[] INCOMPLETE_ZIP = new byte[] {
            0120, 0113, 0003, 0004, 0024, 0000, 0010, 0010, 0010, 0000, 0002, 0035, (byte) 0330,
            0106, 0000, 0000, 0000, 0000, 0000, 0000, 0000, 0000, 0000, 0000, 0000, 0000, 0013,
            0000, 0000, 0000, 0146, 0157, 0157, 0057, 0142, 0141, 0162, 0056, 0160, 0156, 0147 };

    // http://b//21846904
    public void testReadOnIncompleteStream() throws Exception {
        ZipInputStream zi = new ZipInputStream(new ByteArrayInputStream(INCOMPLETE_ZIP));
        ZipEntry ze = zi.getNextEntry();

        // read() and closeEntry() must throw IOExceptions to indicate that
        // the stream is corrupt. The bug above reported that they would loop
        // forever.
        try {
            zi.read(new byte[1024], 0, 1024);
            fail();
        } catch (IOException expected) {
        }

        try {
            zi.closeEntry();
            fail();
        } catch (IOException expected) {
        }

        zi.close();
    }
}
