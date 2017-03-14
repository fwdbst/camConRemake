package hk.junchou.banknotereader.test.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static org.opencv.core.CvType.CV_32F;

public class appActivity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "OCVSample::Activity";
    private static final int OPEN_REQUEST_CODE = 41;
    private appView mOpenCvCameraView;

    private KeyPoint[] keypoints;
    private FeatureDetector featureDetector;
    private DescriptorExtractor descriptorExtractor;
    private Mat mRgba;

    private MatOfKeyPoint sceneDescriptors;
    private DescriptorMatcher descriptorMatcher;

    private MatOfKeyPoint objectDescriptors;
    private MatOfKeyPoint[] descriptorSet;
    private String[] banknoteName = new String[]{"10", "10", "20", "100", "100"};

    private TextToSpeech tts;
    private Thread detectCamera = new DetectCamera();

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(appActivity.this);

                    descriptorSet = new MatOfKeyPoint[63];
                    descriptorSet = initDataSet();

                    createLanguageTTS();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public appActivity() {
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
        mOpenCvCameraView = (appView) findViewById(R.id.tutorial3_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        Thread detectCamera = new DetectCamera();
    }

    public MatOfKeyPoint importArray(String fileName) {
        KeyPoint[] testSet = new KeyPoint[]{};
//        testSet = (getTheArray("hsbc_new_20_front.txt"));
        testSet = (getTheArray(fileName));
        System.out.println("testSet = " + Arrays.toString(testSet));
        MatOfKeyPoint result = new MatOfKeyPoint() {
        };
        result.fromArray(testSet);
        result.reshape(mOpenCvCameraView.getWidth());
        return result;
    }

    public KeyPoint[] getTheArray(String fileName) {
        List<KeyPoint> list = new LinkedList<KeyPoint>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));
            String line = br.readLine();
            while (line != null) {
                String[] ar = line.split(",");
                float a, b, c, d, e;
                int f;
                a = Float.parseFloat(ar[0]);
                b = Float.parseFloat(ar[1]);
                c = Float.parseFloat(ar[2]);
                d = Float.parseFloat(ar[3]);
                e = Float.parseFloat(ar[4]);
                f = Integer.parseInt(ar[5]);

                KeyPoint tempKeyPoint = new KeyPoint(a, b, c, d, e, f);
                list.add(tempKeyPoint);
                line = br.readLine();
            }

        } catch (Exception e) {
            throw new IllegalStateException("Couldn't load array file");
        }
        return list.toArray(new KeyPoint[0]);
    }

    public List<KeyPoint> getTheList(String fileName) {
        List<KeyPoint> list = new LinkedList<KeyPoint>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open(fileName)));
            String line = br.readLine();
            while (line != null) {
                String[] ar = line.split(",");
                float a, b, c, d, e;
                int f, g;
                a = Float.parseFloat(ar[0]);
                b = Float.parseFloat(ar[1]);
                c = Float.parseFloat(ar[2]);
                d = Float.parseFloat(ar[3]);
                e = Float.parseFloat(ar[4]);
                f = Integer.parseInt(ar[5]);

                KeyPoint tempKeyPoint = new KeyPoint(a, b, c, d, e, f);
                list.add(tempKeyPoint);
                line = br.readLine();
            }

        } catch (Exception e) {
            throw new IllegalStateException("Couldn't load array file");
        }
        return list;
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
//        Log.i(TAG, "DetectCamera detectCamera= new DetectCamera();");

        return mRgba;
    }

    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch event");
        Toast.makeText(this, "touched", Toast.LENGTH_SHORT).show();
        mOpenCvCameraView.findFocus();
        detectCamera.run();
        Log.i(TAG, "detectCamera.run();");
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        Uri currentUri = null;

        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == OPEN_REQUEST_CODE) {
                if (resultData != null) {

                    currentUri = resultData.getData();
                    loadPoint(currentUri);
                }
            }
        }
    }

    public void loadPoint(Uri currentUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currentUri);

            Mat objectImage = new Mat();
            MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();

            Utils.bitmapToMat(bitmap, objectImage);

            System.out.println("objectKeyPoints =  " + objectKeyPoints);
            System.out.println("objectImage =  " + objectImage);
            featureDetector = FeatureDetector.create(FeatureDetector.ORB);

            featureDetector.detect(objectImage, objectKeyPoints);

            Toast.makeText(this, "featureDetector success", Toast.LENGTH_SHORT).show();
            System.out.println("objectKeyPoints =  " + objectKeyPoints);
            keypoints = objectKeyPoints.toArray();
            System.out.println("KeyPoint[] keypoints= " + Arrays.toString(keypoints));
            Toast.makeText(this, "KeyPoint success", Toast.LENGTH_SHORT).show();

            objectDescriptors = new MatOfKeyPoint();
            descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
            descriptorExtractor.compute(objectImage, objectKeyPoints, objectDescriptors);
            Toast.makeText(this, "descriptorExtractor success", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MatOfKeyPoint loadPointWithBitmap(String filename) {
        try {
            Bitmap bitmap = getBitmapFromAsset(filename);

            Mat objectImage = new Mat();
            MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();

            Utils.bitmapToMat(bitmap, objectImage);

            System.out.println("objectKeyPoints =  " + objectKeyPoints);
            System.out.println("objectImage =  " + objectImage);
            featureDetector = FeatureDetector.create(FeatureDetector.ORB);

            featureDetector.detect(objectImage, objectKeyPoints);

//            Toast.makeText(this, "featureDetector success", Toast.LENGTH_SHORT).show();
            System.out.println("objectKeyPoints =  " + objectKeyPoints);
            keypoints = objectKeyPoints.toArray();
            System.out.println("KeyPoint[] keypoints= " + Arrays.toString(keypoints));
//            Toast.makeText(this, "KeyPoint success", Toast.LENGTH_SHORT).show();

            objectDescriptors = new MatOfKeyPoint();
            descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
            descriptorExtractor.compute(objectImage, objectKeyPoints, objectDescriptors);
//            Toast.makeText(this, "descriptorExtractor success", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return objectDescriptors;
    }

    public void openFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    public MatOfKeyPoint[] initDataSet() {
        MatOfKeyPoint[] objectDescriptorsSet = new MatOfKeyPoint[63];
        String filename;
        for (int i = 0; i < 5; i++) {
            filename = i + ".jpg";
            objectDescriptorsSet[i] = loadPointWithBitmap(filename);
        }
        return objectDescriptorsSet;
    }

    private Bitmap getBitmapFromAsset(String strName) throws IOException {
        AssetManager assetManager = getAssets();
        InputStream istr = assetManager.open(strName);
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        istr.close();
        return bitmap;
    }

    class DetectCamera extends Thread {

        @Override
        public void run() {
            super.run();

            try {
                int current;
                int[] result = new int[63];
                for (current = 0; current < 5; current++) {
                    Mat sceneImage = mRgba;
                    MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
                    objectDescriptors = descriptorSet[current];

                    if (featureDetector == null) {
                        featureDetector = FeatureDetector.create(FeatureDetector.ORB);
                    }

                    featureDetector.detect(sceneImage, sceneKeyPoints);

                    sceneDescriptors = new MatOfKeyPoint();
                    descriptorExtractor.compute(sceneImage, sceneKeyPoints, sceneDescriptors);

                    List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
                    descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

                    if (objectDescriptors.type() != CV_32F) {
                        objectDescriptors.convertTo(objectDescriptors, CV_32F);
                    }

                    if (sceneDescriptors.type() != CV_32F) {
                        sceneDescriptors.convertTo(sceneDescriptors, CV_32F);
                    }

                    descriptorMatcher.knnMatch(objectDescriptors, sceneDescriptors, matches, 2);
                    System.out.println("Calculating good match list...");
                    LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();

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
                    result[current] = goodMatchesList.size();

                }
                int possibleResult = -1;

                for (int resultCurrent = 0; resultCurrent < 5; resultCurrent++) {
                    if (result[resultCurrent] < 7) continue;
                    if (possibleResult == -1) possibleResult = resultCurrent;
                    if (result[resultCurrent] > result[possibleResult])
                        possibleResult = resultCurrent;
                }
                System.out.println("possibleResult = " + possibleResult);

                if (possibleResult == -1) {
                    tts.speak(" banknote not detected", TextToSpeech.QUEUE_FLUSH, null);
                } else {
                    String speechText = banknoteName[possibleResult];
                    tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null);
                }
            } catch (
                    Exception e
                    )

            {
                e.printStackTrace();
            }
        }

    }


}
