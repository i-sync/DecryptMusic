package com.example.decryptmusic;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.decryptmusic.Adapter.LeftRecyclerViewAdapter;
import com.example.decryptmusic.Adapter.RightRecyclerViewAdapter;
import com.example.decryptmusic.Models.Story;
import com.example.decryptmusic.Utils.FileUtil;
import com.example.decryptmusic.Utils.MediaPlayUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private MediaPlayUtil mediaPlayUtil;
    //control
    private Button btn_decrypt_play;
    private Button btn_decrypt;
    private Button btn_decrypt_all;
    private Button btn_normal_play;
    private Button btn_stop;
    private Button btn_stop_all;
    private TextView txt_album;
    private TextView txt_name;
    private TextView txt_percent;
    private RecyclerView rv_album;
    private RecyclerView rv_story;


    private String rootPath = Environment.getExternalStorageDirectory().getPath();
    private String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    private String dataPath = downloadPath + "/decrypt/data";
    private String sourcePath = downloadPath + "/decrypt/source/"; //String.format("%1$s%4$s%2$s%4$s%3$s%4$s", rootPath, "Music", "source", File.separator);
    private String destPath = downloadPath +  "/decrypt/dest/"; //String.format("%1$s%4$s%2$s%4$s%3$s%4$s", rootPath, "Music", "dest", File.separator);

    private Handler mHandler;

    private Map<String, List<Story>> storyMap = new HashMap<String, List<Story>>();

    private RightRecyclerViewAdapter rAdapter;

    private boolean decryptRunning = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        btn_decrypt_play = findViewById(R.id.btn_decrypt_play);
        btn_decrypt = findViewById(R.id.btn_decrypt);
        btn_decrypt_all = findViewById(R.id.btn_decrypt_all);
        btn_normal_play = findViewById(R.id.btn_normal_play);
        btn_stop = findViewById(R.id.btn_stop);
        btn_stop_all = findViewById(R.id.btn_stop_all);

        txt_album = findViewById(R.id.txt_album);
        txt_name = findViewById(R.id.txt_name);
        txt_percent = findViewById(R.id.txt_percent);

        rv_album = findViewById(R.id.rv_album);
        rv_story = findViewById(R.id.rv_story);

        //init
        btn_decrypt_play.setEnabled(true);
        btn_decrypt.setEnabled(true);
        btn_normal_play.setEnabled(true);
        btn_stop.setEnabled(false);
        btn_stop_all.setEnabled(false);


        mediaPlayUtil = new MediaPlayUtil(sourcePath, destPath);
        mediaPlayUtil.txt_info = txt_percent;

        mHandler = new Handler();

        InitRecyclerView();
    }

    private void InitRecyclerView()
    {
        File dataFolder = new File(dataPath);
        if(!dataFolder.exists())
        {
            return;
        }
        File[] jsonFiles = dataFolder.listFiles();
        for(int i=0; i < jsonFiles.length; i++)
        {
            try {
                String json_str;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                    json_str = new String(Files.readAllBytes(jsonFiles[i].toPath()));
                }
                else
                {
                    json_str = FileUtil.read(jsonFiles[i]);
                }
                JSONObject json_data = new JSONObject(json_str);
                String album_name = json_data.getString("album_name");

                JSONArray story_list = json_data.getJSONArray("story_list");
                List<Story> stories = new ArrayList<Story>();
                for(int j = 0; j < story_list.length(); j++)
                {
                    JSONObject json_story = story_list.getJSONObject(j);
                    Story story = parseJsonToStory(json_story);
                    if (story == null)
                    {
                        continue;
                    }
                    story.setAlbum_name(album_name);
                    stories.add(story);
                }

                storyMap.put(album_name, stories);
            }
            catch (Exception e)
            {
                Log.e("JSON Error", e.getMessage());
            }
        }

        rAdapter = new RightRecyclerViewAdapter(this, txt_name);
        rv_album.setLayoutManager(new LinearLayoutManager(this));
        rv_album.setAdapter(new LeftRecyclerViewAdapter(this,rAdapter, storyMap, txt_album));
        rv_story.setLayoutManager(new LinearLayoutManager(this));
        rv_story.setAdapter(rAdapter);
    }

    public Story parseJsonToStory(JSONObject json)
    {
        try {
            String url = json.getString("url");
            String download_url = json.getString("url");
            String mayday = json.has("mayday")? json.getString("mayday"): "";
            int type = json.getInt("type");
            String name = json.getString("name");
            int order_in_album = json.has("order_in_album")? json.getInt("order_in_album"):json.has("order_in_plan")? json.getInt("order_in_plan"): 0;
            return new Story(download_url,url,mayday,type,name, order_in_album);

        }catch (JSONException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlay(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPlay(null);
    }

    /** 启动解码播放 */
    public void doDecryptPlay(View sender) {
        Story story = rAdapter.getCurrentStory();
        if(decryptRunning) return;
        if(story == null)
        {
            txt_name.setText("Please select story!!!");
            return;
        }
        else if(story.getType() == 1 || story.getMayday().isEmpty())
        {
            txt_name.setText("This Story didn't encrypt!!!");
            return;
        }
        String albumStoryPath = story.getAlbum_name() + "/" + story.getName();
        if (!new File(sourcePath + albumStoryPath).exists())
        {
            txt_percent.setText("Source File doesn't exists!"+ sourcePath + albumStoryPath);
            return;
        }
        mediaPlayUtil.setMayday(story.getMayday());
        mediaPlayUtil.setFileName(albumStoryPath);
        mediaPlayUtil.mRunning = true;
        btn_decrypt_play.setEnabled(false);
        btn_decrypt.setEnabled(false);
        btn_normal_play.setEnabled(false);
        btn_stop.setEnabled(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaPlayUtil.doPlayDecoder();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopPlay(null);
                    }
                });
            }
        }).start();
    }

    /** 启动解码 */
    public void doDecrypt(View sender) {
        Story story = rAdapter.getCurrentStory();
        if(decryptRunning) return;
        if(story == null)
        {
            txt_name.setText("Please select story!!!");
            return;
        }
        String albumStoryPath = story.getAlbum_name() + "/" + story.getName();
        // check source file if exists, if not, return
        File sourceFile = new File(sourcePath + albumStoryPath);
        if (!sourceFile.exists())
        {
            txt_percent.setText("Source File doesn't exists!"+ sourcePath + albumStoryPath);
            return;
        }
        // check dest file if exists, if true, return
        File destFile = new File(destPath + albumStoryPath);
        if(destFile.exists())
        {
            txt_percent.setText("Dest File exists!, return. " + destPath + albumStoryPath);
            return;
        }
        // else, check album folder if exists, if not, create.
        else if(!new File(destPath + story.getAlbum_name()).exists())
        {
            new File(destPath + story.getAlbum_name()).mkdir();
        }

        // check story type, if not encrypt , direct copy file.
        if(story.getType() == 1 || story.getMayday().isEmpty())
        {
            txt_name.setText("This Story didn't encrypt!!! Just Copy!!!");
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                    Files.copy(sourceFile.toPath(), destFile.toPath());
                }
                else {
                    FileUtil.copy(sourceFile, destFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        mediaPlayUtil.setMayday(story.getMayday());
        mediaPlayUtil.setFileName(albumStoryPath);
        mediaPlayUtil.mRunning = true;
        btn_decrypt_play.setEnabled(false);
        btn_decrypt.setEnabled(false);
        btn_normal_play.setEnabled(false);
        btn_stop.setEnabled(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaPlayUtil.doDecryptDecoder();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopPlay(null);
                    }
                });
            }
        }).start();
    }

    /** 启动正常播放 */
    public void doNormalPlay(View sender) {
        Story story = rAdapter.getCurrentStory();
        if(decryptRunning) return;
        if(story == null)
        {
            txt_name.setText("Please select story!!!");
            return;
        }
        String albumStoryPath = story.getAlbum_name() + "/" + story.getName();
        if (!new File(destPath + albumStoryPath).exists())
        {
            txt_percent.setText("Dest File doesn't exists!"+ destPath + albumStoryPath);
            return;
        }
        mediaPlayUtil.setFileName(albumStoryPath);
        mediaPlayUtil.mRunning = true;
        btn_decrypt_play.setEnabled(false);
        btn_decrypt.setEnabled(false);
        btn_normal_play.setEnabled(false);
        btn_stop.setEnabled(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaPlayUtil.doNormalDecoder();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopPlay(null);
                    }
                });
            }
        }).start();
    }

    /** 停止 */
    public void stopPlay(View sender) {
        mediaPlayUtil.mRunning = false;
        btn_decrypt_play.setEnabled(true);
        btn_decrypt.setEnabled(true);
        btn_normal_play.setEnabled(true);
        btn_stop.setEnabled(false);
    }

    // 解码所有数据
    public void doDecryptAll(View sender)
    {
        Log.i("doDecrytButton", "Button Click!");
        decryptRunning = true;
        btn_decrypt_all.setEnabled(false);
        btn_stop_all.setEnabled(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                doDecryptAction();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        stopDecryptAll(null);
                        btn_decrypt_all.setEnabled(true);
                    }
                });
            }
        }).start();

    }

    private void doDecryptAction()
    {
        if(storyMap.isEmpty())
        {
            Log.i("doDecrytAction", "Story is Empty!");
            return;
        }
        // foreach all album
        for(final Map.Entry<String, List<Story>> entry : storyMap.entrySet())
        {
            String key = entry.getKey();
            //txt_album.setText(key);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    txt_album.setText(entry.getKey());
                }
            });
            Log.i("Story Album", String.format("###########%s##########", key));
            //foreach all story
            for(final Story story: entry.getValue())
            {
                /*
                txt_name.setText(story.getName());
                Log.i("Story Album", String.format("============> %s", story.getName()));
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!decryptRunning)
                {
                    break;
                }
                if(true)
                    continue;*/
                //remove above


                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        txt_name.setText(story.getName());
                    }
                });

                Log.i("Story Album", String.format("============> %s", story.getName()));

                //
                final String albumStoryPath = story.getAlbum_name() + "/" + story.getName();
                // check source file if exists, if not, return
                File sourceFile = new File(sourcePath + albumStoryPath);
                if (!sourceFile.exists())
                {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            txt_name.setText("Source File doesn't exists!"+ sourcePath + albumStoryPath);
                        }
                    });
                    Log.i("Story Album", String.format("============> %s", "Source File doesn't exists!"+ sourcePath + albumStoryPath));
                    continue;
                }
                // check dest file if exists, if true, return
                File destFile = new File(destPath + albumStoryPath);
                if(destFile.exists())
                {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            txt_name.setText("Dest File exists!, return. " + destPath + albumStoryPath);
                        }
                    });

                    Log.i("Story Album", String.format("============> %s", "Dest File exists!, return. " + destPath + albumStoryPath));
                    continue;
                }
                // else, check album folder if exists, if not, create.
                else if(!new File(destPath + story.getAlbum_name()).exists())
                {
                    new File(destPath + story.getAlbum_name()).mkdir();
                }

                // check story type, if not encrypt , direct copy file.
                if(story.getType() == 1 || story.getMayday().isEmpty())
                {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            txt_name.setText("This Story didn't encrypt!!! Just Copy!!!");
                        }
                    });
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                            Files.copy(sourceFile.toPath(), destFile.toPath());
                        }
                        else {
                            FileUtil.copy(sourceFile, destFile);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                mediaPlayUtil.mRunning = true;
                mediaPlayUtil.setMayday(story.getMayday());
                mediaPlayUtil.setFileName(albumStoryPath);

                mediaPlayUtil.doDecryptDecoder();


                if(!decryptRunning)
                {
                    break;
                }
            }

            if(!decryptRunning)
            {
                break;
            }
        }
    }

    public void stopDecryptAll(View sender)
    {
        decryptRunning = false;
        btn_stop_all.setEnabled(false);
    }
}
