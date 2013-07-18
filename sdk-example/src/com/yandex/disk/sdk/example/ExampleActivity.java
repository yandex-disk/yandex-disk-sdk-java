/* Copyright (c) 2013 Yandex LLC
 *
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 * License agreement on use of Toolkit
 * «SDK Яндекс.Диска» available at: http://legal.yandex.ru/sdk_agreement
 */

package com.yandex.disk.sdk.example;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExampleActivity extends FragmentActivity {

    private static String TAG = "ExampleActivity";

    public static String FRAGMENT_TAG = "list";

    // create your own client_id on oauth.yandex.ru
    private static String AUTH_URL = "https://oauth.yandex.ru/authorize?response_type=token&client_id=ff90127313fd4378873d6b57914e8e11";

    public static String USERNAME = "example.username";
    public static String TOKEN = "example.token";

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getData() != null) {
            onLogin();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String token = preferences.getString(TOKEN, null);
        if (token == null) {
            new AuthDialogFragment().show(getSupportFragmentManager(), "auth");
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new ListExampleFragment(), FRAGMENT_TAG)
                    .commit();
        }
    }

    private void onLogin () {
        Uri data = getIntent().getData();
        setIntent(null);
        Pattern pattern = Pattern.compile("access_token=(.*?)(&|$)");
        Matcher matcher = pattern.matcher(data.toString());
        if (matcher.find()) {
            final String token = matcher.group(1);
            if (!TextUtils.isEmpty(token)) {
                Log.d(TAG, "onLogin: token: "+token);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putString(USERNAME, "");
                editor.putString(TOKEN, token);
                editor.commit();
            } else {
                Log.w(TAG, "onRegistrationSuccess::empty token");
            }
        } else {
            Log.w(TAG, "onRegistrationSuccess::token not found in return url");
        }
    }

    public void reloadContent() {
        ListExampleFragment fragment = (ListExampleFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        fragment.restartLoader();
    }

    public static class AuthDialogFragment extends DialogFragment {

        public AuthDialogFragment () {
            super();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.example_auth_title)
                    .setMessage(R.string.example_auth_message)
                    .setPositiveButton(R.string.example_auth_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            dialog.dismiss();
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AUTH_URL)));
                        }
                    })
                    .setNegativeButton(R.string.example_auth_negative_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            dialog.dismiss();
                            getActivity().finish();
                        }
                    })
                    .create();
        }
    };
}
