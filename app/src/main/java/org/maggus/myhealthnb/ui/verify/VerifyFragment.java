package org.maggus.myhealthnb.ui.verify;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.maggus.myhealthnb.R;
import org.maggus.myhealthnb.api.dto.ImmunizationsDTO;
import org.maggus.myhealthnb.barcode.JabBarcode;
import org.maggus.myhealthnb.barcode.headers.ChecksumHeader;
import org.maggus.myhealthnb.barcode.headers.CryptoChecksumHeader;
import org.maggus.myhealthnb.databinding.FragmentVerifyBinding;
import org.maggus.myhealthnb.ui.OnSwipeListener;
import org.maggus.myhealthnb.ui.SharedViewModel;
import org.maggus.myhealthnb.ui.StatusFragment;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

public class VerifyFragment extends StatusFragment {
    private static final int PERMISSION_REQUEST_CAMERA = 0;

    private SharedViewModel sharedModel;
    private FragmentVerifyBinding binding;
    private PreviewView previewView;
    private volatile ProcessCameraProvider cameraProvider;
    private volatile Camera camera;
    private LinearLayout scannedDataLayout;
    private ImageView dataStatusImageView;
    private TextView textDataView;
    private Button buttonOk;
    //    private Ringtone goodSound;
    private MediaPlayer validSound;
    //    private Ringtone badSound;
    private MediaPlayer invalidSound;
    private MediaPlayer errorSound;
    private boolean playSounds;

    private ImmunizationsDTO.PatientImmunizationDTO scannedDto;

    private enum Sound {
        Valid,
        Invalid,
        Error
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentVerifyBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textView = binding.textNotifications;
        dataStatusImageView = binding.dataStatusImageView;
        textDataView = binding.textData;
        buttonOk = binding.buttonOk;
        buttonOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onClearScannedData();
            }
        });
        scannedDataLayout = binding.scannedDataLayout;

        try {
            validSound = MediaPlayer.create(getContext(), R.raw.success1);
            invalidSound = MediaPlayer.create(getContext(), R.raw.error1);
            errorSound = MediaPlayer.create(getContext(), R.raw.error2);
            // TODO: or maybe use Ringtones instead?
//            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            goodSound = RingtoneManager.getRingtone(getContext(), notification);
//            Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//            badSound = RingtoneManager.getRingtone(getContext(), alarm);
        } catch (Exception e) {
            Log.w("sound", "Can't initiate sounds");
        }

        setupSwipeListener(root);

        previewView = binding.cameraPreviewView;

        updateUI();

        requestCamera();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        playSounds = prefs.getBoolean("play_sounds", true);
        Log.d("sounds", "Sounds " + (playSounds ? " enabled" : " disabled"));

        getView().setKeepScreenOn(prefs.getBoolean("keep_awake", true));
    }

    @Override
    public void onDestroyView() {
        stopCamera();
        releaseSounds();
        super.onDestroyView();
        binding = null;
    }

    private void setupSwipeListener(View view) {
        view.setOnTouchListener(new OnSwipeListener(getContext()) {
            @Override
            public void onSwipeRight() {
                getMainActivity().goToFragment(R.id.action_navigation_verify_to_navigation_share);
            }
        });
    }

    private void requestCamera() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA)) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Allow the App accessing this device Camera?")
                        .setMessage("The App needs to access this device Camera to scan the Vaccination Verification barcodes")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Log.w("camera", "Camera Permission Denied");
                Toast.makeText(getContext(), "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        formatStatusText("<h6>Please wait...</h6>", Status.Note);
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                final ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();
                getActivity().runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                bindCameraPreview(processCameraProvider);
                            }
                        });
            } catch (ExecutionException | InterruptedException e) {
                Log.e("camera", "Error starting camera", e);
                Toast.makeText(getContext(), "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private void stopCamera() {
        if (cameraProvider != null && camera != null) {
            cameraProvider.unbindAll();
            camera = null;
        }
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider processCameraProvider) {
        // connect camera to an image preview surface
        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);  // PERFORMANCE or COMPATIBLE ?

        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // image analyzer

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                //.setTargetResolution(new Size(1280, 720))
                .setTargetResolution(new Size(720, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getContext()), new QRCodeImageAnalyzer(new QRCodeFoundListener() {
            private int frameCounter = 0;

            @Override
            public void onQRCodeFound(String _qrCode) {
                onBarcodeFound(_qrCode);
            }

            @Override
            public void qrCodeNotFound() {
//                textView.setText("No barcode @" + (frameCounter++));
            }
        }));

        // bind it all together to the camera and to the the app lifecycles
        try {
            cameraProvider = processCameraProvider;
            camera = processCameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
            Log.d("camera", "Camera is ready");
            updateUI();
        } catch (Exception ex) {
            Log.e("camera", "Camera initialization error", ex);
            formatStatusText("Camera error", Status.Warning);
            Toast.makeText(getContext(), "Camera error",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onBarcodeFound(String qrCode) {
        if (scannedDto != null) {
            return;
        }
        Log.d("barcode", "QR Code scanned (" + qrCode.length() + " chars): \"" + qrCode + "\"");
        try {
            JabBarcode jabBarcode = new JabBarcode();
            if (!jabBarcode.isPossibleJabBarcode(qrCode)) {
                throw new IllegalArgumentException("This does not look like JAB barcode format");
            }
            jabBarcode.getRegistry()
                    .registerFormat(ChecksumHeader.class, ImmunizationsDTO.PatientImmunizationDTO.class)
                    .registerFormat(CryptoChecksumHeader.class, ImmunizationsDTO.PatientImmunizationDTO.class);
            scannedDto = (ImmunizationsDTO.PatientImmunizationDTO) jabBarcode.barcodeToObject(qrCode);
            formatStatusText("Barcode scanned", Status.Success);
            formatImmunizations(scannedDto);
        } catch (IOException e) {
            Log.e("barcode", "Barcode validation failed", e);
            formatStatusText("Barcode validation failed", Status.Error);
            doSounds(true, Sound.Error);
        } catch (Exception e) {
            Log.w("barcode", "Unrecognized barcode; " + e.getMessage());
            formatStatusText("Unrecognized barcode", Status.Warning);
        }
    }

    private void formatImmunizations(ImmunizationsDTO.PatientImmunizationDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h6>COVID-19 Immunization Records</h6>");
        sb.append("<big><b><font color='" + colorFromRes(R.color.success_bg) + "'>" + dto.getFirstName().toUpperCase() + " "
                + dto.getLastName().toUpperCase() + "</font></b></big><br>");
        if (dto.getDateOfBirth() != null) {
            sb.append("<b>" + dto.getDateOfBirth() + "</b>");
        }
//        sb.append("<br>");
        String latestCovidImmunizationDate = null;
        int latestCovidImmunizationDoseNo = 0;
        if (dto.getImmunizations() != null) {
            for (ImmunizationsDTO.PatientImmunizationDTO.ImmunizationDTO immuDto : dto.getImmunizations()) {
//            sb.append("<p><b><font color='" + colorFromRes(R.color.success_bg) + "'>" + immuDto.getVaccinationDate() + "</font></b> "
//                    + immuDto.getTradeName() + "</p>");
                if (isCovidVaccination(immuDto)) {
                    int doseNo = Integer.parseInt(immuDto.getDoseNumber());
                    if (doseNo > latestCovidImmunizationDoseNo) {
                        latestCovidImmunizationDoseNo = doseNo;
                        latestCovidImmunizationDate = immuDto.getVaccinationDate();
                    }
                }
            }
        }
        String statusStr = getVaccinationStatusForDozeNumber(latestCovidImmunizationDoseNo);
        boolean isFullyVaccinated = isFullyVaccinated(latestCovidImmunizationDoseNo, latestCovidImmunizationDate);
        int color = isFullyVaccinated ? R.color.success_bg : R.color.error_bg;
        dataStatusImageView.setImageResource(isFullyVaccinated ? R.drawable.ic_baseline_verified_user_24 : R.drawable.ic_baseline_error_24);
        dataStatusImageView.setColorFilter(ContextCompat.getColor(getContext(), color), android.graphics.PorterDuff.Mode.SRC_IN);
//        sb.append("<p>is<hr>");
        sb.append("<p>");
        sb.append("<b><font color='" + colorFromRes(color) + "'>" + statusStr.toUpperCase() + "</font></b>");
        if (latestCovidImmunizationDate != null) {
            sb.append("<br><b>since</b> ");
            sb.append("<b><font color='" + colorFromRes(color) + "'>" + latestCovidImmunizationDate + "</font></b>");
        }
        sb.append("</p>");

        setHtmlText(sb.toString(), textDataView);

        updateUI();

        doSounds(true, isFullyVaccinated ? Sound.Valid : Sound.Invalid);
    }

    private boolean isCovidVaccination(ImmunizationsDTO.PatientImmunizationDTO.ImmunizationDTO immuDto) {
        return true;    // TODO: just assume all listed vaccines are against COVID for now?
//        return immuDto.getTradeName().toUpperCase().contains("COVID");
    }

    private String getVaccinationStatusForDozeNumber(int doseNo) {
        if (doseNo <= 0) {
            return "Unvaccinated";
        } else if (doseNo == 1) {
            return "Partially vaccinated";
        } else if (doseNo == 2) {
            return "Double-vaccinated"; // aka "Fully vaccinated"
        }
        // real future-proofing right here :-)
        else if (doseNo == 3) {
            return "Triple-vaccinated";
        } else if (doseNo == 4) {
            return "Quadruple-vaccinated";
        } else if (doseNo == 5) {
            return "Quintuple-vaccinated";
        } else if (doseNo == 6) {
            return "Sextuple-vaccinated";
        } else {
            return "Super-vaccinated";  // :-)
        }
    }

    private boolean isFullyVaccinated(int doseNo, String lastDate) {
        //TODO: maybe also check the data to be older then 2 weeks?
        return doseNo >= 2;
    }

    private void onClearScannedData() {
        scannedDto = null;
        updateUI();
        doSounds(false, null);
    }

    private void updateUI() {
        if (scannedDto != null) {
            formatStatusText("Barcode scanned", Status.Success);
            scannedDataLayout.setVisibility(View.VISIBLE);
        } else {
            if (camera != null) {
                setHtmlText("<h6>Scan Immunization records QR-code</h6>");
            }
            scannedDataLayout.setVisibility(View.GONE);
        }
    }

    private void playSound(MediaPlayer sound) {
        if (!playSounds) {
            return;
        }
        if (sound != null && !sound.isPlaying()) {
            sound.start();
        }
    }

    private void stopSound(MediaPlayer sound) {
        if (sound != null && sound.isPlaying()) {
            sound.stop();
        }
    }

    private void releaseSounds() {
        stopSound(validSound);
        if (validSound != null) {
            validSound.release();
            validSound = null;
        }
        stopSound(invalidSound);
        if (invalidSound != null) {
            invalidSound.release();
            invalidSound = null;
        }
        stopSound(errorSound);
        if (errorSound != null) {
            errorSound.release();
            errorSound = null;
        }
    }

    private void doSounds(boolean play, Sound type) {
        if (play) {
            if (type == Sound.Valid) {
                stopSound(invalidSound);
                stopSound(errorSound);
                playSound(validSound);
            } else if (type == Sound.Invalid) {
                stopSound(validSound);
                stopSound(errorSound);
                playSound(invalidSound);
            } else if (type == Sound.Error) {
                stopSound(validSound);
                stopSound(invalidSound);
                playSound(errorSound);
            } else {
                Log.w("sounds", "Unexpected sound type: " + type);
            }
        } else {
            stopSound(validSound);
            stopSound(invalidSound);
            stopSound(errorSound);
        }
    }
}