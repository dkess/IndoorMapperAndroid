package me.dkess.indoormapper;

import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {
    private boolean isUndoLongPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button undoButton = (Button) findViewById(R.id.b_undo);
        undoButton.setOnLongClickListener(undoLongClickListener);
        undoButton.setOnTouchListener(undoTouchListener);

        final int[] forceButtonIds = {R.id.b_forceleft, R.id.b_forceforward, R.id.b_forceright};
        for (int i : forceButtonIds) {
            ToggleButton tb = (ToggleButton) findViewById(i);
            final int ii = i;
            tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        for (int j : forceButtonIds) {
                            if (j != ii) {
                                ((ToggleButton) findViewById(j)).setChecked(false);
                            }
                        }
                    }
                }
            });
        }
    }

    private View.OnLongClickListener undoLongClickListener = new View.OnLongClickListener(){
        public boolean onLongClick(View v) {
            isUndoLongPressed = true;
            return false;
        }
    };


    private View.OnTouchListener undoTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View pView, MotionEvent pEvent) {
            if (pEvent.getAction() == MotionEvent.ACTION_DOWN) {
                isUndoLongPressed = false;
            }
            return false;
        }
    };

    private void resetButtons() {
        int[] ids = {R.id.b_left, R.id.b_forward, R.id.b_right,
                R.id.b_forceleft, R.id.b_forceforward, R.id.b_forceright};
        for (int i : ids) {
            ToggleButton tb = (ToggleButton) findViewById(i);
             tb.setChecked(false);
        }
    }

    public void onGoPress(View view) {
        resetButtons();
        ((TextView) findViewById(R.id.textView)).setText("hello");
    }

    public void onUndoPress(View view) {
        if (isUndoLongPressed) {
            ((TextView) findViewById(R.id.textView)).setText("undo pressed");
            resetButtons();
        }
        isUndoLongPressed = false;
    }
}
