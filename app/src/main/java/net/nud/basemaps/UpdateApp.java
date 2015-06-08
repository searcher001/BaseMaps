package net.nud.basemaps;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class UpdateApp extends AsyncTask<String, Void, Void> {
    private Context context;

    public void setContext(Context cont) {
        context = cont;
    }

    @Override
    protected Void doInBackground(String... arg0) {
        try {

            Log.i("File", "starting connection");

            URL url = new URL(arg0[0]);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();

            String temp = Environment.getExternalStorageDirectory() + File.separator + context.getResources().getString(R.string.basemap_local);

            Log.i("File" , "name of file is:" + temp);

            File file = new File(temp);

            Log.i("File", "created file");

            if (file.exists()) {
                Log.i("File", "File does exist!");
                file.delete();
            }

            Log.i("File" , "opening output file stream");

            FileOutputStream fos = new FileOutputStream(file);

            Log.i("File", "Capturing input stream");
            InputStream is = c.getInputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            fos.close();
            is.close();

            Log.i("File", "File should be created now. LAUNCHING INTENT");

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(temp)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

        } catch (Exception ex) {
            Log.e("Update", "EXCEPTION THROWN: " + ex.getMessage());
        }

        return null;
    }
}
