package de.fridgepay.model;

import org.json.JSONException;
import org.json.JSONObject;

import de.fridgepay.utils.Constants;

public class Product {
    private final String ID;
    private final String type;
    private final String name;
    private final String contentVolume;
    private final String price;
    private final String imagePath;
    private int count = 0;

    public Product(String ID, String type, String name, String contentVolume, String price, String imagePath) {
        this.ID = ID;
        this.type = type;
        this.name = name;
        this.contentVolume = contentVolume;
        this.price = price;
        this.imagePath = imagePath;
    }

    public String getID() {
        return ID;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getContentVolume() {
        return contentVolume;
    }

    public String getPrice() {
        return price;
    }

    public String getImagePath() {
        return imagePath;
    }

    public JSONObject getJsonObject() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(Constants.ID, getID());
        object.put(Constants.TYPE, getType());
        object.put(Constants.NAME, getName());
        object.put(Constants.VOLUME, getContentVolume());
        object.put(Constants.PRICE, getPrice());
        object.put(Constants.IMAGE_PATH, getImagePath());
        return object;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Wenn eine Volumenangabe vorhanden ist, wird diese angehängt.
     * @return Name mit Volumenangabe oder nur der Name
     */
    public String getFullName() {
        return getContentVolume().equals("-1") ? getName() : getName() + " " + getContentVolume();
    }
}
