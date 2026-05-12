
package com.clubchretien.abonnement.xss;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * User: BSCAKO
 * Date: 03/06/2021 Time: 12:47
 */


public class XSSResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream capture;
    private ServletOutputStream output;
    private PrintWriter writer;

    public XSSResponseWrapper(HttpServletResponse response) {
        super(response);
        capture = new ByteArrayOutputStream(response.getBufferSize());
    }


    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        }

        if (output == null) {
            // inner class - lets the wrapper manipulate the response
            output = new ServletOutputStream() {
                @Override
                public void write(int b) {
                    capture.write(b);
                }

                @Override
                public void flush() throws IOException {
                    capture.flush();
                }

                @Override
                public void close() throws IOException {
                    capture.close();
                }

                @Override
                public boolean isReady() {
                    return false;
                }

                @Override
                public void setWriteListener(WriteListener arg0) {
                }
            };
        }

        return output;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (output != null) {
            throw new IllegalStateException("getOutputStream() has already been called on this response.");
        }

        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(capture,
                    getCharacterEncoding()));
        }

        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        super.flushBuffer();

        if (writer != null) {
            writer.flush();
        } else if (output != null) {
            output.flush();
        }
    }

    public byte[] getCaptureAsBytes() throws IOException {
        if (writer != null) {
            writer.close();
        } else if (output != null) {
            output.close();
        }

        return capture.toByteArray();
    }

    public String response() throws IOException {
        return new String(getCaptureAsBytes(), getCharacterEncoding());
    }

}



