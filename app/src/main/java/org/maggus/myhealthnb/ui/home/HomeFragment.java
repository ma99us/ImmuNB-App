package org.maggus.myhealthnb.ui.home;

import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.maggus.myhealthnb.R;
import org.maggus.myhealthnb.api.AuthState;
import org.maggus.myhealthnb.api.EnvConfig;
import org.maggus.myhealthnb.api.dto.AuthorizationCodeDTO;
import org.maggus.myhealthnb.api.dto.DemographicsDTO;
import org.maggus.myhealthnb.api.dto.ImmunizationsDTO;
import org.maggus.myhealthnb.api.dto.UserAuthorizationDTO;
import org.maggus.myhealthnb.databinding.FragmentHomeBinding;
import org.maggus.myhealthnb.http.BinaryRequest;
import org.maggus.myhealthnb.http.DocumentRequest;
import org.maggus.myhealthnb.http.JsonRequest;
import org.maggus.myhealthnb.http.ResponseListener;
import org.maggus.myhealthnb.ui.SharedViewModel;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class HomeFragment extends Fragment {

    private SharedViewModel sharedModel;
    private FragmentHomeBinding binding;
    private TextView textView;
    private LinearLayout loginSubLayout;
    private LinearLayout resetSubLayout;
    private Button buttonLogin;
    private Button buttonReset;
    private EditText loginEmailEdit;
    private EditText loginPasswordEdit;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textView = binding.textHome;
        loginSubLayout = binding.loginSubLayout;
        resetSubLayout = binding.resetSubLayout;
        loginEmailEdit = binding.loginEmailEdit;
        loginPasswordEdit = binding.loginPasswordEdit;
        buttonLogin = binding.buttonLogin;
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                onLogin();
                onDummyLogin();  // #TEST ONLY!
            }
        });
        buttonReset = binding.buttonReset;
        buttonReset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onResetData();
            }
        });


        sharedModel.getImmunizations().observe(getViewLifecycleOwner(), new Observer<ImmunizationsDTO.PatientImmunizationDTO>() {
            @Override
            public void onChanged(@Nullable ImmunizationsDTO.PatientImmunizationDTO dto) {
                formatImmunizations(dto);
            }
        });

        formatImmunizations(null);  // TODO: load data from local storage

        //// #TEST
        loginEmailEdit.setText("gerdov@gmail.com"); // #TEST !  // TODO: remove this!
        loginPasswordEdit.setText("SosiHooy123%");  // #TEST !  // TODO: remove this!
        ////

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateUI() {
        if (sharedModel.getImmunizations().getValue() != null) {
            // hide login, show reset
            loginSubLayout.setVisibility(View.GONE);
            resetSubLayout.setVisibility(View.VISIBLE);
        } else {
            // hide reset, show login
            loginSubLayout.setVisibility(View.VISIBLE);
            resetSubLayout.setVisibility(View.GONE);
        }
    }

    private void formatImmunizations(ImmunizationsDTO.PatientImmunizationDTO dto) {
        if (dto == null) {
            setHtmlText("<h6>No Immunization records.<br>Please login to MyHealthNB first.</h6>");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("<h2>COVID-19 Immunization Record</h2>");
            sb.append("<h3><font color='" + colorFromRes(R.color.success_bg) + "'>" + dto.getFirstName() + " " + dto.getLastName() + "</font></h3>");
            sb.append("<h6>" + dto.getDateOfBirth() + "</h6>");
            sb.append("<hr>");
            for (ImmunizationsDTO.PatientImmunizationDTO.ImmunizationDTO immuDto : dto.getImmunizations()) {
                sb.append("<p><b><font color='" + colorFromRes(R.color.success_bg) + "'>" + immuDto.getVaccinationDate() + "</font></b> "
                        + immuDto.getTradeName() + "</p>");
            }
            setHtmlText(sb.toString());
        }

        updateUI();
    }

    private void formatErrorText(String error) {
        if (error != null && !error.isEmpty()) {
            setHtmlText("<h2><font color='" + colorFromRes(R.color.error_bg) + "'>" + error + "</font></h2>");
        } else {
            setHtmlText("<h2><font color='" + colorFromRes(R.color.success_bg) + "'>OK</font></h2>");
        }
    }

    private void setHtmlText(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textView.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        } else {
            textView.setText(Html.fromHtml(html));
        }
    }

    private String colorFromRes(int id) {
        int color = getResources().getColor(id);
        return "#" + String.format("%X", color).substring(2); // !!strip alpha value!!
    }

    public void onResetData() {
        sharedModel.setAuthState(null);
        sharedModel.setEnvConfig(null);
        sharedModel.setImmunizations(null);
    }

    /**
     * For #TEST ONLY!
     */
    public void onDummyLogin() {
        textView.setText("Connecting...");

        ImmunizationsDTO immunizationsDTO = ImmunizationsDTO.buildDummyDTO();
        sharedModel.setImmunizations(immunizationsDTO.getPatientImmunization());
    }

    public void onLogin() {
        String username = loginEmailEdit.getText().toString().trim();
        String password = loginPasswordEdit.getText().toString().trim();
        if(username.isEmpty() || password.isEmpty()){
            Toast.makeText(getContext(), "Username or Password can not be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        textView.setText("Connecting...");

        sharedModel.setDeviceId(AuthState.generateDeviceId(getContext()));
        sharedModel.setNonce(AuthState.generateRandomString(8));

        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);

        // Instantiate the RequestQueue.
//        final RequestQueue queue = Volley.newRequestQueue(getContext());
        final RequestQueue queue = Volley.newRequestQueue(getContext(), new HurlStack() {
            @Override
            protected HttpURLConnection createConnection(URL url) throws IOException {
                HttpURLConnection connection = super.createConnection(url);
                connection.setInstanceFollowRedirects(false);
                return connection;
            }
        });

        downloadEnvConfig(queue);

        //queue.add(postLogin());
    }

    private Request downloadEnvConfig(RequestQueue queue) {
        textView.setText("Loading environment...");

        final String url = "https://myhealthnb.verosource.com/env-config.js";

        return queue.add(new BinaryRequest(Request.Method.GET, url,
                new Response.Listener<byte[]>() {
                    @Override
                    public void onResponse(byte[] response) {
                        EnvConfig envConfig = new EnvConfig();
                        envConfig.load(new String(response).trim());
                        // validate that config looks good
                        if (!envConfig.containsKey("API_URL") || !envConfig.containsKey("API_TOKEN")) {
                            throw new IllegalArgumentException("Can not read Environment Config");
                        }
                        sharedModel.setEnvConfig(envConfig);

                        getIndex(queue);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        formatErrorText("Config didn't work!");
                    }
                }));
    }

    private Request getIndex(RequestQueue queue) {
        textView.setText("Loading Authorization...");

        //final String url = "https://access.id.gnb.ca/as/authorization.oauth2";
        final String url = sharedModel.getEnvConfig().getProperty("DIGITAL_ID_API_URL") + "authorization.oauth2";

        return queue.add(new DocumentRequest(Request.Method.GET, url,
                new Response.Listener<Document>() {
                    @Override
                    public void onResponse(Document response) {
//                        Log.d("http", "Index response: \n" + response.outerHtml());
//                        textHome.setText("Response is: " + "\n" + response.outerHtml().substring(0, 500));
                        Elements forms = response.select("form[method=POST]");
                        if (forms.size() != 1) {    // assume only one form (login) is on the index page
                            throw new IllegalArgumentException("Can not find login form!");
                        }
                        String action = forms.get(0).attr("action");
                        if (action == null || action.isEmpty()) {
                            throw new IllegalArgumentException("Can not find form action url!");
                        }
                        AuthState authState = new AuthState();
                        authState.setUsername(loginEmailEdit.getText().toString().trim());
                        authState.setPassword(loginPasswordEdit.getText().toString().trim());
                        authState.setLoginFormAction(action);
                        sharedModel.setAuthState(authState);

                        postLogin(queue);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("http", "That didn't work!", error);
                        formatErrorText("Site didn't work!");
                    }
                }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("client_id", sharedModel.getEnvConfig().getProperty("DIGITAL_ID_CLIENT_ID"));       // "covid"
                params.put("response_type", "code");
                params.put("scope", "openid did_health");
                params.put("redirect_uri", sharedModel.getEnvConfig().getProperty("DIGITAL_ID_REDIRECT_URI"));        // "https://myhealthnb.verosource.com"
                params.put("nonce", sharedModel.getNonce());         // "V0l0dE8wN1Y"
                return params;
            }
        });
    }

    private Request postLogin(RequestQueue queue) {
        textView.setText("Logging in...");

        //final String url = "https://access.id.gnb.ca/as/wUdzI/resume/as/authorization.ping";
        final String url = "https://access.id.gnb.ca" + sharedModel.getAuthState().getLoginFormAction();
        Log.d("http", "sending login to: " + url);

        return queue.add(new DocumentRequest(Request.Method.POST, url,
                new Response.Listener<Document>() {
                    @Override
                    public void onResponse(Document response) {
                        // Display the first 500 characters of the response string.
//                        Log.d("http", "Login response: \n" + response.outerHtml());
//                        textHome.setText("Response is: " + "\n" + response.outerHtml().substring(0, 500));
                        Log.e("http", "Got an unexpected document. That should not happen! \n" + response.outerHtml());
                        throw new IllegalArgumentException("Got unexpected response!");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null && error.networkResponse.statusCode == 302) {  // expected redirect request.
                            // ignore redirect itself, all we need from is is "code"
                            String location = error.networkResponse.headers.get("location");
                            if (location != null) {
                                Uri uri = Uri.parse(location);
                                String code = uri.getQueryParameter("code");
                                if (code != null && !code.isEmpty()) {
                                    Log.d("http", "authorization code: " + code);
                                    sharedModel.getAuthState().setAuthorizationCode(code);
                                }
                            }
                        }

                        if (sharedModel.getAuthState().getAuthorizationCode() == null) {
                            Log.e("http", "Login didn't work!", error);
                            formatErrorText("Login didn't work!");
                            return; // stop here
                        }

                        postAuthenticationLogin(queue);
                    }
                }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("pf.username", sharedModel.getAuthState().getUsername());
                params.put("pf.pass", sharedModel.getAuthState().getPassword());
                params.put("pf.ok", "clicked");
                params.put("pf.adapterId", "NBHtmlAdapter");    // NBHtmlAdapter //TODO: maybe try some other id?
                return params;
            }
        });
    }

    private Request postAuthenticationLogin(RequestQueue queue) {
        textView.setText("Authorizing user...");

        final String url = sharedModel.getEnvConfig().getProperty("API_URL") + "v1/authentication/login";     // "https://myhealthnb.verosource.com/results-gateway/v1/authentication/login"

        final AuthorizationCodeDTO data = new AuthorizationCodeDTO();
        data.setType("oidc_authorization_registration_flow");
        AuthorizationCodeDTO.CredentialsDTO credentials = new AuthorizationCodeDTO.CredentialsDTO();
        credentials.setAuthorizationCode(sharedModel.getAuthState().getAuthorizationCode());
        data.setCredentials(credentials);

        return queue.add(new JsonRequest<AuthorizationCodeDTO, UserAuthorizationDTO>(Request.Method.POST, url,
                data, UserAuthorizationDTO.class,
                new ResponseListener<UserAuthorizationDTO>() {
                    @Override
                    public void onResponse(UserAuthorizationDTO response, Map<String, String> headers) {
//                        Log.d("http", "Authorization response: \n" + response);
                        String authorizationToken = headers.get("authorization");
                        Log.d("http", "authorizationToken=" + authorizationToken);
                        if (authorizationToken == null || authorizationToken.isEmpty()) {
                            throw new IllegalArgumentException("Can not find authorization token!");
                        }
                        sharedModel.getAuthState().setAuthorizationToken(authorizationToken);

                        //getDemographics(queue);
                        getImmunizations(queue);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("http", "Authorization didn't work!", error);
                        formatErrorText("Authorization didn't work!");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return sharedModel.populateApiRequestHeaders();
            }
        });
    }

    private Request getDemographics(RequestQueue queue) {
        textView.setText("Loading Demographics...");

        final String url = sharedModel.getEnvConfig().getProperty("API_URL") + "v1/demographics/";     //"https://myhealthnb.verosource.com/results-gateway/v1/demographics/";

        return queue.add(new JsonRequest<AuthorizationCodeDTO, DemographicsDTO>(Request.Method.GET, url,
                null, DemographicsDTO.class,
                new ResponseListener<DemographicsDTO>() {
                    @Override
                    public void onResponse(DemographicsDTO response, Map<String, String> headers) {
                        Log.d("http", "Demographics response: \n" + response);
                        if (response == null || response.getDemographic() == null) {
                            throw new IllegalArgumentException("Can not find demographics records!");
                        }
                        sharedModel.setDemographics(response.getDemographic());

//                        getImmunizations(queue);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("http", "Demographics didn't work!", error);
                        formatErrorText("Demographics didn't work!");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return sharedModel.populateApiRequestHeaders();
            }
        });
    }

    private Request getImmunizations(RequestQueue queue) {
        textView.setText("Loading Immunizations...");

        final String url = sharedModel.getEnvConfig().getProperty("API_URL") + "v1/immunization/";            // "https://myhealthnb.verosource.com/results-gateway/v1/immunization/";

        return queue.add(new JsonRequest<AuthorizationCodeDTO, ImmunizationsDTO>(Request.Method.GET, url,
                null, ImmunizationsDTO.class,
                new ResponseListener<ImmunizationsDTO>() {
                    @Override
                    public void onResponse(ImmunizationsDTO response, Map<String, String> headers) {
                        Log.d("http", "Immunization response: \n" + response);
                        if (response == null || response.getPatientImmunization() == null) {
                            throw new IllegalArgumentException("Can not find immunizations records!");
                        }
                        sharedModel.setImmunizations(response.getPatientImmunization());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("http", "Immunizations didn't work!", error);
                        formatErrorText("Immunizations didn't work!");
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return sharedModel.populateApiRequestHeaders();
            }
        });
    }
}