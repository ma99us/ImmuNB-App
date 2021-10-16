package org.maggus.myhealthnb.ui.verify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.maggus.myhealthnb.databinding.FragmentVerifyBinding;
import org.maggus.myhealthnb.ui.SharedViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class VerifyFragment extends Fragment {

    private SharedViewModel sharedModel;
    private FragmentVerifyBinding binding;
    private TextView textView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentVerifyBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textView = binding.textNotifications;
        textView.setText("TODO: scan and verify other's barcode here");
//        notificationsViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

        IntentIntegrator integrator = new IntentIntegrator(getActivity());
        integrator.initiateScan();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            // handle scan result
            Log.d("barcode", "barcode result: " + scanResult);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}