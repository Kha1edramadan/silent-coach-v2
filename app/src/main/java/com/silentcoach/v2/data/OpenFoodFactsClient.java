package com.silentcoach.v2.data;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Read-only barcode lookup. Imported data never becomes verified automatically. */
public final class OpenFoodFactsClient {
    public static class Product {
        public String barcode="", name="", brand="";
        public double kcal, protein, carbs, fat, fiber;
        public boolean found;
    }
    public Product get(String barcode) throws Exception {
        if (barcode == null || !barcode.matches("\\d{7,14}")) throw new IllegalArgumentException("Invalid barcode");
        URL u = new URL("https://world.openfoodfacts.org/api/v3/product/" + normalize(barcode) + "?fields=code,product_name,brands,nutriments");
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setRequestMethod("GET"); c.setConnectTimeout(7000); c.setReadTimeout(7000);
        c.setRequestProperty("User-Agent", "SilentCoachV2/2.0 Android");
        int code = c.getResponseCode();
        BufferedReader r = new BufferedReader(new InputStreamReader(code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder(); String line; while ((line = r.readLine()) != null) b.append(line); r.close(); c.disconnect();
        Product p = new Product(); JSONObject root = new JSONObject(b.toString()); p.found = root.optInt("status",0) == 1;
        if (!p.found) return p;
        JSONObject x = root.optJSONObject("product"); if (x == null) return p;
        p.barcode=x.optString("code",barcode); p.name=x.optString("product_name",""); p.brand=x.optString("brands","");
        JSONObject n=x.optJSONObject("nutriments"); if(n!=null){p.kcal=n.optDouble("energy-kcal_100g",0);p.protein=n.optDouble("proteins_100g",0);p.carbs=n.optDouble("carbohydrates_100g",0);p.fat=n.optDouble("fat_100g",0);p.fiber=n.optDouble("fiber_100g",0);} return p;
    }
    public static String normalize(String x){String s=x.replaceFirst("^0+(?=\\d)","");if(s.length()<=7)while(s.length()<8)s="0"+s;else if(s.length()<=12)while(s.length()<13)s="0"+s;return s;}
}
