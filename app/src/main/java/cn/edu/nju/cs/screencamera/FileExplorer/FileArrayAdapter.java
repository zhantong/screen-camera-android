package cn.edu.nju.cs.screencamera.FileExplorer;

/**
 * Created by zhantong on 16/2/29.
 */

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import cn.edu.nju.cs.screencamera.R;

import java.util.List;


public class FileArrayAdapter extends ArrayAdapter<Item> {

    private Context c;
    private int id;
    private List<Item> items;

    public FileArrayAdapter(Context context, int textViewResourceId,
                            List<Item> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        items = objects;
    }

    public Item getItem(int i) {
        return items.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

               /* create a new view of my layout and inflate it in the row */
        //convertView = ( RelativeLayout ) inflater.inflate( resource, null );

        final Item o = items.get(position);
        if (o != null) {
            TextView fileName = (TextView) v.findViewById(R.id.fileName);
            TextView info = (TextView) v.findViewById(R.id.info);
            TextView date = (TextView) v.findViewById(R.id.date);
                       /* Take the ImageView from layout and set the city's image */
            ImageView imageCity = (ImageView) v.findViewById(R.id.folderIcon);
            String uri = "drawable/" + o.getImage();
            int imageResource = c.getResources().getIdentifier(uri, null, c.getPackageName());
            Drawable image = c.getResources().getDrawable(imageResource);
            imageCity.setImageDrawable(image);

            if (fileName != null)
                fileName.setText(o.getName());
            if (info != null)
                info.setText(o.getData());
            if (date != null)
                date.setText(o.getDate());

        }
        return v;
    }

}
