package com.example.aa_usk_8;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CameraCalibrationActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "CameraCalibActivity";
    private static final int CHESSBOARD_WIDTH = 9;
    private static final int CHESSBOARD_HEIGHT = 6;
    private static final int CALIBRATION_IMAGES_REQUIRED = 50;
    private static final double SQUARE_SIZE = 0.024; // Size of the chessboard square in meters (24mm for A4)

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private List<Mat> imagePoints = new ArrayList<>();
    private List<Mat> objectPoints = new ArrayList<>();
    private Size patternSize = new Size(CHESSBOARD_WIDTH, CHESSBOARD_HEIGHT);
    private MatOfPoint3f objectCorners; // 3D points of the checkerboard squares

    private TextView txtProgress;

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

        setContentView(R.layout.activity_camera_calibration);

        mOpenCvCameraView = findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        txtProgress = findViewById(R.id.txtProgress);

        // Initialize object points once (3D points for the checkerboard)
        objectCorners = new MatOfPoint3f();
        for (int i = 0; i < CHESSBOARD_HEIGHT; i++) {
            for (int j = 0; j < CHESSBOARD_WIDTH; j++) {
                objectCorners.push_back(new MatOfPoint3f(new Point3(j * SQUARE_SIZE, i * SQUARE_SIZE, 0)));
            }
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

        MatOfPoint2f corners = new MatOfPoint2f();
        boolean found = Calib3d.findChessboardCorners(mGray, patternSize, corners);

        if (found) {
            // Refine corner locations for sub-pixel accuracy
            Imgproc.cornerSubPix(mGray, corners, new Size(11, 11), new Size(-1, -1),
                    new org.opencv.core.TermCriteria(org.opencv.core.TermCriteria.EPS + org.opencv.core.TermCriteria.COUNT, 30, 0.1));

            Calib3d.drawChessboardCorners(mRgba, patternSize, corners, found);
            imagePoints.add(corners);

            // Use the precomputed object points
            objectPoints.add(objectCorners);

            // Update progress
            runOnUiThread(() -> txtProgress.setText(String.format("Calibration Progress: %d/%d", imagePoints.size(), CALIBRATION_IMAGES_REQUIRED)));

            // If enough images are captured, calibrate the camera
            if (imagePoints.size() >= CALIBRATION_IMAGES_REQUIRED) {
                calibrateCamera();
            }
        }

        return mRgba;
    }

    private void calibrateCamera() {
        Mat cameraMatrix = Mat.eye(3, 3, CvType.CV_64F);
        Mat distCoeffs = Mat.zeros(8, 1, CvType.CV_64F);
        List<Mat> rvecs = new ArrayList<>();
        List<Mat> tvecs = new ArrayList<>();

        Size imageSize = new Size(mGray.width(), mGray.height());

        // Calibrate the camera
        double rms = Calib3d.calibrateCamera(objectPoints, imagePoints, imageSize, cameraMatrix, distCoeffs, rvecs, tvecs);

        // Log the reprojection error
        Log.i(TAG, "Calibration reprojection error (RMS): " + rms);

        // Save the calibration data
        saveCalibrationData(cameraMatrix, distCoeffs);

        // Notify the user that calibration is complete and return to main activity
        runOnUiThread(() -> {
            Toast.makeText(this, "Camera calibration complete!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(CameraCalibrationActivity.this, MainActivity.class);
            intent.putExtra("calibrationStatus", "Calibration Complete!");
            startActivity(intent);
            finish(); // Close the calibration activity
        });
    }

    private void saveCalibrationData(Mat cameraMatrix, Mat distCoeffs) {
        SharedPreferences prefs = getSharedPreferences("cameraCalibration", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("cameraMatrix", matToString(cameraMatrix));
        editor.putString("distCoeffs", matToString(distCoeffs));
        editor.putBoolean("isCalibrated", true); // Store calibration status

        editor.apply();
        Log.i(TAG, "Calibration data saved successfully");
    }

    private String matToString(Mat mat) {
        StringBuilder sb = new StringBuilder();
        int rows = mat.rows();
        int cols = mat.cols();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sb.append(mat.get(i, j)[0]).append(",");
            }
        }

        return sb.toString();
    }
}
