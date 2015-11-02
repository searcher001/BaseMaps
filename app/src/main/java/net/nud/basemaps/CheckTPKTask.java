package net.nud.basemaps;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class CheckTPKTask extends AsyncTask<String, String, String> {

    // VARIABLES
    private OnTaskCompleted listener;
    private String userName;
    private String password;
    private Boolean completed;
    private Activity activity;
    private Boolean waterUpdate;
    private Boolean sewerUpdate;
    private Boolean orthoUpdate;
    private Boolean connected;

    public CheckTPKTask(OnTaskCompleted list, String user, String pass, Activity act) {

        listener = list;
        userName = user;
        password = pass;
        completed = false;
        activity = act;
        waterUpdate = false;
        sewerUpdate = false;
        orthoUpdate = false;
        connected = false;
    }

    @Override
    protected String doInBackground(String... uri) {

        try {

            HttpURLConnection waterURL = (HttpURLConnection)(new URL(activity.getResources().getString(R.string.server_water_tpk)).openConnection());
            HttpURLConnection sewerURL = (HttpURLConnection)(new URL(activity.getResources().getString(R.string.server_sewer_tpk)).openConnection());
            HttpURLConnection orthoURL = (HttpURLConnection)(new URL(activity.getResources().getString(R.string.server_ortho_tpk)).openConnection());

            // local common directory
            String localDataDir = activity.getResources().getString(R.string.local_data) + File.separator + activity.getResources().getString(R.string.common_files);

            String waterPath = Environment.getExternalStorageDirectory() + File.separator + localDataDir + File.separator + activity.getResources().getString(R.string.local_water);
            String sewerPath = Environment.getExternalStorageDirectory() + File.separator + localDataDir + File.separator + activity.getResources().getString(R.string.local_sewer);
            String orthoPath = Environment.getExternalStorageDirectory() + File.separator + localDataDir + File.separator + activity.getResources().getString(R.string.ortho_photo);

            waterUpdate = checkForUpdate(waterURL, new File(waterPath));
            sewerUpdate = checkForUpdate(sewerURL, new File(sewerPath));
            orthoUpdate = checkForUpdate(orthoURL, new File(orthoPath));

        } catch (Exception ex) {
            return null;
        }

        return null;
    }

    @Override
    protected void onPostExecute(String str) {

        connected = (waterUpdate != null && sewerUpdate != null && orthoUpdate != null);
        listener.onTaskCompleted(connected, waterUpdate, sewerUpdate, orthoUpdate);
    }

    /**
     *  Check the URL against the file modified date
     * @param url - the packed HTTP connection
     * @param file - the file to compare against
     * @return - true if there is an update
     *              - returns null if unable to connect
     */
    private Boolean checkForUpdate(HttpURLConnection url, File file) {

        long lastModified;

        try {

            String userpass = "NUD\\" + userName + ":" + password;
            String basicAuth = "basic " + Base64.encodeToString(userpass.getBytes(), Base64.DEFAULT);
            url.setRequestProperty("Authorization", basicAuth);
            url.setRequestMethod("HEAD");
            url.setConnectTimeout(5000); // Set timeout to 5 seconds
            url.connect();

            // If not connected, return false
            if (url.getResponseCode() != 200) {
                return null;
            }

            lastModified = url.getLastModified();

        } catch (Exception ex) {
            Log.e("EXCEPTION", "checkForUpdate failed due to: " + ex.getMessage());
            return false;
        }

        Log.i("UPDATE", "Last modified: " + new Date(lastModified));
        Log.i("UPDATE", "file last modified: " + new Date(file.lastModified()));

        return (lastModified > file.lastModified());

    }

}
