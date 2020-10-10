package advisor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import static advisor.Links.*;

class AuthSet {

    public static String accessToken;
    private static String authorizationCode;
    private static final HttpClient client = HttpClient.newBuilder().build();

    public static boolean authorize() throws IOException, InterruptedException {
        System.out.println("use this link to request the access code:");
        System.out.println("https://accounts.spotify.com/authorize"
                + "?client_id=" + CLIENT_ID
                + "&redirect_uri=" + REDIRECT_URI
                + "&response_type=" + RESPONSE_TYPE);
        System.out.println("waiting for code...");
        requestAccessCode();
        if (authorizationCode == null) {
            return false;
        }
        System.out.println("code received");
        System.out.println("making http request for access_token...");
        HttpResponse<String> response = requestAccessToken();
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        accessToken = json.get("access_token").getAsString();
        return response.statusCode() == 200;
    }

    public String returnToken(){
        return accessToken;
    }

    private static void requestAccessCode() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/",
                exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    String responseBody;
                    if (query != null && query.contains("code")) {
                        authorizationCode = query.substring(5);
                        latch.countDown();
                        responseBody = "Got the code. Return back to your program.";
                    } else {
                        latch.countDown();
                        responseBody = "Not found authorization code. Try again.";
                    }
                    exchange.sendResponseHeaders(200, responseBody.length());
                    exchange.getResponseBody().write(responseBody.getBytes());
                    exchange.getResponseBody().close();
                }
        );

        server.start();
        latch.await();
        server.stop(10);
    }

    private static HttpResponse<String> requestAccessToken() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=" + GRANT_TYPE
                                + "&code=" + authorizationCode
                                + "&redirect_uri=" + REDIRECT_URI
                                + "&client_id=" + CLIENT_ID
                                + "&client_secret=" + CLIENT_SECRET))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create("https://accounts.spotify.com/api/token"))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}


public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        Controller control = new Controller();
        AuthSet authSet = new AuthSet();
        boolean authorized = false;
        String accessToken = "";

        while (sc.hasNext()) {
            String userRequest = sc.next();
            switch (userRequest) {
                case "auth":
                    if (!authorized) {
                        authorized = AuthSet.authorize();
                        System.out.println(authorized ? "---SUCCESS---" : "---Failed---");
                        accessToken = authSet.returnToken();
                    } else {
                        System.out.println("Already authorized");
                    }
                    break;
                case "featured":
                    if (accessToken.isEmpty()) {
                        System.out.println(Links.ANSWER_DENIED_ACCESS);
                        break;
                    }

                    control.requestFeatured(accessToken, Links.API_SERVER, "");
                    break;
                case "new":
                    if (accessToken.isEmpty()) {
                        System.out.println(Links.ANSWER_DENIED_ACCESS);
                        break;
                    }

                    control.requestNew(accessToken, Links.API_SERVER, "");
                    break;
                case "categories":
                    if (accessToken.isEmpty()) {
                        System.out.println(Links.ANSWER_DENIED_ACCESS);
                        break;
                    }

                    control.requestCategories(accessToken, Links.API_SERVER, "");
                    break;
                case "playlists":
                    String category = sc.nextLine().trim();

                    if (accessToken.isEmpty()) {
                        System.out.println(Links.ANSWER_DENIED_ACCESS);
                        break;
                    }

                    control.requestPlaylists(accessToken, Links.API_SERVER, category);
                    break;
                case "exit":
                    System.out.println("---GOODBYE---");
                    break;
                default:
                    throw new
                            UnsupportedOperationException("Unknown Operation");
            }
        }
    }
}
