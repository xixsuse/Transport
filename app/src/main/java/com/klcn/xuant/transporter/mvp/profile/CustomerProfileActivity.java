package com.klcn.xuant.transporter.mvp.profile;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.klcn.xuant.transporter.R;
import com.klcn.xuant.transporter.model.Customer;
import com.klcn.xuant.transporter.utils.Base64Utils;
import com.klcn.xuant.transporter.utils.ConvertBitmap;
import com.klcn.xuant.transporter.utils.Utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerProfileActivity extends AppCompatActivity implements View.OnClickListener{

    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private String userChoosenTask;
    CircleImageView mAvatar;
    TextView mTxtName, mTxtMail, mTxtPhone;
    ImageView mImgvBack;
    final Context context = this;
    String encodedString="";
    String newName="";

    FirebaseAuth mFirebaseAuth;
    DatabaseReference customers;
    Customer customerModel;

    FirebaseStorage storage;
    StorageReference storageReference;
    private Uri filePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile);
        mAvatar = (CircleImageView) findViewById(R.id.imgv_avatar);
        mTxtName = (TextView) findViewById(R.id.txt_name_cus);
        mImgvBack = (ImageView) findViewById(R.id.toolbar_back);
        mTxtMail = (TextView) findViewById(R.id.txt_profile_mail);
        mTxtPhone = (TextView) findViewById(R.id.txt_profile_phone);


        mAvatar.setOnClickListener(this);
        mTxtName.setOnClickListener(this);
        mImgvBack.setOnClickListener(this);

        mFirebaseAuth= FirebaseAuth.getInstance();
        customers = FirebaseDatabase.getInstance().getReference().child("Customers").child(mFirebaseAuth.getCurrentUser().getUid());

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        customers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                customerModel = dataSnapshot.getValue(Customer.class);
                if(customerModel.getName()!=null || !customerModel.getName().equals("")){
                    Log.e("profile2::::::",customerModel.getName());
                    mTxtName.setText(customerModel.getName());
                    mTxtMail.setText(customerModel.getEmail());
                    mTxtPhone.setText(customerModel.getPhoneNum());
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if(userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {

                }
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Library",
                "Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(CustomerProfileActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result=Utility.checkPermission(CustomerProfileActivity.this);

                if (items[item].equals("Take Photo")) {
                    userChoosenTask ="Take Photo";
                    if(result)
                        cameraIntent();

                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask ="Choose from Library";
                    if(result)
                        galleryIntent();

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_FILE)
        {
            onSelectFromGalleryResult(data);
            filePath= data.getData();

        }
        else if (requestCode == REQUEST_CAMERA)
            onCaptureImageResult(data);
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");

        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Base64Utils myBitMap = new Base64Utils(this);
        mAvatar.setImageBitmap(thumbnail);
        encodedString = myBitMap.getStringFromBitmap(thumbnail);

    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {

        ConvertBitmap myBitMap = new ConvertBitmap(this);
        Bitmap bitmap = null;
        try {
            bitmap = myBitMap.decodeUri(data.getData());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mAvatar.setImageBitmap(bitmap);
        encodedString = myBitMap.getStringFromBitmap(bitmap);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imgv_avatar:
                selectImage();
                break;
            case R.id.txt_name_cus:
                LayoutInflater layoutInflaterAndroid = LayoutInflater.from(context);
                View mView = layoutInflaterAndroid.inflate(R.layout.input_dialog, null);
                AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(context);
                alertDialogBuilderUserInput.setView(mView);

                final EditText userInputDialogEditText = (EditText) mView.findViewById(R.id.userInputDialog);
                alertDialogBuilderUserInput
                        .setCancelable(false)
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                mTxtName.setText(userInputDialogEditText.getText().toString());
                                newName = userInputDialogEditText.getText().toString();
                            }
                        })

                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialogBox, int id) {
                                        dialogBox.cancel();
                                    }
                                });

                AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
                alertDialogAndroid.show();

                break;
            case R.id.toolbar_back:
                if(mTxtName.getText().toString().equals(customerModel.getName())&& filePath.equals(""))
                {
                    finish();
                }else {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                    builder1.setMessage("Bạn có muốn lưu những thay đổi?");
                    builder1.setCancelable(true);
                    builder1.setPositiveButton(
                            "Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Log.e("CustomerProfile::",newName);
                                    Log.e("profile filePath::::::",filePath + "");
                                    customers.child("name").setValue(newName);
                                    customers.child("imgUrl").setValue(filePath);
                                    finish();
                                }
                            });

                    builder1.setNegativeButton(
                            "No",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    finish();
                                }
                            });

                    AlertDialog alert11 = builder1.create();
                    alert11.show();


                }
                break;

        }
    }

//    private void uploadImage() {
//
//        if(filePath != null)
//        {
//            final ProgressDialog progressDialog = new ProgressDialog(this);
//            progressDialog.setTitle("Uploading...");
//            progressDialog.show();
//
//            StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());
//            ref.putFile(filePath)
//                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                        @Override
//                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                            progressDialog.dismiss();
//                            Toast.makeText(CustomerProfileActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            progressDialog.dismiss();
//                            Toast.makeText(CustomerProfileActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
//                        }
//                    })
//                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
//                        @Override
//                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
//                                    .getTotalByteCount());
//                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
//                        }
//                    });
//        }
//    }


}
