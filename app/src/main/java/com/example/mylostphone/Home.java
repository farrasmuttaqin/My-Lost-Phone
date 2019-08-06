package com.example.mylostphone;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.aes128_tokenbase64_farras.AES128TokenBase64;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //deklarasi library AES128TokenBase64
    private static AES128TokenBase64 AES128TokenBase64;

    boolean doubleBackToExitPressedOnce = false;

    //background processing pada thread yang berbeda
    private Thread refreshThread;

    //deklarasi session
    SessionManager sessionManager;

    //permission access location
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();

    private String Secret_text;
    private final static int ALL_PERMISSIONS_RESULT = 101;

    //deklarasi location track untuk mengambil longitude dan latitude dari lokasi yang di tracking
    LocationTrack locationTrack;

    //deklarasi & inisialisasi API URL
    private static String URL_INPUT = "http://mylostphone.000webhostapp.com/server/JSON/api_inputCoordinateSECURE.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        sessionManager.checkLogin();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        HashMap<String,String> user = sessionManager.getUserDetail();

        //inisialisasi session
        final String fullName = user.get(sessionManager.FULLNAME);
        final String id_user = user.get(sessionManager.ID_USER);
        final String access_token = user.get(sessionManager.TOKEN);


        NavigationView nav = (NavigationView) findViewById(R.id.nav_view);
        View headerView = nav.getHeaderView(0);
        TextView navUsername = (TextView) headerView.findViewById(R.id.nama_user);
        navUsername.setText("Hi, "+fullName);


        navigationView.getMenu().getItem(0).setChecked(true);


        //memanggil location permission
        permissions.add(ACCESS_FINE_LOCATION);
        permissions.add(ACCESS_COARSE_LOCATION);

        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (permissionsToRequest.size() > 0)
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }

        //memilih id switch
        Switch sw = (Switch) findViewById(R.id.switch1);

        //mengirim lokasi ketika switch dalam keadaan ON
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //menjalankan thread jika switch dalam keadaan ON
                    refreshThread = new Thread() {
                        @Override
                        public void run() {
                            try {
                                while (!isInterrupted()) {
                                    Thread.sleep(10000);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
//
                                            //memanggil fungsi location tracking pada class Home
                                            locationTrack = new LocationTrack(Home.this);


                                            if (locationTrack.canGetLocation()) {

                                                //mendapatkan nilai longitude dan latitude
                                                final double longitude = locationTrack.getLongitude();
                                                final double latitude = locationTrack.getLatitude();

                                                final String longitude2 = String.valueOf(longitude);
                                                final String latitude2 = String.valueOf(latitude);

                                                // Plaintext & Kunci AES-128
                                                String plaintext = "id_user="+id_user+"&access_token="+access_token+"&longitude="+longitude2+"&latitude="+latitude2;
                                                String kunciAES128 = "./sdafarras83729";

                                                // Mengubah Plaintext & Kunci AES-128 menjadi bentuk byte
                                                byte[] inputText = plaintext.getBytes();
                                                byte[] key = kunciAES128.getBytes();

                                                // Pembuatan objek bernama cipher dari AES Class, sekaligus melakukan inisialisasi
                                                AES128TokenBase64 = new AES128TokenBase64(key);

                                                // Melakukan proses enkripsi dan enkode
                                                Secret_text = new String(AES128TokenBase64.ECB_encrypt(inputText));

                                                // jika terdapat nilai token pada session access_token
                                                if (access_token != ""){
                                                    Secret_text = "Secret_text="+Secret_text+"&access_token="+access_token;
                                                }

                                                //konversi secret_text menjadi byte
                                                byte[] secret_text2 = Secret_text.getBytes();
                                                secret_text2 = Base64.encode(secret_text2, Base64.DEFAULT);

                                                Secret_text = new String(secret_text2);

                                                //melakukan request menggunakan String Request dengan JSON file format
                                                StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_INPUT,
                                                        new Response.Listener<String>() {
                                                            @Override
                                                            public void onResponse(String response) {
                                                                try{
                                                                    JSONObject jsonObject = new JSONObject(response);
                                                                    String success =  jsonObject.getString("message");
                                                                    if (success.equals("success")){
                                                                        Toast.makeText(getApplicationContext(), "Longitude:" + Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude), Toast.LENGTH_SHORT).show();
                                                                    }else{
                                                                        Toast.makeText(getApplicationContext(), "Request Failed", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }catch (JSONException e){
                                                                    e.printStackTrace();
                                                                }
                                                            }
                                                        },
                                                        new Response.ErrorListener() {
                                                            @Override
                                                            public void onErrorResponse(VolleyError error) {
                                                            }
                                                        })
                                                {
                                                    @Override
                                                    protected Map<String,String> getParams() throws AuthFailureError {
                                                        Map<String,String>params = new HashMap<>();
                                                        params.put("Secret_text",Secret_text);
                                                        return params;
                                                    }
                                                };

                                                RequestQueue requestQueue = (RequestQueue) Volley.newRequestQueue(Home.this);
                                                requestQueue.add(stringRequest);

                                            } else {
                                                Toast.makeText(getApplicationContext(), "My Lost Phone Error!, Turn on your Location.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            } catch (InterruptedException e) {
                            }
                        }
                    };
                    refreshThread.start();
                } else {
                    //menghentikan thread jika switch dalam keadaan ON
                    refreshThread.interrupt();
                    // The toggle is disabled
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click back again to exit from My Lost Phone", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce=false;
                }
            }, 2000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }



    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Lost Phone");
            String shareMessage= "\nThis app is Great!!, click bottom link to download.\n\n";
            shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID +"\n\n";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "choose one"));

        } else if (id == R.id.logout) {
            sessionManager.logout();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    //

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<String>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }

                }

                break;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(Home.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


}
