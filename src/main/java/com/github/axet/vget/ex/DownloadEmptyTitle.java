package com.github.axet.vget.ex;

import java.io.IOException;

import com.github.axet.wget.info.ex.DownloadError;

public class DownloadEmptyTitle extends DownloadError {
    private static final long serialVersionUID = 7835308901669107488L;

    public DownloadEmptyTitle() {
    }

    public DownloadEmptyTitle(IOException e) {
        super(e);
    }

    public DownloadEmptyTitle(String str) {
        super(str);
    }
}
