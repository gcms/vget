package com.github.axet.vget.ex;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.wget.info.ex.DownloadError;

public class DownloadFatal extends DownloadError {
    private static final long serialVersionUID = 7835308901669107488L;

    public List<VideoFileInfo> list = new ArrayList<VideoFileInfo>();

    public DownloadFatal() {
    }

    public DownloadFatal(Throwable e) {
        super(e);
    }

    public DownloadFatal(String str) {
        super(str);
    }

    public DownloadFatal(Throwable e, VideoFileInfo v) {
        super(e);
        this.list.add(v);
    }

    public DownloadFatal(Throwable e, List<VideoFileInfo> list) {
        super(e);
        this.list.addAll(list);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        for (VideoFileInfo i : list) {
            if (i.getException() != null) {
                i.getException().printStackTrace(s);
            }
        }
        super.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        for (VideoFileInfo i : list) {
            if (i.getException() != null) {
                i.getException().printStackTrace(s);
            }
        }
        super.printStackTrace(s);
    }
}
