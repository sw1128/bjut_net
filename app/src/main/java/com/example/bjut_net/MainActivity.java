package com.example.bjut_net;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.item:
                String msg = "① 连接到校园网\n----------------------\n② 连接成功后点击“直接使用此网络”\n※ 该步骤很重要，不然会认证无反应\n-----------------------\n③ 回到软件输入学号和密码\n【App会记住学号和密码，不用每次都重新输】\n-----------------------\n④ 点击一键认证\n-----------------------\n教程：\nhttps://zwhyzzz.top/bjut-net";
                //弹出对话框
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("使用说明");
                builder.setMessage(msg);
                builder.setPositiveButton("确定",null);
                builder.show();
                break;
            case R.id.item2:
                showImageDialog2();
                break;
            case R.id.item3:
                openWifiSettings();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void openWifiSettings() {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        startActivity(intent);
    }
    private void showImageDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image, null);
        ImageView imageView = dialogView.findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.qr1);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(dialogView);
        builder.setPositiveButton("保存到相册", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                saveImageToGallery(bitmap);
            }
        });
        builder.show();
    }

    private String generateUniqueFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "my_image_" + timeStamp + ".jpg";
    }

    private void saveImageToGallery(Bitmap bitmap) {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyApp");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String fileName = generateUniqueFileName();
        File file = new File(directory, fileName);

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
            Toast.makeText(MainActivity.this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "保存图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImageDialog2() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_image, null);
        ImageView imageView = dialogView.findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.add); // 替换为您的图片资源
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(dialogView);
        builder.setPositiveButton("保存到相册", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                saveImageToGallery(bitmap);
            }
        });
        builder.show();
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

        textView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageDialog();
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
    }

    //认证
    public void auth1() {
        EditText username = findViewById(R.id.usernameInput);
        EditText password = findViewById(R.id.passwordInput);
        String usernameStr = username.getText().toString();
        String passwordStr = password.getText().toString();

        new Thread(() -> {
            String urlStr = "http://10.21.221.98:801/eportal/portal/login?callback=dr1003&login_method=1&user_account=,0,%s@campus&user_password=%s";
            urlStr = String.format(urlStr, usernameStr, passwordStr);
            Log.d("result111", urlStr);
            boolean authSuccess = networkRequest(urlStr); // 通过返回值判断认证是否成功

            if (authSuccess) {
                auth2(); // 仅在一级认证成功时执行二级认证
            }
        }).start();
    }

    public void auth2() {
        new Thread(() -> {
            String urlStr = "https://lgn.bjut.edu.cn";
            networkRequest2(urlStr);
        }).start();
    }

    private boolean networkRequest(String urlStr){
        HttpURLConnection connection=null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
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
            if (msg.contains("已经在线")) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
                return false;
            } else if (msg.contains("认证成功")) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "一级认证成功！", Toast.LENGTH_SHORT).show();
                });
                return true;
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                });
                return false;
            }
        } catch (Exception e) {
            Log.d("result111", String.valueOf(e));
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "请检查当前是否连接到校园网！", Toast.LENGTH_SHORT).show();
            });
            return false;
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
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
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