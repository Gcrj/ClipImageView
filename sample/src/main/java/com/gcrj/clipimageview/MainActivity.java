package com.gcrj.clipimageview;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.ImageView;

import com.gcrj.clipimageviewlibrary.ClipImageView;

public class MainActivity extends AppCompatActivity {

    private ClipImageView clipImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clipImageView = (ClipImageView) findViewById(R.id.clip_image_view);
        clipImageView.setImageResource(R.drawable.test);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Bitmap bitmap = clipImageView.clip();
            ImageView imageView = new ImageView(this);
            imageView.setPadding(100, 100, 100, 100);
            imageView.setImageBitmap(bitmap);
            new AlertDialog.Builder(this).setView(imageView).setTitle("The clip image is").setPositiveButton("Ok", null).setCancelable(false).show();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
