package org.maggus.myhealthnb;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import org.maggus.myhealthnb.api.dto.ImmunizationsDTO;
import org.maggus.myhealthnb.barcode.JabBarcode;
import org.maggus.myhealthnb.barcode.headers.CryptoChecksumHeader;
import org.maggus.myhealthnb.databinding.ActivityMainBinding;
import org.maggus.myhealthnb.ui.OnSwipeListener;
import org.maggus.myhealthnb.ui.SharedViewModel;

public class MainActivity extends AppCompatActivity {

    private SharedViewModel sharedModel;
    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedModel = new ViewModelProvider(this).get(SharedViewModel.class);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_share, R.id.navigation_verify)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        // read stored immunization state
        readPreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            // launch settings activity
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void readPreferences() {
        try {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            String myImmunizationBarcode = sharedPref.getString(ImmunizationsDTO.PatientImmunizationDTO.class.getSimpleName(), null);
            if (myImmunizationBarcode != null) {
                ImmunizationsDTO.PatientImmunizationDTO dto = new JabBarcode().barcodeToObject(myImmunizationBarcode, CryptoChecksumHeader.class, ImmunizationsDTO.PatientImmunizationDTO.class);
                sharedModel.setImmunizations(dto);

                // if we loaded with existing profile, go right ot the barcode Share view
                Log.d("preferences", "We loaded valid profile from preferences, so swith to barcode view");
                goToFragment(R.id.action_navigation_home_to_navigation_share);
            }
        } catch (Exception e) {
            Log.e("preferences", "Error loading app preferences", e);
            Toast.makeText(this, "Error loading app preferences: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            writePreferences(); // delete current preferences
        }
    }

    public void writePreferences() {
        try {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            ImmunizationsDTO.PatientImmunizationDTO dto = sharedModel.getImmunizations().getValue();
            if (dto != null) {
                String myImmunizationBarcode = new JabBarcode().objectToBarcode(new CryptoChecksumHeader(), dto);
                editor.putString(ImmunizationsDTO.PatientImmunizationDTO.class.getSimpleName(), myImmunizationBarcode);
            } else {
                editor.remove(ImmunizationsDTO.PatientImmunizationDTO.class.getSimpleName());
            }
            editor.apply();
        } catch (Exception e) {
            Log.e("preferences", "Error saving app preferences", e);
            Toast.makeText(this, "Error saving app preferences: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void goToFragment(int action) {
        try {
            navController.navigate(action);
        } catch (Exception ex) {
            Log.w("fragment", "Error while navigating to another Fragment; " + ex.getMessage());
        }
    }
}