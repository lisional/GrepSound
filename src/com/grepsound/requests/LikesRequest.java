package com.grepsound.requests;

import android.util.Log;
import com.grepsound.model.Profile;
import com.grepsound.model.Track;
import com.octo.android.robospice.request.SpiceRequest;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import com.soundcloud.api.Token;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by lisional on 2014-04-21.
 */
public class LikesRequest extends SpiceRequest<ArrayList<Track>> {

    private static String CLIENT_ID = "398a7f28d61b10d5ee14fcb8bff95d68";
    private static String CLIENT_SECRET = "a072bc979fa0a87ef880a529337e7f66";
    Token token;

    public LikesRequest(Token tok) {
        super(null);
        token = tok;
    }

    @Override
    public ArrayList<Track> loadDataFromNetwork() throws Exception {
        ApiWrapper wrapper = new ApiWrapper(CLIENT_ID, CLIENT_SECRET, null, token) ;

        HttpResponse resp = wrapper.get(Request.to("/me/favorites"));

        JSONArray result = new JSONArray(Http.getString(resp));

        ArrayList<Track> tracks = new ArrayList<Track>();

        for (int i = 0; i < result.length(); ++i) {
            Log.i("Profile", " : " + result.get(i).toString());
            tracks.add(new Track((JSONObject) result.get(i)));
        }
        //if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          //  System.out.println("\n" + Http.formatJSON(Http.getString(resp)));
        //}
        //ArrayList<Track> tracks = new Profile(resp);

        return tracks;
    }
}