package com.teachk.publisherYoumu;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import android.content.pm.PackageManager;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
//import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
//import androidx.annotation.NonNull;
//import com.github.shenyuanqing.zxingsimplify.zxing.Activity.CaptureActivity;
import com.google.gson.Gson;
import com.google.zxing.activity.CaptureActivity;
//import com.teachk.publisherYoumu.bgsegment.BgActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {
    Call<ResponseBody> call=null;
    Button LoginButton=null;
    EditText UserName=null;
    TextView ChangeLoginWay=null;
    EditText PSWText=null;
    Button ScanButton=null;
    ImageView imageView=null;
    Button testBtn =null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        testBtn = (Button)findViewById(R.id.button_test);
        LoginButton=(Button)findViewById(R.id.button);
        UserName=(EditText)findViewById(R.id.editText6);
        ChangeLoginWay=(TextView)findViewById(R.id.UserLoginButton);
        PSWText=(EditText)findViewById(R.id.editText7);
        ScanButton=findViewById(R.id.button3);
        imageView=findViewById(R.id.imageView2);
        ChangeLoginWay.setVisibility(View.INVISIBLE);
        LoginButton.setText("登录");
        UserName.setHint("邮箱/手机号码");
        PSWText.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        UserName.setInputType(InputType.TYPE_CLASS_TEXT);
        //Picasso.get().load("http://62.234.50.92/tk/2.bmp").into(imageView);
        List<String> WaitingPermit=new ArrayList<>();
        String[] WaitingPermits;
        String[] AllPermit={"android.permission.RECORD_AUDIO","android.permission.WRITE_EXTERNAL_STORAGE","android.permission.CAMERA","android.permission.INTERNET"};
        for (int i=0;i<=3;i++){
            if (ContextCompat.checkSelfPermission(LoginActivity.this, AllPermit[i]) != PackageManager.PERMISSION_GRANTED){
                WaitingPermit.add(AllPermit[i]);
            }
        }
        if (WaitingPermit.size()!=0){
            WaitingPermits=new String[WaitingPermit.size()];
            ActivityCompat.requestPermissions(this, WaitingPermit.toArray(WaitingPermits), 1);
        }


        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LoginButton.getText().toString()!="登录"){
                    Toast.makeText(LoginActivity.this,"验证码已发送",Toast.LENGTH_LONG).show();
                    LoginButton.setText("登录");
                }
                else if(UserName.getText().equals("")||PSWText.getText().equals("")){
                    Toast.makeText(LoginActivity.this,"用户名或密码错误！",Toast.LENGTH_LONG).show();
                    PSWText.setText("");
                    return;
                }
                else{
                    InputMethodManager imm=(InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(PSWText.getWindowToken(),0);

                    SystemClock.sleep(100);



                    Ask ask=new Ask(UserName.getText().toString(),PSWText.getText().toString());
                    Gson gson=new Gson();
                    String AskText=gson.toJson(ask);
                    RequestBody requestBody=RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),AskText);
                    Retrofit retrofit=new Retrofit.Builder()
                            .baseUrl("http://114.116.180.115:9000/")
                            .build();
                    GetLiveInf.Live LoginService=retrofit.create(GetLiveInf.Live.class);
                    call=LoginService.Login(requestBody);
                    call.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            try{
                                if (response.body()!=null){
                                    try{
                                        JSONObject Userinf=new JSONObject(response.body().string());
                                        if (Userinf.getInt("code")==401){
                                            Toast.makeText(LoginActivity.this,"用户名或密码错误！",Toast.LENGTH_LONG).show();
                                            PSWText.setText("");
                                        }
                                        else{
                                            JSONObject user=Userinf.getJSONObject("data").getJSONObject("user");
                                            Toast.makeText(LoginActivity.this,"登录成功！",Toast.LENGTH_SHORT).show();

                                            Intent intent=new Intent(LoginActivity.this,LivelistActivity.class);
                                            intent.putExtra("aid",user.getString("aid"));
                                            intent.putExtra("cid",user.getString("cid"));
                                            startActivity(intent);
                                            finish();
                                        }
                                    }
                                    catch (JSONException e){
                                        Toast.makeText(LoginActivity.this,"无法连接，请稍后再试",Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            catch (IOException e){

                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(LoginActivity.this,"无法连接服务器，请稍后重试！",Toast.LENGTH_LONG).show();
                        }
                    });
                    /**/

                }
            }
        });

        /*ChangeLoginWay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeLoginWay.setVisibility(View.INVISIBLE);
                LoginButton.setText("登录");
                UserName.setHint("用户名");
                PSWText.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
                PSWText.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
            }
        });*/

        ScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(LoginActivity.this, CaptureActivity.class);
                //Intent intent=new Intent(LoginActivity.this, Practise.class);
                startActivityForResult(intent,1);
                //startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1:
                String AnsText=data.getStringExtra("barCode");

                try {
                    JSONObject jsonObject=new JSONObject(AnsText);
                    Intent intent2=new Intent(LoginActivity.this,SettingActivity.class);
                    intent2.putExtra("mode","2");
                    intent2.putExtra("URL",jsonObject.getString("push_url"));
                    intent2.putExtra("name",jsonObject.getString("name"));
                    startActivity(intent2);
                    Toast.makeText(this,"扫码成功！",Toast.LENGTH_LONG).show();
                    finish();
                }
                catch (JSONException e){
                    Toast.makeText(this,"非直播二维码！",Toast.LENGTH_LONG).show();
                }


        }
    }
}
