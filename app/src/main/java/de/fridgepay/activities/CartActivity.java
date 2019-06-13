package de.fridgepay.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.fridgepay.R;
import de.fridgepay.internet.JsonParser;
import de.fridgepay.model.ListViewAdapter;
import de.fridgepay.model.ModelManager;
import de.fridgepay.model.Product;
import de.fridgepay.utils.Constants;

public class CartActivity extends AppCompatActivity {
    // Konstanten
    public static final String BOUGHT_PRODUCTS = "boughtProducts";
    public static final String COUNT = "count";

    private final Context context = this;
    private SharedPreferences prefs;
    private Button payButton;
    private ListView listView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cart);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        payButton = findViewById(R.id.payButton);

        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cancelNotification();
    }

    private void initView() {
        Map<String, Product> map;
        try {
            map = JsonParser.parseBoughtProducts(prefs.getString(BOUGHT_PRODUCTS, null));
        } catch (JSONException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, R.string.load_list_error, Toast.LENGTH_SHORT).show();
                }
            });
            map = new HashMap<>();
        }

        if (map.isEmpty()) {
            payButton.setEnabled(false);
            findViewById(R.id.noBoughtProducts).setVisibility(View.VISIBLE);
        }

        setPriceSum(map.values());

        initListView(map.values());
    }

    private void initListView(Collection<Product> values) {
        listView = findViewById(R.id.listView);
        listView.setAdapter(new ListViewAdapter((Activity) context, new ArrayList<>(values)));
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                returnProduct((Product) listView.getItemAtPosition(position));
                return true;
            }
        });
    }

    /**
     * Berechnet die Gesamtsumme des zu zahlenden Betrags und zeigt diesen an.
     * @param values Alle Produkte
     */
    private void setPriceSum(Collection<Product> values) {
        double sum = 0;
        for (Product product : values) {
            sum += (product.getCount() * Double.parseDouble(product.getPrice()));
        }

        String priceSum = String.format(Locale.getDefault(),
                "%s %.2f%s", getString(R.string.total), sum, ModelManager.getInstance().getCurrency());
        ((TextView) findViewById(R.id.priceSum)).setText(priceSum);
    }

    public void payProducts(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.pay_products_dialog_title)
                .setMessage(R.string.pay_products_dialog_msg)
                .setNeutralButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putString(BOUGHT_PRODUCTS, null).apply();
                        initView();
                        cancelNotification();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.pay_product_success, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
        builder.create().show();
    }

    /**
     * Fragt ob Kunde zurückgeben will und reduziert gegebenenfalls (oder entfernt) das Produkt.
     * @param returningProduct Produkt, das zurückgegeben werden soll
     */
    private void returnProduct(final Product returningProduct) {
        String msg = String.format("%s \"%s\"", getString(R.string.return_product_dialog_msg), returningProduct.getFullName());
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.return_product_dialog_title)
                .setMessage(msg)
                .setNeutralButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (CartActivity.updateProductCounter(context, returningProduct, false)) {
                            initView();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.return_product_success, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.return_product_error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
        builder.create().show();
    }

    private void cancelNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();
    }

    /**
     * Fügt das Produkt zur Liste der gekauften Produkte hinzu oder erhöht den Zähler und speichert
     * diese.
     * @param context     Context
     * @param product     Gekauftes Produkt
     * @param riseCounter True: Zähler wird erhöht; False: Zähler wird verringert
     * @return True bei Erfolg
     */
    public static boolean updateProductCounter(Context context, Product product, boolean riseCounter) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String boughtProductsJson = prefs.getString(BOUGHT_PRODUCTS, null);

        try {
            // Bisher gekauften Produkte holen
            Map<String, Product> map = JsonParser.parseBoughtProducts(boughtProductsJson);

            // Counter erhöhen oder neu hinzufügen
            Product value = map.get(product.getID());

            // Neue Anzahl berechnen
            int newCount;
            if (riseCounter) {
                newCount = value == null ? 1 : product.getCount() + 1;
            } else {
                newCount = value == null ? 0 : product.getCount() - 1;
            }

            // Produkt aktualisieren oder entfernen, falls Zähler kleiner 1 ist
            if (newCount > 0) {
                product.setCount(newCount);
                map.put(product.getID(), product);
            } else {
                map.remove(product.getID());
            }

            // Gekauften Produkte umwandeln und zurückgeben
            JSONObject jsonObject = new JSONObject();
            JSONArray products = new JSONArray();

            for (Map.Entry<String, Product> entry : map.entrySet()) {
                JSONObject object = entry.getValue().getJsonObject();
                object.put(COUNT, entry.getValue().getCount());
                products.put(object);
            }

            jsonObject.put(Constants.PRODUCTS, products);

            prefs.edit().putString(BOUGHT_PRODUCTS, jsonObject.toString()).apply();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
