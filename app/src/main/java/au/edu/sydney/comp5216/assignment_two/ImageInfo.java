package au.edu.sydney.comp5216.assignment_two;

import android.graphics.Bitmap;
import android.net.Uri;


import java.io.File;
import java.util.Date;

public class ImageInfo {
    // A entity class save the image's information

    private String imagePath;  // Image's path
    private Date date;         // The last modified date
    private Uri uri;           // the item's url

    public ImageInfo (String imagePath) {
        this.imagePath = imagePath;
        File file = new File(imagePath);
        this.date = new Date(file.lastModified());
        this.uri = Uri.fromFile(new File(imagePath));
    }

    public Uri getUri() {
        return uri;
    }

    public String getImagePath() {
        return imagePath;
    }
}
