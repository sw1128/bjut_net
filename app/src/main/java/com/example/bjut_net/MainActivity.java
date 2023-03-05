package com.example.bjut_net;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.item:
                //弹出对话框
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("使用说明");
                builder.setMessage("① 连接到校园网\n② 连接成功后点击“直接使用此网络”\n※ 该步骤很重要，不然会认证无反应\n③ 回到软件输入学号和密码\n【App会记住学号和密码，不用每次都重新输】\n④ 点击一键认证");
                builder.setPositiveButton("确定",null);
                builder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
        EditText username = findViewById(R.id.usernameInput);
        EditText password = findViewById(R.id.passwordInput);
        username.setText(sp.getString("username", ""));
        password.setText(sp.getString("password", ""));

        TextView textView = findViewById(R.id.textView4);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        TextView textView2 = findViewById(R.id.textView5);
        textView2.setMovementMethod(LinkMovementMethod.getInstance());

        //隐藏ImageView
        ImageView imageView = findViewById(R.id.qrcode);
        imageView.setVisibility(View.INVISIBLE);

        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //每次点击都会切换显示和隐藏
                if (imageView.getVisibility() == View.VISIBLE) {
                    imageView.setVisibility(View.INVISIBLE);
                } else {
                    imageView.setVisibility(View.VISIBLE);
                }
            }
        });

        Button button = findViewById(R.id.loginBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
    }

    public void login() {
        EditText username = findViewById(R.id.usernameInput);
        EditText password = findViewById(R.id.passwordInput);
        String usernameStr = username.getText().toString();
        String passwordStr = password.getText().toString();

        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("username", usernameStr);
        editor.putString("password", passwordStr);
        editor.apply();

        auth1();
        auth2();
    }

    //认证
    public void auth1() {
        EditText username = findViewById(R.id.usernameInput);
        EditText password = findViewById(R.id.passwordInput);
        String usernameStr = username.getText().toString();
        String passwordStr = password.getText().toString();

        new Thread(() -> {
            String urlStr = "http://10.21.221.98:801/eportal/?c=Portal&a=login&login_method=1&user_account=%s@campus&user_password=%s";
            urlStr = String.format(urlStr, usernameStr, passwordStr);
            Log.d("result111", urlStr);
            networkRequest(urlStr);
        }).start();
    }

    public void auth2() {
        new Thread(() -> {
            String urlStr = "https://lgn.bjut.edu.cn";
            networkRequest2(urlStr);
        }).start();
    }

    private void networkRequest(String urlStr){
        HttpURLConnection connection=null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code" + responseCode);
            }
            String result = getStringByStream(connection.getInputStream()).split("\\(")[1].split("\\)")[0];
            String msg = new JSONObject(result).getString("msg");
            Log.d("result111", msg);
            if (msg.equals("")) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "一级认证已完成，请勿重复认证", Toast.LENGTH_SHORT).show();
                });
            } else if (msg.equals("认证成功")) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "一级认证成功", Toast.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "一级认证失败，请检查学号或密码", Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void networkRequest2(String urlStr) {
        EditText username = findViewById(R.id.usernameInput);
        EditText password = findViewById(R.id.passwordInput);
        String usernameStr = username.getText().toString();
        String passwordStr = password.getText().toString();
        HttpURLConnection connection=null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            //设置请求方式 GET / POST 一定要大小
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            DataOutputStream dos=new DataOutputStream(connection.getOutputStream());
            String param="DDDDD=%s&upass=%s&v46s=1&v6ip=&f4serip=172.30.201.10&0MKKey=";
            param = String.format(param, usernameStr, passwordStr);
            dos.writeBytes(param);
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code" + responseCode);
            }
            String result = getStringByStream2(connection.getInputStream());
            Log.d("result111", result);
            if (result != null && result.contains("登录成功窗")) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "二级认证成功，可以上网啦！", Toast.LENGTH_SHORT).show();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "二级认证失败", Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getStringByStream(InputStream inputStream){
        Reader reader;
        try {
            reader=new InputStreamReader(inputStream,"UTF-8");
            char[] rawBuffer=new char[512];
            StringBuffer buffer=new StringBuffer();
            int length;
            while ((length=reader.read(rawBuffer))!=-1){
                buffer.append(rawBuffer,0,length);
            }
            return buffer.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getStringByStream2(InputStream inputStream){
        Reader reader;
        try {
            reader=new InputStreamReader(inputStream,"GBK");
            char[] rawBuffer=new char[512];
            StringBuffer buffer=new StringBuffer();
            int length;
            while ((length=reader.read(rawBuffer))!=-1){
                buffer.append(rawBuffer,0,length);
            }
            return buffer.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}