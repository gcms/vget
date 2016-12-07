# YouTube

This document explain how vget download video from youtube servers. If you'd to improve, fix bugs or create new parser class this document would help.

This is very simple. All work done in two steps.

1) vget open html video page https://www.youtube.com/watch?v=sPKtTIckdkc and extract direct video links

2) then vget pass url to [wget](https://github.com/axet/wget) library which does actual download.

How wget download a file - does not matter, it handles the job correctly and do not fail. But since vget works with generated html it may fail which happens often because YouTube changes generated html.

YouTube have simple server structure. Main http server, which handles http requests and generate your html page. And CDN servers, which handles video files requests from youtube html5 player.

After you request html video page html server generate a page with randomly selected CDN servers and return it to the user. Depend on CDN server it may have different software installed, and handle video urls different.

Some CDN server may have encruption installed, some may have new urls and additional security parameters. Who know what would be next.

YouTube may generate page with proper url secret parameters which you have to parse, if they change the scheme - vget stop to download video. Go and find out why.

Video URL may be few types. First type - combined video stream, video and sound comes togeter. This stream have quality cap - combined stream have maximum 720p. And second type of the stream split stream: video and sound streams provided as separate URLS. This split streams have no quality limitations, so you may download 4k videos. Here a lot of other types of streams like 3d streams. You can find out what types of stream youtube used to be on Wikipedia: http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs

# Source

[YouTubeParser.java](/src/main/java/com/github/axet/vget/vhs/YouTubeParser.java)

vget hack youtube in few ways, it may emulate embedded video links, or emulate browser html.

Main extract function is  extract() it call for booth extract using embedded url and extract using browser url.  

  *  extractLinks()
    * streamCpature()
    * extractEmbedded()

It is simple. We ask YouTube to generate our html in two different ways. Somethims embedded links allows you to watch country restreicted videos, or age restricted vidoes. So it is good to have booth calls.

Then we parse html using extractHtmlInfo() and inside it extractUrlEncodedVideos().

 extractHtmlInfo() - extract encrypted urls hidden in youtube html page.

extractUrlEncodedVideos() converts buggly encrupted URLS to proper download urls, which will be transfered dirrectly to wget library. Ususally this method fails. Last time, for example youtube added "sig=" url parameter instead of "signature=".

# Hacking

YouTube change and may change protocol in the future. Here is a several libraries which follow YouTube "improvements" and look into it. If you'd like to keep this library updated and wont debug youtube hacks alone you may look what people saying here:

  * https://github.com/rg3/youtube-dl
    * https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/youtube.py
  * http://sourceforge.net/projects/ytd2
    * https://sourceforge.net/p/ytd2/code/HEAD/tree/trunk/ytd2/src/main/java/zsk/YTDownloadThread.java
  * https://github.com/pculture/miro
    * https://github.com/pculture/miro/blob/master/tv/lib/flashscraper.py
  * https://github.com/soimort/you-get
    * https://github.com/soimort/you-get/blob/develop/src/you_get/extractors/youtube.py
