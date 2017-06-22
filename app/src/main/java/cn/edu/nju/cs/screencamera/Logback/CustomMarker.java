package cn.edu.nju.cs.screencamera.Logback;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Created by zhantong on 2016/11/4.
 */

public class CustomMarker {
    public static final Marker barcodeConfig = MarkerFactory.getMarker("barcodeConfig");
    public static final Marker fecParameters = MarkerFactory.getMarker("fecParameters");
    public static final Marker raw= MarkerFactory.getMarker("raw");
    public static final Marker processed= MarkerFactory.getMarker("processed");
    public static final Marker source= MarkerFactory.getMarker("source");
    public static final Marker sha1= MarkerFactory.getMarker("sha1");
}
