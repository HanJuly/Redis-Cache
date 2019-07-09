package com.han.rediswebcache.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CacheResponseWrapper extends HttpServletResponseWrapper {
    private CharArrayWriter bufferd;
    private PrintWriter printWriter;
    private ServletOutputStream outputStream;
    private ByteArrayOutputStream byteArrayOutputStream;

    public CacheResponseWrapper(HttpServletResponse response) {
        super(response);
        bufferd = new CharArrayWriter();
        printWriter = new PrintWriter(bufferd);
        byteArrayOutputStream = new ByteArrayOutputStream();
        outputStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                byteArrayOutputStream.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }
        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return printWriter;
    }

    public String getBufferd() {
        return bufferd.toString();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    public String getBody() {
        return new String(byteArrayOutputStream.toByteArray());
    }
}
