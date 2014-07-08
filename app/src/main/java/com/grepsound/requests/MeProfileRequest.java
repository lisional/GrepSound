package com.grepsound.requests;

import com.grepsound.activities.Api;
import com.grepsound.model.User;
import com.octo.android.robospice.request.SpiceRequest;
import com.soundcloud.api.Http;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.json.JSONObject;


/**
 * Created by lisional on 2014-04-18.
 */
public class MeProfileRequest extends SpiceRequest<User> {


    public MeProfileRequest(Class<User> clazz) {
        super(clazz);
    }

    @Override
    public User loadDataFromNetwork() throws Exception {
        HttpResponse resp = Api.wrapper.get(Request.to("/me"));
        JSONObject result = Http.getJSON(resp);
        return new User(result);
    }
}