package me.dkess.indoormapper;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListViewCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ChooseNode extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_node);

        final Intent data = new Intent();

        Intent intent = getIntent();
        final List<IntString> items =
                (List) intent.getSerializableExtra("me.dkess.indoormapper.DATA");

        String[] item_strs = new String[items.size()];
        int i = 0;
        for (IntString is : items) {
            item_strs[i] = is.n + ": " + is.s;
            i++;
        }
        ListView listView = (ListView) findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, item_strs);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                data.putExtra("me.dkess.indoormapper.CHOICE_ID", items.get(position).n);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        EditText editText = (EditText) findViewById(R.id.editText);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    data.putExtra("me.dkess.indoormapper.CHOICE_ID", -1);
                    System.out.println("the text box has text "+v.getText());
                    data.putExtra("me.dkess.indoormapper.NEW_NODE", v.getText().toString());
                    setResult(RESULT_OK, data);
                    finish();
                    return true;
                }
                return false;
            }
        });
    }
}
