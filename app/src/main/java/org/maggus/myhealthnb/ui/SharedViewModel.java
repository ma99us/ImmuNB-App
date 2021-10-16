package org.maggus.myhealthnb.ui;

import com.android.volley.AuthFailureError;

import org.maggus.myhealthnb.api.AuthState;
import org.maggus.myhealthnb.api.EnvConfig;
import org.maggus.myhealthnb.api.dto.DemographicsDTO;
import org.maggus.myhealthnb.api.dto.ImmunizationsDTO;

import java.util.HashMap;
import java.util.Map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import lombok.Getter;
import lombok.Setter;

public class SharedViewModel extends ViewModel {
    @Getter @Setter
    private AuthState authState;
    @Getter @Setter
    private EnvConfig envConfig;
    @Getter @Setter
    private String deviceId;
    @Getter @Setter
    private String nonce;

    public String getApiOrigin(){
        if(envConfig == null){
            return null;
        }
        return envConfig.getProperty("DIGITAL_ID_REDIRECT_URI");
    }

    public String getApiHost(){
        String url = getApiOrigin();
        if (url != null && url.startsWith("https://")) {
            url = url.substring("https://".length());
        }
        return url;
    }

    public String getApiReferer(){
        String url = getApiOrigin();
        if (url != null && authState != null) {
            url += "/?code=" + authState.getAuthorizationCode();
        }
        return url;
    }

    public Map<String, String> populateApiRequestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", getApiHost());              // "myhealthnb.verosource.com"
        headers.put("Origin", getApiOrigin());          // "https://myhealthnb.verosource.com"
        headers.put("Referer", getApiReferer());        // "https://myhealthnb.verosource.com/?code=" + code

        headers.put("x-vsf-api-token", getEnvConfig().getProperty("API_TOKEN")); // "YTFkNDYyMWEtYmU4NC00MmU0LWJjNDYtZDQ3NTA1NjFkYTJl"
        headers.put("x-vsf-client-version", "MyHeathNB-v0.1.0-App");             // "MyHeathNB-v0.1.0-Desktop"  // TODO: Maybe try something else?
        headers.put("x-vsf-meta-device-id", getDeviceId());                      // "cdddaf26-a457-48df-8117-642f5bc596fc"

        if (getAuthState().getAuthorizationToken() != null) {
            headers.put("authorization", getAuthState().getAuthorizationToken());   // Bearer sda123qsd12e34qWwd4rqQFd....
        }

        return headers;
    }

    private final MutableLiveData<DemographicsDTO.DemographicDTO> demographics = new MutableLiveData<DemographicsDTO.DemographicDTO>();
    private final MutableLiveData<ImmunizationsDTO.PatientImmunizationDTO> immunizations = new MutableLiveData<ImmunizationsDTO.PatientImmunizationDTO>();

    public void setDemographics(DemographicsDTO.DemographicDTO item) {
        demographics.setValue(item);
    }

    public LiveData<DemographicsDTO.DemographicDTO> getDemographics() {
        return demographics;
    }

    public void setImmunizations(ImmunizationsDTO.PatientImmunizationDTO item) {
        immunizations.setValue(item);
    }

    public LiveData<ImmunizationsDTO.PatientImmunizationDTO> getImmunizations() {
        return immunizations;
    }
}
