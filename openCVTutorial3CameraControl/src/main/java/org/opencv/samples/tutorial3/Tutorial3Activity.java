package org.opencv.samples.tutorial3;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;
import static org.opencv.imgproc.Imgproc.remap;

public class Tutorial3Activity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "OCVSample::Activity";
    private static final int OPEN_REQUEST_CODE = 41;

    private static final int COLOR_BLUE = 0;
    private static final int COLOR_GREEN = 1;
    private static final int COLOR_RED = 2;

    private static final int[] VALUE= new int[]{10,20,50,100,500,1000};


    private Tutorial3View mOpenCvCameraView;
    private KeyPoint[] keypoints;
    private FeatureDetector featureDetector;
    private DescriptorExtractor descriptorExtractor;
    private Mat mRgba;
    private MatOfKeyPoint sceneDescriptors;
    private DescriptorMatcher descriptorMatcher;
    private FeatureDetector fd2;

    private MatOfKeyPoint objectDescriptors;

//    private Mat colorMat;
    private Mat colorMatBlue;
    private Mat colorMatGreen;
    private Mat colorMatRed;

    private Mat mRgbaClone;

    private Mat colorLabel;
    private Mat colorLabel2;
    private Scalar mBlobColorHsv;
    private Scalar mBlobColorHsv2;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorRgba2;

    int hsvType;
    int hsvType2;
    int[] type;

    private MatOfKeyPoint[] preloadedobjectDescriptors;



    private TextToSpeech tts;
    private Thread detectCamera = new DetectCamera();

    /*
    private ImageView imageview;
    private ImageView imageview2;
    private ImageView imageview3;
    private ImageView imageview4;
    private ImageView imageview5;
*/



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(Tutorial3Activity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public Tutorial3Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback)) {
            Log.e("TEST", "Cannot connect to OpenCV Manager");
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.tutorial3_surface_view);
        mOpenCvCameraView = (Tutorial3View) findViewById(R.id.tutorial3_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        createLanguageTTS();
        Thread detectCamera = new DetectCamera();
        if (objectDescriptors == null) openFile(mOpenCvCameraView);


    }

    @Override
    protected void onStart()
    {
        super.onStart();
        /*
        imageview = (ImageView) findViewById(R.id.imageview);
        imageview2 = (ImageView) findViewById(R.id.imageview2);
        imageview3 = (ImageView) findViewById(R.id.imageview3);
        imageview4 = (ImageView) findViewById(R.id.imageview4);
        imageview5 = (ImageView) findViewById(R.id.imageview5);
        */
    }


    private void createLanguageTTS() {
        if (tts == null) {
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int arg0) {
                    // TTS 初始化成功
                    if (arg0 == TextToSpeech.SUCCESS) {
                        // 指定的語系: 英文(美國)
                        Locale l = Locale.US;  // 不要用 Locale.ENGLISH, 會預設用英文(印度)

                        // 目前指定的【語系+國家】TTS, 已下載離線語音檔, 可以離線發音
                        if (tts.isLanguageAvailable(l) == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                            tts.setLanguage(l);
                        }
                    }
                }
            }
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (tts != null)
            tts.shutdown();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        detectCamera.destroy();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
/*
        Mat colorLabel = mRgba.submat(4, 68, 4, 68);
        if (mBlobColorRgba!=null)
        colorLabel.setTo(mBlobColorRgba);
*/
        colorLabel = mRgba.submat(4, 128, 4, 128);
        if (mBlobColorRgba!=null) colorLabel.setTo(mBlobColorRgba);
        colorLabel2 = mRgba.submat(4,128,132,132+128);
        if (mBlobColorRgba2!=null) colorLabel2.setTo(mBlobColorRgba2);

        Imgproc.rectangle(mRgba,new Point(mRgba.width()/ 6,mRgba.height()/6),new Point(mRgba.width()/ 6*5 ,mRgba.height()/6*5),new Scalar(128,0,0),1);
        Imgproc.rectangle(mRgba,new Point(mRgba.width()/2 - 120,mRgba.height()/2 - 30),new Point(mRgba.width()/2 -60 ,mRgba.height()/2 - 30 +60),new Scalar(128,128,128),1);
        Imgproc.rectangle(mRgba,new Point(mRgba.width()/2 + 60,mRgba.height()/2 - 30),new Point(mRgba.width()/2 +120 ,mRgba.height()/2 - 30 +60),new Scalar(128,128,128),1);

//        Imgproc.rectangle(touch);


        /*
        if (colorLabel!=null) {
            colorLabel2 = mRgba.submat(4, 4 + colorLabel.rows(), 132, 132 + colorLabel.cols());
            colorLabel2.setTo(mBlobColorRgba2);
        }

        */

//        if (mBlobColorRgba2!=null) colorLabel2.setTo(mBlobColorRgba2);

//        Mat aMat = mRgba.submat(132, 128, 4, 128);
//        if (mRgbaClone!=null)         aMat.setTo(mRgbaClone);
//     colorLabel.setTo(new Scalar(0,1,0));

        return mRgba;
    }

    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch event");
//        Toast.makeText(this, "touched", Toast.LENGTH_SHORT).show();
        mOpenCvCameraView.findFocus();
        mOpenCvCameraView.enableFpsMeter();


        Rect touchedRect = new Rect();

        touchedRect.x = mRgba.width()/2 - 120;
        touchedRect.y = mRgba.height()/2 - 30;

        touchedRect.width=60;
        touchedRect.height=60;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.medianBlur(touchedRegionRgba,touchedRegionRgba,5);

        Imgproc.cvtColor(touchedRegionRgba,touchedRegionHsv,Imgproc.COLOR_RGB2HSV_FULL);
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);

        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;
        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        double mBlue = mBlobColorRgba.val[0];
        double mGreen = mBlobColorRgba.val[1];
        double mRed = mBlobColorRgba.val[2];


        Log.i(TAG, "mBlue = "+mBlue);
        Log.i(TAG, "mGreen = "+mGreen);
        Log.i(TAG, "mRed = "+mRed);

        Rect touchedRect2 = new Rect();

        touchedRect2.x = mRgba.width()/2+120;
        touchedRect2.y = mRgba.height()/2 - 30;

        touchedRect2.width=60;
        touchedRect2.height=60;

        Mat touchedRegionRgba2 = mRgba.submat(touchedRect2);

        Mat touchedRegionHsv2 = new Mat();

        Imgproc.medianBlur(touchedRegionRgba2,touchedRegionRgba2,5);

        Imgproc.cvtColor(touchedRegionRgba2,touchedRegionHsv2,Imgproc.COLOR_RGB2HSV_FULL);
        mBlobColorHsv2 = Core.sumElems(touchedRegionHsv2);

        pointCount = touchedRect2.width * touchedRect2.height;
        for (int i = 0; i < mBlobColorHsv2.val.length; i++)
            mBlobColorHsv2.val[i] /= pointCount;
        mBlobColorRgba2 = converScalarHsv2Rgba(mBlobColorHsv2);

        hsvType = hsvDetermine(mBlobColorHsv);
        hsvType2 = hsvDetermine(mBlobColorHsv2);

        if (hsvType == hsvType2){
            Toast.makeText(this, "The 2 hue matched! result = "+hsvType, Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "The 2 hue does not matched.", Toast.LENGTH_SHORT).show();
        }

        detectCamera.run();
//        Log.i(TAG, "detectCamera.run();");
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        Uri currentUri = null;

        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == OPEN_REQUEST_CODE) {
                if (resultData != null) {

                    currentUri = resultData.getData();

                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currentUri);

                        Mat objectImage = new Mat();
                        Utils.bitmapToMat(bitmap, objectImage);
                        objectDescriptors = new MatOfKeyPoint();
                        objectImage.copyTo(objectDescriptors);
                        System.out.println("objectDescriptors = " + objectDescriptors);
                        Imgproc.cvtColor(objectDescriptors, objectDescriptors, Imgproc.COLOR_BGR2GRAY);
                        if (objectDescriptors.type() != CV_32F) {
                            objectDescriptors.convertTo(objectDescriptors, CV_32F);
                        }

                        Toast.makeText(this, "descriptorExtractor success", Toast.LENGTH_SHORT).show();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    private int hsvDetermine(Scalar hsvScalar){
        /*
                return reference :
                $10 : 0
                $20 : 1
                $50 : 2
                $100 : 3
                $500 : 4
                $1000 : 5
                no match : -1
                */
        double hue = hsvScalar.val[0];
        double sat = hsvScalar.val[1];

        System.out.println("");
        System.out.println("input hue: hue = "+hue+", sat = "+sat+", v = "+hsvScalar.val[2]);

        if ((hue > 170 && hue < 220) && (sat >60 && sat < 135)){
            return 0;
        }
        if ((hue > 120 && hue < 170)&& (sat >25 && sat < 120)){
            return 1;
        }
        /*
        if (hue > 170 || hue < 25){
            System.out.println("hue > 170 || hue < 25");
            System.out.println("hue = "+hue+", sat = "+sat);
            if (sat > 0 && sat < 20){
                return 3;
            }
            if (hue < 180 && sat>15 && sat <40){
                return 0;
            }

            */
        /*
        if (hue > 340 || hue < 50){
            return 3;
        }
        */


        System.out.println("hue going to -1");
        System.out.println("hue = "+hue+", sat = "+sat+", v = "+hsvScalar.val[2]);
        return -1;
    };


    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }


    public void openFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    public void initDataSet() {
        MatOfKeyPoint[] objectDescriptorsSet = new MatOfKeyPoint[]{};





    }

    class DetectCamera extends Thread {

        @Override
        public void run() {
            super.run();
            int status = 0;
            if (hsvType != hsvType2 || hsvType == -1 || hsvType2 == -1){
                tts.speak("No banknote", TextToSpeech.QUEUE_FLUSH, null);
                return;
            }
            int targetType = hsvType;

            try {
                Mat sceneImage = mRgba.submat((mRgba.rows()/ 6), (mRgba.rows()/ 6*5), (mRgba.cols()/6),(mRgba.cols()/6*5));

                MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
                featureDetector = FeatureDetector.create(FeatureDetector.ORB);
                featureDetector.detect(sceneImage, sceneKeyPoints);

                sceneDescriptors = new MatOfKeyPoint();
                descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
                descriptorExtractor.compute(sceneImage, sceneKeyPoints, sceneDescriptors);
                System.out.println("sceneDescriptors = " + sceneDescriptors);

                List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
                descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);


                if (sceneDescriptors.type() != CV_32F) {
                    sceneDescriptors.convertTo(sceneDescriptors, CV_32F);
                }

                System.out.println("objectDescriptors type = " + objectDescriptors.type());
                System.out.println("sceneDescriptors type = " + sceneDescriptors.type());

                if (objectDescriptors.empty() || sceneDescriptors.empty()) {
                    System.out.println("objectDescriptors or sceneDescriptors empty");
                    return;
                }


                descriptorMatcher.knnMatch(sceneDescriptors, objectDescriptors, matches, 2);
                System.out.println("Calculating good match list...");
                LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();

//        float nndrRatio = 0.7f;
                float nndrRatio = 0.72f;
                for (int i = 0; i < matches.size(); i++) {
                    MatOfDMatch matofDMatch = matches.get(i);
                    DMatch[] dmatcharray = matofDMatch.toArray();
                    DMatch m1 = dmatcharray[0];
                    DMatch m2 = dmatcharray[1];

                    if (m1.distance <= m2.distance * nndrRatio) {
                        goodMatchesList.addLast(m1);
                    }
                }
                System.out.println("goodMatchesList.size()= " + goodMatchesList.size());
                if (goodMatchesList.size() >= 7) {
                    tts.speak("Object Found", TextToSpeech.QUEUE_FLUSH, null);
                } else {
                    tts.speak("Object Not Found", TextToSpeech.QUEUE_FLUSH, null);
                }
                sceneKeyPoints.release();
                sceneDescriptors.release();
                goodMatchesList.remove();

                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }





}
