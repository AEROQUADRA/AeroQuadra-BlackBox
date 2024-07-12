// DetectActivity.java
package com.example.aa_usk_8;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.aruco.DetectorParameters;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.calib3d.Calib3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DetectActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "DetectActivity";

    private Mat mRgba;
    private Mat mGray;
    private Dictionary dictionary;
    private DetectorParameters detectorParameters;
    private CameraBridgeViewBase mOpenCvCameraView;

    private boolean isWaitingForMarkers = false;

    // Camera parameters
    private Mat cameraMatrix;
    private MatOfDouble distCoeffs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
            return;
        }

        setContentView(R.layout.activity_detect);

        mOpenCvCameraView = findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_250);
        detectorParameters = DetectorParameters.create();

        // Define camera parameters
        cameraMatrix = new Mat(3, 3, CvType.CV_64F);
        distCoeffs = new MatOfDouble(new double[]{0, 0, 0, 0, 0});

        // Assuming fx = fy and cx, cy are the principal point coordinates
        // Set these according to your camera calibration
        double focalLength = 800; // Example value, set according to your calibration
        double principalPointX = 640; // Example value, set according to your calibration
        double principalPointY = 480; // Example value, set according to your calibration

        cameraMatrix.put(0, 0, focalLength);
        cameraMatrix.put(0, 1, 0);
        cameraMatrix.put(0, 2, principalPointX);
        cameraMatrix.put(1, 0, 0);
        cameraMatrix.put(1, 1, focalLength);
        cameraMatrix.put(1, 2, principalPointY);
        cameraMatrix.put(2, 0, 0);
        cameraMatrix.put(2, 1, 0);
        cameraMatrix.put(2, 2, 1);
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
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.enableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

        // Perform marker detection
        List<Mat> corners = new ArrayList<>();
        Mat ids = new Mat();
        Aruco.detectMarkers(mGray, dictionary, corners, ids, detectorParameters);

        // Check if markers are detected
        if (ids.total() > 0) {
            handleMarkerDetection(ids, corners);
            isWaitingForMarkers = false; // Reset flag if markers are detected
        } else {
            if (!isWaitingForMarkers) {
                Log.i(TAG, "No markers detected. Waiting for markers...");
                isWaitingForMarkers = true; // Set flag to indicate waiting
            }
        }

        return mRgba;
    }

    private void handleMarkerDetection(Mat ids, List<Mat> corners) {
        MatOfInt idsMatOfInt = new MatOfInt(ids);
        int[] idsArray = idsMatOfInt.toArray();

        // Log detected markers
        StringBuilder detectedMarkersLog = new StringBuilder("Detected markers: ");
        for (int id : idsArray) {
            detectedMarkersLog.append(id).append(", ");
        }
        Log.i(TAG, detectedMarkersLog.toString());

        // Check if marker ID 0 is detected
        if (idsArray[0] == 0) {
            // Start ProgramEndsActivity
            Intent intent = new Intent(DetectActivity.this, ProgramEndsActivity.class);
            startActivity(intent);
        } else {
            // Perform pose estimation
            Mat rvecs = new Mat();
            Mat tvecs = new Mat();
            Aruco.estimatePoseSingleMarkers(corners, 0.05f, cameraMatrix, distCoeffs, rvecs, tvecs);

            // Assuming only one marker is detected
            double[] tvec = tvecs.get(0, 0);
            double distance = Math.sqrt(tvec[0] * tvec[0] + tvec[1] * tvec[1] + tvec[2] * tvec[2]) * 1000; // Convert to mm

            Log.i(TAG, "Distance to marker (mm): " + distance);

            // Start RotationActivity and pass detected marker ID and distance
            Intent intent = new Intent(DetectActivity.this, RotationActivity.class);
            intent.putExtra("detectedMarkerId", String.valueOf(idsArray[0])); // Assuming only one marker is detected
            intent.putExtra("distanceToMarker", distance);
            startActivity(intent);
        }
    }
}
