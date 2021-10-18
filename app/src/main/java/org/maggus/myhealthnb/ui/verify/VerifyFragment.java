package org.maggus.myhealthnb.ui.verify;

import android.Manifest;
import android.content.pm.PackageManager;
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
import org.maggus.myhealthnb.barcode.ChecksumCryptoHeader;
import org.maggus.myhealthnb.barcode.ChecksumHeader;
import org.maggus.myhealthnb.barcode.JabBarcode;
import org.maggus.myhealthnb.databinding.FragmentVerifyBinding;
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

public class VerifyFragment extends StatusFragment {
    private static final int PERMISSION_REQUEST_CAMERA = 0;

    private SharedViewModel sharedModel;
    private FragmentVerifyBinding binding;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private LinearLayout scannedDataLayout;
    private ImageView dataStatusImageView;
    private TextView textDataView;
    private Button buttonOk;

    private ImmunizationsDTO.PatientImmunizationDTO scannedDto;

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

//        notificationsViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

//        IntentIntegrator integrator = new IntentIntegrator(getActivity());
//        integrator.initiateScan();

        previewView = binding.cameraPreviewView;

        cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());

        updateUI();

        requestCamera();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
//        if (scanResult != null) {
//            // handle scan result
//            Log.d("barcode", "barcode result: " + scanResult);
//        } else {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//    }

    private void requestCamera() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            } else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
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
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("camera", "Error starting camera", e);
                Toast.makeText(getContext(), "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        setHtmlText("<h6>Scan Immunization records QR-code</h6>");

        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);  // COMPATIBLE ?

        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // image analyzer

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getContext()), new QRCodeImageAnalyzer(new QRCodeFoundListener() {
            @Override
            public void onQRCodeFound(String _qrCode) {
                onBarcodeFound(_qrCode);
                //qrCodeFoundButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void qrCodeNotFound() {
//                textView.setText("No barcode");
                //qrCodeFoundButton.setVisibility(View.INVISIBLE);
//                Log.d("barcode", "QR Code not found");
            }
        }));

        // bind it all together and to the the app lifecycles
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);
        Log.d("barcode", "Camera preview and image analyzer are bound");
    }

    private void onBarcodeFound(String qrCode) {
        if (scannedDto != null) {
            return;
        }
        Log.d("barcode", "QR Code scanned (" + qrCode.length() + " chars): \"" + qrCode + "\"");
//        Toast.makeText(getContext(), qrCode, Toast.LENGTH_SHORT).show();
//        textView.setText("Barcode found");
        try {
            JabBarcode jabBarcode = new JabBarcode();
            if(!jabBarcode.isPossibleJabBarcode(qrCode)){
                throw new IllegalArgumentException("Does not look like JAB barcode format");
            }
            jabBarcode.getRegistry()
                    .registerFormat(ChecksumHeader.class, ImmunizationsDTO.PatientImmunizationDTO.class)
                    .registerFormat(ChecksumCryptoHeader.class, ImmunizationsDTO.PatientImmunizationDTO.class);
            scannedDto = (ImmunizationsDTO.PatientImmunizationDTO) jabBarcode.barcodeToObject(qrCode);
            formatStatusText("Barcode scanned", Status.Success);
            formatImmunizations(scannedDto);
        } catch (Exception e) {
            Log.w("barcode", "Barcode parsing failed", e);
            formatStatusText("Unrecognized barcode", Status.Error);
        }
    }

    private void formatImmunizations(ImmunizationsDTO.PatientImmunizationDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h6>COVID-19 Immunization Record</h6>");
        sb.append("<big><b><font color='" + colorFromRes(R.color.success_bg) + "'>" + dto.getFirstName().toUpperCase() + " "
                + dto.getLastName().toUpperCase() + "</font></b></big><br>");
        sb.append("<b>" + dto.getDateOfBirth() + "</b>");
//        sb.append("<br>");
        String latestCovidImmunizationDate = null;
        int latestCovidImmunizationDoseNo = 0;
        if (dto.getImmunizations() != null) {
            for (ImmunizationsDTO.PatientImmunizationDTO.ImmunizationDTO immuDto : dto.getImmunizations()) {
//            sb.append("<p><b><font color='" + colorFromRes(R.color.success_bg) + "'>" + immuDto.getVaccinationDate() + "</font></b> "
//                    + immuDto.getTradeName() + "</p>");

//            if(immuDto.getTradeName().toUpperCase().contains("COVID")){   // TODO: just assume all vaccines against COVID for now?
                int doseNo = Integer.parseInt(immuDto.getDoseNumber());
                if (doseNo > latestCovidImmunizationDoseNo) {
                    latestCovidImmunizationDoseNo = doseNo;
                    latestCovidImmunizationDate = immuDto.getVaccinationDate();
                }
//            }
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
            sb.append("<br><b>since</b><br>");
            sb.append("<b><font color='" + colorFromRes(color) + "'>" + latestCovidImmunizationDate + "</font></b>");
        }
        sb.append("</p>");

        setHtmlText(sb.toString(), textDataView);

        updateUI();
    }

    private String getVaccinationStatusForDozeNumber(int doseNo){
        if(doseNo <= 0){
            return "Unvaccinated";
        }
        else if(doseNo == 1){
            return "Partially vaccinated";
        }
        else if(doseNo == 2){
            return "Double-vaccinated"; // aka "Fully vaccinated"
        }
        // real future-proofing right here :-)
        else if(doseNo == 3){
            return "Triple-vaccinated";
        }
        else if(doseNo == 4){
            return "Quadruple-vaccinated";
        }
        else if(doseNo == 5){
            return "Quintuple-vaccinated";
        }
        else if(doseNo == 6){
            return "Sextuple-vaccinated";
        }
        else{
            return "Super-vaccinated";  // :-)
        }
    }

    private boolean isFullyVaccinated(int doseNo, String lastDate){
        //TODO: maybe also check the data to be older then 2 weeks?
        return doseNo >=2;
    }

    private void onClearScannedData() {
        scannedDto = null;
        updateUI();
    }

    private void updateUI() {
        if (scannedDto != null) {
            formatStatusText("Barcode scanned", Status.Success);
            scannedDataLayout.setVisibility(View.VISIBLE);
        } else {
            setHtmlText("<h6>Scan and verify an Immunization barcode.</h6>");
            scannedDataLayout.setVisibility(View.GONE);
        }
    }
}