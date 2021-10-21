package org.maggus.myhealthnb.ui.share;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.maggus.myhealthnb.R;
import org.maggus.myhealthnb.api.dto.ImmunizationsDTO;
import org.maggus.myhealthnb.barcode.JabBarcode;
import org.maggus.myhealthnb.barcode.headers.CryptoChecksumHeader;
import org.maggus.myhealthnb.databinding.FragmentShareBinding;
import org.maggus.myhealthnb.ui.OnSwipeListener;
import org.maggus.myhealthnb.ui.SharedViewModel;
import org.maggus.myhealthnb.ui.StatusFragment;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

public class ShareFragment extends StatusFragment {

    private SharedViewModel sharedModel;
    private FragmentShareBinding binding;
    private ImageView imageView;

    private boolean shareDob;
    private String barcode;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentShareBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textView = binding.textDashboard;
        imageView = binding.imageView;
        imageView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            // show barcode once imageView is fully inflated
            generateImmunizationBarcode();
        });
        sharedModel.getImmunizations().observe(getViewLifecycleOwner(), new Observer<ImmunizationsDTO.PatientImmunizationDTO>() {
            @Override
            public void onChanged(@Nullable ImmunizationsDTO.PatientImmunizationDTO dto) {
                // show barcode once dto changes
                barcode = null;
                generateImmunizationBarcode();
            }
        });

        setupSwipeListener(root);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        shareDob = prefs.getBoolean("share_dob", true);
        Log.d("barcode", "Share DOB " + (shareDob ? " enabled" : " disabled"));
        // show barcode once settings changes
        barcode = null;
        generateImmunizationBarcode();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupSwipeListener(View view) {
        view.setOnTouchListener(new OnSwipeListener(getContext()) {
            @Override
            public void onSwipeRight() {
                getMainActivity().goToFragment(R.id.action_navigation_share_to_navigation_home);
            }

            public void onSwipeLeft() {
                getMainActivity().goToFragment(R.id.action_navigation_share_to_navigation_verify);
            }
        });
    }

    private void generateImmunizationBarcode() {
        ImmunizationsDTO.PatientImmunizationDTO dto = sharedModel != null ? sharedModel.getImmunizations().getValue() : null;
        if (dto == null) {
            setHtmlText("<h6>No Immunization records for the barcode.<br>Please login to MyHealthNB first.</h6>");
            imageView.setVisibility(View.GONE);
        } else {
            setHtmlText("<h6>Your Immunization barcode</h6>" +
                    "<h3><font color='" + colorFromRes(R.color.success_bg) + "'>" + dto.getFirstName() + " " + dto.getLastName() + "</font></h3>");

            final int width = imageView.getWidth();
            final int height = imageView.getHeight();
            if (width == 0 || height == 0) {
                return;
            }

            imageView.setVisibility(View.VISIBLE);

            Executor mSingleThreadExecutor = Executors.newSingleThreadExecutor();
            mSingleThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (barcode != null) {
                            return;
                        }
                        JabBarcode jabBarcode = new JabBarcode();
                        // redacted copy of immunization records, safe to share with public (more secure)
                        ImmunizationsDTO.PatientImmunizationDTO filtered = dto.filter();
                        if (!shareDob) {
                            filtered.setDateOfBirth(null);  // do not include DOB in the barcode
                        }
                        //String barcode = jabBarcode.objectToBarcode(new ChecksumHeader(), filtered);  // human readable format
                        barcode = jabBarcode.objectToBarcode(new CryptoChecksumHeader(), filtered);  // obfuscated format (more secure)
                        Log.d("barcode", "our barcode (" + barcode.length() + " chars): \"" + barcode + "\"");

                        Bitmap bitmap = buildQrCode(barcode, width, height);

                        updateUI(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });

                    } catch (Exception e) {
                        updateUI(new Runnable() {
                            @Override
                            public void run() {
                                Log.e("barcode", "Error generating barcode", e);
                                formatStatusText("Error generating barcode", Status.Error);
                            }
                        });
                    }
                }
            });
        }
    }

    private synchronized void updateUI(Runnable runnable) {
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(runnable);
        }
    }

    private Bitmap buildQrCode(String data, int width, int height) throws WriterException {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        BitMatrix bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bitmap.setPixel(i, j, bitMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }
}