package org.googlecode.vkontakte_android;

import org.googlecode.vkontakte_android.service.CheckingService;
import org.googlecode.vkontakte_android.service.IVkontakteService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class HomeGridActivity extends Activity implements OnItemClickListener {

    private GridView mHomeGrid = null;
    private final static String TAG = "org.googlecode.vkontakte_android.HomeGridActivity";

    public IVkontakteService mVKService;
    private VkontakteServiceConnection mVKServiceConnection = new VkontakteServiceConnection();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.homegrid);


        mHomeGrid = (GridView) findViewById(R.id.HomeGrid);
        mHomeGrid.setNumColumns(3);
        mHomeGrid.setAdapter(new HomeGridAdapter(this));
        mHomeGrid.setOnItemClickListener(this);
        this.setTitle(getResources().getString(R.string.app_name) + " > " + "Home");

        // Binding service
        bindService();
        
        final EditText statusEdit =(EditText) findViewById(R.id.StatusEditText);
        statusEdit.setInputType(InputType.TYPE_NULL);        
        
        statusEdit.setOnTouchListener(new OnTouchListener(){
        	@Override
        	public boolean onTouch(View v, MotionEvent event) {
            statusEdit.setInputType(InputType.TYPE_CLASS_TEXT); 
        	statusEdit.onTouchEvent(event); 
        	return true; // consume touch even
        	}

        	});
    }

    private void backToHome() {
        this.setTitle(getResources().getString(R.string.app_name) + " > " + "Home");
        setProgressBarIndeterminateVisibility(false);
    }

    private void showRequests() {
        Intent i = new Intent(this, FriendsListTabActivity.class);
        i.putExtra(FriendsListTabActivity.SHOW_ONLY_NEW, true);
        startActivity(i);
    }

    /*
    private void showFriends() {
        Intent i = new Intent(this, FriendsListTabActivity.class);
//        i.putExtra(FriendsListTabActivity.SHOW_ONLY_NEW, false);
        startActivity(i);
    }
    */

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        setProgressBarIndeterminateVisibility(true);
        this.setTitle(getResources().getString(R.string.app_name) + " > " + (String) arg1.getTag());

        if (arg1.getTag().equals("Settings")) {
            startActivity(new Intent(this, CSettings.class));
        } else if (arg1.getTag().equals("Requests")) {
            showRequests();
        } else if (arg1.getTag().equals("Help")) {
            AboutDialog.makeDialog(this).show();
            backToHome();
        }
        // Not implemented
        else if (arg1.getTag().equals("Help")
                || arg1.getTag().equals("Search")
                || arg1.getTag().equals("Photos")) {
            Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
            backToHome();
            return;
        } else {
            Intent i = new Intent(this, CGuiTest.class);
            i.putExtra("tabToShow", (String) arg1.getTag());
            startActivity(i);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Activity Resumed");
        backToHome();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "Activity Stopped");
        //try {m_vkService.stop();} catch (RemoteException e) {e.printStackTrace();}
    }

    @Override
    public void onDestroy(){
    	super.onDestroy();
        Log.d(TAG, "Activity Destroyed");
    	unbindService(mVKServiceConnection);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.LogoutMenuItem:
                try {
                    mVKService.logout();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                try {
                    login();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.AboutMenuItem:
                AboutDialog.makeDialog(this).show();
                return true;
            case R.id.ExitMenuItem:
                try {
                    mVKService.stop();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void login() throws RemoteException {
        // TODO handle JSONException in api methods

        if (mVKService.loginAuth()) {
            Log.d(TAG, "Already authorized");
            //initializeUserStuff();
            return;
        }

        final LoginDialog ld = new LoginDialog(this);
        ld.setTitle(R.string.please_login);
        ld.show();


        ld.setOnLoginClick(new View.OnClickListener() {
            public void onClick(View view) {
               	if (! ld.checkCorrectInput(ld.getLogin(), ld.getPass())) {
                	return;
            	} 
            	ld.showProgress();
                String login = ld.getLogin();
                String pass = ld.getPass();
                Log.i(TAG, login + ":--hidden--" );

                new AsyncTask<String, Void, Boolean>() {
                    @Override
                    protected void onPostExecute(Boolean result) {
                        ld.stopProgress();
                        if (result) {
                            ld.dismiss();
                            //initializeUserStuff();
                        } else {
                        	ld.showErrorMessage("Cannot login");
                        }
                    }

                    @Override
                    protected Boolean doInBackground(String... params) {
                        try {
                            return mVKService.login(params[0], params[1]);
                        } catch (RemoteException e) {
                            CGuiTest.fatalError("RemoteException");
                            ld.stopProgress();
                            e.printStackTrace();
                            return false;
                        }
                    }

                }.execute(login, pass);
            }
        });

        ld.setOnCancelClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mVKService.stop();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                ld.dismiss();
                finish();
            }
        });
    }


    // =========  RPC stuff ====================
    /**
     * Binds the service
     */
    private void bindService() {
        Intent i = new Intent(this, CheckingService.class);
        bindService(i, mVKServiceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Binding the service");
    }

    class VkontakteServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder boundService) {
            mVKService = IVkontakteService.Stub.asInterface((IBinder) boundService);
            Log.d(TAG, "Service has been connected");
            // Try to login by saved prefs or show Login Dialog
            try {
                login();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Service has been disconnected");
        }
    }
}
