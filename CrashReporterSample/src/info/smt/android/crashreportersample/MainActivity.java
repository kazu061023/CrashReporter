package info.smt.android.crashreportersample;

import com.kazuapps.libs.crashreportermain.CrashReporter;

import android.os.Bundle;
import android.app.Activity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        CrashReporter.launch(this, false, true);
    }
}
