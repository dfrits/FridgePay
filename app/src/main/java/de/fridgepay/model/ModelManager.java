package de.fridgepay.model;

import android.graphics.Bitmap;

import java.util.List;

/**
 * Verwaltet die Ergebnisse aus dem Json-String.
 */
public class ModelManager {
    private static ModelManager instance;
    private JsonResult jsonResult;

    private ModelManager() {

    }

    public static ModelManager getInstance() {
        if (instance == null) {
            instance = new ModelManager();
        }

        return instance;
    }

    private JsonResult getJsonResult() {
        return jsonResult == null ? new JsonResult() : jsonResult;
    }

    public void setJsonResult(JsonResult jsonResult) {
        this.jsonResult = jsonResult;
    }

    public String getCurrency() {
        return getJsonResult().getCurrency();
    }

    public Bitmap getPlaceHolder() {
        return getJsonResult().getPlaceHolder();
    }

    public List<Product> getProductList() {
        return getJsonResult().getProductList();
    }
}
