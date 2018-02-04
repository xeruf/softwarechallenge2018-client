package xerus.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends FilterInputStream {

    long count;
    
    public CountingInputStream(InputStream in) {
        super(in);
    }
    
    public long getCount() {
        return count;
    }
    
    @Override
    public int read() throws IOException
    {
        final int read = super.read();
        if(read>=0) count++;
        return read;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        final int read = super.read(b, off, len);
        if(read>0) count+=read;
        return read;
    }
    
    @Override
    public long skip(long n) throws IOException {
        final long skipped = super.skip(n);
        if(skipped>0) count+=skipped;
        return skipped;
    }
    
}