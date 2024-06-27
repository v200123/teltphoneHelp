package com.u2tzjtne.telephonehelper.http.download;

public interface ProgressListener {
    void update(String url, long bytesRead, long contentLength, boolean done);
}