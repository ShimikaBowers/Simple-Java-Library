/**
 *
 */
package simple.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;

/**
 * Convenience methods for Readers.
 * @author Ken
 *
 */
public final class ReadWriterFactory {
	/**
	 * @param file
	 * @return a FileReader wrapped in a BufferedInputStream
	 * @throws FileNotFoundException
	 */
	public static BufferedReader getBufferedReader(final File file) throws FileNotFoundException {
		return new BufferedReader(new FileReader(file));
	}
	/**
	 * Wraps a Reader in a BufferedReader.
	 * @param rd
	 * @return Reader wrapped in a BufferedReader. If <var>rd</var> is already
	 * 			a BufferedReader then it is casted and returned.
	 */
	public static BufferedReader getBufferedReader(final Reader rd) {
		if(rd instanceof java.io.BufferedReader)
			return (BufferedReader)rd;
		return new BufferedReader(rd);
	}
	/**
	 * Reads all available data from reader.
	 * @param reader Source
	 * @return Everything contained in reader as a string.
	 * @throws IOException
	 */
	public static String readFully(final Reader reader) throws IOException {
		final StringWriter writer = new StringWriter();
		FileUtil.copy(reader, writer, 1024);
		return writer.toString();
	}
	public static void readInto(final Reader reader, final StringBuilder buf) throws IOException {
		final char[] buffer = new char[1024];
		int length = 0;
		while ((length=reader.read(buffer))!=-1) {
			buf.append(buffer, 0, length);
		}
	}
	/**
	 * Reads until <code>end</code> is reached. The returned String includes the end character.<br>
	 * As a result of this behaviour, an empty string means that the end has been reached.
	 * @param in Reader to read from.
	 * @param end Character to stop at.
	 * @return String of read characters.
	 * @throws IOException
	 */
	public static String readUntil(final Reader in, final char end) throws IOException {
		final StringBuffer buf = new StringBuffer(255);
		final char[] cbuf = new char[1];
		while((in.read(cbuf)!=-1)){
			buf.append(cbuf[0]);
			if (cbuf[0]==end) {
				break;
			}
		}
		return buf.toString();
	}
	/**
	 * Reads until <code>end</code> is reached. The returned String includes the end character.
	 * @param in Reader to read from.
	 * @param end Character to stop at.
	 * @return String of read characters.
	 * @throws IOException
	 */
	public static String readUntil(final Reader in,final int limit) throws IOException{
		final char[] cbuf = new char[limit];
		final int read=in.read(cbuf);
		if(read==-1)return null;else return new String(Arrays.copyOfRange(cbuf, 0, read));
	}
	/** Reads until <code>end</code> is found or the end of the stream is reached.
	 * @param rd
	 * @param end
	 * @param throwEOF Weather or not it should throw an EOFException if EOF is reached.
	 * @return The content read including the <code>end</code> string. Returns null if the first character read is EOF.
	 * @throws IOException
	 * @throws EOFException
	 */
	public static String readUntil(final Reader rd, final String end, final boolean throwEOF) throws IOException, EOFException {
		final StringBuilder buf = new StringBuilder(100);
		final char[] c = new char[1];
		int read = 0;
		while((read=rd.read(c))!=-1) {
			buf.append(c[0]);
			if (end.length() <= buf.length())
				if (buf.substring(buf.length()-end.length()).equalsIgnoreCase(end)) {
					break;
				}
		}
		if (read==-1 && throwEOF) throw new EOFException("End of file reached before '"+end+"' was found.");
		if (buf.length()==0) return null;
		return buf.toString();
	}
	/** Reads until <code>end</code> is found or the end of the stream is reached.
	 * @param rd
	 * @param end
	 * @return The content read including the <code>end</code> string. Returns null if the first character read is EOF.
	 * @throws IOException
	 */
	public static String readUntil(final Reader rd, final String end) throws IOException {
		return readUntil(rd,end,false);
	}
	/**Reads until the first non-whitespace character is found.
	 * @param rd
	 * @return The non-whitespace character or -1
	 * @throws IOException
	 */
	public static int skipWhitespace(final Reader rd) throws IOException {
		int c = rd.read();
		if (c==-1) return c;
		do {
			if (!Character.isWhitespace((char)c)) {
				break;
			}
			c = rd.read();
		} while(c!=-1);
		return c;
	}
	/**
	 * @param file
	 * @return a FileWriter wrapped in a BufferedInputStream
	 * @throws FileNotFoundException
	 */
	public static BufferedWriter getBufferedWriter(final File file) throws IOException {
		return new BufferedWriter(new FileWriter(file));
	}
	/**
	 * Wraps a Writer in a BufferedWriter.
	 * @param wr
	 * @return Writer wrapped in a BufferedWriter. If <var>wr</var> is already
	 * 			a BufferedWriter then it is casted and returned.
	 */
	public static BufferedWriter getBufferedWriter(final Writer wr) {
		if(wr instanceof java.io.BufferedWriter)
			return (BufferedWriter)wr;
		return new BufferedWriter(wr);
	}
}
