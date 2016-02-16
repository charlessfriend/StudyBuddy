 /**
  * © Copyright IBM Corporation 2015
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  **/

package com.ibm.watson.developer_cloud.android.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Vector;

import android.app.FragmentTransaction;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.app.ActionBar;
import android.app.Fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

// IBM Watson SDK
import com.ibm.watson.developer_cloud.android.AlchemyLanguage.AlchemyLanguage;
import com.ibm.watson.developer_cloud.android.rank_and_retrieve.rank_and_retrieve;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.dto.SpeechConfiguration;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.ISpeechDelegate;
import com.ibm.watson.developer_cloud.android.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.android.speech_common.v1.TokenProvider;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	TextView textTTS;

    ActionBar.Tab tabSTT, tabTTS;
    FragmentTabSTT fragmentTabSTT = new FragmentTabSTT();
    FragmentTabTTS fragmentTabTTS = new FragmentTabTTS();

    public static class FragmentTabSTT extends Fragment implements ISpeechDelegate {

        // session recognition results
        private static String mRecognitionResults = "";

        private enum ConnectionState {
            IDLE, CONNECTING, CONNECTED
        }

        ConnectionState mState = ConnectionState.IDLE;
        public View mView = null;
        public Context mContext = null;
        public JSONObject jsonModels = null;
        private Handler mHandler = null;

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            mView = inflater.inflate(R.layout.tab_stt, container, false);
            mContext = getActivity().getApplicationContext();
            mHandler = new Handler();
            String username = "175cc058-9844-40da-924f-7b53bd2800ce";
            String password = "xAmBJVqYcKBZ";
            String serviceURL = "https://stream.watsonplatform.net/text-to-speech/api";

            TextToSpeech.sharedInstance().initWithContext(this.getHost(serviceURL));
            TextToSpeech.sharedInstance().setCredentials(username, password);
            TextToSpeech.sharedInstance().setVoice("en-US_MichaelVoice");
            setText();
            if (initSTT() == false) {
                displayResult("Error: no authentication credentials/token available, please enter your authentication information");
                return mView;
            }

            /*if (jsonModels == null) {
                jsonModels = new STTCommands().doInBackground();
                if (jsonModels == null) {
                    displayResult("Please, check internet connection.");
                    return mView;
                }
            }*/
            addItemsOnSpinnerModels();

            displayStatus("please, press the button to start speaking");
            Button buttonRecord = (Button)mView.findViewById(R.id.buttonRecord);
            buttonRecord.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {

                    if (mState == ConnectionState.IDLE) {
                        mState = ConnectionState.CONNECTING;
                        Log.d(TAG, "onClickRecord: IDLE -> CONNECTING");
                        //Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerModels);
                        //spinner.setEnabled(false);
                        mRecognitionResults = "";
                        displayResult(mRecognitionResults);
                        SpeechToText.sharedInstance().setModel(getString(R.string.modelDefault));
                        displayStatus("connecting to the STT service...");
                        // start recognition
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... none) {
                                SpeechToText.sharedInstance().recognize();
                                return null;
                            }
                        }.execute();
                        setButtonLabel(R.id.buttonRecord, "Connecting...");
                        setButtonState(true);
                    } else if (mState == ConnectionState.CONNECTED) {
                        String serviceURL = "https://gateway.watsonplatform.net/retrieve-and-rank/api";
                        mState = ConnectionState.IDLE;
                        Log.d(TAG, "onClickRecord: CONNECTED -> IDLE");
                        Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerModels);
                        spinner.setEnabled(true);
                        SpeechToText.sharedInstance().stopRecognition();
                        rank_and_retrieve.sharedInstance().setClusterId("sc237ba773_d55f_4718_9bbf_8bcf12896f2e");
                        rank_and_retrieve.sharedInstance().setCollection_name("studybuddy_collection");
                        rank_and_retrieve.sharedInstance().setCredentials("5a342fb6-af11-4d8b-87fc-6a15461ef59d", "UDh37kVhNdG5");
                        rank_and_retrieve.sharedInstance().setHostURL(getHost(serviceURL));
                        rank_and_retrieve.sharedInstance().setRankerId("42B250x11-rank-913");
                        try {
                            gradeAnswer();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        setButtonState(false);
                    }
                }
            });

            return mView;
        }

        private void gradeAnswer() throws IOException, JSONException {
            Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerModels);
            String answerText = getAnswerText((String)spinner.getSelectedItem());
            //compareKeywords(answerText);
            TextView textView = (TextView)mView.findViewById(R.id.sttStatus);
            //textView.setText(compareKeywords(answerText));
            textView.setText("dsgdfg");

        }

        private int compareKeywords(String text) throws IOException, JSONException {
            String serviceURL = "http://gateway-a.watsonplatform.net/calls/text/TextGetRankedKeywords";

            AlchemyLanguage.sharedInstance().setHostURL(getHost(serviceURL));
            AlchemyLanguage.sharedInstance().setApikey("13ee4c0986c7593c937de62f1b577c4ca4745cc9");
            HttpResponse response = AlchemyLanguage.sharedInstance().extractKeywords(text);
            if (response == null) return 0;

            JSONObject object = getJSONObject(response.getEntity().getContent());
            JSONArray keywords = object.getJSONArray("keywords");
            ArrayList<String> keywordList = new ArrayList<String>();

            for(int i = 0; i < keywords.length() - 1; i++){
                keywordList.add(((JSONObject)keywords.get(i)).getString("text"));
            }

            HttpResponse userResponse = AlchemyLanguage.sharedInstance().extractKeywords(mRecognitionResults);
            if (userResponse == null) return 0;
            JSONObject userAnswerObject = getJSONObject(userResponse.getEntity().getContent());
            if(userAnswerObject.getString("status") == "ERROR") return 0;
            JSONArray userKeywords = userAnswerObject.getJSONArray("keywords");

            int matchingKeywordsCount = 0;
            for(int i = 0; i < userKeywords.length() - 1; i++){
                if (keywordList.contains(((JSONObject)keywords.get(i)).getString("text")))
                    matchingKeywordsCount++;
            }

            return matchingKeywordsCount / keywords.length() * 100;
        }

       /* private String getModelSelected() {

            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerModels);
            ItemModel item = (ItemModel)spinner.getSelectedItem();
            return item.getModelName();
        }*/

        public URI getHost(String url){
            try {
                return new URI(url);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return null;
        }

        // initialize the connection to the Watson STT service
        private boolean initSTT() {

            // DISCLAIMER: please enter your credentials or token factory in the lines below
            //String username = "212812da-2e3e-4275-8630-ba84973a3233";
            //String password = "XfNfTrvLokts";

            String username = "c8220206-5e25-407e-8ab4-00499e63bdd3";
            String password = "XlIgm119ibVI";

            String serviceURL = "wss://stream.watsonplatform.net/speech-to-text/api";

            SpeechConfiguration sConfig = new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_OGGOPUS);
            //SpeechConfiguration sConfig = new SpeechConfiguration(SpeechConfiguration.AUDIO_FORMAT_DEFAULT);

            SpeechToText.sharedInstance().initWithContext(this.getHost(serviceURL), getActivity().getApplicationContext(), sConfig);

            // Basic Authentication
            if (username.equals(getString(R.string.defaultUsername)) == false) {
                SpeechToText.sharedInstance().setCredentials(username, password);
            } else {
                // no authentication method available
                return false;
            }

            SpeechToText.sharedInstance().setModel(getString(R.string.modelDefault));
            SpeechToText.sharedInstance().setDelegate(this);

            return true;
        }

        protected void setText() {

            Typeface roboto = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "font/Roboto-Bold.ttf");
            Typeface notosans = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "font/NotoSans-Regular.ttf");

            // title
            TextView viewTitle = (TextView)mView.findViewById(R.id.title);
            String strTitle = getString(R.string.sttTitle);
            SpannableStringBuilder spannable = new SpannableStringBuilder(strTitle);
            spannable.setSpan(new AbsoluteSizeSpan(47), 0, strTitle.length(), 0);
            spannable.setSpan(new CustomTypefaceSpan("", roboto), 0, strTitle.length(), 0);
            viewTitle.setText(spannable);
            viewTitle.setTextColor(0xFF325C80);

            // instructions
            TextView viewInstructions = (TextView)mView.findViewById(R.id.instructions);
            String strInstructions = getString(R.string.sttInstructions);
            SpannableString spannable2 = new SpannableString(strInstructions);
            spannable2.setSpan(new AbsoluteSizeSpan(20), 0, strInstructions.length(), 0);
            spannable2.setSpan(new CustomTypefaceSpan("", notosans), 0, strInstructions.length(), 0);
            viewInstructions.setText(spannable2);
            viewInstructions.setTextColor(0xFF121212);
        }

        /*public class ItemModel {

            private JSONObject mObject = null;

            public ItemModel(JSONObject object) {
                mObject = object;
            }

            public String toString() {
                try {
                    return mObject.getString("description");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            public String getModelName() {
                try {
                    return mObject.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }*/

        private String[] arraySpinner;

        protected void addItemsOnSpinnerModels() {

            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerModels);
            /*int iIndexDefault = 0;

            JSONObject obj = jsonModels;
            ItemModel [] items = null;
            try {
                JSONArray models = obj.getJSONArray("models");

                // count the number of Broadband models (narrowband models will be ignored since they are for telephony data)
                Vector<Integer> v = new Vector<>();
                for (int i = 0; i < models.length(); ++i) {
                    if (models.getJSONObject(i).getString("name").indexOf("Broadband") != -1) {
                        v.add(i);
                    }
                }
                items = new ItemModel[v.size()];
                int iItems = 0;
                for (int i = 0; i < v.size() ; ++i) {
                    items[iItems] = new ItemModel(models.getJSONObject(v.elementAt(i)));
                    if (models.getJSONObject(v.elementAt(i)).getString("name").equals(getString(R.string.modelDefault))) {
                        iIndexDefault = iItems;
                    }
                    ++iItems;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (items != null) {
		        ArrayAdapter<ItemModel> spinnerArrayAdapter = new ArrayAdapter<ItemModel>(getActivity(), android.R.layout.simple_spinner_item, items);
		        spinner.setAdapter(spinnerArrayAdapter);
                spinner.setSelection(iIndexDefault);
            }*/

            this.arraySpinner = new String[] {
                    "what similarity laws must be obeyed when constructing aeroelastic models of heated high speed aircraft.",
                    "what are the structural and aeroelastic problems associated with flight of high speed aircraft.",
                    "what methods -dash exact or approximate -dash are presently available for predicting body pressures at angle of attack.",
                    "papers on internal /slip flow/ heat transfer studies.",
                    "are real-gas transport properties for air available over a wide range of enthalpies and densities.",
                    "what is the basic mechanism of the transonic aileron buzz.",
                    "papers on shock-sound wave interaction.",
                    "material properties of photoelastic materials.",
                    "hat application has the linear theory design of curved wings.",
                    "experimental results on hypersonic viscous interaction.",
                    "previous solutions to the boundary layer similarity equations",
                    "what is known regarding asymptotic solutions to the exact boundary layer equations.",
                    "what are wind-tunnel corrections for a two-dimensional aerofoil mounted off-centre in a tunnel.",
                    "what is the theoretical heat transfer rate at the stagnation point of a blunt body.",
                    "experimental techniques in shell vibration.",
                    "what are the aerodynamic interference effects on the fin lift and body lift of a fin-body combination."
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, arraySpinner);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerModels);
                    String text = (String)spinner.getSelectedItem();
                    TextToSpeech.sharedInstance().setVoice("en-US_MichaelVoice");
                    TextToSpeech.sharedInstance().synthesize(text);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }

            });
        }

        public void displayResult(final String result) {
            final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
                    TextView textResult = (TextView)mView.findViewById(R.id.textResult);
                    textResult.setText(result);
                }
            };

            new Thread(){
                public void run(){
                    mHandler.post(runnableUi);
                }
            }.start();
        }

        public void displayStatus(final String status) {
            /*final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
                    TextView textResult = (TextView)mView.findViewById(R.id.sttStatus);
                    textResult.setText(status);
                }
            };
            new Thread(){
                public void run(){
                    mHandler.post(runnableUi);
                }
            }.start();*/
        }

        /**
         * Change the button's label
         */
        public void setButtonLabel(final int buttonId, final String label) {
            final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
                    Button button = (Button)mView.findViewById(buttonId);
                    button.setText(label);
                }
            };
            new Thread(){
                public void run(){
                    mHandler.post(runnableUi);
                }
            }.start();
        }

        /**
         * Change the button's drawable
         */
        public void setButtonState(final boolean bRecording) {

            final Runnable runnableUi = new Runnable(){
                @Override
                public void run() {
                    int iDrawable = bRecording ? R.drawable.button_record_stop : R.drawable.button_record_start;
                    Button btnRecord = (Button)mView.findViewById(R.id.buttonRecord);
                    btnRecord.setBackground(getResources().getDrawable(iDrawable));
                }
            };
            new Thread(){
                public void run(){
                    mHandler.post(runnableUi);
                }
            }.start();
        }

        // delegages ----------------------------------------------

        public void onOpen() {
            Log.d(TAG, "onOpen");
            displayStatus("successfully connected to the STT service");
            setButtonLabel(R.id.buttonRecord, "Stop recording");
            mState = ConnectionState.CONNECTED;
        }

        public void onError(String error) {

            Log.e(TAG, error);
            //displayResult(error);
            mState = ConnectionState.IDLE;
        }

        public void onClose(int code, String reason, boolean remote) {
            Log.d(TAG, "onClose, code: " + code + " reason: " + reason);
            displayStatus("connection closed");
            setButtonLabel(R.id.buttonRecord, "Record");
            mState = ConnectionState.IDLE;
        }

        public void onMessage(String message) {

            Log.d(TAG, "onMessage, message: " + message);
            try {
                JSONObject jObj = new JSONObject(message);
                // state message
                if(jObj.has("state")) {
                    Log.d(TAG, "Status message: " + jObj.getString("state"));
                }
                // results message
                if (jObj.has("results")) {
                    //if has result
                    Log.d(TAG, "Results message: ");
                    JSONArray jArr = jObj.getJSONArray("results");
                    for (int i=0; i < jArr.length(); i++) {
                        JSONObject obj = jArr.getJSONObject(i);
                        JSONArray jArr1 = obj.getJSONArray("alternatives");
                        String str = jArr1.getJSONObject(0).getString("transcript");
                        // remove whitespaces if the language requires it
                        String model = "en-US_BroadbandModel";
                        if (model.startsWith("ja-JP") || model.startsWith("zh-CN")) {
                            str = str.replaceAll("\\s+","");
                        }
                        String strFormatted = Character.toUpperCase(str.charAt(0)) + str.substring(1);
                        if (obj.getString("final").equals("true")) {
                            String stopMarker = (model.startsWith("ja-JP") || model.startsWith("zh-CN")) ? "。" : ". ";
                            mRecognitionResults += strFormatted.substring(0,strFormatted.length()-1) + stopMarker;
                            displayResult(mRecognitionResults);
                        } else {
                            displayResult(mRecognitionResults + strFormatted);
                        }
                        break;
                    }
                } else {
                    //displayResult("unexpected data coming from stt server: \n" + message);
                }

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON");
                e.printStackTrace();
            }
        }

        public void onAmplitude(double amplitude, double volume) {
            //Logger.e(TAG, "amplitude=" + amplitude + ", volume=" + volume);
        }
    }

    @NonNull
    private static JSONObject getJSONObject(InputStream is) throws IOException, JSONException {
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder responseStrBuilder = new StringBuilder();
        String inputStr;
        while ((inputStr = streamReader.readLine()) != null)
            responseStrBuilder.append(inputStr);
        return new JSONObject(responseStrBuilder.toString());
    }

    public static String getAnswerText(String question) throws IOException, JSONException {
        HttpResponse response = rank_and_retrieve.sharedInstance().rankAndRetrieve(question);
        if(response == null) return "";

        JSONObject object = getJSONObject(response.getEntity().getContent());
        JSONObject resp = object.getJSONObject("response");
        JSONArray docs = resp.getJSONArray("docs");
        String allData = "";
        JSONObject jo = (JSONObject) docs.get(0);
        allData += ((JSONArray)jo.get("body")).get(0);
        return allData;
    }

    public static class FragmentTabTTS extends Fragment {

        public View mView = null;
        public Context mContext = null;
        public JSONObject jsonVoices = null;
        private Handler mHandler = null;

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.d(TAG, "onCreateTTS");
            mView = inflater.inflate(R.layout.tab_tts, container, false);
            mContext = getActivity().getApplicationContext();

            setText();
            if (initTTS() == false) {
                TextView viewPrompt = (TextView) mView.findViewById(R.id.prompt);
                viewPrompt.setText("Error: no authentication credentials or token available, please enter your authentication information");
                return mView;
            }

            if (jsonVoices == null) {
                jsonVoices = new TTSCommands().doInBackground();
                if (jsonVoices == null) {
                    return mView;
                }
            }
            addItemsOnSpinnerVoices();

            Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerVoices);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    String serviceURL = "https://gateway.watsonplatform.net/retrieve-and-rank/api";
                    Spinner spinner = (Spinner) mView.findViewById(R.id.spinnerVoices);
                    rank_and_retrieve.sharedInstance().setClusterId("sc237ba773_d55f_4718_9bbf_8bcf12896f2e");
                    rank_and_retrieve.sharedInstance().setCollection_name("studybuddy_collection");
                    rank_and_retrieve.sharedInstance().setCredentials("5a342fb6-af11-4d8b-87fc-6a15461ef59d", "UDh37kVhNdG5");
                    rank_and_retrieve.sharedInstance().setHostURL(getHost(serviceURL));
                    rank_and_retrieve.sharedInstance().setRankerId("42B250x11-rank-913");
                    try {
                        String answer = getAnswerText((String)spinner.getSelectedItem());
                        updatePrompt(answer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }
            });

            mHandler = new Handler();
            return mView;
        }

        public static String getAnswerText(String question) throws IOException, JSONException {
            HttpResponse response = rank_and_retrieve.sharedInstance().rankAndRetrieve(question);
            if(response == null) return "";

            JSONObject object = getJSONObject(response.getEntity().getContent());
            JSONObject resp = object.getJSONObject("response");
            JSONArray docs = resp.getJSONArray("docs");
            String allData = "";
            JSONObject jo = (JSONObject) docs.get(0);
            allData += ((JSONArray)jo.get("body")).get(0);
            return allData;
        }

        public URI getHost(String url){
            try {
                return new URI(url);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return null;
        }

        private boolean initTTS() {

            // DISCLAIMER: please enter your credentials or token factory in the lines below

            String username = "175cc058-9844-40da-924f-7b53bd2800ce";
            String password = "xAmBJVqYcKBZ";
            String serviceURL = "https://stream.watsonplatform.net/text-to-speech/api";

            TextToSpeech.sharedInstance().initWithContext(this.getHost(serviceURL));
            TextToSpeech.sharedInstance().setCredentials(username, password);
            TextToSpeech.sharedInstance().setVoice("en-US_MichaelVoice");

            return true;
        }

        protected void setText() {

            Typeface roboto = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "font/Roboto-Bold.ttf");
            Typeface notosans = Typeface.createFromAsset(getActivity().getApplicationContext().getAssets(), "font/NotoSans-Regular.ttf");

            TextView viewTitle = (TextView)mView.findViewById(R.id.title);
            String strTitle = getString(R.string.ttsTitle);
            SpannableString spannable = new SpannableString(strTitle);
            spannable.setSpan(new AbsoluteSizeSpan(47), 0, strTitle.length(), 0);
            spannable.setSpan(new CustomTypefaceSpan("", roboto), 0, strTitle.length(), 0);
            viewTitle.setText(spannable);
            viewTitle.setTextColor(0xFF325C80);

            TextView viewInstructions = (TextView)mView.findViewById(R.id.instructions);
            String strInstructions = getString(R.string.ttsInstructions);
            SpannableString spannable2 = new SpannableString(strInstructions);
            spannable2.setSpan(new AbsoluteSizeSpan(20), 0, strInstructions.length(), 0);
            spannable2.setSpan(new CustomTypefaceSpan("", notosans), 0, strInstructions.length(), 0);
            viewInstructions.setText(spannable2);
            viewInstructions.setTextColor(0xFF121212);
        }

        public class ItemVoice {

            public JSONObject mObject = null;

            public ItemVoice(JSONObject object) {
                mObject = object;
            }

            public String toString() {
                try {
                    return mObject.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        private String[] arraySpinner;
        public void addItemsOnSpinnerVoices() {
            this.arraySpinner = new String[] {
                    "what similarity laws must be obeyed when constructing aeroelastic models of heated high speed aircraft.",
                    "what are the structural and aeroelastic problems associated with flight of high speed aircraft.",
                    "what methods -dash exact or approximate -dash are presently available for predicting body pressures at angle of attack.",
                    "papers on internal /slip flow/ heat transfer studies.",
                    "are real-gas transport properties for air available over a wide range of enthalpies and densities.",
                    "what is the basic mechanism of the transonic aileron buzz.",
                    "papers on shock-sound wave interaction.",
                    "material properties of photoelastic materials.",
                    "hat application has the linear theory design of curved wings.",
                    "experimental results on hypersonic viscous interaction.",
                    "previous solutions to the boundary layer similarity equations",
                    "what is known regarding asymptotic solutions to the exact boundary layer equations.",
                    "what are wind-tunnel corrections for a two-dimensional aerofoil mounted off-centre in a tunnel.",
                    "what is the theoretical heat transfer rate at the stagnation point of a blunt body.",
                    "experimental techniques in shell vibration.",
                    "what are the aerodynamic interference effects on the fin lift and body lift of a fin-body combination."
            };

            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerVoices);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, arraySpinner);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerVoices);
                    String text = (String)spinner.getSelectedItem();
                    TextToSpeech.sharedInstance().setVoice("en-US_MichaelVoice");
                    TextToSpeech.sharedInstance().synthesize(text);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // your code here
                }

            });

        }

        // return the selected voice
        public String getSelectedVoice() {

            // return the selected voice
            Spinner spinner = (Spinner)mView.findViewById(R.id.spinnerVoices);
            ItemVoice item = (ItemVoice)spinner.getSelectedItem();
            String strVoice = null;
            try {
                strVoice = item.mObject.getString("name");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return strVoice;
        }

        public void updatePrompt(final String strAnswer) {
            TextView viewPrompt = (TextView)mView.findViewById(R.id.prompt);
            viewPrompt.setText(strAnswer);
        }
    }

    public class MyTabListener implements ActionBar.TabListener {

        Fragment fragment;
        public MyTabListener(Fragment fragment) {
            this.fragment = fragment;
        }

        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.replace(R.id.fragment_container, fragment);
        }

        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.remove(fragment);
        }

        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            // nothing done here
        }
    }


    public static class STTCommands extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... none) {

            return SpeechToText.sharedInstance().getModels();
        }
    }

    public static class TTSCommands extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... none) {

            return TextToSpeech.sharedInstance().getVoices();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Strictmode needed to run the http/wss request for devices > Gingerbread
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
				
        setContentView(R.layout.activity_tab_text);

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        tabSTT = actionBar.newTab().setText("Testing");
        tabTTS = actionBar.newTab().setText("Learning");

        tabSTT.setTabListener(new MyTabListener(fragmentTabSTT));
        tabTTS.setTabListener(new MyTabListener(fragmentTabTTS));

        actionBar.addTab(tabSTT);
        actionBar.addTab(tabTTS);


	}

    static class MyTokenProvider implements TokenProvider {

        String m_strTokenFactoryURL = null;

        public MyTokenProvider(String strTokenFactoryURL) {
            m_strTokenFactoryURL = strTokenFactoryURL;
        }

        public String getToken() {

            Log.d(TAG, "attempting to get a token from: " + m_strTokenFactoryURL);
            try {
                // DISCLAIMER: the application developer should implement an authentication mechanism from the mobile app to the
                // server side app so the token factory in the server only provides tokens to authenticated clients
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(m_strTokenFactoryURL);
                HttpResponse executed = httpClient.execute(httpGet);
                InputStream is = executed.getEntity().getContent();
                StringWriter writer = new StringWriter();
                IOUtils.copy(is, writer, "UTF-8");
                String strToken = writer.toString();
                Log.d(TAG, strToken);
                return strToken;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Play TTS Audio data
	 * 
	 * @param view
	 */
	public void playTTS(View view) throws JSONException {

        TextToSpeech.sharedInstance().setVoice("en-US_MichaelVoice");

		//Get text from text box
		textTTS = (TextView)fragmentTabTTS.mView.findViewById(R.id.prompt);
		String ttsText=textTTS.getText().toString();
		Log.d(TAG, ttsText);
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(textTTS.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);

		//Call the sdk function
		TextToSpeech.sharedInstance().synthesize(ttsText);
	}
}
