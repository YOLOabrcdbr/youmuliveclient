package com.teachk.publisherYoumu;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

public class RegActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);

        final Button LoginButton=(Button)findViewById(R.id.button);
        final EditText PhoneNumber=(EditText)findViewById(R.id.editText6);
        final EditText UserName=(EditText)findViewById(R.id.editText5);
        final CheckBox Protocol=(CheckBox)findViewById(R.id.checkBox);
        final EditText PSWText=(EditText)findViewById(R.id.editText7);

        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LoginButton.getText().toString()!="注册"){
                    Toast.makeText(RegActivity.this,"验证码已发送",Toast.LENGTH_LONG).show();
                    LoginButton.setText("注册");
                }
                else{
                    Toast.makeText(RegActivity.this,"注册成功！",Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });


        Protocol.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) LoginButton.setEnabled(true);
            }
        });
    }
}

