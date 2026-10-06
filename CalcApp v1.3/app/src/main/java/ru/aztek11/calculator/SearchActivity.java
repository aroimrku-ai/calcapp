package ru.aztek11.calculator;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class SearchActivity extends Activity {

    private WebView web;
    private EditText urlBar;
    private ProgressBar progress;
    private static final String HOME = "https://duckduckgo.com/";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(0xFF1A1A1A);
        bar.setPadding(8, 8, 8, 8);

        Button back = new Button(this);
        back.setText("<");
        back.setTextColor(0xFFFFFFFF);
        back.setBackgroundColor(0xFF333333);
        back.setOnClickListener(v -> finish());

        urlBar = new EditText(this);
        urlBar.setHint("Поиск или URL");
        urlBar.setSingleLine(true);
        urlBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlBar.setTextColor(0xFFFFFFFF);
        urlBar.setHintTextColor(0xFF666666);
        urlBar.setBackgroundColor(0xFF222222);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        urlBar.setLayoutParams(lp);

        Button go = new Button(this);
        go.setText(">");
        go.setTextColor(0xFFFFFFFF);
        go.setBackgroundColor(0xFF4CAF50);
        go.setOnClickListener(v -> doGo());

        bar.addView(back);
        bar.addView(urlBar);
        bar.addView(go);

        progress = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(ProgressBar.GONE);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                progress.setProgress(p);
                if (p < 100) progress.setVisibility(ProgressBar.VISIBLE);
                else progress.setVisibility(ProgressBar.GONE);
            }
        });

        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        web.setLayoutParams(wlp);

        root.addView(bar);
        root.addView(progress);
        root.addView(web);
        setContentView(root);

        web.loadUrl(HOME);

        urlBar.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) { doGo(); return true; }
            return false;
        });
    }

    private void doGo() {
        String q = urlBar.getText().toString().trim();
        if (q.isEmpty()) return;
        String url;
        if (q.matches("^https?://.*")) url = q;
        else if (q.matches("^[\\w-]+\\.[a-z]{2,}(/.*)?$")) url = "https://" + q;
        else url = HOME + "?q=" + android.net.Uri.encode(q);
        web.loadUrl(url);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(code, e);
    }
}
