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
import java.util.Arrays;
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

    // Scaling factor
    private float scalingFactor = 1.0f; // Default scaling factor

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

        // Set the correct ArUco dictionary (4x4_250)
        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_250);

        // Use sub-pixel corner refinement for accuracy
        detectorParameters = DetectorParameters.create();
        detectorParameters.set_cornerRefinementMethod(Aruco.CORNER_REFINE_SUBPIX);

        // Retrieve saved camera parameters (ensure this is calibrated correctly)
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

        // Retrieve the saved scaling factor
        SharedPreferences scalingPrefs = getSharedPreferences("scalingFactorData", MODE_PRIVATE);
        scalingFactor = scalingPrefs.getFloat("scalingFactor", 1.0f); // Default scaling factor is 1.0
        Log.i(TAG, "Scaling factor loaded: " + scalingFactor);
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
        return Arrays.asList(mOpenCvCameraView);
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

        List<Mat> corners = new ArrayList<>();
        Mat ids = new Mat();
        Aruco.detectMarkers(mGray, dictionary, corners, ids, detectorParameters);

        if (ids.total() > 0) {
            try {
                // Draw detected markers
                Aruco.drawDetectedMarkers(mRgb, corners, ids);

                Mat rvecs = new Mat();
                Mat tvecs = new Mat();

                // Marker size is 5 cm (0.05 meters)
                float markerSizeMeters = 0.05f;
                Aruco.estimatePoseSingleMarkers(corners, markerSizeMeters, cameraMatrix, distCoeffs, rvecs, tvecs);

                // Convert MatOfDouble to Mat (for distCoeffs)
                Mat distCoeffsMat = new Mat(distCoeffs.rows(), distCoeffs.cols(), CvType.CV_64F);
                distCoeffs.copyTo(distCoeffsMat);

                // Keep track of the closest marker
                int closestMarkerId = -1;
                double closestDistance = Double.MAX_VALUE;

                // Loop through each detected marker
                for (int i = 0; i < ids.rows(); i++) {
                    double[] tvec = tvecs.get(i, 0);

                    // Calculate Euclidean distance based on the translation vector (in meters, converted to cm)
                    double distance = Math.sqrt(tvec[0] * tvec[0] + tvec[1] * tvec[1] + tvec[2] * tvec[2]) * 100;

                    // Apply the scaling factor to adjust the distance
                    double adjustedDistance = distance * scalingFactor;

                    // Log adjusted distance for debugging
                    Log.i(TAG, "Marker ID: " + (int) ids.get(i, 0)[0] + " | Adjusted Distance: " + adjustedDistance + " cm");

                    // Update closest marker logic
                    if (adjustedDistance < closestDistance) {
                        closestDistance = adjustedDistance;
                        closestMarkerId = (int) ids.get(i, 0)[0];
                    }

                    // Reshape vectors for drawing axes
                    Mat rvec = rvecs.row(i).reshape(3, 1);
                    Mat tvecMat = tvecs.row(i).reshape(3, 1);

                    // Draw axes on the marker
                    Calib3d.drawFrameAxes(mRgb, cameraMatrix, distCoeffsMat, rvec, tvecMat, (float) (markerSizeMeters * 0.6));

                    // Display adjusted distance on the frame
                    String distanceStr = String.format("ID: %d Adjusted Distance: %.2f cm", (int) ids.get(i, 0)[0], adjustedDistance);
                    Point textPosition = new Point(corners.get(i).get(0, 0));
                    Imgproc.putText(mRgb, distanceStr, textPosition, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 0, 0), 2);
                }

                // Move to MoveActivity with the closest marker info
                if (closestMarkerId != -1) {
                    Intent intent = new Intent(DetectArucoActivity.this, MoveActivity.class);
                    intent.putExtra("detectedMarkerId", closestMarkerId);
                    intent.putExtra("distanceToMarker", closestDistance); // Pass adjusted distance to MoveActivity
                    startActivity(intent);
                    finish(); // Close this activity and move to MoveActivity
                }

            } catch (Exception e) {
                Log.e(TAG, "Error during pose estimation: " + e.getMessage());
            }
        } else {
            Log.i(TAG, "No markers detected");
        }

        return mRgb;
    }

    // Convert string to Mat (camera matrix)
    private Mat stringToMat(String matString) {
        String[] values = matString.split(",");
        int numValues = values.length;
        int size = (int) Math.sqrt(numValues);

        Mat mat = new Mat(size, size, CvType.CV_64F);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                mat.put(i, j, Double.parseDouble(values[i * size + j]));
            }
        }

        return mat;
    }

    // Convert string to MatOfDouble (distortion coefficients)
    private MatOfDouble stringToMatOfDouble(String matString) {
        String[] values = matString.split(",");
        double[] data = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            data[i] = Double.parseDouble(values[i]);
        }

        return new MatOfDouble(data);
    }
}
