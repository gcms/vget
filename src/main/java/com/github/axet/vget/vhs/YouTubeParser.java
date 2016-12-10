package com.github.axet.vget.vhs;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.vget.info.VideoInfo.States;
import com.github.axet.vget.vhs.YouTubeInfo.AudioQuality;
import com.github.axet.vget.vhs.YouTubeInfo.Container;
import com.github.axet.vget.vhs.YouTubeInfo.Encoding;
import com.github.axet.vget.vhs.YouTubeInfo.StreamAudio;
import com.github.axet.vget.vhs.YouTubeInfo.StreamCombined;
import com.github.axet.vget.vhs.YouTubeInfo.StreamInfo;
import com.github.axet.vget.vhs.YouTubeInfo.StreamVideo;
import com.github.axet.vget.vhs.YouTubeInfo.YoutubeQuality;
import com.github.axet.wget.WGet;
import com.github.axet.wget.info.ex.DownloadError;
import com.github.axet.wget.info.ex.DownloadRetry;

public class YouTubeParser extends VGetParser {

    static public class VideoDownload {
        public StreamInfo stream;
        public URL url;

        public VideoDownload(StreamInfo s, URL u) {
            this.stream = s;
            this.url = u;
        }
    }

    static public class VideoContentFirst implements Comparator<VideoDownload> {
        int ordinal(VideoDownload o1) {
            if (o1.stream instanceof StreamCombined) {
                StreamCombined c1 = (StreamCombined) o1.stream;
                return c1.vq.ordinal();
            }
            if (o1.stream instanceof StreamVideo) {
                StreamVideo c1 = (StreamVideo) o1.stream;
                return c1.vq.ordinal();
            }
            if (o1.stream instanceof StreamAudio) {
                StreamAudio c1 = (StreamAudio) o1.stream;
                return c1.aq.ordinal();
            }
            throw new RuntimeException("bad video array type");
        }

        @Override
        public int compare(VideoDownload o1, VideoDownload o2) {
            Integer i1 = ordinal(o1);
            Integer i2 = ordinal(o2);
            Integer ic = i1.compareTo(i2);

            return ic;
        }

    }

    public static class DecryptSignature {
        public String sig;

        public DecryptSignature(String signature) {
            this.sig = signature;
        }

        public String s(int b, int e) {
            return sig.substring(b, e);
        }

        public String s(int b) {
            return sig.substring(b, b + 1);
        }

        public String se(int b) {
            return s(b, sig.length());
        }

        public String s(int b, int e, int step) {
            String str = "";

            while (b != e) {
                str += sig.charAt(b);
                b += step;
            }
            return str;
        }

        // https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/youtube.py
        public String decrypt() {
            switch (sig.length()) {
            case 93:
                return s(86, 29, -1) + s(88) + s(28, 5, -1);
            case 92:
                return s(25) + s(3, 25) + s(0) + s(26, 42) + s(79) + s(43, 79) + s(91) + s(80, 83);
            case 91:
                return s(84, 27, -1) + s(86) + s(26, 5, -1);
            case 90:
                return s(25) + s(3, 25) + s(2) + s(26, 40) + s(77) + s(41, 77) + s(89) + s(78, 81);
            case 89:
                return s(84, 78, -1) + s(87) + s(77, 60, -1) + s(0) + s(59, 3, -1);
            case 88:
                return s(7, 28) + s(87) + s(29, 45) + s(55) + s(46, 55) + s(2) + s(56, 87) + s(28);
            case 87:
                return s(6, 27) + s(4) + s(28, 39) + s(27) + s(40, 59) + s(2) + se(60);
            case 86:
                return s(80, 72, -1) + s(16) + s(71, 39, -1) + s(72) + s(38, 16, -1) + s(82) + s(15, 0, -1);
            case 85:
                return s(3, 11) + s(0) + s(12, 55) + s(84) + s(56, 84);
            case 84:
                return s(78, 70, -1) + s(14) + s(69, 37, -1) + s(70) + s(36, 14, -1) + s(80) + s(0, 14, -1);
            case 83:
                return s(80, 63, -1) + s(0) + s(62, 0, -1) + s(63);
            case 82:
                return s(80, 37, -1) + s(7) + s(36, 7, -1) + s(0) + s(6, 0, -1) + s(37);
            case 81:
                return s(56) + s(79, 56, -1) + s(41) + s(55, 41, -1) + s(80) + s(40, 34, -1) + s(0) + s(33, 29, -1)
                        + s(34) + s(28, 9, -1) + s(29) + s(8, 0, -1) + s(9);
            case 80:
                return s(1, 19) + s(0) + s(20, 68) + s(19) + s(69, 80);
            case 79:
                return s(54) + s(77, 54, -1) + s(39) + s(53, 39, -1) + s(78) + s(38, 34, -1) + s(0) + s(33, 29, -1)
                        + s(34) + s(28, 9, -1) + s(29) + s(8, 0, -1) + s(9);
            }

            throw new RuntimeException(
                    "Unable to decrypt signature, key length " + sig.length() + " not supported; retrying might work");
        }
    }

    // thanks to @github.com/chberger
    public static class DecryptSignatureHtml5 {
        public String sig;
        public URI playerURI;
        public static ConcurrentMap<String, String> playerCache = new ConcurrentHashMap<String, String>();

        public DecryptSignatureHtml5(String signatur, URI playerURI) {
            this.sig = signatur;
            this.playerURI = playerURI;
        }

        /**
         * Gets the corresponding html5player.js in order to decode youtube video signature
         * 
         * @return player.js file
         */
        public String getHtml5PlayerScript(final AtomicBoolean stop, final Runnable notify) {
            String url = playerCache.get(playerURI.toString());

            if (url == null) {
                try {
                    String result = WGet.getHtml(playerURI.toURL(), new WGet.HtmlLoader() {
                        @Override
                        public void notifyRetry(int retry, int delay, Throwable e) {
                            notify.run();
                        }

                        @Override
                        public void notifyMoved() {
                            notify.run();
                        }

                        @Override
                        public void notifyDownloading() {
                            notify.run();
                        }
                    }, stop);
                    playerCache.put(playerURI.toString(), result);
                    return result;
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }

            return url;
        }

        /**
         * Determines the main decode function name. Unfortunately the name of the decode-funtion might change from
         * version to version, but the part of the code that makes use of this function usually doesn't change. So let's
         * give it a try.
         * 
         * @param playerJS
         *            corresponding javascript html5 player file
         * @return name of decode-function or null
         */
        public String getMainDecodeFunctionName(String playerJS) {
            Pattern decodeFunctionName = Pattern.compile("\\.sig\\|\\|([a-zA-Z0-9$]+)\\(");
            Matcher decodeFunctionNameMatch = decodeFunctionName.matcher(playerJS);
            if (decodeFunctionNameMatch.find()) {
                return decodeFunctionNameMatch.group(1);
            }
            return null;
        }

        /**
         * Extracts the relevant decode functions of the html5player script. Besides the main decode function we need to
         * extract some utility functions the decode-function is using.
         * 
         * @param playerJS
         *            the html5player script
         * @param functionName
         *            the main decode function name
         * @return returns only those functions which are relevant for the signature decoding
         */
        public String extractDecodeFunctions(String playerJS, String functionName) {
            StringBuilder decodeScript = new StringBuilder();
            Pattern decodeFunction = Pattern
                    // this will probably change from version to version so
                    // changes have to be done here
                    .compile(String.format("(%s=function\\([a-zA-Z0-9$]+\\)\\{.*?\\})[,;]", functionName),
                            Pattern.DOTALL);
            Matcher decodeFunctionMatch = decodeFunction.matcher(playerJS);
            if (decodeFunctionMatch.find()) {
                decodeScript.append(decodeFunctionMatch.group(1)).append(';');
            } else {
                throw new DownloadError("Unable to extract the main decode function!");
            }

            // determine the name of the helper function which is used by the
            // main decode function
            Pattern decodeFunctionHelperName = Pattern.compile("\\);([a-zA-Z0-9]+)\\.");
            Matcher decodeFunctionHelperNameMatch = decodeFunctionHelperName.matcher(decodeScript.toString());
            if (decodeFunctionHelperNameMatch.find()) {
                final String decodeFuncHelperName = decodeFunctionHelperNameMatch.group(1);

                Pattern decodeFunctionHelper = Pattern.compile(
                        String.format("(var %s=\\{[a-zA-Z0-9]*:function\\(.*?\\};)", decodeFuncHelperName),
                        Pattern.DOTALL);
                Matcher decodeFunctionHelperMatch = decodeFunctionHelper.matcher(playerJS);
                if (decodeFunctionHelperMatch.find()) {
                    decodeScript.append(decodeFunctionHelperMatch.group(1));
                } else {
                    throw new DownloadError("Unable to extract the helper decode functions!");
                }

            } else {
                throw new DownloadError("Unable to determine the name of the helper decode function!");
            }
            return decodeScript.toString();
        }

        /**
         * Decodes the youtube video signature using the decode functions provided in the html5player script.
         */
        public String decrypt(AtomicBoolean stop, Runnable notify) {
            ScriptEngineManager manager = new ScriptEngineManager();
            // use a js script engine
            ScriptEngine engine = manager.getEngineByName("JavaScript");

            final String playerScript = getHtml5PlayerScript(stop, notify);
            final String decodeFuncName = getMainDecodeFunctionName(playerScript);
            final String decodeScript = extractDecodeFunctions(playerScript, decodeFuncName);

            String decodedSignature = null;
            try {
                // evaluate script
                engine.eval(decodeScript);
                Invocable inv = (Invocable) engine;
                // execute the javascript code directly
                decodedSignature = (String) inv.invokeFunction(decodeFuncName, sig);
            } catch (Exception e) {
                throw new DownloadError("Unable to decrypt signature!");
            }

            return decodedSignature;
        }
    }

    public static class VideoUnavailablePlayer extends DownloadError {
        private static final long serialVersionUID = 10905065542230199L;

        public VideoUnavailablePlayer() {
            super("unavailable-player");
        }
    }

    public static class AgeException extends DownloadError {
        private static final long serialVersionUID = 1L;

        public AgeException() {
            super("Age restriction, account required");
        }
    }

    public static class PrivateVideoException extends DownloadError {
        private static final long serialVersionUID = 1L;

        public PrivateVideoException() {
            super("Private video");
        }

        public PrivateVideoException(String s) {
            super(s);
        }
    }

    public static class EmbeddingDisabled extends DownloadError {
        private static final long serialVersionUID = 1L;

        public EmbeddingDisabled(String msg) {
            super(msg);
        }
    }

    public static class VideoDeleted extends DownloadError {
        private static final long serialVersionUID = 1L;

        public VideoDeleted(String msg) {
            super(msg);
        }

    }

    public YouTubeParser() {
    }

    public static boolean probe(URL url) {
        return url.toString().contains("youtube.com");
    }

    public List<VideoDownload> extractLinks(final YouTubeInfo info) {
        return extractLinks(info, new AtomicBoolean(), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public List<VideoDownload> extractLinks(final YouTubeInfo info, final AtomicBoolean stop, final Runnable notify) {
        try {
            List<VideoDownload> sNextVideoURL = new ArrayList<VideoDownload>();

            try {
                streamCapture(sNextVideoURL, info, stop, notify);
            } catch (DownloadError e) {
                try {
                    extractEmbedded(sNextVideoURL, info, stop, notify);
                } catch (EmbeddingDisabled ee) {
                    throw e;
                }
            }
            return sNextVideoURL;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * do not allow to download age restricted videos
     * 
     * @param info
     * @param stop
     * @param notify
     * @throws Exception
     */
    public void streamCapture(List<VideoDownload> sNextVideoURL, final YouTubeInfo info, final AtomicBoolean stop,
            final Runnable notify) throws Exception {
        String html;
        html = WGet.getHtml(info.getWeb(), new WGet.HtmlLoader() {
            @Override
            public void notifyRetry(int retry, int delay, Throwable e) {
                info.setRetrying(retry, delay, e);
                notify.run();
            }

            @Override
            public void notifyDownloading() {
                info.setState(States.DOWNLOADING);
                notify.run();
            }

            @Override
            public void notifyMoved() {
                info.setState(States.RETRYING);
                notify.run();
            }
        }, stop);
        extractHtmlInfo(sNextVideoURL, info, html, stop, notify);
        extractIcon(info, html);
    }

    /**
     * Add resolution video for specific youtube link.
     * 
     * @param url
     *            download source url
     * @throws MalformedURLException
     */
    public void filter(List<VideoDownload> sNextVideoURL, String itag, URL url) {
        Integer i = Integer.decode(itag);
        StreamInfo vd = itagMap.get(i);

        sNextVideoURL.add(new VideoDownload(vd, url));
    }

    // http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs

    public static final Map<Integer, StreamInfo> itagMap = new HashMap<Integer, StreamInfo>() {
        private static final long serialVersionUID = -6925194111122038477L;

        {
            put(120, new StreamCombined(Container.FLV, Encoding.H264, YoutubeQuality.p720, Encoding.AAC,
                    AudioQuality.k128));
            put(102, new StreamCombined(Container.WEBM, Encoding.VP8, YoutubeQuality.p720, Encoding.VORBIS,
                    AudioQuality.k192));
            put(101, new StreamCombined(Container.WEBM, Encoding.VP8, YoutubeQuality.p360, Encoding.VORBIS,
                    AudioQuality.k192)); // webm
            put(100, new StreamCombined(Container.WEBM, Encoding.VP8, YoutubeQuality.p360, Encoding.VORBIS,
                    AudioQuality.k128)); // webm
            put(85, new StreamCombined(Container.MP4, Encoding.H264, YoutubeQuality.p1080, Encoding.AAC,
                    AudioQuality.k192)); // mp4
            put(84, new StreamCombined(Container.MP4, Encoding.H264, YoutubeQuality.p720, Encoding.AAC,
                    AudioQuality.k192)); // mp4
            put(83, new StreamCombined(Container.MP4, Encoding.H264, YoutubeQuality.p240, Encoding.AAC,
                    AudioQuality.k96)); // mp4
            put(82, new StreamCombined(Container.MP4, Encoding.H264, YoutubeQuality.p360, Encoding.AAC,
                    AudioQuality.k96)); // mp4
            put(46, new StreamCombined(Container.WEBM, Encoding.VP8, YoutubeQuality.p1080, Encoding.VORBIS,
                    AudioQuality.k192)); // webm
            put(45, new StreamCombined(Container.WEBM, Encoding.VP8, YoutubeQuality.p720, Encoding.VORBIS,
                    AudioQuality.k192)); // webm
            put(44, new StreamCombined(Container.WEBM, Encoding.VP8, YoutubeQuality.p480, Encoding.VORBIS,
                    AudioQuality.k128)); // webm
            put(43, new StreamCombined(Container.WEBM, Encoding.VP8, YoutubeQuality.p360, Encoding.VORBIS,
                    AudioQuality.k128)); // webm
            put(38, new StreamCombined(Container.MP4, Encoding.H264, YoutubeQuality.p3072, Encoding.AAC,
                    AudioQuality.k192)); // mp4
            put(37, new StreamCombined(Container.MP4, Encoding.H264, YoutubeQuality.p1080, Encoding.AAC,
                    AudioQuality.k192)); // mp4
            put(36, new StreamCombined(Container.GP3, Encoding.MP4, YoutubeQuality.p240, Encoding.AAC,
                    AudioQuality.k36)); // 3gp
            put(35, new StreamCombined(Container.FLV, Encoding.H264, YoutubeQuality.p480, Encoding.AAC,
                    AudioQuality.k128)); // flv
            put(34, new StreamCombined(Container.FLV, Encoding.H264, YoutubeQuality.p360, Encoding.AAC,
                    AudioQuality.k128)); // flv
            put(22, new StreamCombined(Container.MP4, Encoding.H264, YoutubeQuality.p720, Encoding.AAC,
                    AudioQuality.k192)); // mp4
            put(18, new StreamCombined(Container.MP4, Encoding.H264, YoutubeQuality.p360, Encoding.AAC,
                    AudioQuality.k96)); // mp4
            put(17, new StreamCombined(Container.GP3, Encoding.MP4, YoutubeQuality.p144, Encoding.AAC,
                    AudioQuality.k24)); // 3gp
            put(6, new StreamCombined(Container.FLV, Encoding.H263, YoutubeQuality.p270, Encoding.MP3,
                    AudioQuality.k64)); // flv
            put(5, new StreamCombined(Container.FLV, Encoding.H263, YoutubeQuality.p240, Encoding.MP3,
                    AudioQuality.k64)); // flv

            put(133, new StreamVideo(Container.MP4, Encoding.H264, YoutubeQuality.p240));
            put(134, new StreamVideo(Container.MP4, Encoding.H264, YoutubeQuality.p360));
            put(135, new StreamVideo(Container.MP4, Encoding.H264, YoutubeQuality.p480));
            put(136, new StreamVideo(Container.MP4, Encoding.H264, YoutubeQuality.p720));
            put(137, new StreamVideo(Container.MP4, Encoding.H264, YoutubeQuality.p1080));
            put(138, new StreamVideo(Container.MP4, Encoding.H264, YoutubeQuality.p2160));
            put(160, new StreamVideo(Container.MP4, Encoding.H264, YoutubeQuality.p144));
            put(242, new StreamVideo(Container.WEBM, Encoding.VP9, YoutubeQuality.p240));
            put(243, new StreamVideo(Container.WEBM, Encoding.VP9, YoutubeQuality.p360));
            put(244, new StreamVideo(Container.WEBM, Encoding.VP9, YoutubeQuality.p480));
            put(247, new StreamVideo(Container.WEBM, Encoding.VP9, YoutubeQuality.p720));
            put(248, new StreamVideo(Container.WEBM, Encoding.VP9, YoutubeQuality.p1080));
            put(264, new StreamVideo(Container.MP4, Encoding.H264, YoutubeQuality.p1440));
            put(271, new StreamVideo(Container.WEBM, Encoding.VP9, YoutubeQuality.p1440));
            put(272, new StreamVideo(Container.WEBM, Encoding.VP9, YoutubeQuality.p2160));
            put(278, new StreamVideo(Container.WEBM, Encoding.VP9, YoutubeQuality.p144));
            put(298, new StreamVideo(Container.MP4, Encoding.H264, YoutubeQuality.p720));
            put(299, new StreamVideo(Container.MP4, Encoding.H264, YoutubeQuality.p1080));
            put(302, new StreamVideo(Container.WEBM, Encoding.VP9, YoutubeQuality.p720));
            put(303, new StreamVideo(Container.WEBM, Encoding.VP9, YoutubeQuality.p1080));

            put(139, new StreamAudio(Container.MP4, Encoding.AAC, AudioQuality.k48));
            put(140, new StreamAudio(Container.MP4, Encoding.AAC, AudioQuality.k128));
            put(141, new StreamAudio(Container.MP4, Encoding.AAC, AudioQuality.k256));
            put(171, new StreamAudio(Container.WEBM, Encoding.VORBIS, AudioQuality.k128));
            put(172, new StreamAudio(Container.WEBM, Encoding.VORBIS, AudioQuality.k192));

            put(249, new StreamAudio(Container.WEBM, Encoding.OPUS, AudioQuality.k50));
            put(250, new StreamAudio(Container.WEBM, Encoding.OPUS, AudioQuality.k70));
            put(251, new StreamAudio(Container.WEBM, Encoding.OPUS, AudioQuality.k160));
        }
    };

    public static String extractId(URL url) {
        {
            Pattern u = Pattern.compile("youtube.com/watch?.*v=([^&]*)");
            Matcher um = u.matcher(url.toString());
            if (um.find())
                return um.group(1);
        }

        {
            Pattern u = Pattern.compile("youtube.com/v/([^&]*)");
            Matcher um = u.matcher(url.toString());
            if (um.find())
                return um.group(1);
        }

        return null;
    }

    /**
     * allows to download age restricted videos
     * 
     * @param info
     * @param stop
     * @param notify
     * @throws Exception
     */
    public void extractEmbedded(List<VideoDownload> sNextVideoURL, final YouTubeInfo info, final AtomicBoolean stop,
            final Runnable notify) throws Exception {
        String id = extractId(info.getWeb());
        if (id == null) {
            throw new RuntimeException("unknown url");
        }

        info.setTitle(String.format("https://www.youtube.com/watch?v=%s", id));

        String get = String.format("https://www.youtube.com/get_video_info?authuser=0&video_id=%s&el=embedded", id);

        URL url = new URL(get);

        String qs = WGet.getHtml(url, new WGet.HtmlLoader() {
            @Override
            public void notifyRetry(int retry, int delay, Throwable e) {
                info.setRetrying(retry, delay, e);
                notify.run();
            }

            @Override
            public void notifyDownloading() {
                info.setState(States.DOWNLOADING);
                notify.run();
            }

            @Override
            public void notifyMoved() {
                info.setState(States.RETRYING);
                notify.run();
            }
        }, stop);

        Map<String, String> map = getQueryMap(qs);

        if (map.get("status").equals("fail")) {
            String r = URLDecoder.decode(map.get("reason"), WGet.UTF8);
            if (map.get("errorcode").equals("150"))
                throw new EmbeddingDisabled("error code 150");
            if (map.get("errorcode").equals("100"))
                throw new VideoDeleted("error code 100");

            throw new DownloadError(r);
            // throw new PrivateVideoException(r);
        }

        info.setTitle(URLDecoder.decode(map.get("title"), WGet.UTF8));

        // String fmt_list = URLDecoder.decode(map.get("fmt_list"), UTF8);
        // String[] fmts = fmt_list.split(",");

        String url_encoded_fmt_stream_map = URLDecoder.decode(map.get("url_encoded_fmt_stream_map"), WGet.UTF8);

        extractUrlEncodedVideos(sNextVideoURL, url_encoded_fmt_stream_map, info, stop, notify);

        // 'iurlmaxresæ or 'iurlsd' or 'thumbnail_url'
        String icon = map.get("thumbnail_url");
        icon = URLDecoder.decode(icon, WGet.UTF8);
        info.setIcon(new URL(icon));
    }

    public void extractIcon(VideoInfo info, String html) {
        try {
            Pattern title = Pattern.compile("itemprop=\"thumbnailUrl\" href=\"(.*)\"");
            Matcher titleMatch = title.matcher(html);
            if (titleMatch.find()) {
                String sline = titleMatch.group(1);
                sline = StringEscapeUtils.unescapeHtml4(sline);
                info.setIcon(new URL(sline));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getQueryMap(String qs) {
        try {
            qs = qs.trim();
            List<NameValuePair> list;
            list = URLEncodedUtils.parse(new URI(null, null, null, -1, null, qs, null), WGet.UTF8);
            HashMap<String, String> map = new HashMap<String, String>();
            for (NameValuePair p : list) {
                map.put(p.getName(), p.getValue());
            }
            return map;
        } catch (URISyntaxException e) {
            throw new RuntimeException(qs, e);
        }
    }

    public void extractHtmlInfo(List<VideoDownload> sNextVideoURL, YouTubeInfo info, String html, AtomicBoolean stop,
            Runnable notify) throws Exception {
        {
            Pattern age = Pattern.compile("(verify_age)");
            Matcher ageMatch = age.matcher(html);
            if (ageMatch.find())
                throw new AgeException();
        }

        {
            Pattern age = Pattern.compile("(unavailable-player)");
            Matcher ageMatch = age.matcher(html);
            if (ageMatch.find())
                throw new VideoUnavailablePlayer();
        }

        // grab html5 player url
        {
            Pattern playerURL = Pattern.compile("(//.*?/player-[\\w\\d\\-]+\\/.*\\.js)");
            Matcher playerVersionMatch = playerURL.matcher(html);
            if (playerVersionMatch.find()) {
                info.setPlayerURI(new URI("https:" + playerVersionMatch.group(1)));
            }
        }

        // combined streams
        {
            Pattern urlencod = Pattern.compile("\"url_encoded_fmt_stream_map\":\"([^\"]*)\"");
            Matcher urlencodMatch = urlencod.matcher(html);
            if (urlencodMatch.find()) {
                String url_encoded_fmt_stream_map;
                url_encoded_fmt_stream_map = urlencodMatch.group(1);

                // normal embedded video, unable to grab age restricted videos
                Pattern encod = Pattern.compile("url=(.*)");
                Matcher encodMatch = encod.matcher(url_encoded_fmt_stream_map);
                if (encodMatch.find()) {
                    String sline = encodMatch.group(1);

                    extractUrlEncodedVideos(sNextVideoURL, sline, info, stop, notify);
                }

                // stream video
                Pattern encodStream = Pattern.compile("stream=(.*)");
                Matcher encodStreamMatch = encodStream.matcher(url_encoded_fmt_stream_map);
                if (encodStreamMatch.find()) {
                    String sline = encodStreamMatch.group(1);

                    String[] urlStrings = sline.split("stream=");

                    for (String urlString : urlStrings) {
                        urlString = StringEscapeUtils.unescapeJava(urlString);

                        Pattern link = Pattern.compile("(sparams.*)&itag=(\\d+)&.*&conn=rtmpe(.*),");
                        Matcher linkMatch = link.matcher(urlString);
                        if (linkMatch.find()) {

                            String sparams = linkMatch.group(1);
                            String itag = linkMatch.group(2);
                            String url = linkMatch.group(3);

                            url = "https" + url + "?" + sparams;

                            url = URLDecoder.decode(url, WGet.UTF8);

                            filter(sNextVideoURL, itag, new URL(url));
                        }
                    }
                }
            }
        }

        // separate streams
        {
            Pattern urlencod = Pattern.compile("\"adaptive_fmts\":\\s*\"([^\"]*)\"");
            Matcher urlencodMatch = urlencod.matcher(html);
            if (urlencodMatch.find()) {
                String url_encoded_fmt_stream_map;
                url_encoded_fmt_stream_map = urlencodMatch.group(1);

                // normal embedded video, unable to grab age restricted videos
                Pattern encod = Pattern.compile("url=(.*)");
                Matcher encodMatch = encod.matcher(url_encoded_fmt_stream_map);
                if (encodMatch.find()) {
                    String sline = encodMatch.group(1);

                    extractUrlEncodedVideos(sNextVideoURL, sline, info, stop, notify);
                }

                // stream video
                Pattern encodStream = Pattern.compile("stream=(.*)");
                Matcher encodStreamMatch = encodStream.matcher(url_encoded_fmt_stream_map);
                if (encodStreamMatch.find()) {
                    String sline = encodStreamMatch.group(1);

                    String[] urlStrings = sline.split("stream=");

                    for (String urlString : urlStrings) {
                        urlString = StringEscapeUtils.unescapeJava(urlString);

                        Pattern link = Pattern.compile("(sparams.*)&itag=(\\d+)&.*&conn=rtmpe(.*),");
                        Matcher linkMatch = link.matcher(urlString);
                        if (linkMatch.find()) {

                            String sparams = linkMatch.group(1);
                            String itag = linkMatch.group(2);
                            String url = linkMatch.group(3);

                            url = "https" + url + "?" + sparams;

                            url = URLDecoder.decode(url, WGet.UTF8);

                            filter(sNextVideoURL, itag, new URL(url));
                        }
                    }
                }
            }
        }

        {
            Pattern title = Pattern.compile("<meta name=\"title\" content=(.*)");
            Matcher titleMatch = title.matcher(html);
            if (titleMatch.find()) {
                String sline = titleMatch.group(1);
                String name = sline.replaceFirst("<meta name=\"title\" content=", "").trim();
                name = StringUtils.strip(name, "\">");
                name = StringEscapeUtils.unescapeHtml4(name);
                info.setTitle(name);
            }
        }
    }

    public void extractUrlEncodedVideos(List<VideoDownload> sNextVideoURL, String sline, YouTubeInfo info,
            AtomicBoolean stop, Runnable notify) throws Exception {
        String[] urlStrings = sline.split("url=");

        for (String urlString : urlStrings) {
            urlString = StringEscapeUtils.unescapeJava(urlString);

            String urlFull = URLDecoder.decode(urlString, WGet.UTF8);

            // universal request
            {
                String url = null;
                {
                    Pattern link = Pattern.compile("([^&,]*)[&,]");
                    Matcher linkMatch = link.matcher(urlString);
                    if (linkMatch.find()) {
                        url = linkMatch.group(1);
                        url = URLDecoder.decode(url, WGet.UTF8);
                    }
                }

                String itag = null;
                {
                    Pattern link = Pattern.compile("itag=(\\d+)");
                    Matcher linkMatch = link.matcher(urlFull);
                    if (linkMatch.find()) {
                        itag = linkMatch.group(1);
                    }
                }

                String sig = null;

                if (sig == null) {
                    Pattern link = Pattern.compile("&signature=([^&,]*)");
                    Matcher linkMatch = link.matcher(urlFull);
                    if (linkMatch.find()) {
                        sig = linkMatch.group(1);
                    }
                }

                if (sig == null) {
                    Pattern link = Pattern.compile("sig=([^&,]*)");
                    Matcher linkMatch = link.matcher(urlFull);
                    if (linkMatch.find()) {
                        sig = linkMatch.group(1);
                    }
                }

                if (sig == null) {
                    Pattern link = Pattern.compile("[&,]s=([^&,]*)");
                    Matcher linkMatch = link.matcher(urlFull);
                    if (linkMatch.find()) {
                        sig = linkMatch.group(1);

                        if (info.getPlayerURI() == null) {
                            DecryptSignature ss = new DecryptSignature(sig);
                            sig = ss.decrypt();
                        } else {
                            DecryptSignatureHtml5 ss = new DecryptSignatureHtml5(sig, info.getPlayerURI());
                            sig = ss.decrypt(stop, notify);
                        }
                    }
                }

                if (url != null && itag != null && sig != null) {
                    try {
                        url += "&signature=" + sig;

                        filter(sNextVideoURL, itag, new URL(url));
                        continue;
                    } catch (MalformedURLException e) {
                        // ignore bad urls
                    }
                }
            }
        }
    }

    @Override
    public List<VideoFileInfo> extract(VideoInfo vinfo, AtomicBoolean stop, Runnable notify) {
        List<VideoDownload> videos = extractLinks((YouTubeInfo) vinfo, stop, notify);

        if (videos.size() == 0) {
            // rare error:
            //
            // The live recording you're trying to play is still being processed
            // and will be available soon. Sorry, please try again later.
            //
            // retry. since youtube may already rendrered propertly quality.
            throw new DownloadRetry("empty video download list," + " wait until youtube will process the video");
        }

        List<VideoDownload> audios = new ArrayList<VideoDownload>();

        for (int i = videos.size() - 1; i > 0; i--) {
            if (videos.get(i).stream == null) {
                videos.remove(i);
            } else if ((videos.get(i).stream instanceof StreamAudio)) {
                audios.add(videos.remove(i));
            }
        }

        Collections.sort(videos, new VideoContentFirst());
        Collections.sort(audios, new VideoContentFirst());

        for (int i = 0; i < videos.size();) {
            VideoDownload v = videos.get(i);

            YouTubeInfo yinfo = (YouTubeInfo) vinfo;
            yinfo.setStreamInfo(v.stream);

            VideoFileInfo info = new VideoFileInfo(v.url);

            if (v.stream instanceof StreamCombined) {
                vinfo.setInfo(Arrays.asList(info));
            }

            if (v.stream instanceof StreamVideo) {
                if (audios.size() > 0) {
                    VideoFileInfo info2 = new VideoFileInfo(audios.get(0).url);
                    vinfo.setInfo(Arrays.asList(info, info2));
                } else {
                    // no audio stream?
                    vinfo.setInfo(Arrays.asList(info));
                }
            }

            vinfo.setSource(v.url);
            return vinfo.getInfo();
        }

        // throw download stop if user choice not maximum quality and we have no
        // video rendered by youtube

        throw new DownloadError("no video with required quality found,"
                + " increace VideoInfo.setVq to the maximum and retry download");
    }

    @Override
    public VideoInfo info(URL web) {
        return new YouTubeInfo(web);
    }
}
