package com.kazuapps.libs.crashreportermain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

@SuppressLint("WorldReadableFiles")
public class CrashReporter implements UncaughtExceptionHandler{
	private static Context sContext;
	private static PackageInfo sPkgInfo;
	private static ActivityManager.MemoryInfo sMemInfo = new ActivityManager.MemoryInfo();
	private static final UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();

	/** レポートのファイル名	 */
	private static final String REPORT_FILENAME = "CrashReport.txt";
	/** レポートの送信元 */
	private static final String MAILADDRESS_FROM = "hogehoge@example.com";
	/** レポートの送信先 */
	private static final String MAILADDRESS_TO = "hoge@example.co.jp";

	private static boolean isMailApp = false;
	private static boolean isUserMessage = false;


	/**
	 * クラッシュレポーターの起動
	 * @param context Context
	 * @param mailApp Gmailアプリで送信する場合はtrueにする
	 * @param userMessage ユーザーからのメッセージダイアログの表示を行うか？
	 */
	public static void launch(Context context, boolean mailApp, boolean userMessage){
		sContext = context;
		isMailApp = mailApp;
		isUserMessage = userMessage;
		try{
			sPkgInfo = sContext.getPackageManager().getPackageInfo(sContext.getPackageName(), 0);

			// クラッシュレポートが存在した場合
			if(checkReport()) showSendCheckDialog();
		}catch(NameNotFoundException e){
			e.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
		Thread.setDefaultUncaughtExceptionHandler(new CrashReporter());
	}

	/**
	 * クラッシュレポートが存在するかチェックする
	 */
	private static boolean checkReport(){
		File file = sContext.getFileStreamPath(REPORT_FILENAME);
		if(!file.exists()) return false;

		return true;
	}


	/**
	 * ダイアログを表示させて、ユーザーにクラッシュレポートを送信するか確認する
	 */
	private static void showSendCheckDialog(){
		AlertDialog.Builder alert = new AlertDialog.Builder(sContext);
		alert.setTitle(sContext.getResources().getString(R.string.crashReportDialogTitle));
		alert.setMessage(sContext.getResources().getString(R.string.crashReportDialogMessage));
		alert.setPositiveButton(sContext.getResources().getString(R.string.crashReportSend), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(!isUserMessage) preparationSendCrashReport(null);
				else sendMessageDialog();

				dialog.dismiss();
			}
		});
		alert.setNegativeButton(sContext.getResources().getString(R.string.crashReportSendCancel), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		alert.create().show();

	}

	/**
	 * ユーザーからのメッセージ入力ダイアログ
	 */
	private static void sendMessageDialog(){
		LayoutInflater layout = LayoutInflater.from(sContext);
		final View digView = layout.inflate(R.layout.dialog, null);
		final EditText message = (EditText)digView.findViewById(R.id.userMessageText);

		AlertDialog.Builder alert = new AlertDialog.Builder(sContext);
		alert.setTitle(R.string.crashReportMessageTitle);
		alert.setView(digView);
		alert.setPositiveButton(sContext.getResources().getString(R.string.crashReportSend), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				preparationSendCrashReport(message.getText().toString());
				dialog.dismiss();
			}
		});
		alert.setNegativeButton(sContext.getResources().getString(R.string.crashReportSendCancel), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		alert.create().show();
	}


	/**
	 * クラッシュレポートの送信準備を行う
	 * @param userMsg
	 */
	private static void preparationSendCrashReport(String userMsg){
		StringBuffer sb = new StringBuffer();

		if(isUserMessage == true && userMsg != null && userMsg.length() > 0){
			// ユーザーのメッセージが存在する
			sb.append(sContext.getResources().getString(R.string.crashReportUserMessageText)).append("\n").append(userMsg).append("\n\n");
		}

		T<String, String> t = createMessage();

		/** クラッシュレポートがnullになっている場合は、テキストファイルを正常に読み込めていないため、送信を行わない */
		if(t._1 == null){
			Toast.makeText(sContext, sContext.getResources().getString(R.string.crashReportMessageGetError), Toast.LENGTH_SHORT).show();
			return;
		}

		sb.append(t._1);

		if(!isMailApp){
			// メールアプリを使用しない場合
			ReportSendAsyncTask task = new ReportSendAsyncTask(sContext);

			/**
			 * サーバーURL
			 * 宛先メールアドレス
			 * タイトル
			 * 内容
			 * 送信元メールアドレス
			 */

			task.execute("http://smt-android.info/android/labs/crashReport/CrashReporter.php",
					MAILADDRESS_TO,
					t._0,
					sb.toString(),
					MAILADDRESS_FROM);
		}else{
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_EMAIL, new String[] {MAILADDRESS_TO});
			intent.putExtra(Intent.EXTRA_SUBJECT, t._0);
			intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
			intent.setType("text/plain");
			intent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
			sContext.startActivity(intent);
		}
		
		reportDelete();
	}


	/**
	 * 一時保存されたレポートを取得する
	 * @return 本文となるレポート
	 */
	private static T<String, String> createMessage(){
		File file = sContext.getFileStreamPath(REPORT_FILENAME);
		if(!file.exists()) return null;

		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		String msgTitle = null;
		try{
			try{
				br = new BufferedReader(new FileReader(file));
				String line;
				while((line = br.readLine()) != null){
					if(msgTitle == null){
						msgTitle = sContext.getResources().getString(R.string.crashReportTitle, line);
					}
					sb.append(line).append("\n");
				}
			}finally{
				br.close();
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return new T<String, String>(msgTitle, sb.toString());
	}

	@SuppressWarnings("deprecation")
	@Override
	public void uncaughtException(Thread thread, Throwable ex){
		ex.printStackTrace();

		Time time = new Time();
		time.setToNow();

		String date = time.year + "/" + (time.month + 1) + "/" + time.monthDay + "/ " + time.hour + ":" + time.minute + ":" + time.second + " (" + time.timezone + ")"; 

		PrintWriter writer = null;
		try{
			writer = new PrintWriter(sContext.openFileOutput(REPORT_FILENAME, Context.MODE_WORLD_READABLE));
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}catch (NullPointerException e){
			e.printStackTrace();
		}

		try{
			writer.println(date);
			writer.println("\n");
			writer.println("[AppInfo]");

			if(sPkgInfo != null){
				// アプリの情報が取得できた場合は、パッケージネームなどを出力する
				writer.println("PackageName: " + sPkgInfo.packageName);
				writer.println("VersionName: " + sPkgInfo.versionName);
				writer.println("VersionCode: " + sPkgInfo.versionCode);
			}else{
				// アプリの情報が取得できなかった場合
				writer.println("None");
			}

			writer.println("\n");

			writer.println("[DeviceInfo]");
			((ActivityManager)sContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(sMemInfo);

			// デバイス名
			writer.println("DeviceName: " + Build.DEVICE);
			writer.println("ModelName: " + Build.MODEL);
			writer.println("OSVersion: " + "Android " + Build.VERSION.RELEASE + "(" +  Build.VERSION.SDK_INT + ")");
			writer.println("TotalMemorySize: " + getTotalMemorySize() + "MB");
			writer.println("FreeMemorySize: " + sMemInfo.availMem / 1024 / 1024 + "MB");
			writer.println("UsedMemorySize:" + (getTotalMemorySize() - (sMemInfo.availMem / 1024 / 1024))  + "MB");
			writer.println("ThresholdMemory: " + sMemInfo.threshold / 1024 / 1024 + "MB");
			writer.println("LowMemoryFlag: " + sMemInfo.lowMemory);
			writer.println("\n");

			ex.printStackTrace(writer);
		}finally{
			writer.close();
		}

		handler.uncaughtException(thread, ex);
	}

	private long getTotalMemorySize() {
		try {
			Process process = Runtime.getRuntime().exec("cat /proc/meminfo");
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
			String line = null;
			while ((line = br.readLine()) != null) {
				// "totalMemTotal:         xxxxxxx kB"の部分だけ抜き出す
				if (line.contains("MemTotal:")) {
					line = line.replaceAll("\\D+", "");
					return Long.parseLong(line) / 1024;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0L;
	}
	
	/**
	 * レポートの送信完了後に、保存したファイルを削除する。
	 */
	private static void reportDelete(){
		sContext.deleteFile(REPORT_FILENAME);
	}
}

class T<T1, T2>{
	public T1 _0;
	public T2 _1;

	T(T1 t0, T2 t1){
		_0 = t0;
		_1 = t1;
	}
}
