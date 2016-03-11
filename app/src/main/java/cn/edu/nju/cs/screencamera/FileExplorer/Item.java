package cn.edu.nju.cs.screencamera.FileExplorer;

import java.util.Comparator;

/**
 * Created by zhantong on 16/2/29.
 */
public class Item implements Comparable<Item> {
    private String name;
    private String data;
    private String date;
    private String path;
    private String image;

    public Item(String n, String d, String dt, String p, String img) {
        name = n;
        data = d;
        date = dt;
        path = p;
        image = img;

    }

    public String getName() {
        return name;
    }

    public String getData() {
        return data;
    }

    public String getDate() {
        return date;
    }

    public String getPath() {
        return path;
    }

    public String getImage() {
        return image;
    }
    public static final Comparator<Item> BY_NAME=new Comparator<Item>() {
        @Override
        public int compare(Item lhs, Item rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    };
    public static final Comparator<Item> BY_DATE=new Comparator<Item>() {
        @Override
        public int compare(Item lhs, Item rhs) {
            return lhs.getDate().compareTo(rhs.getDate());
        }
    };
    public static final Comparator<Item> BY_DATE_REVERSE=new Comparator<Item>() {
        @Override
        public int compare(Item lhs, Item rhs) {
            return rhs.getDate().compareTo(lhs.getDate());
        }
    };
    public int compareTo(Item o) {
        if (this.name != null)
            return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }
}
