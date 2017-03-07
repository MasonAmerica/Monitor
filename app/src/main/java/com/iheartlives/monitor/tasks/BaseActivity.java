package com.iheartlives.monitor.tasks;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.iheartlives.monitor.R;

/**
 * Activity that ensures things stay full screen.
 */
public class BaseActivity extends AppCompatActivity {

    protected boolean mHasOptionsMenu = true;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            updateSystemViews();
        }
    }

    private void updateSystemViews() {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE;
        if (!mHasOptionsMenu) {
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        View topLevel = findViewById(R.id.top_level);
        topLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSystemViews();
            }
        });
    }
}
