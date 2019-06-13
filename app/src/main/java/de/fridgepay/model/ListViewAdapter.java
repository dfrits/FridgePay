package de.fridgepay.model;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import de.fridgepay.R;

public class ListViewAdapter extends ArrayAdapter<Product> {
    private final ModelManager modelManager = ModelManager.getInstance();
    private final ImageLoader imageLoader = ImageLoader.getInstance();
    private final List<Product> data;
    private final Activity context;

    public ListViewAdapter(Activity context, List<Product> data) {
        super(context, R.layout.list_row, data);
        this.data = data;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();

        if (view == null) {
            view = inflater.inflate(R.layout.list_row, null);
        }

        TextView productName = view.findViewById(R.id.row_title1);
        TextView productPrice = view.findViewById(R.id.row_title2);
        TextView productCount = view.findViewById(R.id.row_title3);
        final ImageView image = view.findViewById(R.id.image);

        Product product = data.get(position);

        String name = product.getFullName();
        productName.setText(name);
        Log.d("CreateView", "ProduktName= " + name);

        String price = context.getString(R.string.price) + " " + product.getPrice() + modelManager.getCurrency();
        productPrice.setText(price);

        if (product.getCount() > 0) {
            productCount.setVisibility(View.VISIBLE);
            String text = "Anzahl: " + product.getCount();
            productCount.setText(text);
        }

        Log.d("CreateView", "ImagePath= " + product.getImagePath());
        image.setImageDrawable(new BitmapDrawable(context.getResources(), modelManager.getPlaceHolder()));
        imageLoader.displayImage(product.getImagePath(), image);

        return view;
    }
}
