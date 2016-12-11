package com.github.axet.vget.info;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class VideoInfo {

    public enum States {
        QUEUE, EXTRACTING, EXTRACTING_DONE, DOWNLOADING, RETRYING, DONE, ERROR, STOP
    }

    // user friendly url (not direct video stream url)
    private URL web;

    private URL source;
    private List<VideoFileInfo> info = new ArrayList<VideoFileInfo>();
    private String title;
    private URL icon;

    // states, three variables
    private States state;
    private Throwable exception;
    private int delay;
    private int retry;

    /**
     * 
     * @param web
     *            user firendly url
     */
    public VideoInfo(URL web) {
        this.setWeb(web);

        reset();
    }

    /**
     * check if we have call extract()
     * 
     * @return true - if extract() already been called
     */
    synchronized public boolean empty() {
        return info == null;
    }

    /**
     * reset videoinfo state. make it simialar as after calling constructor
     */
    synchronized public void reset() {
        setState(States.QUEUE);

        info = null;
        title = null;
        icon = null;
        exception = null;
        delay = 0;
    }

    synchronized public String getTitle() {
        return title;
    }

    synchronized public void setTitle(String title) {
        this.title = title;
    }

    synchronized public List<VideoFileInfo> getInfo() {
        return info;
    }

    synchronized public void setInfo(List<VideoFileInfo> info) {
        this.info = info;
    }

    synchronized public URL getWeb() {
        return web;
    }

    synchronized public void setWeb(URL source) {
        this.web = source;
    }

    synchronized public States getState() {
        return state;
    }

    synchronized public void setState(States state) {
        this.state = state;
        this.exception = null;
        this.delay = 0;
    }

    synchronized public void setState(States state, Throwable e) {
        this.state = state;
        this.exception = e;
        this.delay = 0;
    }

    synchronized public int getDelay() {
        return delay;
    }

    synchronized public int getRetry() {
        return retry;
    }

    synchronized public void setRetrying(int r, int delay, Throwable e) {
        this.delay = delay;
        this.retry = r;
        this.exception = e;
        this.state = States.RETRYING;
    }

    synchronized public Throwable getException() {
        return exception;
    }

    synchronized public void setException(Throwable exception) {
        this.exception = exception;
    }

    synchronized public URL getIcon() {
        return icon;
    }

    synchronized public void setIcon(URL icon) {
        this.icon = icon;
    }

    synchronized public URL getSource() {
        return source;
    }

    synchronized public void setSource(URL source) {
        this.source = source;
    }

}