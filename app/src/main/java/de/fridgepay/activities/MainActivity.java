package de.fridgepay.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.json.JSONException;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import de.fridgepay.R;
import de.fridgepay.internet.JsonParser;
import de.fridgepay.model.JsonResult;
import de.fridgepay.model.ListViewAdapter;
import de.fridgepay.model.ModelManager;
import de.fridgepay.model.NotificationPublisher;
import de.fridgepay.model.Product;
import de.fridgepay.utils.Constants;

public class MainActivity extends AppCompatActivity implements JsonParser.Callback {
    private final Context context = this;
    private ListView listView;
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        ImageLoaderConfiguration loaderConfiguration = ImageLoaderConfiguration.createDefault(context);
        ImageLoader.getInstance().init(loaderConfiguration);

        listView = findViewById(R.id.listView);
        refreshLayout = findViewById(R.id.refreshLayout);

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadList();
            }
        });

        loadList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scheduleNotification();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();
    }

    /**
     * Startet den Parser.
     */
    private void loadList() {
        new JsonParser(this).execute(Constants.PRODUCT_SRC_URL);
    }

    @Override
    public void onDownloadComplete(JsonResult result) {
        refreshLayout.setRefreshing(false);

        if (listView == null) return;

        ModelManager modelManager = ModelManager.getInstance();
        modelManager.setJsonResult(result);

        listView.setAdapter(new ListViewAdapter((Activity) context, modelManager.getProductList()));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                buyProduct((Product) listView.getItemAtPosition(position));
            }
        });
    }

    @Override
    public void onError(Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, R.string.load_list_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Fragt den Kunden ob er es wirklich kaufen will und fügt das Produkt gegebenenfalls dem
     * Warenkorb hinzu.
     * @param product Zu kaufendes Produkt
     */
    private void buyProduct(final Product product) {
        String msg = String.format("%s \"%s\"", getString(R.string.buy_product_dialog_msg), product.getFullName());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.buy_product_dialog_title)
                .setMessage(msg)
                .setNeutralButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (CartActivity.updateProductCounter(context, product, true)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.buy_product_success, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.buy_product_error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
        builder.create().show();
    }

    public void mOpenCartClicked(MenuItem item) {
        Intent intent = new Intent(context, CartActivity.class);
        startActivity(intent);
    }

    /**
     * Erstellt den Notification channel ab API-Level 16.
     */
    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        CharSequence name = getString(R.string.channel_notify_name);
        String description = getString(R.string.channel_notify_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(Constants.PAY_NOTIFY_CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Setzt die Notification, falls notwendig, auf den letzten Werktag diesen Monats oder auf den
     * nächsten, falls dieser vor heute liegt.
     */
    private void scheduleNotification() {
        if (isNotificationNotNecessary()) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.PAY_NOTIFY_CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notify_payment_msg))
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher_round);

        Intent intent = new Intent(context, CartActivity.class);
        PendingIntent activity = PendingIntent.getActivity(context, Constants.PAY_NOTIFY_ID, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(activity);

        Intent notificationIntent = new Intent(context, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, Constants.PAY_NOTIFY_ID);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, builder.build());
        notificationIntent.setAction(NotificationPublisher.NOTIFICATION_ACTION);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, Constants.PAY_NOTIFY_ID,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + getDelay(Calendar.getInstance());
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    /**
     * Prüft ob der Warenkorb leer ist.
     * @return True, wenn der Warenkorb leer ist
     */
    private boolean isNotificationNotNecessary() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            Map<String, Product> map = JsonParser.parseBoughtProducts(prefs.getString(CartActivity.BOUGHT_PRODUCTS, null));
            return map.isEmpty();
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Berechnet die Differenz von heute zum nächsten letzen Werktag.
     * @param calendar Ausgangspunkt der Berechnung
     * @return Differenz von heute zum letzten nächsten Werktag
     */
    private long getDelay(Calendar calendar) {
        int lastDate = calendar.getActualMaximum(Calendar.DATE);
        calendar.set(Calendar.DATE, lastDate);

        return calculateDelay(calendar);
    }

    /**
     * Berrechnet den Unterschied von heute zu dem letzten Werktag diesen Monats oder des nächsten,
     * falls dieser vor dem heutigen Tag liegt.
     * @param calendar Der letzte Tag des Monats
     * @return Differenz von heute zum nächsten letzten Werktag
     */
    private long calculateDelay(Calendar calendar) {
        int lastDay = calendar.get(Calendar.DAY_OF_WEEK);
        switch (calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.GERMAN)) {
            case "Samstag":
                lastDay -= 1;
                break;
            case "Sonntag":
                lastDay -= 2;
                break;
        }
        calendar.set(Calendar.DAY_OF_WEEK, lastDay);

        long lastWorkingDayMillis = calendar.getTime().getTime();
        long currentTimeMillis = Calendar.getInstance().getTime().getTime();
        long delay = lastWorkingDayMillis - currentTimeMillis;

        if (delay < 0) {
            int currentMonth = calendar.get(Calendar.MONTH);
            int nextMonth;
            int nextYear;
            if (currentMonth == calendar.getMaximum(Calendar.MONTH)) {
                nextMonth = 0;
                nextYear = calendar.get(Calendar.YEAR) + 1;
            } else {
                nextMonth = currentMonth + 1;
                nextYear = calendar.get(Calendar.YEAR);
            }
            calendar.set(nextYear, nextMonth, 1);
            delay = getDelay(calendar);
        }

        return delay;
    }
}
