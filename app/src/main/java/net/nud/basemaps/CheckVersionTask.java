package net.nud.basemaps;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CheckVersionTask extends AsyncTask<String, String, String> {

    private OnTaskCompleted listener;

    public CheckVersionTask(OnTaskCompleted list) {
        listener = list;
    }

    @Override
    protected String doInBackground(String... uri) {

        String versionText;
        HttpURLConnection con;
        try {
            URL u = new URL(uri[0]);

            con = (HttpURLConnection) u.openConnection();

            InputStream in;

            try {
                in = con.getInputStream();
            } catch (Exception ex) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            String line;

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            while ((line = reader.readLine()) != null) {
                sb.append(line + " ");
            }

            versionText = sb.toString();

        } catch (Exception ex) {
            return null;
        }

        return versionText;
    }

    @Override
    protected void onPostExecute(String version) {
        if (version != null) {
            String toReturn = version.replace(" ", "");
            listener.onTaskCompleted(toReturn);
        }
    }

}
