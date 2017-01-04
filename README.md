# vget

Video Download Library.

Support for Youtube, Vimeo. Multithread downloads. Based on multithread download library https://github.com/axet/wget.

Pure java!

How this library works: [YouTube.md](docs/YouTube.md)

## Exceptions

Here are a two kind of exceptions.

1) Fatal exception. all RuntimeException's
  We shall stop application

2) DownloadError (extends RuntimeException)
  We unable to process following url and shall stop to download it

## Example Direct Download

```java
package com.github.axet.vget;

import java.io.File;
import java.net.URL;

public class DirectDownload {

    public static void main(String[] args) {
        try {
            // ex: http://www.youtube.com/watch?v=Nj6PFaDmp6c
            String url = args[0];
            // ex: "/Users/axet/Downloads"
            String path = args[1];
            VGet v = new VGet(new URL(url), new File(path));
            v.download();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
```

## Example Application Managed Download

```java
package com.github.axet.vget;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.vhs.VimeoInfo;
import com.github.axet.vget.vhs.YouTubeInfo;
import com.github.axet.wget.SpeedInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadInfo.Part;
import com.github.axet.wget.info.DownloadInfo.Part.States;
import com.github.axet.wget.info.ex.DownloadInterruptedError;

public class AppManagedDownload {

    static class VGetStatus implements Runnable {
        VideoInfo videoinfo;
        long last;

        Map<VideoFileInfo, SpeedInfo> map = new HashMap<VideoFileInfo, SpeedInfo>();

        public VGetStatus(VideoInfo i) {
            this.videoinfo = i;
        }

        public SpeedInfo getSpeedInfo(VideoFileInfo dinfo) {
            SpeedInfo speedInfo = map.get(dinfo);
            if (speedInfo == null) {
                speedInfo = new SpeedInfo();
                speedInfo.start(dinfo.getCount());
                map.put(dinfo, speedInfo);
            }
            return speedInfo;
        }

        @Override
        public void run() {
            List<VideoFileInfo> dinfoList = videoinfo.getInfo();

            // notify app or save download state
            // you can extract information from DownloadInfo info;
            switch (videoinfo.getState()) {
            case EXTRACTING:
            case EXTRACTING_DONE:
            case DONE:
                if (videoinfo instanceof YouTubeInfo) {
                    YouTubeInfo i = (YouTubeInfo) videoinfo;
                    System.out.println(videoinfo.getState() + " " + i.getVideoQuality());
                } else if (videoinfo instanceof VimeoInfo) {
                    VimeoInfo i = (VimeoInfo) videoinfo;
                    System.out.println(videoinfo.getState() + " " + i.getVideoQuality());
                } else {
                    System.out.println("downloading unknown quality");
                }
                for (VideoFileInfo d : videoinfo.getInfo()) {
                    SpeedInfo speedInfo = getSpeedInfo(d);
                    speedInfo.end(d.getCount());
                    System.out.println(String.format("file:%d - %s (%s)", dinfoList.indexOf(d), d.targetFile,
                            formatSpeed(speedInfo.getAverageSpeed())));
                }
                break;
            case ERROR:
                System.out.println(videoinfo.getState() + " " + videoinfo.getDelay());

                if (dinfoList != null) {
                    for (DownloadInfo dinfo : dinfoList) {
                        System.out.println("file:" + dinfoList.indexOf(dinfo) + " - " + dinfo.getException() + " delay:"
                                + dinfo.getDelay());
                    }
                }
                break;
            case RETRYING:
                System.out.println(videoinfo.getState() + " " + videoinfo.getDelay());

                if (dinfoList != null) {
                    for (DownloadInfo dinfo : dinfoList) {
                        System.out.println("file:" + dinfoList.indexOf(dinfo) + " - " + dinfo.getState() + " "
                                + dinfo.getException() + " delay:" + dinfo.getDelay());
                    }
                }
                break;
            case DOWNLOADING:
                long now = System.currentTimeMillis();
                if (now - 1000 > last) {
                    last = now;

                    String parts = "";

                    for (VideoFileInfo dinfo : dinfoList) {
                        SpeedInfo speedInfo = getSpeedInfo(dinfo);
                        speedInfo.step(dinfo.getCount());

                        List<Part> pp = dinfo.getParts();
                        if (pp != null) {
                            // multipart download
                            for (Part p : pp) {
                                if (p.getState().equals(States.DOWNLOADING)) {
                                    parts += String.format("part#%d(%.2f) ", p.getNumber(),
                                            p.getCount() / (float) p.getLength());
                                }
                            }
                        }
                        System.out.println(String.format("file:%d - %s %.2f %s (%s)", dinfoList.indexOf(dinfo),
                                videoinfo.getState(), dinfo.getCount() / (float) dinfo.getLength(), parts,
                                formatSpeed(speedInfo.getCurrentSpeed())));
                    }
                }
                break;
            default:
                break;
            }
        }
    }

    public static String formatSpeed(long s) {
        if (s > 0.1 * 1024 * 1024 * 1024) {
            float f = s / 1024f / 1024f / 1024f;
            return String.format("%.1f GB/s", f);
        } else if (s > 0.1 * 1024 * 1024) {
            float f = s / 1024f / 1024f;
            return String.format("%.1f MB/s", f);
        } else {
            float f = s / 1024f;
            return String.format("%.1f kb/s", f);
        }
    }

    public static void main(String[] args) {
        // ex: http://www.youtube.com/watch?v=Nj6PFaDmp6c
        String url = args[0];
        // ex: /Users/axet/Downloads/
        File path = new File(args[1]);

        try {
            final AtomicBoolean stop = new AtomicBoolean(false);

            URL web = new URL(url);

            // [OPTIONAL] limit maximum quality, or do not call this function if
            // you wish maximum quality available.
            //
            // if youtube does not have video with requested quality, program
            // will raise en exception.
            VGetParser user = null;

            // create proper html parser depends on url
            user = VGet.parser(web);

            // download limited video quality from youtube
            // user = new YouTubeQParser(YoutubeQuality.p480);

            // download mp4 format only, fail if non exist
            // user = new YouTubeMPGParser();

            // create proper videoinfo to keep specific video information
            VideoInfo videoinfo = user.info(web);

            VGet v = new VGet(videoinfo, path);

            VGetStatus notify = new VGetStatus(videoinfo);

            // [OPTIONAL] call v.extract() only if you d like to get video title
            // or download url link before start download. or just skip it.
            v.extract(user, stop, notify);

            System.out.println("Title: " + videoinfo.getTitle());
            List<VideoFileInfo> list = videoinfo.getInfo();
            if (list != null) {
                for (VideoFileInfo d : list) {
                    // [OPTIONAL] setTarget file for each download source video/audio
                    // use d.getContentType() to determine which or use
                    // v.targetFile(dinfo, ext, conflict) to set name dynamically or
                    // d.targetFile = new File("/Downloads/CustomName.mp3");
                    // to set file name manually.
                    System.out.println("Download URL: " + d.getSource());
                }
            }

            v.download(user, stop, notify);
        } catch (DownloadInterruptedError e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

## Options

```java
package com.github.axet.wget;

import com.github.axet.wget.info.BrowserInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.URLInfo;

public class OptionsList {
    public static void main(String[] args) {
        DirectMultipart.THREAD_COUNT = 3;
        SpeedInfo.SAMPLE_LENGTH = 1000;
        SpeedInfo.SAMPLE_MAX = 20;
        BrowserInfo.USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.97 Safari/537.36";
        DownloadInfo.PART_LENGTH = 5 * 1024 * 1024; // bytes
        URLInfo.READ_TIMEOUT = 5 * 1000; // milliseconds
        URLInfo.CONNECT_TIMEOUT = 5 * 1000; // milliseconds
        RetryWrap.RETRY_COUNT = 5; /// 5 times then fail or -1 for infinite
        RetryWrap.RETRY_DELAY = 3; // seconds between retries
    }
}
```

## Central Maven Repo

```xml
<dependency>
  <groupId>com.github.axet</groupId>
  <artifactId>vget</artifactId>
  <version>1.2.4</version>
</dependency>
```
