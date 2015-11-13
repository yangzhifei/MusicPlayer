package com.ckt.musicplayer;

import java.util.ArrayList;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.ckt.modle.LogUtil;
import com.ckt.modle.Mp3Info;
import com.ckt.ui.CustomProgressBar;
import com.ckt.utils.JsonUtils;
import com.ckt.utils.MusicPlayerService;

public class MainActivity extends Activity implements View.OnClickListener,ServiceConnection{
//列表按钮
	private ImageButton list_but = null;
//	播放按钮
	private ImageButton play_but =null;
//	cd与中间镂空控件
	private ImageView cd_view = null;
	private ImageView center_view =null;
	private MediaPlayer mPlayer;
//	歌曲进度显示控件
	private CustomProgressBar circle_progress = null;
	private ProgressBar sound_progress = null;
	private MusicPlayerService.MusicPlayerBinder mBinder;  
    private MediaPlayer mediaPlayer = null;// 播放器       
    private AudioManager audioMgr = null; // Audio管理器，用了控制音量
    private ArrayList<Mp3Info> musicList; // 音乐列表
    private Mp3Info currentSong;
    private ValueAnimator valueAnimator=null;
    float value=0;
    private Handler handler = new Handler();
 	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		
		Intent startServiceIntent = new Intent(this,MusicPlayerService.class);
		startService(startServiceIntent);
		Intent bindIntent = new Intent(this,MusicPlayerService.class);
		bindService(bindIntent,this, BIND_AUTO_CREATE);
		
		cd_view = (ImageView)findViewById(R.id.CD_img);
		circle_progress = (CustomProgressBar)findViewById(R.id.circle_pro);

//		sound_progress = (ProgressBar)findViewById(R.id.sound_progress);
		center_view = (ImageView)findViewById(R.id.music_center);
		list_but = (ImageButton)findViewById(R.id.list_btn);
		play_but = (ImageButton)findViewById(R.id.play_btn);
		list_but.setOnClickListener(this);
		play_but.setOnClickListener(this);
		Bitmap bit = BitmapFactory.decodeResource(getResources(),
				R.drawable.wangfei);
		cd_view.setBackground(new BitmapDrawable(getCircleBitmap(this, bit,
				200)));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == ShowSongActivity.RESULT_CODE) { // 返回了点击的歌曲的index
			int index = data.getIntExtra("position", -1);
			currentSong = musicList.get(0);
			if (index >= 0) {// 这里面做相关的响应---->播放第index首歌
				LogUtil.v("MusicPlayerService", "用户点击播放:" + index);
				currentSong = musicList.get(index);
			}
		}
	}


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		// 打开歌曲列表
		case R.id.list_btn:
			Intent intent = new Intent(MainActivity.this,
					ShowSongActivity.class);
			intent.putExtra("list", JsonUtils.changeListToJsonObj(musicList)); //启动Activity并把歌曲列表传过去
			startActivityForResult(intent, 1111);  //这里要用startActivityForResult
			break;

		case R.id.play_btn:
			// 播放CD旋转动画
			
			/*Animation anim = AnimationUtils.loadAnimation(MainActivity.this,
					R.anim.my_rotate);
			LinearInterpolator lir = new LinearInterpolator();
			anim.setInterpolator(lir);
			cd_view.startAnimation(anim);
			Animation anim_1 = AnimationUtils.loadAnimation(MainActivity.this,
					R.anim.my_rotate_2);
			LinearInterpolator lir_1 = new LinearInterpolator();
			anim.setInterpolator(lir_1);
			center_view.startAnimation(anim_1);*/
			if(valueAnimator == null) {
				valueAnimator = ValueAnimator.ofFloat(360);
				LinearInterpolator lir = new LinearInterpolator();
				valueAnimator.setInterpolator(lir);
				valueAnimator.setRepeatCount(-1);
		        valueAnimator.setDuration(20000).addUpdateListener(new AnimatorUpdateListener() {
					
					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						// TODO Auto-generated method stub
						cd_view.setRotation((Float) animation.getAnimatedValue()+value);	
					}
				});
			}
	     
			// 播放音乐
			try {
				int state = mBinder.getPlayerState();
				if (state == 0) { // 停止 ----> 播放
					mPlayer.reset();
					mPlayer.setDataSource(musicList.get(0).getPath());
					mPlayer.prepare();
					mPlayer.start();
//					valueAnimator.setFloatValues(value);
					valueAnimator.start();
					circle_progress.start();
					state = 1;
				} else if (state == 1) { // 播放--->暂停
					mPlayer.pause();
					valueAnimator.cancel();
					circle_progress.stop();
					value = cd_view.getRotation();
					state = 2;
				} else { // 暂停---->播放
					mPlayer.start();
					valueAnimator.start();
					circle_progress.start();
					state = 1;
				}
				mBinder.setPlayerState(state);
			} catch (Exception e) {
			}
		}
	}

	public static Bitmap getCircleBitmap(Context context, Bitmap src, float radius) {  
    	radius = dipTopx(context, radius);   
    	int w = src.getWidth();   
    	int h = src.getHeight();  
    	int canvasW = Math.round(radius * 2);  
    	Bitmap bitmap = Bitmap.createBitmap(canvasW, canvasW,       
    			Bitmap.Config.ARGB_8888);   
    	Canvas canvas = new Canvas(bitmap);  
    			Path path = new Path();   
    			path.addCircle(radius, radius, radius, Path.Direction.CW);   
    			canvas.clipPath(path);   
    			Paint paint = new Paint();  
    			paint.setAntiAlias(true);   
    			Rect srcRect = new Rect(0, 0, w, h);  
    			Rect dstRect = new Rect(0, 0, canvasW, canvasW);  
    			canvas.drawBitmap(src, srcRect, dstRect, paint);  
    			return bitmap;
    }
	
	public static float dipTopx(Context context, float dpValue) {  
    	final float scale = context.getResources().getDisplayMetrics().density;   
    	return  (dpValue * scale + 0.5f);
    }
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		// TODO Auto-generated method stub
		mBinder = (MusicPlayerService.MusicPlayerBinder)service;
		mPlayer = mBinder.getMusicPlayer();
		this.musicList = mBinder.getMusicList();
		LogUtil.v("MainActivity", "onServiceConnected");
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		// TODO Auto-generated method stub
		LogUtil.v("MainActivity", "onServiceDisconnected");
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if(valueAnimator!=null) {
			valueAnimator.cancel();
			value = cd_view.getRotation();
		}
		super.onPause();
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		if(valueAnimator != null) {
			if(mPlayer.isPlaying()) {
				handler.postDelayed(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						valueAnimator.start();
					}
				}, 500);
			}
		}
		super.onResume();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unbindService(this);
//		Intent stopServiceIntent = new Intent(this,MusicPlayerService.class);
//		stopService(stopServiceIntent);
		LogUtil.v("MainActivity", "onDestory");
	}

}
