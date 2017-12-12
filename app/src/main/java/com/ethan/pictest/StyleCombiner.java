/*
 *    Copyright (C) 2017 MINDORKS NEXTGEN PRIVATE LIMITED
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.ethan.pictest;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.Tensor;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by amitshekhar on 16/03/17.
 */

/**
 * A classifier specialized to label images using TensorFlow.
 */
public class StyleCombiner implements Combiner {

    private static final String TAG = "StyleCombiner";

    private String inputContent;
    private String inputStyle;
    private String outputName;
    private int outSize = 512;

    // Pre-allocated buffers.
    private float[] outputs;
    private int [] intOutputs;
    private String[] outputNames;

    private TensorFlowInferenceInterface inferenceInterface;

    private StyleCombiner() {
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager  The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param inputContent     The input size. A square image of inputSize x inputSize is assumed.
     * @param inputStyle     The label of the image input node.
     * @param outputName    The label of the output node.
     * @throws IOException
     */
    public static Combiner create(
            AssetManager assetManager,
            String modelFilename,
            String inputContent,
            String inputStyle,
            String outputName)
            throws IOException {
        StyleCombiner c = new StyleCombiner();
        c.inputContent = inputContent;
        c.inputStyle = inputStyle;
        c.outputName = outputName;
        c.outputNames = new String[]{c.outputName};
        c.outputs = new float[c.outSize*c.outSize*3];
        c.intOutputs = new int[c.outSize*c.outSize];
;
        c.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);
        return c;
    }

    @Override
    public  Bitmap combinePic(Bitmap bmp1, Bitmap bmp2) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("combinePic");

        // Copy the input data into TensorFlow.
        Trace.beginSection("feedNodeContent");

        int[] cIntValues =  new int[bmp1.getWidth() * (bmp1.getHeight())];
        float[] cFloatValues = new float[bmp1.getWidth() * bmp1.getHeight()*3];
        int a  = bmp1.getPixel(0,0);
        int r = Color.red(a);
        int g = Color.green(a);
        int b = Color.blue(a);
        int aa = Color.rgb(r,g,b);
        int h = bmp1.getHeight();
        int w  = bmp1.getWidth();
        bmp1.getPixels(cIntValues, 0, bmp1.getWidth(), 0, 0, bmp1.getWidth(), bmp1.getHeight());
        for (int i = 0; i < cIntValues.length; ++i) {
            final int val = cIntValues[i];
            cFloatValues[i * 3] = ((val >> 16) & 0xFF) ;
            cFloatValues[i * 3 + 1] = ((val >> 8) & 0xFF) ;
            cFloatValues[i * 3 + 2] = (val & 0xFF);
        }

        /*手工填充
        */
        File sdcard= Environment.getExternalStorageDirectory();
        File myfile=new File(sdcard,"clog.txt");
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(myfile), "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            int i = 0;
            while ((line = br.readLine()) != null) {
                //cFloatValues[i] = Integer.parseInt(line.trim());
                cFloatValues[i] = Float.parseFloat(line.trim());
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        inferenceInterface.feed(
                inputContent, cFloatValues,1, bmp1.getHeight(),bmp1.getWidth(),3);

        Trace.endSection();

        // Copy the input data into TensorFlow.
        Trace.beginSection("feedNodeStyle");

        int[] sIntValues =  new int[bmp2.getWidth() * bmp2.getHeight()];
        float[] sFloatValues = new float[bmp2.getWidth() * bmp2.getHeight()*3];
        bmp2.getPixels(sIntValues, 0, bmp2.getWidth(), 0, 0, bmp2.getWidth(), bmp2.getHeight());
        for (int i = 0; i < sIntValues.length; ++i) {
            final int val = sIntValues[i];
            sFloatValues[i * 3] = ((val >> 16) & 0xFF);
            sFloatValues[i * 3 + 1] = ((val >> 8) & 0xFF);
            sFloatValues[i * 3 + 2] = (val & 0xFF);
        }


        myfile=new File(sdcard,"slog.txt");
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(myfile), "UTF-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            int i = 0;
            while ((line = br.readLine()) != null) {
                //sFloatValues[i] = Integer.parseInt(line.trim());
                sFloatValues[i] = Float.parseFloat(line.trim());
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        /*inferenceInterface.feed(
                inputStyle, sFloatValues, 1, bmp2.getHeight(),bmp2.getWidth(),3);


        /*
        手工填充
        */


        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("runInference");
        inferenceInterface.run(outputNames);
        Trace.endSection();

        // Copy the output Tensor back into the output array.
        Trace.beginSection("readNodeFloat");
        float [] outputs2 = new float[2097152];
        inferenceInterface.fetch(outputName, outputs2);


        File sdcard1= Environment.getExternalStorageDirectory();
        File myfile1=new File(sdcard,"log.txt");
        FileOutputStream fos= null;
        try {
            fos = new FileOutputStream(myfile1);
            OutputStreamWriter osw=new OutputStreamWriter(fos);
            for(int i=0; i<outputs2.length;i++){
                osw.write(String.valueOf(outputs2[i])+"\n");
            }
            osw.flush();
            osw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        for (int i = 0; i < intOutputs.length; ++i) {
            intOutputs[i] =
                    0xFF000000
                            | (((int) (outputs[i * 3])) << 16)
                            | (((int) (outputs[i * 3 + 1])) << 8)
                            | ((int) (outputs[i * 3 + 2]));
        }

        Bitmap bitmap =  Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(intOutputs, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        Trace.endSection();
        return bitmap;
    }

    @Override
    public void close() {
        inferenceInterface.close();
    }
}


