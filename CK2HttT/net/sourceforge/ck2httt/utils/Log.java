package net.sourceforge.ck2httt.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;

public class Log {
 
    static private Tee _out;
 
    static class Tee extends FilterOutputStream {
        private OutputStream _dup;
 
        public Tee(OutputStream out, OutputStream dup) {
            super(out);
            _dup = dup;
        }
     
        public void write(int b) throws IOException {
            out.write(b);
            _dup.write(b);
        }
 
        public void write(byte[] b) throws IOException {
            out.write(b);
            _dup.write(b);
        }
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b,off,len);
            _dup.write(b,off,len);
        }
        public void flush() throws IOException {
            out.flush();
            _dup.flush();
        }
        public void close() throws IOException {
            _dup.close();
        }
    }
 
    static public void init(String file) throws IOException {
        _out = new Tee(System.out,new PrintStream(file));
        System.setOut(new PrintStream(_out));
    }
    static public void terminate()  throws IOException {
        _out.flush();
        _out.close();
    }
}