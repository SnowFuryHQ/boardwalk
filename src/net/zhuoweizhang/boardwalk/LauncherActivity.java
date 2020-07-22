package net.zhuoweizhang.boardwalk;

import java.io.File;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;



import net.zhuoweizhang.boardwalk.potato.*;
import net.zhuoweizhang.boardwalk.util.PlatformUtils;
import java.io.IOException;
import net.zhuoweizhang.boardwalk.util.*;

public class LauncherActivity extends Activity implements View.OnClickListener, LaunchMinecraftTask.Listener,
	AdapterView.OnItemSelectedListener {

	public static final String[] versionsSupported = {"1.7.10", "1.8.7"};

	public TextView usernameText;
	static public TextView ram;
	public Button loginButton;
	public TextView progressText;
	public ProgressBar progressBar;
	public Button playButton;
	public TextView recommendationText;
	public boolean refreshedToken = false;
	public boolean isLaunching = false;
	//public Button logoutButton;
	public Button RootModeButton;
	public Button importResourcePackButton;
	public Spinner versionSpinner;
	public List<String> versionsStringList = new ArrayList<String>();
	public ArrayAdapter<String> versionSpinnerAdapter;

	private static Thread extractThread;

	public static final int REQUEST_BROWSE_FOR_CREDENTIALS = 1013; // date when this constant was added
	public static final int REQUEST_BROWSE_FOR_RESOURCE_PACK = 1014;

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			enableLaunchButton();
		}
	};

	public void onCreate(Bundle icicle) {
		try{
		new File("/data/data/net.zhuoweizhang.boardwalk/android_log.txt").delete();
		}catch(Exception e){}
		try
		{
			new ProcessBuilder("logcat", "-v", "long", "-f", "/data/data/net.zhuoweizhang.boardwalk/android_log.txt").start();
		}
		catch (IOException e)
		{}
		super.onCreate(icicle);
		setContentView(R.layout.launcher_layout);
		
		PermissionUtils.GrantExternalRW(this);
		
		loginButton = (Button) findViewById(R.id.launcher_login_button);
		usernameText = (TextView) findViewById(R.id.launcher_username_text);
		ram = (TextView) findViewById(R.id.ram);
		progressText = (TextView) findViewById(R.id.launcher_progress_text);
		progressBar = (ProgressBar) findViewById(R.id.launcher_progress_bar);
		loginButton.setOnClickListener(this);
		playButton = (Button) findViewById(R.id.launcher_play_button);
		playButton.setOnClickListener(this);
		recommendationText = (TextView) findViewById(R.id.launcher_recommendation_text);
		//logoutButton = (Button) findViewById(R.id.launcher_logout_button);
		//logoutButton.setOnClickListener(this);
		RootModeButton = (Button) findViewById(R.id.launcher_root_mode_button);
		RootModeButton.setOnClickListener(this);
		importResourcePackButton = (Button) findViewById(R.id.launcher_import_resource_pack_button);
		importResourcePackButton.setOnClickListener(this);
		versionSpinner = (Spinner) findViewById(R.id.launcher_version_spinner);
		versionSpinner.setOnItemSelectedListener(this);
		updateVersionSpinner();
		//updateUiWithLoginStatus();
		//updateRecommendationText();
		playButton.setEnabled(false);
		handler.sendEmptyMessageDelayed(1337, 1000*1); // 1 seconds
		//refreshToken();

	
	}

	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences prefs = this.getSharedPreferences("launcher_prefs", 0);
		prefs.edit().putString("auth_lastEmail", usernameText.getText().toString()).apply();
	}

	@Override
	protected void onResume() {
		super.onResume();
		usernameText.setText(getSharedPreferences("launcher_prefs", 0).getString("auth_lastEmail", ""));
	}
	
	public static Integer getRAM(){
		return Integer.parseInt(ram.getText().toString());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/*public void updateUiWithLoginStatus() {
		boolean loggedIn = isLoggedIn();
		usernameText.setEnabled(!loggedIn);
		//passwordText.setVisibility(loggedIn? View.GONE: View.VISIBLE);
		playButton.setText(getResources().getText(loggedIn? (refreshedToken? R.string.play_regular : R.string.play_offline)
			: R.string.play_demo));
		loginButton.setVisibility(loggedIn? View.GONE: View.VISIBLE);
		//logoutButton.setVisibility(loggedIn? View.VISIBLE: View.GONE);
		//importCredentialsButton.setVisibility(loggedIn? View.GONE: View.VISIBLE);
	}*/

	public void onClick(View v) {
		if (v == loginButton) {
			doLogin();
		} else if (v == playButton) {
			File f = new File(this.getDir("runtime", 0).getAbsolutePath()+"/librarylwjglopenal-20100824.jar");
			if(f.exists()){
				doPreLaunch();
			}else{
				Toast.makeText(this,"未解压runtime，请解压",Toast.LENGTH_LONG).show();
			}
			
		} else if (v == RootModeButton) {
			doRoot();
		} else if (v == importResourcePackButton) {
			//doBrowseForResourcePack();
			Toast.makeText(this,"正在解压...",Toast.LENGTH_LONG).show();
			extractThread = new Thread(new ExtractRuntime(this));
			extractThread.start();
		}
	}
	public void doRoot(){
		AlertDialog.Builder root_note = new AlertDialog.Builder(LauncherActivity.this);
		root_note.setTitle("提示");
		root_note.setMessage("root模式实际上是关闭SELinux，软重启后才能生效，请注意保存重要数据。\n 你也可以手动软重启 \n 执行的命令为： \n setenforce 0 (关闭SELinux) \n am restart (软重启) \n PS：用了多线程，请等待root软件授权完毕");
		root_note.setPositiveButton("关闭SELinux并软重启", new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface p1, int p2)
				{
					runrootcommand.CloseSELinux();
					runrootcommand.HotReboot();
				}
		});
		root_note.setNeutralButton("仅关闭SELinux", new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface p1, int p2)
			{
				runrootcommand.CloseSELinux();
				Toast.makeText(LauncherActivity.this,"授权超级用户后，您应该手动软重启",Toast.LENGTH_LONG).show();
			}
	
			
		});
		
		root_note.create().show();
	}

	public void doLogin() {
		//new LoginTask(this).execute(usernameText.getText().toString(), passwordText.getText().toString());
	}

	public void doPreLaunch() {
		// Do we have an interstitial loaded?
		
		doLaunch();
		
	}

	public void doLaunch() {
		isLaunching = true;
		
		progressBar.setVisibility(View.VISIBLE);
		loginButton.setVisibility(View.GONE);
		//logoutButton.setVisibility(View.GONE);
		playButton.setVisibility(View.GONE);
		versionSpinner.setVisibility(View.GONE);
		RootModeButton.setVisibility(View.GONE);
		importResourcePackButton.setVisibility(View.GONE);
		new LaunchMinecraftTask(this, this).execute();
	}

	public void onProgressUpdate(String s) {
		progressText.setText(s);
	}

	/*public boolean isLoggedIn() {
		return getSharedPreferences("launcher_prefs", 0).getString("auth_accessToken", null) != null;
	}*/

	/*public void updateRecommendationText() {
		StringBuilder builder = new StringBuilder();
		if (PlatformUtils.getNumCores() < 2) {
			builder.append(getResources().getText(R.string.recommendation_dual_core)).append("\n");
		}
		/*if (PlatformUtils.getTotalMemory() < (900000L * 1024L)) { // 900MB
			builder.append(getResources().getText(R.string.recommendation_memory)).append("\n");
		}*/
		/*recommendationText.setText(builder.toString());
	}
*/
	

	public void onBackPressed() {
		if (isLaunching) return;
		super.onBackPressed();
	}

	
	public void onLaunchError() {
		isLaunching = false;
		playButton.setVisibility(View.VISIBLE);
		importResourcePackButton.setVisibility(View.VISIBLE);
		versionSpinner.setVisibility(View.VISIBLE);
		
		progressBar.setVisibility(View.GONE);
		//updateUiWithLoginStatus();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(getResources().getString(R.string.about_app));
		/*if (Build.VERSION.SDK_INT >= 16) { // Jelly Bean
			menu.add(getResources().getString(R.string.export_log));
		}*/
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		CharSequence itemName = item.getTitle();
		if (itemName.equals(getResources().getString(R.string.about_app))) {
			startActivity(new Intent(this, AboutAppActivity.class));
			return true;
		} else if (false) {
			//doExportLog();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == versionSpinner) {
			String theVersion = versionsStringList.get(position);
			SharedPreferences prefs = this.getSharedPreferences("launcher_prefs", 0);
			if (prefs.getString("selected_version", MainActivity.VERSION_TO_LAUNCH).equals(theVersion)) return;
			prefs.edit().putString("selected_version", theVersion).apply();
			System.out.println("Version: " + theVersion);
		}
	}
	public void onNothingSelected(AdapterView<?> parent) {
	}

	private String[] listVersionsInstalled() {
		File versionsDir = new File(Environment.getExternalStorageDirectory(), "boardwalk/gamedir/versions");
		String[] retval = versionsDir.list();
		if (retval == null) retval = new String[0];
		return retval;
	}

	private void updateVersionSpinner() {
		versionsStringList.addAll(Arrays.asList(versionsSupported));
		for (String s: listVersionsInstalled()) {
			if (!versionsStringList.contains(s)) versionsStringList.add(s);
		}
		String selectedVersion = getSharedPreferences("launcher_prefs", 0).
			getString("selected_version", MainActivity.VERSION_TO_LAUNCH);
		if (!versionsStringList.contains(selectedVersion)) versionsStringList.add(selectedVersion);
		versionSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, versionsStringList);
		versionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		versionSpinner.setAdapter(versionSpinnerAdapter);

		versionSpinner.setSelection(versionsStringList.indexOf(selectedVersion));
		//new RefreshVersionListTask(this).execute();
	}

	public void addToVersionSpinner(List<String> newVersions) {
		String selectedVersion = getSharedPreferences("launcher_prefs", 0).
			getString("selected_version", MainActivity.VERSION_TO_LAUNCH);
		versionsStringList.clear();
		versionsStringList.addAll(newVersions);
		for (String s: listVersionsInstalled()) {
			if (!versionsStringList.contains(s)) versionsStringList.add(s);
		}

		int selectedVersionIndex = versionsStringList.indexOf(selectedVersion);
		System.out.println("Selected version: " + selectedVersion + " index: " + selectedVersionIndex);
		versionSpinnerAdapter.notifyDataSetChanged();
		versionSpinner.setSelection(selectedVersionIndex);
	}

	private void enableLaunchButton() {
		playButton.setEnabled(true);
	}

	/*private void refreshToken() {
		if (!isLoggedIn()) return;
		//new RefreshAuthTokenTask(this).execute();
	}*/

	public void waitForExtras() {
		// from Listener class.
		if (extractThread != null) {
			try {
				extractThread.join();
			} catch (InterruptedException lolnope) {
				lolnope.printStackTrace();
			}
		}
	}

	

}
