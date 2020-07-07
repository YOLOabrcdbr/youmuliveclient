package com.teachk.publisherYoumu;

import android.content.Intent;
//import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LivelistActivity extends AppCompatActivity {
    Call<ResponseBody>call=null;
    TextView txv=null;
    ListView listView=null;
    JSONObject jsonObject=null;
    JSONArray jsondata=null;
    List<String>LiveList=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livelist);

        Intent UserInf=getIntent();
        String cid=UserInf.getStringExtra("cid");
        String aid=UserInf.getStringExtra("aid");

        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl("http://114.116.180.115:9000/")
                .build();
        GetLiveInf.Live LiveService=retrofit.create(GetLiveInf.Live.class);
        txv=findViewById(R.id.textView4);
        listView=findViewById(R.id.MainListview);
        call=LiveService.GetRoom(cid,aid);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String s=response.body().string();
                    try {
                        jsonObject=new JSONObject(s);
                        jsondata=jsonObject.getJSONArray("data");
                        LiveList=new ArrayList<>();
                        for (int i=0;i<jsondata.length();i++){
                            LiveList.add(jsondata.getJSONObject(i).getString("name"));
                        }
                        if (jsondata.length()!=0){
                            txv.setVisibility(View.INVISIBLE);
                            listView.setVisibility(View.VISIBLE);
                        }
                        ArrayAdapter<String>adapter=new ArrayAdapter<String>(LivelistActivity.this,android.R.layout.simple_list_item_1,LiveList);
                        listView.setAdapter(adapter);
                    }
                    catch (JSONException e){
                    }
                }
                catch (IOException e){
                    txv.setText("1");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                String s="a";
                txv.setText(s);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try{
                    Intent intent2=new Intent(LivelistActivity.this,SettingActivity.class);
                    intent2.putExtra("mode","2");
                    intent2.putExtra("URL",jsondata.getJSONObject(position).getString("push_url"));
                    intent2.putExtra("name",jsondata.getJSONObject(position).getString("name"));
                    startActivity(intent2);
                }
                catch (JSONException e){

                }
            }
        });
    }
}
