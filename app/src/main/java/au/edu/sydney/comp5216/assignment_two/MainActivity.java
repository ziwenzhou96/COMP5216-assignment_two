package au.edu.sydney.comp5216.assignment_two;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;

import javax.xml.transform.Result;

public class MainActivity extends AppCompatActivity {

    public static final String APP_TAG = "TakePhoto";

    //request codes
    public final int TAKE_PHOTO_CODE = 648;

    // set permission
    MarshmallowPermission marshmallowPermission = new MarshmallowPermission(this);

    // define var
    private ArrayList<ImageInfo> imageList;
    private GridViewAdapter gridViewAdapter;
    private GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = (GridView)findViewById(R.id.gridview);
        readImage();
        gridViewAdapter = new GridViewAdapter(this, imageList);
        gridView.setAdapter(gridViewAdapter);

        Intent intent = new Intent(this, LongRunningService.class);
        startService(intent);
    }

    // read photo from local save
    public void readImage() {
        if (!marshmallowPermission.checkPermissionForReadfiles()) {
            marshmallowPermission.requestPermissionForReadfiles();
        } else {
            imageList = new ArrayList<ImageInfo>();
            Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, null, null, null);
            try {
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    ImageInfo imageInfo = new ImageInfo(path);
                    imageList.add(imageInfo);
                }
            } catch (Exception ex) {
                Log.e("read photo error", ex.getStackTrace().toString());
            }
        }
    }

    // handle take photo action
    public void onTakePhotoClick(View v) {
        // Check permissions
        if (!marshmallowPermission.checkPermissionForCamera()
                || !marshmallowPermission.checkPermissionForExternalStorage()) {
            marshmallowPermission.requestPermissionForCamera();
        } else {
            // create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivityForResult(intent,TAKE_PHOTO_CODE);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PHOTO_CODE) {
            if (resultCode == RESULT_OK) {
                ImageInfo imageInfo = new ImageInfo(data.getStringExtra("path"));
                imageList.add(imageInfo);
                gridViewAdapter.notifyDataSetChanged();
                backUpFile(imageInfo.getUri());
                Toast.makeText(MainActivity.this, "Back Up Successfully",Toast.LENGTH_SHORT).show();
            }
        }

    }

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();

    public void synchroniseFromCloud(View v){
        final StorageReference islandRef = storageRef.child(APP_TAG);
//        imageList.clear();
        islandRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        for (StorageReference item : listResult.getItems()) {
                            // All the items under listRef.
//                            File localFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
//                                    APP_TAG + File.separator + item.getName());

                            File rootPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), APP_TAG );
                            if(!rootPath.exists()) {
                                rootPath.mkdirs();
                            }
                            final File localFile = new File(rootPath,item.getName());
                            if(localFile.exists()){
                                System.out.println("skip++++++++++++++++++++++++");
                                continue;
                            }
                            item.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    // Local temp file has been created
                                    System.out.println(localFile.toString());
                                    imageList.add(new ImageInfo(localFile.getAbsolutePath()));
                                    gridViewAdapter = new GridViewAdapter(MainActivity.this, imageList);
                                    gridView.setAdapter(gridViewAdapter);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Uh-oh, an error occurred!
                    }
                });
    }

    public void backUpFile(Uri file) {
        System.out.println("Pictures"+File.separator+file.getLastPathSegment()+"++++++++++++++++++++++++++++");
        StorageReference fileRef = storageRef.child(APP_TAG+File.separator+file.getLastPathSegment());
        UploadTask uploadTask = fileRef.putFile(file);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
            }
        });
    }
}
