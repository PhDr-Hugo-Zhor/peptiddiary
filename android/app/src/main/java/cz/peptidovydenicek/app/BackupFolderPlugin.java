package cz.peptidovydenicek.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Lets the app remember a single folder (chosen once by the user via the
 * system folder picker) and write backup files into it from then on,
 * without asking again -- using Android's Storage Access Framework.
 */
@CapacitorPlugin(name = "BackupFolder")
public class BackupFolderPlugin extends Plugin {

    private static final String PREFS = "backup_folder_prefs";
    private static final String KEY_URI = "folder_uri";
    private static final String KEY_NAME = "folder_name";

    @PluginMethod
    public void pickFolder(PluginCall call) {
        saveCall(call);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(call, intent, "pickFolderResult");
    }

    @ActivityCallback
    private void pickFolderResult(PluginCall call, androidx.activity.result.ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            call.reject("CANCELLED");
            return;
        }
        Uri treeUri = result.getData().getData();
        if (treeUri == null) {
            call.reject("Nepodařilo se získat vybranou složku");
            return;
        }

        getContext().getContentResolver().takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        );

        DocumentFile dir = DocumentFile.fromTreeUri(getContext(), treeUri);
        String name = (dir != null && dir.getName() != null) ? dir.getName() : "Vybraná složka";

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        prefs.edit().putString(KEY_URI, treeUri.toString()).putString(KEY_NAME, name).apply();

        JSObject ret = new JSObject();
        ret.put("uri", treeUri.toString());
        ret.put("name", name);
        call.resolve(ret);
    }

    @PluginMethod
    public void getFolder(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        String uri = prefs.getString(KEY_URI, null);
        String name = prefs.getString(KEY_NAME, null);
        JSObject ret = new JSObject();
        if (uri != null) {
            ret.put("uri", uri);
            ret.put("name", name);
        }
        call.resolve(ret);
    }

    @PluginMethod
    public void clearFolder(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        prefs.edit().clear().apply();
        call.resolve();
    }

    @PluginMethod
    public void saveFile(PluginCall call) {
        String filename = call.getString("filename");
        String content = call.getString("content");
        if (filename == null || content == null) {
            call.reject("Chybí filename nebo content");
            return;
        }

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        String uriStr = prefs.getString(KEY_URI, null);
        if (uriStr == null) {
            call.reject("NO_FOLDER");
            return;
        }

        Uri treeUri = Uri.parse(uriStr);
        DocumentFile dir = DocumentFile.fromTreeUri(getContext(), treeUri);
        if (dir == null || !dir.canWrite()) {
            call.reject("NO_PERMISSION");
            return;
        }

        DocumentFile existing = dir.findFile(filename);
        if (existing != null) {
            existing.delete();
        }
        DocumentFile file = dir.createFile("application/json", filename);
        if (file == null) {
            call.reject("Nepodařilo se vytvořit soubor ve vybrané složce");
            return;
        }

        try (OutputStream out = getContext().getContentResolver().openOutputStream(file.getUri())) {
            if (out == null) {
                call.reject("Nepodařilo se otevřít soubor pro zápis");
                return;
            }
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e) {
            call.reject("Chyba při zápisu: " + e.getMessage());
            return;
        }

        JSObject ret = new JSObject();
        ret.put("uri", file.getUri().toString());
        call.resolve(ret);
    }
}
