package com.ibm.watson.developer_cloud.android.AlchemyLanguage;

import android.util.Log;

import com.ibm.watson.developer_cloud.android.speech_common.v1.TokenProvider;
import com.ibm.watson.developer_cloud.android.text_to_speech.v1.TTSUtility;

import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by charles on 2/14/2016.
 */
public class AlchemyLanguage {

       protected static final String TAG = "RankAndRetrieve";

        private TTSUtility ttsUtility;
        private String username;
        private String password;
        private URI hostURL;
        private TokenProvider tokenProvider = null;
        private String apikey;
        private String text;
        private String outputMode = "json";

        /**Speech Recognition Shared Instance
         *
         */
        private static AlchemyLanguage _instance = null;

        public static AlchemyLanguage sharedInstance(){
            if(_instance == null){
                synchronized(AlchemyLanguage.class){
                    _instance = new AlchemyLanguage();
                }
            }
            return _instance;
        }

        /**
         * ttps://gateway.watsonplatform.net/retrieve-and-rank/api/v1/solr_clusters/sc1ca23733_faa8_49ce_b3b6_dc3e193264c6/solr/example-collection/fcselect?ranker_id=B2E325-rank-67&q=what%20is%20the%20basic%20mechanism%20of%20the%20transonic%20aileron%20buzz&wt=json&fl=id,title"
         * solr_cluster_id":"sc237ba773_d55f_4718_9bbf_8bcf12896f2e"
         * "ranker_id":"42B250x11-rank-913"
         */
        public HttpResponse extractKeywords(String query) {
            String server = this.hostURL.toString();
            try {
                return createPost(server, apikey, query);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public static HttpResponse createPost(String server, String apikey, String text) throws Exception {
            String url = server;

            //HTTP GET Client
            HttpClient httpClient = new DefaultHttpClient();
            //Add params
            List<BasicNameValuePair> params = new LinkedList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("apikey", apikey));
            params.add(new BasicNameValuePair("outputMode", "json"));
            params.add(new BasicNameValuePair("text", text));
            HttpGet httpGet = new HttpGet(url+"?"+ URLEncodedUtils.format(params, "utf-8"));
            httpGet.setHeader("accept","application/json");
            // use token based authentication if possible, otherwise Basic Authentication will be used
            /*if (token != null) {
                Log.d(TAG, "using token based authentication");
                httpGet.setHeader("X-Watson-Authorization-Token",token);
            } else {
                Log.d(TAG, "using basic authentication");
                httpGet.setHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
            }*/
            HttpResponse response = httpClient.execute(httpGet);
            return response;
        }

        private void buildAuthenticationHeader(HttpGet httpGet) {

            // use token based authentication if possible, otherwise Basic Authentication will be used
            if (this.tokenProvider != null) {
                Log.d(TAG, "using token based authentication");
                httpGet.setHeader("X-Watson-Authorization-Token",this.tokenProvider.getToken());
            } else {
                Log.d(TAG, "using basic authentication");
                httpGet.setHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(this.username, this.password), "UTF-8", false));
            }
        }

        /**
         * Set credentials
         * @param username
         * @param password
         */
        public void setCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        /**
         * Set host URL
         * @param hostURL
         */
        public void setHostURL(URI hostURL) {
            this.hostURL = hostURL;
        }

        public void setApikey(String apikey) {
            this.apikey = apikey;
        }

}
