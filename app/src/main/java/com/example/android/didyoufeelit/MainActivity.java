
/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.didyoufeelit;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;
import static com.example.android.didyoufeelit.Utils.LOG_TAG;

/**
 * Displays the perceived strength of a single earthquake event based on responses from people who
 * felt the earthquake.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * URL for earthquake data from the USGS dataset
     */
    private static final String LOG_E = MainActivity.class.getSimpleName();
    private static final String USGS_REQUEST_URL =
            "http://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2016-01-01&endtime=2016-05-02&minfelt=50&minmagnitude=5";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendtask didYouFeelIt = new backendtask();

        didYouFeelIt.execute();

        // Update the information displayed to the user.
        //updateUi(earthquake);
    }

    private void updateUi(Event earthquake) {
        TextView titleTextView = (TextView) findViewById(R.id.title);
        titleTextView.setText(earthquake.title);

        TextView tsunamiTextView = (TextView) findViewById(R.id.number_of_people);
        tsunamiTextView.setText(getString(R.string.num_people_felt_it, earthquake.numOfPeople));

        TextView magnitudeTextView = (TextView) findViewById(R.id.perceived_magnitude);
        magnitudeTextView.setText(earthquake.perceivedStrength);
    }


    private class backendtask extends AsyncTask<URL, Void, Event> {

        @Override
        protected Event doInBackground(URL... urls) {
            URL url = createUrl(USGS_REQUEST_URL);

            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
            }
            Event earthquake = extractFeatureFromJson(jsonResponse);

            return earthquake;
        }

        @Override
        protected void onPostExecute(Event earthquake) {
            if (earthquake == null) {
                return;
            }
            updateUi(earthquake);
        }

        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.connect();

                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else {
                    Log.e(LOG_E, "Error Response code" + urlConnection.getResponseCode());
                }

            } catch (IOException e) {
                Log.e(LOG_E, "Error retriving the earthquake JSON Request", e);

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            return jsonResponse;

        }

        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }

            }
            return output.toString();
        }

        private Event extractFeatureFromJson(String jsonResponse) {
            try {
                JSONObject root = new JSONObject(jsonResponse);
                JSONArray featureArray = root.getJSONArray("features");

                if (featureArray.length() > 0) {
                    JSONObject firstFeature = featureArray.getJSONObject(0);
                    JSONObject properties = firstFeature.getJSONObject("properties");

                    String title = properties.getString("title");
                    String numberOfPeople = properties.getString("felt");
                    String perceivedStrength = properties.getString("cdi");

                    // Create a new {@link Event} object
                    return new Event(title, numberOfPeople, perceivedStrength);

                }

            } catch (JSONException e) {
                Log.e(LOG_E, "Problem parsing the earthquake JSON results", e);
            }
            return null;
        }


        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_E, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

    }
    // Perform the HTTP request for earthquake data and process the response.


    /**
     * Update the UI with the given earthquake information.
     */

}
