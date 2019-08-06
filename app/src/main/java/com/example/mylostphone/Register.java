package com.example.mylostphone;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import com.example.aes128_tokenbase64_farras.AES128TokenBase64;

public class Register extends AppCompatActivity {
    private EditText fullName,userName,password1,password2;
    private ProgressBar loading;
    Button register,login;

    //deklarasi library AES128TokenBase64
    private static AES128TokenBase64 cipher;

    //deklarasi & inisialisasi API URL
    private static String URL_REGIST = "http://mylostphone.000webhostapp.com/server/JSON/api_registerSECURE.php";

    //deklarasi session
    SessionManager sessionManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = new SessionManager(this);


        loading=findViewById(R.id.loading);
        fullName = findViewById(R.id.fullname);
        userName = findViewById(R.id.username);
        password1 = findViewById(R.id.passwords);
        password2 = findViewById(R.id.passwords2);

        register = findViewById(R.id.register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                int a = 0;
                String m_fullName = fullName.getText().toString().trim();
                String m_userName = userName.getText().toString().trim();
                String m_password1 = password1.getText().toString().trim();
                String m_password2 = password2.getText().toString().trim();

                if (m_fullName.isEmpty()||m_fullName.length()<4){
                    fullName.setError("Please Input Valid Full Name");
                    a =1;
                }
                if (m_userName.isEmpty()){
                    userName.setError("Please Input Username");
                    a =1;
                }
                if (m_password1.isEmpty()||m_password1.length()<6){
                    password1.setError("Passwords must be at least 6 characters in length");
                    a =1;
                }
                if (m_password2.isEmpty()){
                    password2.setError("Please Input Confirm Password");
                    a =1;
                }else{
                    if (!m_password1.equals(m_password2)){
                        password2.setError("Password and confirm password don't match");
                        a =1;
                    }
                }


                if (a==0){
                    Regist();
                }

            }
        });

        login = findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Login();
            }
        });

    }

    private void Login(){
        Intent intent = new Intent(Register.this,MainActivity.class);
        startActivity(intent);
    }
    private void Regist(){
        loading.setVisibility(View.VISIBLE);
        register.setVisibility(View.GONE);

        final String fullName = this.fullName.getText().toString().trim();
        final String userName = this.userName.getText().toString().trim();
        final String password2 = this.password2.getText().toString().trim();

        // Plaintext & Kunci AES-128
        String plaintext = "fullName="+fullName+"&userName="+userName+"&password="+password2;
        String kunciAES128 = "./sdafarras83729";

        // Mengubah Plaintext & Kunci AES-128 menjadi bentuk byte
        byte[] inputText = plaintext.getBytes();
        byte[] key = kunciAES128.getBytes();

        // Pembuatan objek bernama cipher dari AES Class, sekaligus melakukan inisialisasi
        cipher = new AES128TokenBase64(key);

        // Melakukan proses enkripsi dan enkode
        final String secretKey = new String(cipher.ECB_encrypt(inputText));

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_REGIST,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            JSONObject jsonObject = new JSONObject(response);
                            String success =  jsonObject.getString("success");

                            if (success.equals("1")){
                                Toast.makeText(Register.this,"Register Success!!",Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(Register.this,MainActivity.class);
                                startActivity(intent);
                            }
                            if (success.equals("0")){
                                Toast.makeText(Register.this,"Register failed, username already taken",Toast.LENGTH_SHORT).show();
                                loading.setVisibility(View.GONE);
                                register.setVisibility(View.VISIBLE);
                            }
                        }catch (JSONException e){
                            e.printStackTrace();
                            Toast.makeText(Register.this,"Register Error!! "+e.toString(),Toast.LENGTH_SHORT).show();
                            System.out.println(response);
                            loading.setVisibility(View.GONE);
                            register.setVisibility(View.VISIBLE);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(Register.this,"Connection error, failed to register",Toast.LENGTH_SHORT).show();
                        loading.setVisibility(View.GONE);
                        register.setVisibility(View.VISIBLE);
                    }
                })
        {
            @Override
            protected Map<String,String> getParams() throws AuthFailureError{
                Map<String,String>params = new HashMap<>();
                params.put("Secret_text",secretKey);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }
}

