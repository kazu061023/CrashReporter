package com.kazuapps.libs.crashreportermain;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class ReportSendAsyncTask extends AsyncTask<String, Void, String> {
	private Context mContext;

	public ReportSendAsyncTask(Context ctx) {
		mContext = ctx;
	}

	@Override
	protected void onPreExecute(){
		Toast.makeText(mContext, mContext.getResources().getString(R.string.crashReportMessageSending), Toast.LENGTH_SHORT).show();
	}

	
	@Override
	protected String doInBackground(String... params) {
		URI uri = null;
		try{
			uri = new URI(params[0]);
		}catch(URISyntaxException e){
			e.printStackTrace();
		}

		HttpPost request = new HttpPost(uri);

		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair("toMailAddress", params[1]));
		postParams.add(new BasicNameValuePair("crashDataTitle", params[2]));
		postParams.add(new BasicNameValuePair("crashDataBody", params[3]));
		postParams.add(new BasicNameValuePair("senderMailAdderss", params[4]));

		try {
			request.setEntity(new UrlEncodedFormEntity(postParams, "Shift-JIS"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		DefaultHttpClient httpClient = new DefaultHttpClient();
		String ret = null;
		try {
			ret = httpClient.execute(request, new ResponseHandler<String>() {
				@Override
				public String handleResponse(HttpResponse response) throws IOException{
					switch (response.getStatusLine().getStatusCode()) {
					case HttpStatus.SC_OK:
						return EntityUtils.toString(response.getEntity(), "UTF-8");
					default:
						return null;
					}
				}
			});
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			httpClient.getConnectionManager().shutdown();
		}
		
		return ret;
	}
	
	@Override
    protected void onPostExecute(String result){
		if(result != null){
			Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
		}
    }

}
