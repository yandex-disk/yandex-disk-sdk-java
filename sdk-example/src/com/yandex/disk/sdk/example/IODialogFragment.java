package com.yandex.disk.sdk.example;

import android.app.ProgressDialog;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

public class IODialogFragment extends DialogFragment {

    protected static final String CREDENTIALS = "example.credentials";

    protected ProgressDialog dialog;

    public void sendException(final Exception ex) {
        dialog.dismiss();
        Toast.makeText(getActivity(), ex.getMessage(), Toast.LENGTH_LONG).show();
    }

    // TODO move all common code here

}
