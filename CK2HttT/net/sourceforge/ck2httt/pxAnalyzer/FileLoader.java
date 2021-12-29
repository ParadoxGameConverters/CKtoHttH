package net.sourceforge.ck2httt.pxAnalyzer;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.Reader;

import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;
import java.nio.BufferUnderflowException;

/**
 * This class is used to map a file into memory.
 * It is useful to improve performances with large files.
 * It provides a transition towards the Reader class, so that common io methods
 * can easily be used.
 * 
 * @author Copyright 2007 Yves Prélot ; distributed under the terms of the GNU General Public License
 */
public class FileLoader {

	/**
	 * Modes for opening the mapped file.
	 */
	static public enum Mode {
		READ       ("r", FileChannel.MapMode.READ_ONLY),
		READ_WRITE ("rw",FileChannel.MapMode.READ_WRITE),
		PRIVATE    ("rw",FileChannel.MapMode.PRIVATE);
		
		final private String               _rafMode;
		final private FileChannel.MapMode  _mapMode;
		private Mode(String rafMode, FileChannel.MapMode mapMode) { _rafMode=rafMode; _mapMode=mapMode; }
		public String              rafMode() { return _rafMode; }
		public FileChannel.MapMode mapMode() { return _mapMode; }
	};
	
	
	/**
	 * Maps the file into memory
	 * @param file the file to map
	 * @param mode the mode of the file READ_ONLY, READ_WRITE, PRIVATE
	 * @return the BufferReader associated with the map.
	 */
	static public BufferReader load(File file, Mode mode) throws IOException {
		FileChannel channel = new RandomAccessFile(file, mode.rafMode()).getChannel();
		BufferReader b  = new BufferReader(channel.map(mode.mapMode(), 0, (int)channel.size()));
		channel.close();
		b._buffer.load();
		return b;
	}
	/**
	 * Maps the file into memory
	 * @param filename the name of the file to map
	 * @param mode the mode of the file READ_ONLY, READ_WRITE, PRIVATE
	 * @return the BufferReader associated with the map.
	 */
	static public BufferReader load(String filename, Mode mode) throws IOException {
		return load(new File(filename), mode);
	}
	
	/**
	 * A class making the transition between a mapped file and a Reader.
	 * @author yvesp
	 */
	static public class BufferReader extends Reader {
		private BufferReader(MappedByteBuffer b) { _buffer=b; }
		/**
		 * The underlying MappedByteBuffer
		 */
		public MappedByteBuffer _buffer = null;
		
		public void    close() {}
		public void    mark(int readAheadLimit)            { _buffer.mark(); }
		public boolean markSupported()                     { return true;    }
		public int     read() throws IOException           { try { return _buffer.get(); } catch(BufferUnderflowException e) { throw new IOException(e.getMessage()); } }
		public boolean ready()                             { return true; }
		public void    reset()                             { _buffer.reset(); }
		public long    skip(long n) {
			if (n<0) throw new IllegalArgumentException();
			int p = _buffer.position();
			if (p+n>=_buffer.capacity()) n=_buffer.capacity()-p-1;
			_buffer.position((int)(p+n));
			return n;
		}
		public int read(char[] cbuf, int off, int len) {
			int p = _buffer.position();
			if (p+len>_buffer.capacity()) len=_buffer.capacity()-p;
			if (len<=0) return -1;
			byte[] b = new byte[len];
			try { _buffer.get(b); } catch(BufferUnderflowException e) { return -1; }
			for (int i=0; i<len; i++) cbuf[i+off]=(char)(0x00ff&(int)b[i]);
			return len;
		}
		/**
		 * Resets the buffer to an absolute position. In particular, you caqn reset
		 * the Reader to it's start by calling position(0).
		 */
		public void position(int n) { _buffer.position(n); }
		/**
		 * Gets the full data in a possibly huge array
		 */
		public byte[] getAll() {
			final int __LGBUFF = 8196;
			byte[] b      = new byte[_buffer.capacity()];
			int    l      = (_buffer.remaining()<__LGBUFF) ? _buffer.remaining() : __LGBUFF;
			int    offset = 0;
			while (l>0) {
				_buffer.get(b,offset,l);
				offset+=l;
				if (_buffer.remaining()<l) l=_buffer.remaining();
			}
			_buffer.rewind();
			return b;
		}
	}
	
}
