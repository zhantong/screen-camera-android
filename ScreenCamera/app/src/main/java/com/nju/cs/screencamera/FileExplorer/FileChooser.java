package com.nju.cs.screencamera.FileExplorer;

/**
 * Created by zhantong on 16/2/29.
 */

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.text.DateFormat;

import android.os.Bundle;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Environment;
import android.view.View;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.nju.cs.screencamera.R;


public class FileChooser extends ListActivity{

    private File currentDir;
    private FileArrayAdapter adapter;
    private Comparator<? super Item> currentComparator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listfragment_main);

        RadioGroup radioGroup=(RadioGroup)findViewById(R.id.options);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId==R.id.by_name){
                    fill(currentDir,Item.BY_NAME);
                }
                else if(checkedId==R.id.by_date){
                    fill(currentDir,Item.BY_DATE);
                }
                else if(checkedId==R.id.by_date_reverse){
                    fill(currentDir,Item.BY_DATE_REVERSE);
                }
            }
        });
        currentDir = new File(Environment.getExternalStorageDirectory().getPath());
        RadioButton radioButton=(RadioButton)findViewById(R.id.by_name);
        radioButton.setChecked(true);
        //fill(currentDir,Item.BY_NAME);
    }
    private String humanReadableByteCount(long bytes){
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = ("KMGTPE").charAt(exp-1)+"";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
    private void fill(File currentFolder,Comparator<? super Item> comparator) {
        currentComparator=comparator;
        File[] foldersAndFiles = currentFolder.listFiles();
        this.setTitle("当前目录: " + currentFolder.getAbsolutePath());
        List<Item> folders = new ArrayList<>();
        List<Item> files = new ArrayList<>();
        for (File folderOrFile : foldersAndFiles) {
            Date lastModDate = new Date(folderOrFile.lastModified());
            DateFormat formater = DateFormat.getDateTimeInstance();
            String date_modify = formater.format(lastModDate);
            if (folderOrFile.isDirectory()) {
                File[] child = folderOrFile.listFiles();
                int itemCount = 0;
                if (child != null) {
                    itemCount = child.length;
                }
                String childCount="共 "+itemCount+" 项";
                folders.add(new Item(folderOrFile.getName(), childCount, date_modify, folderOrFile.getAbsolutePath(), "directory_icon"));
            } else {
                files.add(new Item(folderOrFile.getName(), humanReadableByteCount(folderOrFile.length()), date_modify, folderOrFile.getAbsolutePath(), "file_icon"));
            }
        }
        Collections.sort(folders,comparator);
        Collections.sort(files, comparator);
        folders.addAll(files);
        if (!currentFolder.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getPath()))
            folders.add(0, new Item("..", "上级目录", "", currentFolder.getParent(), "directory_up"));
        adapter = new FileArrayAdapter(FileChooser.this, R.layout.list_row, folders);
        this.setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Item item = adapter.getItem(position);
        if (item.getImage().equalsIgnoreCase("directory_icon") || item.getImage().equalsIgnoreCase("directory_up")) {
            currentDir = new File(item.getPath());
            fill(currentDir,currentComparator);
        } else {
            onFileClick(item);
        }
    }

    private void onFileClick(Item item) {
        //Toast.makeText(this, "Folder Clicked: "+ currentDir, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        //intent.putExtra("GetPath", currentDir.toString());
        //intent.putExtra("GetFileName", item.getName());
        intent.putExtra("GetFilePath", item.getPath());
        setResult(RESULT_OK, intent);
        finish();
    }
}
