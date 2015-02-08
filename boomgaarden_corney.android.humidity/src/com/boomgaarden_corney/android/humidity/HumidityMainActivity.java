package com.boomgaarden_corney.android.humidity;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class HumidityMainActivity extends Activity implements
		SensorEventListener {

	private final String DEBUG_TAG = "DEBUG_HUMIDITY";
	private final String SERVER_URL = "http://54.86.68.241/humidity/test.php";

	private TextView txtResults;
	private SensorManager sensorManager;

	private String errorMsg;

	private float humidityAccuracy;
	private float humidityValue0;
	private float humidityValue1;
	private float humidityValue2;
	private float humidityMaxRange = 0;
	private float humidityPower = 0;
	private float humidityResolution = 0;
	private int  humiditySensorType;
	private int numHumidityChanges = 0;
	private int humidityVersion = 0;
	private int humidityHashCode = 0;
	private Sensor mHumidity;
	private long humidityTimeStamp;
	private String humidityVendor;

	private List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsHumidity = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsSensor = new ArrayList<NameValuePair>();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_humidity_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);

		// Setup Humidity Manager and Provider
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mHumidity = sensorManager
				.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
		

		setDeviceData();
		showDeviceData();
		sendDeviceData();

		if (mHumidity == null){
			setErrorMsg("No Humidity Detected");
			showErrorMsg();
			sendErrorMsg();
		} else{
			setSensorData();
			showSensorData();
			sendSensorData();
		}
		

	}

	/* Request Humidity updates at startup */
	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, mHumidity, SensorManager.SENSOR_DELAY_NORMAL);

	}

	/* Remove the Humiditylistener updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (mHumidity != null) {
			if ((event.sensor.getType() == mHumidity.getType()) && numHumidityChanges < 10) {
				
				numHumidityChanges++;
				setHumidityData(event);
				showHumidityData();
				sendHumidityData();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.humidity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("HUMIDITY")) {
			writer.write(buildPostRequest(paramsHumidity));
			paramsHumidity = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		}  else if (postParameters.equals("SENSOR")) {
			writer.write(buildPostRequest(paramsSensor));
			paramsSensor = new ArrayList<NameValuePair>();
		}

		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();
		
		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Humidity
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setDeviceData() {
		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));
	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}

	private void setHumidityData(SensorEvent humidity) {
		humidityAccuracy = humidity.accuracy;
		humiditySensorType = humidity.sensor.getType();
		humidityTimeStamp = humidity.timestamp;
		humidityValue0 = humidity.values[0];
		humidityHashCode = humidity.hashCode();

		paramsHumidity.add(new BasicNameValuePair("Humidity Update Count",
				String.valueOf(numHumidityChanges)));
		paramsHumidity.add(new BasicNameValuePair("Accuracy", String
				.valueOf(humidityAccuracy)));
		paramsHumidity.add(new BasicNameValuePair("Sensor Type", String
				.valueOf(humiditySensorType)));
		paramsHumidity.add(new BasicNameValuePair("Time Stamp", String
				.valueOf(humidityTimeStamp)));
		paramsHumidity.add(new BasicNameValuePair(
				"values[0]: Relative ambient air humidity in percent", String
						.valueOf(humidityValue0)));
		paramsHumidity.add(new BasicNameValuePair(
				"Hash Code Value", String
						.valueOf(humidityHashCode)));
	}
	
	private void setSensorData() {
		humidityMaxRange = mHumidity.getMaximumRange();
		humidityPower = mHumidity.getPower();
		humidityResolution = mHumidity.getResolution();
		humidityVendor = mHumidity.getVendor();
		humidityVersion = mHumidity.getVersion();
		
		paramsSensor.add(new BasicNameValuePair("Max Range", String
						.valueOf(humidityMaxRange)));
		paramsSensor.add(new BasicNameValuePair("Power", String
				.valueOf(humidityPower)));
		paramsSensor.add(new BasicNameValuePair("Resolution", String
				.valueOf(humidityResolution)));
		paramsSensor.add(new BasicNameValuePair("Vendor", String
				.valueOf(humidityVendor)));
		paramsSensor.add(new BasicNameValuePair("Version", String
				.valueOf(humidityVersion)));
	}

	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");

		txtResults.append("\n");

	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void showHumidityData() {
		StringBuilder results = new StringBuilder();

		results.append("Humidity Update Count: "
				+ String.valueOf(numHumidityChanges) + "\n");
		results.append("Humidity Accuracy: " + String.valueOf(humidityAccuracy) + "\n");
		results.append("Humidity Sensor Type: " + String.valueOf(humiditySensorType) + "\n");
		results.append("Humidity Time Stamp: " + String.valueOf(humidityTimeStamp) + "\n");
		results.append("Humidity Vaule 0 (X axis): " + String.valueOf(humidityValue0) + "\n");
		results.append("Humidity Hash Code " + String.valueOf(humidityHashCode) + "\n");
		
		txtResults.append(new String(results));
		txtResults.append("\n");
	}
	
	private void showSensorData() {
		StringBuilder results = new StringBuilder();
		
		results.append("Max Range: " + String.valueOf(humidityMaxRange) + "\n");
		results.append("Power: " + String.valueOf(humidityPower) + "\n");
		results.append("Resolution: " + String.valueOf(humidityResolution) + "\n");
		results.append("Vendor: " + String.valueOf(humidityVendor) + "\n");
		results.append("Version: " + String.valueOf(humidityVersion) + "\n");
		
		txtResults.append(new String(results));
		txtResults.append("\n");
	}

	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Humidity info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Humidity info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendHumidityData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Humidity info
			new SendHttpRequestTask().execute(SERVER_URL, "HUMIDITY");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendSensorData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Humidity info
			new SendHttpRequestTask().execute(SERVER_URL, "SENSOR");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}
