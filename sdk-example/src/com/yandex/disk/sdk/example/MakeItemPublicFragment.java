/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.sdk.example;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.WebdavException;

import java.io.IOException;

public class MakeItemPublicFragment extends IODialogFragment {

    private static final String TAG = "MakeFolderFragment";

    private static final String WORK_FRAGMENT_TAG = "MakeFolderFragment.Background";

    private static final String URL_TO_PUBLIC_PATH = "example.url.to.public.path";
    private static final String MAKE_PUBLIC_OR_EXPIRE = "example.make.public.or.expire";

    private Credentials credentials;
    private String path;
    private boolean makePublicOrExpire;

    private MakeItemPublicRetainedFragment workFragment;

    public static MakeItemPublicFragment newInstance(Credentials credentials, String path, boolean makePublicOrExpire) {
        MakeItemPublicFragment fragment = new MakeItemPublicFragment();

        Bundle args = new Bundle();
        args.putParcelable(CREDENTIALS, credentials);
        args.putString(URL_TO_PUBLIC_PATH, path);
        args.putBoolean(MAKE_PUBLIC_OR_EXPIRE, makePublicOrExpire);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        credentials = getArguments().getParcelable(CREDENTIALS);
        path = getArguments().getString(URL_TO_PUBLIC_PATH);
        makePublicOrExpire = getArguments().getBoolean(MAKE_PUBLIC_OR_EXPIRE);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        workFragment = (MakeItemPublicRetainedFragment) fragmentManager.findFragmentByTag(WORK_FRAGMENT_TAG);
        if (workFragment == null || workFragment.getTargetFragment() == null) {
            workFragment = new MakeItemPublicRetainedFragment();
            fragmentManager.beginTransaction().add(workFragment, WORK_FRAGMENT_TAG).commit();
            workFragment.changePublicState(getActivity(), credentials, path, makePublicOrExpire);
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

    public void onComplete(String url) {
        dialog.dismiss();
        if (url != null) {
            ShowNewPublicUrlDialogFragment.newInstance(url).show(getFragmentManager(), "showNewPublicUrlDialog");
        } else {
            Toast.makeText(getActivity(), R.string.example_publish_show_url_revoked, Toast.LENGTH_LONG).show();
        }
        ((ExampleActivity) getActivity()).reloadContent();
    }

    public static class MakeItemPublicRetainedFragment extends IODialogRetainedFragment {

        public void changePublicState(final Context context, final Credentials credentials, final String path, final boolean makePublicOrExpire) {

            new AsyncTask<Void, Void, String>() {

                @Override
                protected String doInBackground(Void... params) {
                    TransportClient client = null;
                    try {
                        client = TransportClient.getInstance(context, credentials);
                        if (makePublicOrExpire) {
                            return client.publish(path);
                        } else {
                            client.unpublish(path);
                        }
                    } catch (IOException ex) {
                        Log.d(TAG, "makePublicOrExpire", ex);
                        sendException(ex);
                    } catch (WebdavException ex) {
                        Log.d(TAG, "makePublicOrExpire", ex);
                        sendException(ex);
                    } finally {
                        TransportClient.shutdown(client);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String url) {
                    MakeItemPublicFragment targetFragment = (MakeItemPublicFragment) getTargetFragment();
                    if (targetFragment != null) {
                        targetFragment.onComplete(url);
                    }
                }
            }.execute();
        }
    }

    public static class ShowNewPublicUrlDialogFragment extends DialogFragment {

        protected static final String NEW_PUBLIC_URL = "example.new.public.url";

        protected String url;

        public static ShowNewPublicUrlDialogFragment newInstance(String url) {
            ShowNewPublicUrlDialogFragment fragment = new ShowNewPublicUrlDialogFragment();

            Bundle args = new Bundle();
            args.putString(NEW_PUBLIC_URL, url);
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            url = getArguments().getString(NEW_PUBLIC_URL);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.example_publish_show_url_title)
                    .setMessage(url)
                    .setNegativeButton(R.string.example_publish_show_url_negative_button, null)
                    .create();
        }
    }
}
