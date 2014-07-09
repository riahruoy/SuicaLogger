package com.fuyo.suicalogger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class LogUploader extends IntentService {

	public LogUploader(String name) {
		super(name);
	}
	public LogUploader() {
		super("LogUploader");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String url = intent.getStringExtra("url");
		String[] keys = intent.getStringArrayExtra("paramKeys");
		String[] values = intent.getStringArrayExtra("paramValues");


		if (keys.length != values.length) {
			return;
		}

		HttpParams httpParams = new BasicHttpParams();
		httpParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, Integer.valueOf(1000));
		httpParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, Integer.valueOf(30000));
		HttpClient httpClient = new DefaultHttpClient(httpParams);

		HttpPost httpPost = new HttpPost(url);
		List<NameValuePair> param = new ArrayList<NameValuePair>();
		ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
	
			@Override
			public String handleResponse(HttpResponse response)
					throws ClientProtocolException, IOException {
				switch (response.getStatusLine().getStatusCode()) {
				case HttpStatus.SC_OK:
					String body = EntityUtils.toString(response.getEntity(), "UTF-8");
					return body;
				default:
					return "NG";
				}
			}

		};
		for (int i = 0; i < keys.length; i++) {
			param.add(new BasicNameValuePair(keys[i], values[i]));
		}
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(param, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		try {
			Log.d("intent", "connecting");
			httpClient.execute(httpPost, responseHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		httpClient.getConnectionManager().shutdown();
		param = null;
	}

}
