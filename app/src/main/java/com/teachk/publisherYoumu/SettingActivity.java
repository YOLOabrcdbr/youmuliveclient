package com.teachk.publisherYoumu;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class SettingActivity extends AppCompatActivity {

    Spinner Texture=null;
    Button StartButton=null;
    EditText URL=null;
    EditText Title=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Texture=findViewById(R.id.spinner);
        URL=findViewById(R.id.editText);
        StartButton=findViewById(R.id.button2);
        Title=findViewById(R.id.editText2);

        //String[] TextureList={"1080P 横向(1920*1080)","1080P 纵向(1080*1920)","720P 横向(1280*720)","720P 纵向(720*1280)","480P 横向(720*480)","480P 纵向(480*720)"};
        String[] TextureList={"1080P 横向(1920*1080)","1080P 纵向(1080*1920)","720P 横向(1280*720)","720P 纵向(720*1280)"};
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,TextureList);
        Texture.setAdapter(adapter);
        Texture.setSelection(0);

        Intent StartMode=getIntent();
        URL.setText("rtmp://39.106.194.43:1935/live360/trystream");
        if (StartMode.getStringExtra("mode").equals("2")){
            URL.setText(StartMode.getStringExtra("URL"));
            URL.setEnabled(false);
            Title.setText(StartMode.getStringExtra("name"));
            Title.setEnabled(false);
        }

        StartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int Pos=Texture.getSelectedItemPosition();
                if (Pos%2==0){
                    Intent intent=new Intent(SettingActivity.this,MainLandScapeActivity.class);
                    intent.putExtra("Texture",Pos);
                    intent.putExtra("URL",URL.getText().toString());
                    startActivity(intent);
                }
                else{
                    Intent intent=new Intent(SettingActivity.this,MainActivity.class);
                    intent.putExtra("Texture",Pos);
                    intent.putExtra("URL",URL.getText().toString());
                    startActivity(intent);
                }

            }
        });
    }
}
