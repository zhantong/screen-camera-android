package cn.edu.nju.cs.screencamera;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by zhantong on 2016/11/24.
 */

public class Districts implements Iterable<District>{
    public static final int MARGIN=0;
    public static final int BORDER=1;
    public static final int PADDING=2;
    public static final int MAIN=3;
    private List<District> districts=new ArrayList<>();
    public Districts(){
        for(int i=0;i<4;i++){
            districts.add(new District());
        }
    }
    public District get(int part){
        return districts.get(part);
    }

    @Override
    public Iterator<District> iterator() {
        return districts.iterator();
    }
}
