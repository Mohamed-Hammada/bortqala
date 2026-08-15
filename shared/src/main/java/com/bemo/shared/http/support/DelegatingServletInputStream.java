package com.bemo.shared.http.support;

import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * Servlet input stream that reads from a plain {@link InputStream}. Used by
 * {@link MultipleReadRequestWrapper} to replay a buffered request body.
 */
public class DelegatingServletInputStream extends ServletInputStream {

    private final InputStream source;

    public DelegatingServletInputStream(InputStream source) {
        this.source = source;
    }

    @Override
    public boolean isFinished() {
        try {
            return source.available() == 0;
        } catch (IOException e) {
            return true;
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("Asynchronous reads are not supported");
    }

    @Override
    public int read() throws IOException {
        return source.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return source.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}
