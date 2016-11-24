package cn.edu.nju.cs.screencamera;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by zhantong on 2016/11/24.
 */

public class District implements Iterable<Zone>{
    public static final int LEFT=0;
    public static final int UP=1;
    public static final int RIGHT=2;
    public static final int DOWN=3;
    public static final int LEFT_UP=4;
    public static final int RIGHT_UP=5;
    public static final int LEFT_DOWN=6;
    public static final int RIGHT_DOWN=7;

    public static final int MAIN=0;

    public static final int NUM_TYPES=8;

    private List<Zone> zones=new ArrayList<>();
    public District(){
        for(int i=0;i<NUM_TYPES;i++){
            zones.add(null);
        }
    }
    public Zone get(int part){
        return zones.get(part);
    }
    public void set(int part,Zone zone){
        zones.set(part,zone);
    }

    @Override
    public Iterator<Zone> iterator() {
        return zones.iterator();
    }
}
