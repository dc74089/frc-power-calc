package com.vegetarianbaconite.powercalc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.vegetarianbaconite.powercalc.models.Match;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class SimpleTbaApi {
    private final String BASE_URL = "http://www.thebluealliance.com/api/v3";
    private OkHttpClient client;
    private String apiKey;
    private Gson gson = new Gson();

    public SimpleTbaApi(String apiKey) {
        client = new OkHttpClient();
        this.apiKey = apiKey;
    }

    public List<Match> getMatches(String compCode) {
        Response response = get(String.format("/event/%s/matches", compCode));
        Type type = new TypeToken<List<Match>>() {
        }.getType();

        try {
            return gson.fromJson(response.body().string(), type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private Response get(String path) {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .addHeader("X-TBA-Auth-Key", apiKey)
                .get()
                .build();

        try {
            return client.newCall(request).execute();
        } catch (IOException e) {
            return null;
        }
    }
}
