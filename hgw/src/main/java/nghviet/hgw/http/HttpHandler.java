package nghviet.hgw.http;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

public class HttpHandler {
    private static HttpHandler instance = null;

    private final Gson gson = new Gson();

    private HttpClient client = null;

    private String jwtToken = null;

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public static HttpHandler getInstance() {
        if(instance == null) instance = new HttpHandler();
        return instance;
    }

    public ArrayList<String> request(String method, String uri, final Map<String, String> body, boolean jwt) {
        HttpUriRequest req = null;
        try {
            if(method.equals("GET")) req = new HttpGet(uri);
            if(method.equals("POST")) req = new HttpPost(uri);

            if(req == null) return null;

            if(jwt) {
                if(jwtToken == null || jwtToken.length() == 0) return null;

                req.setHeader("Authorization", "Bearer " + jwtToken);
            }

            if(body != null && method.equals("POST")) {
                HttpPost temp = (HttpPost) req;
                temp.setEntity(new StringEntity(gson.toJson(body)));

            }

            req.setHeader("Accept", "application/json");
            req.setHeader("Content-type", "application/json");

            HttpResponse response = client.execute(req);

            if(response.getStatusLine().getStatusCode() != 200) return null;

            HttpEntity entity = response.getEntity();
            InputStream instream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(instream));

            ArrayList<String> r = new ArrayList<>();

            String line;
            while((line = reader.readLine()) != null) {
                r.add(line);
            }

            return r;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private HttpHandler() {
        client = HttpClientBuilder.create().build();
    }
}
