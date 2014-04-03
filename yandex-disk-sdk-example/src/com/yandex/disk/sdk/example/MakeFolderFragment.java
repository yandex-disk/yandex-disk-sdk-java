/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.sdk.example;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.WebdavException;

import java.io.IOException;

public class MakeFolderFragment extends IODialogFragment {

    private static final String TAG = "MakeFolderFragment";

    private static final String WORK_FRAGMENT_TAG = "MakeFolderFragment.Background";

    private static final String MKDIR_PATH = "example.mkdir.path";
    private static final String MKDIR_NAME = "example.mkdir.name";

    private Credentials credentials;
    private String path, name;

    private MakeFolderRetainedFragment workFragment;

    public static MakeFolderFragment newInstance(Credentials credentials, String path, String name) {
        MakeFolderFragment fragment = new MakeFolderFragment();

        Bundle args = new Bundle();
        args.putParcelable(CREDENTIALS, credentials);
        args.putString(MKDIR_PATH, path);
        args.putString(MKDIR_NAME, name);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        credentials = getArguments().getParcelable(CREDENTIALS);
        path = getArguments().getString(MKDIR_PATH);
        name = getArguments().getString(MKDIR_NAME);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        workFragment = (MakeFolderRetainedFragment) fragmentManager.findFragmentByTag(WORK_FRAGMENT_TAG);
        if (workFragment == null || workFragment.getTargetFragment() == null) {
            workFragment = new MakeFolderRetainedFragment();
            fragmentManager.beginTransaction().add(workFragment, WORK_FRAGMENT_TAG).commit();
            workFragment.makeFolder(getActivity(), credentials, path+"/"+name);
        }
        workFragment.setTargetFragment(this, 0);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (workFragment != null) {
            workFragment.setTargetFragment(null, 0);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog = new ProgressDialog(getActivity());
        dialog.setTitle(R.string.example_progress_mkfolder_title);
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setButton(ProgressDialog.BUTTON_NEUTRAL, getText(R.string.example_make_folder_negative_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                dialog.dismiss();
                onCancel();
            }
        });
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        onCancel();
    }

    private void onCancel() {
//        workFragment.cancelDownload();
    }

    public void onComplete() {
        dialog.dismiss();
        Toast.makeText(getActivity(), R.string.example_progress_mkfolder_done, Toast.LENGTH_LONG).show();
        ((ExampleActivity) getActivity()).reloadContent();
    }

    public static class MakeFolderRetainedFragment extends IODialogRetainedFragment {

        public void makeFolder(final Context context, final Credentials credentials, final String path) {

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    TransportClient client = null;
                    try {
                        client = TransportClient.getInstance(context, credentials);
                        client.makeFolder(path);
                    } catch (IOException ex) {
                        Log.d(TAG, "makeFolder", ex);
                        sendException(ex);
                    } catch (WebdavException ex) {
                        Log.d(TAG, "makeFolder", ex);
                        sendException(ex);
                    } finally {
                        TransportClient.shutdown(client);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    MakeFolderFragment targetFragment = (MakeFolderFragment) getTargetFragment();
                    if (targetFragment != null) {
                        targetFragment.onComplete();
                    }
                }
            }.execute();
        }
    }
}
