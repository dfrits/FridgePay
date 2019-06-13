package de.fridgepay.internet;

import android.os.AsyncTask;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fridgepay.activities.CartActivity;
import de.fridgepay.model.JsonResult;
import de.fridgepay.model.Product;
import de.fridgepay.utils.Constants;

public class JsonParser extends AsyncTask<String, Void, JsonResult> {
    private final Callback callback;

    public JsonParser(Callback callback) {
        this.callback = callback;
    }

    /**
     * Lädt die aktuelle Liste herunter und wandelt diese um.
     * @param params Url der aktuellen Liste
     * @return Liste der enthaltenen Produkten
     */
    @Override
    protected JsonResult doInBackground(String... params) {
        String url = params[0];

        if (url == null) {
            return new JsonResult();
        }

        HttpHandler httpHandler = new HttpHandler();
        String jsonStr = httpHandler.makeServiceCall(url);

        if (jsonStr != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonStr);

                // Allgemeine Daten auslesen
                String imagePath = jsonObject.getString(Constants.IMAGE_PATH);
                String pHImagePath = jsonObject.getString(Constants.PHIMAGE_PATH);
                String currency = jsonObject.getString(Constants.CURRENCY);
                String volumeUnit = jsonObject.getString(Constants.VOLUME_UNIT);

                // Produkte auslesen
                JSONArray products = jsonObject.getJSONArray(Constants.PRODUCTS);
                List<Product> productList = new ArrayList<>();

                for (int i = 0; i < products.length(); i++) {
                    JSONObject product = products.getJSONObject(i);

                    String id = product.getString(Constants.ID);
                    String type = product.getString(Constants.TYPE);
                    String name = product.getString(Constants.NAME);
                    String volume = product.getString(Constants.VOLUME);
                    String price = product.getString(Constants.PRICE);
                    String imageName = product.getString(Constants.IMAGE);

                    String productImagePath = imageName.isEmpty() ? pHImagePath : imagePath + imageName;
                    volume = volume.equals("-1") ? volume : volume + volumeUnit;

                    productList.add(new Product(id, type, name, volume, price, productImagePath));
                }

                Collections.sort(productList, new Comparator<Product>() {
                    @Override
                    public int compare(Product o1, Product o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });

                ImageLoader imageLoader = ImageLoader.getInstance();
                return new JsonResult(imageLoader.loadImageSync(pHImagePath), currency, volumeUnit, productList);
            } catch (JSONException e) {
                callback.onError(e);
            }
        }
        return new JsonResult();
    }

    @Override
    protected void onPostExecute(JsonResult result) {
        super.onPostExecute(result);
        callback.onDownloadComplete(result);
    }

    /**
     * Wandelt den Json-String der gekauften Produkte um.
     * @param jsonStr String mit den gekauften Produkten
     * @return Map mit den IDs als Key und den Produkten als Value
     * @throws JSONException .
     */
    public static Map<String, Product> parseBoughtProducts(String jsonStr) throws JSONException {
        Map<String, Product> map = new HashMap<>();

        if (jsonStr != null) {
            JSONObject jsonObject = new JSONObject(jsonStr);
            JSONArray boughtProducts = jsonObject.getJSONArray(Constants.PRODUCTS);

            for (int i = 0; i < boughtProducts.length(); i++) {
                JSONObject object = boughtProducts.getJSONObject(i);

                String id = object.getString(Constants.ID);
                String imagePath = object.getString(Constants.IMAGE_PATH);
                String name = object.getString(Constants.NAME);
                int count = object.getInt(CartActivity.COUNT);
                String price = object.getString(Constants.PRICE);
                String volume = object.getString(Constants.VOLUME);
                String type = object.getString(Constants.TYPE);

                Product product = new Product(id, type, name, volume, price, imagePath);
                product.setCount(count);
                map.put(product.getID(), product);
            }
        }

        return map;
    }

    public interface Callback {
        void onDownloadComplete(JsonResult result);

        void onError(Exception e);
    }
}
