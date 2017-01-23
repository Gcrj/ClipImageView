# ClipImageView
从一个图片中裁剪出一个正方形区域（多为选取头像使用）
### Compile
```
allprojects {
    repositories {
         ...
         maven { url "https://jitpack.io" }
         }
    }
```
```
dependencies {
    compile 'com.github.Gcrj:ClipImageView:0.0.1'
    }
```

### 使用方法
xml:
```
<com.gcrj.clipimageviewlibrary.ClipImageView
    android:id="@+id/clip_image_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```

java:
```
ClipImageView clipImageView = (ClipImageView) findViewById(R.id.clip_image_view);
clipImageView.setImageResource(R.drawable.test);
//clipImageView.setImageBitmap(bitmap);
clipImageView.setClipSize(200);//默认150
Bitmap bitmap = clipImageView.clip();
...
 ```

### 注意事项
放入ClipImageView中的bitmap大小要你自己控制，小心OOM！