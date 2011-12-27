package corelyzer.plugins.expeditionmanager.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Some File- and URL-related utility methods.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class FileUtils {

    /**
     * Copies from an InputStream to an OutputStream.
     * 
     * @param in
     *            the InputStream.
     * @param out
     *            the OutputStream.
     * @return true if the copy was successful, false otherwise.
     */
    public static boolean copy(final InputStream in, final OutputStream out) {
        final ReadableByteChannel srcChannel = Channels.newChannel(in);
        final WritableByteChannel dstChannel = Channels.newChannel(out);
        final ByteBuffer buffer = ByteBuffer.allocate(5 * 1024 * 1024); // 5MB

        try {
            while (srcChannel.read(buffer) >= 0) {
                buffer.flip();
                dstChannel.write(buffer);
                buffer.clear();
            }
            return true;
        } catch (final IOException e) {
            // do nothing
            silentClose(srcChannel);
            silentClose(dstChannel);
        }
        return false;
    }

    /**
     * Copy a file.
     * 
     * @param source
     *            the source file.
     * @param destination
     *            the destination file.
     * @return true if the copy was successful, false otherwise.
     */
    public static boolean copyFile(final File source, final File destination) {
        // don't do anything if it doesn't exist
        if (!source.exists()) {
            return false;
        }

        // create the directory structure if needed
        if ((destination.getParentFile() != null)
                && (!destination.getParentFile().exists())) {
            destination.getParentFile().mkdirs();
        }

        // copy the file
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        try {
            srcChannel = new FileInputStream(source).getChannel();
            dstChannel = new FileOutputStream(destination).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            return true;
        } catch (final IOException ioe) {
            // do nothing
        } finally {
            silentClose(srcChannel);
            silentClose(dstChannel);
        }
        return false;
    }

    /**
     * Gets a File object from a URL.
     * 
     * @param url
     *            the URL.
     * @return the File or null if the URL didn't correspond to a file.
     */
    public static File getFile(final URL url) {
        // convert the URL to a File
        File f = null;
        try {
            f = new File(url.toURI());
        } catch (Exception e) {
            f = new File(url.getPath());
        }

        // make sure the file exists
        if ((f == null) || !f.exists()) {
            return null;
        } else {
            return f;
        }
    }

    /**
     * Gets a URL from a File.
     * 
     * @param file
     *            the file.
     * @return the URL or null if no corresponding URL.
     */
    public static URL getURL(final File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Move a file.
     * 
     * @param source
     *            the source file.
     * @param destination
     *            the destination file.
     * @return true if the move was successful, false otherwise.
     */
    public static boolean moveFile(final File source, final File destination) {
        // delete the destination if
        if (destination.exists()) {
            destination.delete();
        }

        // if the native approach doesn't work, try copy and delete
        if (!source.renameTo(destination)) {
            return copyFile(source, destination) && source.delete();
        }
        return true;
    }

    /**
     * Close the stream silently.
     * 
     * @param stream
     *            the stream.
     */
    public static void silentClose(final Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (final IOException e) {
                // do nothing
            }
        }
    }

    private FileUtils() {
        // not intended to be instantiated
    }
}
