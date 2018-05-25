package com.klcn.xuant.transporter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.klcn.xuant.transporter.common.Common;
import com.klcn.xuant.transporter.model.Driver;
import com.rengwuxian.materialedittext.MaterialEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class DriverLoginActivity extends AppCompatActivity implements View.OnClickListener{

//    @BindView(R.id.edt_email)
//    EditText mEdtEmail;
//
    @BindView(R.id.root_layout)
    RelativeLayout mRootLayout;

    @BindView(R.id.btn_sign_in)
    Button mBtnSignIn;

    @BindView(R.id.btn_register)
    Button mBtnRegister;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthStateListener;
    private FirebaseDatabase db;
    private DatabaseReference drivers;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                                            .setDefaultFontPath("fonts/Arkhip_font.ttf")
                                            .setFontAttrId(R.attr.fontPath)
                                            .build());
        setContentView(R.layout.activity_driver_login);
        ButterKnife.bind(this);

        // Init Firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        drivers = db.getReference(Common.drivers_tbl);

        firebaseAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    Intent intent = new Intent(getApplicationContext(), DriverMainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        // Init view
        mBtnSignIn.setOnClickListener(this);
        mBtnRegister.setOnClickListener(this);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_sign_in:
                    showSignInDialog();
                break;
            case R.id.btn_register:
                    showRegisterDialog();
                break;
        }
    }

    private void showRegisterDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("REGISTER");
        builder.setMessage("Please user email to register");
        builder.setCancelable(true);

        LayoutInflater inflater = LayoutInflater.from(this);
        View registerLayout = inflater.inflate(R.layout.layout_register_driver,null);

        builder.setView(registerLayout);
        final AlertDialog dialog;
        dialog = builder.create();

        final MaterialEditText editEmail = registerLayout.findViewById(R.id.edt_email_register);
        final MaterialEditText editPassword = registerLayout.findViewById(R.id.edt_pass_register);
        final MaterialEditText editName = registerLayout.findViewById(R.id.edt_name_register);
        final MaterialEditText editPhone = registerLayout.findViewById(R.id.edt_phone_register);
        final TextView txtRegister = registerLayout.findViewById(R.id.txt_register);
        final TextView txtCancel = registerLayout.findViewById(R.id.txt_cancel);

        // Set button dialog
        txtRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(editEmail.getText().toString())){
                    Toast.makeText(getApplicationContext(), "Please enter email address",Toast.LENGTH_SHORT)
                            .show();
                }else if(TextUtils.isEmpty(editPassword.getText().toString())){
                    Toast.makeText(getApplicationContext(), "Please enter password",Toast.LENGTH_SHORT)
                            .show();
                }else if(TextUtils.isEmpty(editName.getText().toString())){
                    Toast.makeText(getApplicationContext(), "Please enter your name",Toast.LENGTH_SHORT)
                            .show();
                }else if(TextUtils.isEmpty(editPhone.getText().toString())){
                    Toast.makeText(getApplicationContext(), "Please enter phone number",Toast.LENGTH_SHORT)
                            .show();
                }else if(editPassword.getText().toString().length()<6){
                    Toast.makeText(getApplicationContext(), "Password too short",Toast.LENGTH_SHORT)
                            .show();
                }else {

                    mFirebaseAuth.createUserWithEmailAndPassword(editEmail.getText().toString(),editPassword.getText().toString())
                            .addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(!task.isSuccessful()){
                                        dialog.dismiss();
                                        Snackbar.make(mRootLayout, "Register fail! "+task.getException().getMessage(), Snackbar.LENGTH_LONG)
                                                .show();
                                    }else{
                                        Driver driver = new Driver();
                                        driver.setEmail(editEmail.getText().toString());
                                        driver.setName(editName.getText().toString());
                                        driver.setPhoneNum(editPhone.getText().toString());
                                        drivers.child(mFirebaseAuth.getCurrentUser().getUid())
                                                .setValue(driver)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Snackbar.make(mRootLayout, "Register success", Snackbar.LENGTH_LONG)
                                                                .show();
                                                        dialog.dismiss();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Snackbar.make(mRootLayout, "Create fail! "+e.getMessage(), Snackbar.LENGTH_LONG)
                                                                .show();
                                                    }
                                                });
                                    }
                                }
                            });


                }
            }
        });

        txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    private void showSignInDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("SIGN IN");
        builder.setCancelable(true);

        LayoutInflater inflater = LayoutInflater.from(this);
        View signInLayout = inflater.inflate(R.layout.layout_sign_in_driver,null);

        builder.setView(signInLayout);
        final AlertDialog dialog;
        dialog = builder.create();

        final MaterialEditText editEmail = signInLayout.findViewById(R.id.edt_email_sign_in);
        final MaterialEditText editPassword = signInLayout.findViewById(R.id.edt_pass_sign_in);
        final TextView txtSignIn = signInLayout.findViewById(R.id.txt_sign_in);
        final TextView txtCancel = signInLayout.findViewById(R.id.txt_cancel);

        // Set button dialog
        txtSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(editEmail.getText().toString())){
                    Toast.makeText(getApplicationContext(), "Please enter email address",Toast.LENGTH_SHORT)
                            .show();
                }else if(TextUtils.isEmpty(editPassword.getText().toString())){
                    Toast.makeText(getApplicationContext(), "Please enter password",Toast.LENGTH_SHORT)
                            .show();
                }else {
                    mFirebaseAuth.signInWithEmailAndPassword(editEmail.getText().toString(),editPassword.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(!task.isSuccessful()){
                                        Toast.makeText(getApplicationContext(), "Sign in fail! "+task.getException().getMessage(),
                                                Toast.LENGTH_LONG).show();
                                        dialog.dismiss();
                                    }else{
                                        Toast.makeText(getApplicationContext(), "Sign in success!",
                                                Toast.LENGTH_LONG).show();
                                        dialog.dismiss();
                                    }
                                }
                            });
                }
            }
        });

        txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(firebaseAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        mFirebaseAuth.addAuthStateListener(firebaseAuthStateListener);
    }
}
