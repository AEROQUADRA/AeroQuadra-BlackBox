package com.example.aa_usk_8;

import android.content.Intent;
import android.content.SharedPreferences;
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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.calib3d.Calib3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DetectArucoActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "DetectArucoActivity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Dictionary dictionary;
    private DetectorParameters detectorParameters;

    private Mat mRgba;
    private Mat mRgb;
    private Mat mGray;

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

        setContentView(R.layout.activity_detect_aruco);

        mOpenCvCameraView = findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_250);
        detectorParameters = DetectorParameters.create();

        // Retrieve saved camera parameters
        SharedPreferences prefs = getSharedPreferences("cameraCalibration", MODE_PRIVATE);
        String cameraMatrixString = prefs.getString("cameraMatrix", "");
        String distCoeffsString = prefs.getString("distCoeffs", "");

        if (cameraMatrixString.isEmpty() || distCoeffsString.isEmpty()) {
            Toast.makeText(this, "Camera calibration data not found. Please calibrate the camera first.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            cameraMatrix = stringToMat(cameraMatrixString);
            distCoeffs = stringToMatOfDouble(distCoeffsString);
            Log.i(TAG, "Calibration data loaded successfully");
        } catch (Exception e) {
            Toast.makeText(this, "Error parsing calibration data. Please recalibrate the camera.", Toast.LENGTH_LONG).show();
            finish();
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
        mRgb = new Mat(height, width, CvType.CV_8UC3);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        Log.i(TAG, "Camera view started");
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mRgb.release();
        mGray.release();
        Log.i(TAG, "Camera view stopped");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, mRgb, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(mRgb, mGray, Imgproc.COLOR_RGB2GRAY);
        Log.i(TAG, "Frame captured and converted to grayscale");

        List<Mat> corners = new ArrayList<>();
        Mat ids = new Mat();
        Aruco.detectMarkers(mGray, dictionary, corners, ids, detectorParameters);
        Log.i(TAG, "Marker detection performed");

        if (ids.total() > 0) {
            try {
                Aruco.drawDetectedMarkers(mRgb, corners, ids);
                Log.i(TAG, "Detected markers drawn on frame");

                Mat rvecs = new Mat();
                Mat tvecs = new Mat();
                Aruco.estimatePoseSingleMarkers(corners, 0.05f, cameraMatrix, distCoeffs, rvecs, tvecs);
                Log.i(TAG, "Pose estimation performed");

                int closestMarkerId = -1;
                double closestDistance = Double.MAX_VALUE;

                for (int i = 0; i < ids.rows(); i++) {
                    double[] rvec = rvecs.get(i, 0);
                    double[] tvec = tvecs.get(i, 0);
                    double distance = Math.sqrt(tvec[0] * tvec[0] + tvec[1] * tvec[1] + tvec[2] * tvec[2]) * 100; // Convert to cm

                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestMarkerId = (int) ids.get(i, 0)[0];
                    }

                    Calib3d.drawFrameAxes(mRgb, cameraMatrix, distCoeffs, new MatOfDouble(rvec), new MatOfDouble(tvec), 0.05f);
                    String distanceStr = String.format("ID: %d Distance: %.2f cm", (int) ids.get(i, 0)[0], distance);
                    Point textPosition = new Point(corners.get(i).get(0, 0));
                    Imgproc.putText(mRgb, distanceStr, textPosition, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 0, 0), 2);
                    Log.i(TAG, "Distance displayed for marker ID: " + (int) ids.get(i, 0)[0]);
                }

                if (closestMarkerId != -1) {
                    Intent intent = new Intent(this, MoveActivity.class);
                    intent.putExtra("detectedMarkerId", closestMarkerId);
                    startActivity(intent);
                    finish();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error drawing detected markers: " + e.getMessage());
            }
        } else {
            Log.i(TAG, "No markers detected");
        }

        return mRgb;
    }

    private Mat stringToMat(String matString) {
        String[] values = matString.split(",");
        int numValues = values.length;
        int size = (int) Math.sqrt(numValues);

        if (size * size != numValues) {
            throw new IllegalArgumentException("Invalid matrix string format.");
        }

        Mat mat = new Mat(size, size, CvType.CV_64F);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                mat.put(i, j, Double.parseDouble(values[i * size + j]));
            }
        }

        return mat;
    }

    private MatOfDouble stringToMatOfDouble(String matString) {
        String[] values = matString.split(",");
        int numValues = values.length;
        MatOfDouble mat = new MatOfDouble();
        double[] data = new double[numValues];

        for (int i = 0; i < numValues; i++) {
            data[i] = Double.parseDouble(values[i]);
        }

        mat.fromArray(data);
        return mat;
    }
}
