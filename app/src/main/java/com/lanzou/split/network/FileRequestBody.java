package com.lanzou.split.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.lanzou.split.event.OnFileIOListener;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;

public class FileRequestBody extends RequestBody {

    private final RequestBody requestBody;

    private final OnFileIOListener listener;

    public FileRequestBody(RequestBody requestBody, OnFileIOListener listener) {
        this.requestBody = requestBody;
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }

    private long currentLength;

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        long length = contentLength();
        ForwardingSink forwardingSink = new ForwardingSink(sink) {
            @Override
            public void write(@NonNull Buffer source, long byteCount) throws IOException {
                if (listener != null) {
                    currentLength += byteCount;
                    listener.onProgress(currentLength, length, byteCount);
                }
                super.write(source, byteCount);
            }
        };
        BufferedSink buffer = Okio.buffer(forwardingSink);
        requestBody.writeTo(buffer);
        buffer.flush();
    }
}
