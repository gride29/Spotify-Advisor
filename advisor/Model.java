package advisor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Model {

    // Gets names of artists from Spotify's API

    public List<String> collectNames(final JsonObject jo) {

        List<String> names = new ArrayList<>();

        for (JsonElement item : jo.getAsJsonArray("items")) {

            String name = item.getAsJsonObject()
                    .get("name")
                    .getAsString();

            names.add(name);
        }

        return names;
    }

    // Gets links to albums from Spotify's API

    public List<String> collectLinks(final JsonObject jo) {

        List<String> links = new ArrayList<>();

        for (JsonElement item : jo.getAsJsonArray("items")) {

            String link = item.getAsJsonObject()
                    .getAsJsonObject("external_urls")
                    .get("spotify")
                    .getAsString();

            links.add(link);
        }

        return links;
    }

}
