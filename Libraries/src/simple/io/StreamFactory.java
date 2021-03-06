/**
 *
 */
package simple.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**convenience methods for InputStream and OutputStream
 * @author Ken
 *
 */
public class StreamFactory {

	/**
	 * @param file
	 * @return a FileInputStream wrapped in a BufferedInputStream
	 * @throws FileNotFoundException
	 */
	public static BufferedInputStream getBufferedInputStream(final File file) throws FileNotFoundException {
		return new BufferedInputStream(new FileInputStream(file));
	}
	/**
	 * Takes an InputStream and wraps it in a BufferedInputStream.
	 * @param in
	 * @return InputStream wrapped in a BufferedInputStream. If <var>in</var> is already
	 * 			a BufferedInputStream then it is casted and returned.
	 */
	public static BufferedInputStream getBufferedInputStream(final InputStream in) {
		if (in instanceof BufferedInputStream)
			return (BufferedInputStream)in;
		return new BufferedInputStream(in);
	}
	/**
	 * Reads until <code>end</code> is reached. The returned array includes the end byte.
	 * @param in InputStream to read from.
	 * @param end Byte to stop at.
	 * @return An array of the bytes read.
	 * @throws IOException
	 * @deprecated
	 * @see StreamUtil#readUntil(InputStream, byte)
	 */
	public static byte[] readUntil(final InputStream in, final byte end) throws IOException {
		return StreamUtil.readUntil(in, end);
	}
	/**Reads until the first non-whitespace character is found.
	 * @param rd
	 * @return The non-whitespace character or -1
	 * @throws IOException
	 * @deprecated
	 * @see StreamUtil#skipWhitespace(InputStream)
	 */
	public static char skipWhitespace(final InputStream rd) throws IOException {
		return StreamUtil.skipWhitespace(rd);
	}

	/**
	 * @param file
	 * @return A FileOutputStream wrapped in a BufferedOutputStream.
	 * @throws FileNotFoundException If the file is a directory or if it cannot be created if it does not exist.
	 */
	public static BufferedOutputStream getBufferedOutputStream(final File file) throws FileNotFoundException {
		return new BufferedOutputStream(new FileOutputStream(file));
	}
	/**
	 * @param file
	 * @param append
	 * @return A FileOutputStream wrapped in a BufferedOutputStream.
	 * @throws FileNotFoundException If the file is a directory or if it cannot be created if it does not exist.
	 */
	public static BufferedOutputStream getBufferedOutputStream(final File file,boolean append) throws FileNotFoundException {
		return new BufferedOutputStream(new FileOutputStream(file,append));
	}
	/**
	 * Wraps an OutputStream in a BufferedOutputStream.
	 * @param out
	 * @return OutputStream wrapped in a BufferedOutputStream. If <var>out</var> is already
	 * 			a BufferedOutputStream then it is casted and returned.
	 */
	public static BufferedOutputStream getBufferedOutputStream(final OutputStream out) {
		if (out instanceof BufferedOutputStream)
			return (BufferedOutputStream)out;
		return new BufferedOutputStream(out);
	}
}
