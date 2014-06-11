package com.pubnub.examples.pubnubsubscribe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class PubnubService extends Service {

	private static final String TAG = "PubnubGcm";

	public static final String ACTION_FROM_SERVICE = null;

	private static final int NOTIFICATION_ID = 1;

	public static String device_channel = "Android-1";
	public static String public_channel = "public";
	public static String auth_key = "Android-AuthToken";
	public static String uuid = "Android-user1";
	private NotificationManager mNotificationManager;

	public static String publish_key = "pam";
	public static String subscribe_key = "pam";

	public static Pubnub pubnub = null;
	
	PowerManager.WakeLock wl = null;

	
	public static void initPubnub() {
		if (pubnub != null) return;
		pubnub = new Pubnub(publish_key, subscribe_key, false);
		pubnub.setUUID(uuid);
		pubnub.setAuthKey(auth_key);
		pubnub.setMaxRetries(1000);
	}
	
	private void sendNotification(String msg) {
		mNotificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, PubnubActivity.class), 0);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.icon)
				.setContentTitle("PubNub Notification")
				.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
				.setContentText(msg);

		mBuilder.setContentIntent(contentIntent);
		mBuilder.setAutoCancel(true);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

		try {
			Uri notification = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),
					notification);
			r.play();
		} catch (Exception e) {
			Log.i("PUBNUB", e.toString());
		}
	}
	
	private void notifyUser(JSONObject message) {
		Log.i("PUBNUB", message.toString());
		
		String notification = null;
		Object data = null;
		try {
			data = message.get("data");
		} catch (JSONException e1) {
			Log.i("PUBNUB", e1.toString());
			return;
		}
		try {
			notification = message.getString("message");
		} catch (JSONException e) {
			notification = data.toString();
		}

		
		if (PubnubActivity.activityStarted) {
			Intent intent = new Intent("android.intent.action.MAIN");
			intent.putExtra("data", message.toString());
			sendBroadcast(intent);
		} else {
			sendNotification(notification);
		}
		
		if (data.equals("crash")) {
			// crash now
			String s = null;
			System.out.println(s.toString());
		}
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		   super.onStartCommand(intent, flags, startId);
		   return START_STICKY;
	}

	public void onCreate() {
		super.onCreate();
		initPubnub();
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SubscribeAtBoot");
		if (wl != null) {
			wl.acquire();
			Log.i("PUBNUB", "Partial Wake Lock : " + wl.isHeld());
		}

		Log.i("PUBNUB", "PubnubService created...");

		try {
			pubnub.subscribe(new String[]{public_channel, device_channel}, new Callback() {
				@Override
				public void successCallback(String channel, Object message) {
					notifyUser((JSONObject) message);
				}

				@Override
				public void errorCallback(String channel, Object message) {
					notifyUser((JSONObject) message);
				}
			});
		} catch (PubnubException e) {
			Log.i("PUBNUB", e.toString());
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (wl != null) {
			wl.release();
			Log.i("PUBNUB", "Partial Wake Lock : " + wl.isHeld());
			Toast.makeText(this, "Partial Wake Lock : " + wl.isHeld(),
					Toast.LENGTH_LONG).show();
			wl = null;
		}
		Toast.makeText(this, "PubnubService destroyed...", Toast.LENGTH_LONG)
				.show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}