package de.ct.ctificate;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button connectBtn = (Button) findViewById(R.id.connectButton);
        connectBtn.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditText url = (EditText) findViewById(R.id.Url);
                    new CertificateLoadingRequest(
                            getBaseContext(),
                            (ExpandableListView) findViewById(R.id.certificateChainView)).execute(
                            url.getText().toString());
                    url.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        );
    }
}
