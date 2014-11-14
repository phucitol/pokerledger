package org.pokerledger.pokerledgermobile;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.pokerledger.pokerledgermobile.helper.DatabaseHelper;
import org.pokerledger.pokerledgermobile.model.Session;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    private static final int ACTIVE_RESULT = 1;
    private static final int FINISHED_RESULT = 2;

    ListView list;
    //private ProgressBar pb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //pb.setVisibility(View.VISIBLE);

        int v = 0;
        try {
            v = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // Huh? Really?
        }
        new CheckUpdates(this).execute(v);


        list = (ListView)findViewById(R.id.list);

        new LoadActiveSessions().execute();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentManager manager = getFragmentManager();
                Gson gson = new Gson();

                Bundle b = new Bundle();
                b.putString("SESSION_JSON", gson.toJson(parent.getAdapter().getItem(position)));

                EditSessionFragment dialog = new EditSessionFragment();
                dialog.setArguments(b);
                dialog.show(manager, "EditSession");
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_settings :
                return true;
            case R.id.add_session :
                FragmentManager manager = getFragmentManager();

                AddSessionFragment dialog = new AddSessionFragment();
                dialog.show(manager, "AddSession");
                break;
            case R.id.history :
                Intent history = new Intent(this, HistoryActivity.class);
                this.startActivity(history);
                break;
            case R.id.statistics :
                Intent statistics = new Intent(this, StatisticsActivity.class);
                this.startActivity(statistics);
                break;
            //case R.id.sync :
                //start sync activity, displays username/password text inputs, login button, register button, forgot password button
                            //(have radio group(login, register, reset password) that displays different forms
                //login onclick

                    //if username == db.sync.username || db.sync.sync_num == 0
                        //validate username/password against website db
                        //if valid
                            //if website.sync.sync_num != db.sync.sync_num
                                //alert:This acct is currently synced with a different phone. Continuing will overwrite all session data currently on this phone
                                // and make this the primary phone
                                //if continue
                                    //wipe phone db
                                    //set db.username/password
                                    //DL xml of new acct records
                                    //parse and insert
                                    //increment website sync_num & set phone sync_num
                                //else
                                    //return to sync activity
                            //else
                                //upload xml of all unsynced sessions and set as synced
                        //else
                            //alert: the credentials you entered are not valid
                            //return to sync activity

                    //elseif username != db.sync.username
                        //alert: this phone is already synced with a different acct. If you continue all phone session data will be overwritten.
                        //if continue
                            //validate username/password against website db
                            //if valid
                                //wipe phone db
                                //set db.username/password
                                //DL xml of new acct records
                                //parse and insert
                                //increment website sync_num & set phone sync_num
                            //else
                                //alert: the credentials you entered are not valid
                        //else
                            //return to sync activity

                //register onclick
                    //check if username/password are valid
                    //if valid
                        //if db.sync.sync_num == 0
                            //create new user on website
                            //set db.username/password
                            //increment sync_num on website
                            //set db.sync.sync_num to website
                            //upload xml of all unsynced sessions and set as synced
                        //else
                            //alert: this phone is already synced with a different acct. If you continue all phone session data will be overwritten.
                            //if continue
                                //create new user on website
                                //set db.username/password
                                //increment sync_num on website
                                //set db.sync.sync_num to website
                                //wipe phone
                            //else
                                //return to sync activity
                    //else
                        //alert: The username password are do not meet X criteria
                        //return to sync activity

                //forgot password onlick
                    //alertdialog asking for acct email
                    //if email is in records
                        //set website password to temp password
                        //email temp password
                    //else
                        //alert: acct not found

                //break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String json = "";
            json = data.getStringExtra("SESSION_JSON");

            if (requestCode == ACTIVE_RESULT) {
                new SaveActiveSession(new Gson().fromJson(json, Session.class)).execute();
            }
            else if (requestCode == FINISHED_RESULT) {
                new SaveFinishedSession(new Gson().fromJson(json, Session.class)).execute();
            }
        }
    }

    protected void notifyListChange() {
        new LoadActiveSessions().execute();
    }

    public class LoadActiveSessions extends AsyncTask<Void, Void, Void> {
        ArrayList<Session> sessions = new ArrayList<Session>();

        public LoadActiveSessions() {}

        @Override
        protected Void doInBackground(Void... params) {
            DatabaseHelper db;
            db = new DatabaseHelper(getApplicationContext());
            sessions = db.getSessions(1);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            SessionListAdapter adapter = new SessionListAdapter(MainActivity.this, sessions);

            list.setAdapter(adapter);
        }
    }

    public class SaveActiveSession extends AsyncTask<Void, Void, Void> {
        Session current;

        public SaveActiveSession(Session s) {
            this.current = s;
        }

        @Override
        protected Void doInBackground(Void... params) {
            DatabaseHelper db;
            db = new DatabaseHelper(getApplicationContext());
            db.saveSession(this.current);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            new LoadActiveSessions().execute();
        }
    }

    public class SaveFinishedSession extends AsyncTask<Void, Void, Void> {
        Session current;

        public SaveFinishedSession(Session s) {
            this.current = s;
        }

        @Override
        protected Void doInBackground(Void... params) {
            DatabaseHelper db;
            db = new DatabaseHelper(getApplicationContext());
            db.saveSession(this.current);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            new LoadActiveSessions().execute();
        }
    }

    private class CheckUpdates extends AsyncTask<Integer, Integer, String>{
        private Context mContext;
        private ProgressDialog pdia;

        public CheckUpdates (Context c) {
            this.mContext = c;
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            pdia = new ProgressDialog(mContext);
            pdia.setMessage("Checking for update...");
            pdia.show();
        }

        @Override
        protected String doInBackground(Integer... params) {
            return postData(params[0]);
        }

        @Override
        protected void onPostExecute(final String responseString){
            pdia.dismiss();

            if (!responseString.equals("")) {
                AlertDialog dialog = new AlertDialog.Builder(mContext).create();
                dialog.setTitle("Confirmation");
                dialog.setMessage("There is an update. Download and Install?");
                dialog.setCancelable(false);
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonId) {
                        new InstallUpdate(mContext).execute("http://layout.pokerledger.org/apks/"+responseString);
                    }
                });
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonId) {
                    }
                });
                dialog.setIcon(android.R.drawable.ic_dialog_alert);
                dialog.show();
            }
            //new InstallUpdate(mContext).execute("http://layout.pokerledger.org/apks/version0-7-3.apk");
        }

        public String postData(Integer version) {
            HttpClient httpclient = new DefaultHttpClient();
            // specify the URL you want to post to
            HttpPost httppost = new HttpPost("http://layout.pokerledger.org/updatecheck.php");
            HttpResponse response = null;
            try {
                // create a list to store HTTP variables and their values
                List nameValuePairs = new ArrayList();
                // add an HTTP variable and value pair
                nameValuePairs.add(new BasicNameValuePair("currentVersion", Integer.toString(version)));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                // send the variable and value, in other words post, to the URL
                response = httpclient.execute(httppost);
            } catch (ClientProtocolException e) {
                // process execption
            } catch (IOException e) {
                // process execption
            }

            HttpEntity entity = response.getEntity();
            String responseString = "";
            try {
                responseString = EntityUtils.toString(entity, "UTF-8");
            } catch (IOException e) {
                //really?
            }

            return responseString;
        }
    }

    public class InstallUpdate extends AsyncTask<String, Integer, String> {
        private Context mContext;
        private ProgressDialog pdia;

        public InstallUpdate (Context c) {
            this.mContext = c;
        }

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            pdia = new ProgressDialog(mContext);
            pdia.setMessage("Downloading update");
            pdia.setIndeterminate(false);
            pdia.setMax(100);
            pdia.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pdia.setCancelable(true);
            pdia.show();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            String path = getFilesDir().getAbsolutePath() + "/plm_update";

            try {
                URL url = new URL(sUrl[0]);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.connect();

                int fileLength = connection.getContentLength(); //is returning -1 because the content-length header field is not set probably

                // download the file
                InputStream input = new BufferedInputStream(url.openStream());
                //OutputStream output = new FileOutputStream(path);
                FileOutputStream output = mContext.openFileOutput("plm_update.apk", Context.MODE_WORLD_READABLE|Context.MODE_WORLD_WRITEABLE);

                byte data[] = new byte[1024];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
                Log.e("YourApp", "Well that didn't work out so well...");
                Log.e("YourApp", e.getMessage());
            }
            return path;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            Log.v("progress", Integer.toString(progress[0]));
            pdia.setProgress(progress[0]);
        }

        // begin the installation by opening the resulting file
        @Override
        protected void onPostExecute(String path) {
            pdia.dismiss();

            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(new File(getFilesDir() + "/plm_update.apk")), "application/vnd.android.package-archive" );
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Log.d("Lofting", "About to install new .apk");
            mContext.startActivity(i);
        }
    }

}
