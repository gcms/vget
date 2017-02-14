package com.github.axet.vget;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class YouTubeTest extends TestCase {

    private File target;

    public void setUp() throws IOException {
        target = new File(FileUtils.getTempDirectory(), RandomStringUtils.randomAlphabetic(10));
        assert target.mkdirs();
    }

    public void testSimpleDownload() throws MalformedURLException {
        URL url = new URL("https://www.youtube.com/watch?v=18E7ZK4cSv4");
        final VGet vget = new VGet(url, target);
        vget.download(new AtomicBoolean(false), new AppManagedDownload.VGetStatus(vget.getVideo()));

        assertTrue(target.listFiles().length > 0);
        for (File f : target.listFiles()) {
            if (f.getTotalSpace() > 0)
                return;
        }

        assertTrue("All files are empty", false);
    }

    public void testDownload() throws IOException {
        check("https://www.youtube.com/watch?v=kRNmQmNSD90");
//        check("https://www.youtube.com/watch?v=vPHZlUcPx6s");
//        check("https://www.youtube.com/watch?v=USm8i0DG1Sk");
//        check("https://www.youtube.com/watch?v=NqmjBSurfWY");
//        check("https://www.youtube.com/watch?v=kj8DQ_mT7B8");
//        check("https://www.youtube.com/watch?v=YQHsXMglC9A");
//        check("https://www.youtube.com/watch?v=Nj6PFaDmp6c");
//        check("https://www.youtube.com/watch?v=ykQQ7LhtUTM");
//        check("https://www.youtube.com/watch?v=KoUT0ZfPiFk");
//        check("https://www.youtube.com/watch?v=n3ofDzjO0AU");
//        check("https://www.youtube.com/watch?v=NOcteldxsgM");
//        check("https://www.youtube.com/watch?v=Nj6PFaDmp6c");
    }

    void check(String url) throws IOException {
        AppManagedDownload.main(new String[] { url, target.getCanonicalPath() });
    }

    public void tearDown() throws IOException {
        if (target.exists()) {
            FileUtils.deleteDirectory(target);
        }
    }
}
