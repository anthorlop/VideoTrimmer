package es.anthorlop.videotrimmer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity {

    @InjectView(R.id.buttonSelect)
    Button buttonSelect;

    private static final int SELECT_PHOTO = 567843;

    private static final int VIDEO_RECORDERED = 452684;

    private String url_file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        buttonSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGalleryToSendFiles(v);
            }
        });

    }

    private void openGalleryToSendFiles(View view) {
        // create example file to save to OneDrive
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("video/*");
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == SELECT_PHOTO) {
            if (resultCode == RESULT_OK) {

                Uri selectedImage = data.getData();

                String filepath = getRealPathFromURI(selectedImage, this);

                if (filepath != null) {

                    File file = new File(Environment.getExternalStorageDirectory().getPath() + "/anthorlopCamera");
                    if (!file.exists()) {
                        file.mkdirs();
                    }

                    Date d = new Date();
                    String timestamp = String.valueOf(d.getTime());

                    url_file = Environment.getExternalStorageDirectory().getPath() + "/anthorlopCamera/preview_" + timestamp + ".mp4";

                    File original = new File(filepath);
                    File destino = new File(url_file);

                    try {
                        FileUtils.copyFile(original, destino);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    goToPreviewVideo();
                } else {
                    Toast.makeText(MainActivity.this, "Se ha producido en error", Toast.LENGTH_LONG).show();
                }
            }

        } else if (requestCode == VIDEO_RECORDERED) {
            if (resultCode == RESULT_OK) {

                String path = data.getExtras().getString(VideoRecorderedPlayerActivity.VIDEO_PATH);


                Toast.makeText(MainActivity.this, "El vídeo se ha editado correctamente '" + path + "'", Toast.LENGTH_LONG).show();

            } else if (resultCode == RESULT_CANCELED && data != null && data.getExtras() != null
                    && data.getExtras().containsKey(VideoRecorderedPlayerActivity.VIDEO_PATH)) {

                // el video ha sido editado y se vuelve a mostrar

                String path = data.getExtras().getString(VideoRecorderedPlayerActivity.VIDEO_PATH);

                // eliminamos el video inicial
                File orig = new File(path.replace("_edited.mp4", ".mp4"));
                if (orig.exists() && orig.isFile()) {
                    orig.delete();
                }

                // cambiamos el nombre del video editado por el inicial
                File nuevo = new File(path);
                nuevo.renameTo(new File(path.replace("_edited.mp4", ".mp4")));

                goToPreviewVideo();
            } else if (resultCode == RESULT_CANCELED) {

                Toast.makeText(MainActivity.this, "Se ha cancelado la edición del video", Toast.LENGTH_LONG).show();

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void goToPreviewVideo() {
        Intent intent = new Intent(this, VideoRecorderedPlayerActivity.class);
        Bundle extras = new Bundle();
        extras.putString(VideoRecorderedPlayerActivity.VIDEO_PATH, url_file);
        intent.putExtras(extras);
        startActivityForResult(intent, VIDEO_RECORDERED);
        //finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.reset(this);
    }

    public String getRealPathFromURI(Uri contentUri, Context context) {
        String videoPath = contentUri.getPath();
        try{
            String[] proj = { MediaStore.Video.Media.DATA };
            //Cursor cursor = activity.managedQuery(contentUri, proj, null, null,
            //null);
            Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            Log.i("cursor", "upload-->" + cursor);
            Log.i("contentUri", "upload-->" + contentUri);
            Log.i("proj", "upload-->" + proj);
            int position=0;

            if (cursor !=null && cursor.moveToPosition(position)) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                Log.i("column_index", "Upload-->" + column_index);
                videoPath = cursor.getString(column_index);  //I got a null pointer exception here.(But cursor hreturns saome value)
                Log.i("videoPath", "Upload-->" + videoPath);
                cursor.close();

            }

        } catch(Exception e){
            return contentUri.getPath();
        }

        return videoPath;

    }
}
