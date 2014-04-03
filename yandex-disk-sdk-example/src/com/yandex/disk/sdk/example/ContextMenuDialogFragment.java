/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.sdk.example;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

abstract class ContextMenuDialogFragment extends DialogFragment {

    protected static final String CREDENTIALS = "example.credentials";
    protected static final String LIST_ITEM = "example.list.item";

    protected Credentials credentials;
    protected ListItem listItem;

    protected static <T extends ContextMenuDialogFragment> T newInstance(T fragment, Credentials credentials, ListItem listItem) {
        Bundle args = new Bundle();
        args.putParcelable(CREDENTIALS, credentials);
        args.putParcelable(LIST_ITEM, listItem);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        credentials = getArguments().getParcelable(CREDENTIALS);
        listItem = getArguments().getParcelable(LIST_ITEM);
    }
}
