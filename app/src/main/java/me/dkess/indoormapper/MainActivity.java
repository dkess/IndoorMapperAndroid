package me.dkess.indoormapper;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;

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

        ((ToggleButton) findViewById(R.id.walking_toggle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                walkingTogglePressed(isChecked);
            }
        });
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

    private void walkingTogglePressed(boolean isWalking) {
        File file = new File(getExternalFilesDir(null), "walking_log.txt");
        try {
            FileWriter fileWriter = new FileWriter(file, true);
            String status = isWalking ? "y " : "n ";
            fileWriter.write(status + System.currentTimeMillis() + "\n");
            fileWriter.close();
        } catch (IOException e) {}
    }
}
