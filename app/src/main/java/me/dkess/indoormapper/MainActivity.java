package me.dkess.indoormapper;

import android.content.Intent;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    private static final IndoorMapper.Direction[] dir_const = {
            IndoorMapper.Direction.left,
            IndoorMapper.Direction.forward,
            IndoorMapper.Direction.right};

    private static final int[] turnButtonIds = {R.id.b_left, R.id.b_forward, R.id.b_right};
    private static final int[] forceButtonIds = {R.id.b_forceleft, R.id.b_forceforward, R.id.b_forceright};


    private boolean isUndoLongPressed = false;
    IndoorMapper indoorMapper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.textView)).setMovementMethod(new ScrollingMovementMethod());

        displayMsg("App started");

        Button undoButton = (Button) findViewById(R.id.b_undo);
        undoButton.setOnLongClickListener(undoLongClickListener);
        undoButton.setOnTouchListener(undoTouchListener);

        /*
        for (int i = 0; i < turnButtonIds.length; i++) {
            final int j = i;
            ToggleButton tb = (ToggleButton) findViewById(turnButtonIds[i]);
            tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        selectedDirs.add(dir_const[j]);
                    } else {
                        selectedDirs.remove(dir_const[j]);
                    }
                }
            });
        }


        for (int i : forceButtonIds) {
            ToggleButton tb = (ToggleButton) findViewById(i);
            final int ii = i;
            tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        for (int j = 0; j < 3; j++) {
                            if (forceButtonIds[j] != ii) {
                                ((ToggleButton) findViewById(forceButtonIds[j])).setChecked(false);
                            } else {
                                forceTurnSelect = dir_const[j];
                            }
                        }
                    } else {
                        forceTurnSelect = null;
                    }
                }
            });
        }
        */
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

    private EnumSet<IndoorMapper.Direction> getSelectedDirs() {
        EnumSet<IndoorMapper.Direction> dirs = EnumSet.noneOf(IndoorMapper.Direction.class);
        for (int i = 0; i < 3; i++) {
            ToggleButton tb = (ToggleButton) findViewById(turnButtonIds[i]);
            if (tb.isChecked()) {
                dirs.add(dir_const[i]);
            }
        }
        return dirs;
    }

    private IndoorMapper.Direction getSelectedForceTurn() {
        for (int i = 0; i < 3; i++) {
            ToggleButton tb = (ToggleButton) findViewById(forceButtonIds[i]);
            if (tb.isChecked()) {
                return dir_const[i];
            }
        }
        return null;
    }

    private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private void displayMsg(String msg) {
        msg = dateFormat.format(new Date()) + " " + msg + "\n";
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(msg + textView.getText());
    }

    public void onGoPress(View view) {
        File file = new File(getExternalFilesDir(null), "nodes.json");
        IndoorMapper.IndoorMap map = IndoorMapper.IndoorMap.readFromFile(file);

        indoorMapper = new IndoorMapper(map);
        IndoorMapper.ForkResult res = indoorMapper.fork_part1(getSelectedDirs());
        if (res == null) {
            displayMsg("Unexpected directions");
        } else if (res.on != null) {
            displayMsg("On node "+res.on_id+": "+res.on);
            interpret_part2(
                    indoorMapper.fork_part2(res.on_id, null, getSelectedForceTurn()));
        } else {
            Intent intent = new Intent(this, ChooseNode.class);
            intent.putExtra("me.dkess.indoormapper.DATA", res.choices);
            startActivityForResult(intent, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                Bundle res = data.getExtras();
                int choice = res.getInt("me.dkess.indoormapper.CHOICE_ID", -2);
                if (choice == -1) {
                    String newNode = res.getString("me.dkess.indoormapper.NEW_NODE");
                    interpret_part2(
                            indoorMapper.fork_part2(choice, newNode, getSelectedForceTurn()));
                } else {
                    interpret_part2(
                            indoorMapper.fork_part2(choice, null, getSelectedForceTurn()));
                }
            }
        }
    }

    private void interpret_part2(IndoorMapper.Direction nextDir) {
        displayMsg("You should turn "+nextDir);
        File file = new File(getExternalFilesDir(null), "nodes.json");
        boolean writeRes = indoorMapper.map.writeToFile(file);
        if (!writeRes) {
            displayMsg("File error! :(");
        }
        resetButtons();
    }

    public void onUndoPress(View view) {
        if (isUndoLongPressed) {
            File file = new File(getExternalFilesDir(null), "nodes.json");
            IndoorMapper.IndoorMap map = IndoorMapper.IndoorMap.readFromFile(file);
            indoorMapper = new IndoorMapper(map);
            IntString res = indoorMapper.undo();

            displayMsg("Undo. Now at " + res.n + ": "+res.s);
            boolean writeRes = indoorMapper.map.writeToFile(file);
            if (!writeRes) {
                displayMsg("File error! :(");
            }
            resetButtons();

        }
        isUndoLongPressed = false;
    }
}
