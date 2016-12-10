package com.github.axet.vget;

import java.net.URL;
import java.util.List;

import com.github.axet.vget.vhs.YouTubeInfo;
import com.github.axet.vget.vhs.YouTubeParser;
import com.github.axet.vget.vhs.YouTubeParser.VideoDownload;

public class ExtractDownloadLinks {

    public static void main(String[] args) {
        try {
            // ex: http://www.youtube.com/watch?v=Nj6PFaDmp6c
            String url = args[0];

            YouTubeInfo info = new YouTubeInfo(new URL(url));

            YouTubeParser parser = new YouTubeParser();

            List<VideoDownload> list = parser.extractLinks(info);

            for (VideoDownload d : list) {
                System.out.println(d.stream + " " + d.url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
