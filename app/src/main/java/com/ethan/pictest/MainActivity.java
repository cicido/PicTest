package com.ethan.pictest;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ImageView mPicSynthesisIV;
    private ImageView mPicOneIV;
    private ImageView mPicTwoIV;
    Bitmap bmp1 = null;
    Bitmap bmp2 = null;
    //String by1;
    //String by2;


    private static final String MODEL_FILE = "file:///android_asset/model1.pb";
    private static final String CONTENT_INPUT_NODE = "input_content:0";
    private static final String STYLE_INPUT_NODE = "input_style:0";
    private static final String OUTPUT_NODE = "output:0";


    private Combiner sc = null;
    private Executor executor = Executors.newSingleThreadExecutor();

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sc = StyleCombiner.create(
                            getAssets(),
                            MODEL_FILE,
                            CONTENT_INPUT_NODE,
                            STYLE_INPUT_NODE,
                            OUTPUT_NODE);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // load model
        initTensorFlowAndLoadModel();

        mPicSynthesisIV = (ImageView) findViewById(R.id.pic_synthesis);
        mPicOneIV = (ImageView) findViewById(R.id.pic_1);
        mPicOneIV.setTag("pic_1");
        mPicOneIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto(mPicOneIV);
            }
        });

        mPicTwoIV = (ImageView) findViewById(R.id.pic_2);
        mPicTwoIV.setTag("pic_2");
        mPicTwoIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto(mPicTwoIV);
            }
        });

        //执行合并
        findViewById(R.id.synthesis).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap =  Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
                mPicSynthesisIV.setImageBitmap(doSynthesis());

            }
        });
    }

    /*
    public static Bitmap getBitmapFromAsset(final Context context, final String filePath) {
        final AssetManager assetManager = context.getAssets();

        Bitmap bitmap = null;
        try {
            final InputStream inputStream = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (final IOException e) {
            LOGGER.e("Error opening bitmap!", e);
        }

        return bitmap;
    }
    */


    private Bitmap doSynthesis() {
        /*
        File pic_1 = new File(Environment.getExternalStorageDirectory(), mPicOneIV.getTag() + ".jpg");
        File pic_2 = new File(Environment.getExternalStorageDirectory(), mPicTwoIV.getTag() + ".jpg");


        Uri imageUri1 = Uri.fromFile(pic_1);
        Uri imageUri2 = Uri.fromFile(pic_2);

        ContentResolver cr = this.getContentResolver();

        Bitmap bmp1 = null;
        Bitmap bmp2 = null;
        try {
            bmp1 = BitmapFactory.decodeStream(new FileInputStream(pic_1.getAbsoluteFile()));
            bmp2 = BitmapFactory.decodeStream(new FileInputStream(pic_2.getAbsoluteFile()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        */
        return sc.combinePic(bmp1,bmp2);
    }

    private void takePhoto(ImageView iv) {
        File outputImage = new File(Environment.getExternalStorageDirectory(), iv.getTag() + ".jpg");
        /*
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        Uri imageUri = Uri.fromFile(outputImage);
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.putExtra("scale", true);
        intent.putExtra("crop", true);
        intent.setType("image/*");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        System.out.println("id:" + iv.getId());
        startActivityForResult(intent, getCode(iv));
    }

    private int getCode(ImageView iv){
        if(iv.getId() == mPicOneIV.getId()){
            return Short.MAX_VALUE - 1;
        } else {
            return Short.MAX_VALUE - 2;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri = data.getData();
        ContentResolver cr = this.getContentResolver();
        Bitmap bmp = null;
        try {
            /*
            InputStream input = cr.openInputStream(uri);
            int fileSize =  input.available();
            byte []  b =  new byte[fileSize];
            input.read(b, 0, fileSize);
            */
            bmp = BitmapFactory.decodeStream(cr.openInputStream(uri));
            if (requestCode == Short.MAX_VALUE - 1) {
                mPicOneIV.setImageBitmap(bmp);
                bmp1 = bmp;
                /*
                by1 = b.toString();

                File sdcard= Environment.getExternalStorageDirectory();
                File myfile=new File(sdcard,"log1.txt");
                FileOutputStream fos= null;
                try {
                    fos = new FileOutputStream(myfile);
                    OutputStreamWriter osw=new OutputStreamWriter(fos);
                    for(int i=0; i<b.length;i++){
                        osw.write(String.valueOf(b[i])+"\n");
                    }
                    osw.flush();
                    osw.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                */
            } else if (requestCode == Short.MAX_VALUE - 2) {
                mPicTwoIV.setImageBitmap(bmp);
                bmp2 = bmp;
                //by2 = b.toString();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
