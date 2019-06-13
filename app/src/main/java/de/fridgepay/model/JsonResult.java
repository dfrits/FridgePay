package de.fridgepay.model;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Beinhaltet die Daten aus dem Json-String der heruntergeladen wurde.
 */
public class JsonResult {
    // Allgemeine Daten
    private final Bitmap placeHolder;
    private final String currency;
    private final String volumeUnit;

    // Produktliste
    private final List<Product> productList;

    public JsonResult() {
        placeHolder = null;
        currency = "";
        volumeUnit = "";
        productList = new ArrayList<>();
    }

    public JsonResult(Bitmap placeHolder, String currency, String volumeUnit, List<Product> productList) {
        this.placeHolder = placeHolder;
        this.currency = currency;
        this.volumeUnit = volumeUnit;
        this.productList = productList;
    }

    Bitmap getPlaceHolder() {
        return placeHolder;
    }

    String getCurrency() {
        return currency;
    }

    String getVolumeUnit() {
        return volumeUnit;
    }

    List<Product> getProductList() {
        return productList;
    }
}
