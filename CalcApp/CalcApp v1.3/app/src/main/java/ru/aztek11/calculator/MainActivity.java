package ru.aztek11.calculator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    private WebView web;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int REQ_FILE = 1001;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        web = new WebView(this);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.setWebViewClient(new WebViewClient());

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view,
                                             ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                filePathCallback = callback;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, REQ_FILE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (res == Activity.RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        results = new Uri[]{ data.getData() };
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(req, res, data);
    }

    private class Bridge {

        @JavascriptInterface
        public void openSearch() {
            runOnUiThread(() -> {
                Intent i = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(i);
            });
        }

        @JavascriptInterface
        public void openNotes() {
            runOnUiThread(() -> {
                Intent i = new Intent(MainActivity.this, NotesActivity.class);
                startActivity(i);
            });
        }

        @JavascriptInterface
        public void openTelegram(String username) {
            final String user = (username == null || username.isEmpty())
                    ? "" : username.replace("@", "");

            runOnUiThread(() -> {
                try {
                    Intent tg = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("tg://resolve?domain=" + user));
                    tg.setPackage("org.telegram.messenger");
                    startActivity(tg);
                    return;
                } catch (Exception ignored) {}

                try {
                    Intent tgx = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("tg://resolve?domain=" + user));
                    tgx.setPackage("org.thunderdog.challegram");
                    startActivity(tgx);
                    return;
                } catch (Exception ignored) {}

                new android.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("Telegram не установлен")
                    .setMessage("Установите Telegram из магазина приложений.")
                    .setPositiveButton("OK", null)
                    .show();
            });
        }

        private File vaultDir() {
            File dir = new File(getFilesDir(), "vault");
            if (!dir.exists()) dir.mkdirs();
            return dir;
        }

        @JavascriptInterface
        public String saveFile(String name, String mime, String base64Data) {
            try {
                byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
                String safeName = System.currentTimeMillis() + "_" +
                        name.replaceAll("[^a-zA-Z0-9._-]", "_");
                File out = new File(vaultDir(), safeName);
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(bytes);
                fos.close();

                SharedPreferences sp = getSharedPreferences("vault_meta", MODE_PRIVATE);
                sp.edit()
                  .putString(safeName + "_name", name)
                  .putString(safeName + "_mime", mime)
                  .putLong(safeName + "_added", System.currentTimeMillis())
                  .apply();

                return safeName;
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        @JavascriptInterface
        public String listFiles() {
            try {
                File[] files = vaultDir().listFiles();
                if (files == null) return "[]";

                SharedPreferences sp = getSharedPreferences("vault_meta", MODE_PRIVATE);

                StringBuilder sb = new StringBuilder("[");
                boolean first = true;
                for (File f : files) {
                    if (!first) sb.append(",");
                    first = false;

                    String key = f.getName();
                    String origName = sp.getString(key + "_name", key);
                    String mime = sp.getString(key + "_mime", "application/octet-stream");
                    long added = sp.getLong(key + "_added", f.lastModified());

                    sb.append("{")
                      .append("\"key\":\"").append(escape(key)).append("\",")
                      .append("\"name\":\"").append(escape(origName)).append("\",")
                      .append("\"type\":\"").append(escape(mime)).append("\",")
                      .append("\"size\":").append(f.length()).append(",")
                      .append("\"added\":").append(added)
                      .append("}");
                }
                sb.append("]");
                return sb.toString();
            } catch (Exception e) {
                return "[]";
            }
        }

        @JavascriptInterface
        public String readFile(String key) {
            try {
                File f = new File(vaultDir(), key);
                if (!f.exists()) return "ERROR:not found";
                byte[] bytes = new byte[(int) f.length()];
                FileInputStream fis = new FileInputStream(f);
                fis.read(bytes);
                fis.close();
                return Base64.encodeToString(bytes, Base64.NO_WRAP);
            } catch (Exception e) {
                return "ERROR:" + e.getMessage();
            }
        }

        @JavascriptInterface
        public boolean deleteFile(String key) {
            try {
                File f = new File(vaultDir(), key);
                boolean ok = !f.exists() || f.delete();

                SharedPreferences sp = getSharedPreferences("vault_meta", MODE_PRIVATE);
                sp.edit()
                  .remove(key + "_name")
                  .remove(key + "_mime")
                  .remove(key + "_added")
                  .apply();

                return ok;
            } catch (Exception e) {
                return false;
            }
        }

        private String escape(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
