package advisor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class Controller {

    public interface RequestMethod {
        void req(String accessToken, String apiServer, String category)
                throws IOException, InterruptedException;
    }

    // Default request command

    HttpResponse<String> request(final String accessToken, final String apiServer, final String uriPart) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .uri(URI.create(apiServer + "/v1/browse/" + uriPart))
                .GET()
                .build();

        return HttpClient
                .newBuilder()
                .build()
                .send(request,
                        HttpResponse.BodyHandlers.ofString());
    }

    // Gets the categories names from Spotify's API

    public void requestCategories(final String accessToken, final String apiServer, final String category) throws IOException, InterruptedException {
        String uriPart = "categories";
        String userRequest = "categories";

        HttpResponse<String> response =
                request(accessToken, apiServer, uriPart);

        String json = response.body();

        JsonObject joCategories = JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonObject(userRequest);

        List<String> categories = new Model().collectNames(joCategories);
        new View().printNames(categories);
    }

    // Gets the features from Spotify's API

    public void requestFeatured(final String accessToken, final String apiServer, final String category) throws IOException, InterruptedException {
        String uriPart = "featured-playlists";
        String userRequest = "playlists";

        HttpResponse<String> response =
                request(accessToken, apiServer, uriPart);

        String json = response.body();

        JsonObject joPlaylists = JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonObject(userRequest);

        Model model = new Model();

        List<String> playListsNames = model.collectNames(joPlaylists);
        List<String> playListsLinks = model.collectLinks(joPlaylists);

        new View().printNamesAndLinks(playListsNames, playListsLinks);
    }

    // Gets new releases from Spotify's API

    public void requestNew(final String accessToken, final String apiServer, final String category) throws IOException, InterruptedException {
        String uriPart = "new-releases";
        String userRequest = "albums";

        HttpResponse<String> response =
                request(accessToken, apiServer, uriPart);

        String json = response.body();

        JsonObject joAlbums = JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonObject(userRequest);

        Model model = new Model();

        List<String> names = model.collectNames(joAlbums);
        List<String> artists = collectArtists(joAlbums);
        List<String> links = model.collectLinks(joAlbums);

        new View().printNamesArtistsLinks(names, artists, links);
    }

    private static List<String> collectArtists(final JsonObject jo) {

        List<String> artists = new ArrayList<>();

        for (JsonElement item : jo.getAsJsonArray("items")) {
            for (JsonElement artist : item.getAsJsonObject()
                    .getAsJsonArray("artists")) {
                artists.add(artist.getAsJsonObject()
                        .get("name").getAsString());
            }
        }

        return artists;
    }

    // Gets playlists by providing a category name

    public void requestPlaylists(final String accessToken, final String apiServer, final String requestedCategory) throws IOException, InterruptedException {
        List<String> names = new ArrayList<>();
        List<String> links = new ArrayList<>();

        String uriPart = "categories";
        String userRequest = "categories";

        HttpResponse<String> response = request(accessToken, apiServer, uriPart);

        String json = response.body();

        JsonObject joCategories = JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonObject(userRequest);

        for (JsonElement item : joCategories
                .getAsJsonArray("items")) {

            String currentCategory = item.getAsJsonObject()
                    .get("name")
                    .getAsString();
            if (currentCategory.equals(requestedCategory)) {
                String categoryId = item.getAsJsonObject()
                        .get("id")
                        .getAsString();

                String uriPartForCategories = "categories/" + categoryId + "/playlists";

                HttpResponse<String> responseWithPlaylistInCategory =
                        request(accessToken, apiServer, uriPartForCategories);

                String playlistsInCategoryAsJson =
                        responseWithPlaylistInCategory.body();

                if (failedRequest(playlistsInCategoryAsJson)) {
                    return;
                }

                JsonObject joPlaylistsInCategory = JsonParser
                        .parseString(playlistsInCategoryAsJson)
                        .getAsJsonObject()
                        .getAsJsonObject("playlists");

                Model model = new Model();

                names = model.collectNames(joPlaylistsInCategory);
                links = model.collectLinks(joPlaylistsInCategory);
            }
        }

        checkCategory(names, links);
    }

    private boolean failedRequest(String playlistsInCategoryAsJson) {

        if (playlistsInCategoryAsJson.contains("error")) {

            JsonObject error = JsonParser
                    .parseString(playlistsInCategoryAsJson)
                    .getAsJsonObject()
                    .getAsJsonObject("error");
            System.out.println(error.get("message"));

            return true;
        }

        return false;
    }

    private static void checkCategory(List<String> names, List<String> links) {

        if (names.isEmpty()) {
            System.out.println("Unknown category name.");
        } else {
            new View().printNamesAndLinks(names, links);
        }
    }
}
