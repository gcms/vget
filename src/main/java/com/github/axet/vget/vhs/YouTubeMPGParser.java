package com.github.axet.vget.vhs;

import java.net.URL;
import java.util.List;

import com.github.axet.vget.vhs.YouTubeInfo.Container;
import com.github.axet.vget.vhs.YouTubeInfo.StreamInfo;

public class YouTubeMPGParser extends YouTubeParser {

    public YouTubeMPGParser() {
    }

    public void filter(List<VideoDownload> sNextVideoURL, String itag, URL url) {
        Integer i = Integer.decode(itag);
        StreamInfo vd = itagMap.get(i);

        // get rid of webm
        if (vd.c == Container.WEBM)
            return;

        super.filter(sNextVideoURL, itag, url);
    }
}
