/*
 * Лицензионное соглашение на использование набора средств разработки
 * «SDK Яндекс.Диска» доступно по адресу: http://legal.yandex.ru/sdk_agreement
 *
 */

package com.yandex.disk.sdk.example;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;

import java.util.List;

public class ListExampleFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<ListItem>> {

    private static final String TAG = "ListExampleFragment";

    private static final String CURRENT_DIR_KEY = "example.current.dir";

    private static final int GET_FILE_TO_UPLOAD = 100;

    private static final String ROOT = "/";

    private Credentials credentials;
    private String currentDir;

    private ListExampleAdapter adapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setDefaultEmptyText();

        setHasOptionsMenu(true);

        registerForContextMenu(getListView());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String username = preferences.getString(ExampleActivity.USERNAME, null);
        String token = preferences.getString(ExampleActivity.TOKEN, null);

        credentials = new Credentials(username, token);

        Bundle args = getArguments();
        if (args != null) {
            currentDir = args.getString(CURRENT_DIR_KEY);
        }
        if (currentDir == null) {
            currentDir = ROOT;
        }
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(!ROOT.equals(currentDir));

        adapter = new ListExampleAdapter(getActivity());
        setListAdapter(adapter);
        setListShown(false);
        getLoaderManager().initLoader(0, null, this);
    }

    public void restartLoader() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle(getListItem(menuInfo).getDisplayName());

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.example_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListItem listItem = getListItem(item.getMenuInfo());
        switch (item.getItemId()) {
            case R.id.example_context_publish:
                Log.d(TAG, "onContextItemSelected: publish: listItem="+listItem);
                if (listItem.getPublicUrl() != null) {
                    ShowPublicUrlDialogFragment.newInstance(credentials, listItem).show(getFragmentManager(), "showPublicUrlDialog");
                } else {
                    MakeItemPublicFragment.newInstance(credentials, listItem.getFullPath(), true).show(getFragmentManager(), "makeItemPublic");
                }
                return true;
            case R.id.example_context_move:
                RenameMoveDialogFragment.newInstance(credentials, listItem).show(getFragmentManager(), "renameMoveDialog");
                return true;
            case R.id.example_context_delete:
                DeleteItemDialogFragment.newInstance(credentials, listItem).show(getFragmentManager(), "deleteItemDialog");
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private ListItem getListItem(ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        return (ListItem) getListAdapter().getItem(info.position);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.example_action_bar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStack();
                break;
            case R.id.example_add_file:
                openAddFileDialog();
                break;
            case R.id.example_make_folder:
                MakeFolderDialogFragment.newInstance(credentials, currentDir).show(getFragmentManager(), "makeFolderName");
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void openAddFileDialog() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, getText(R.string.example_loading_get_file_to_upload_chooser_title)), GET_FILE_TO_UPLOAD);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_FILE_TO_UPLOAD:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    if ("file".equalsIgnoreCase(uri.getScheme())) {
                        uploadFile(uri.getPath());
                    } else {
                        Toast.makeText(getActivity(), R.string.example_get_file_unsupported_scheme, Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Loader<List<ListItem>> onCreateLoader(int i, Bundle bundle) {
        return new ListExampleLoader(getActivity(), credentials, currentDir);
    }

    @Override
    public void onLoadFinished(final Loader<List<ListItem>> loader, List<ListItem> data) {
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
        if (data.isEmpty()) {
            Exception ex = ((ListExampleLoader) loader).getException();
            if (ex != null) {
                setEmptyText(((ListExampleLoader) loader).getException().getMessage());
            } else {
                setDefaultEmptyText();
            }
        } else {
            adapter.setData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<ListItem>> loader) {
        adapter.setData(null);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        ListItem item = (ListItem) getListAdapter().getItem(position);
        Log.d(TAG, "onListItemClick(): "+item);
        if (item.isCollection()) {
            changeDir(item.getFullPath());
        } else {
            downloadFile(item);
        }
    }

    protected void changeDir(String dir) {
        Bundle args = new Bundle();
        args.putString(CURRENT_DIR_KEY, dir);

        ListExampleFragment fragment = new ListExampleFragment();
        fragment.setArguments(args);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment, ExampleActivity.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    private void downloadFile(ListItem item) {
        DownloadFileFragment.newInstance(credentials, item).show(getFragmentManager(), "download");
    }

    private void uploadFile(String path) {
        UploadFileFragment.newInstance(credentials, currentDir, path).show(getFragmentManager(), "upload");
    }

    private void setDefaultEmptyText() {
        setEmptyText(getString(R.string.example_no_files));
    }

    public static class ListExampleAdapter extends ArrayAdapter<ListItem> {
        private final LayoutInflater inflater;

        public ListExampleAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2);
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setData(List<ListItem> data) {
            clear();
            if (data != null) {
                addAll(data);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            } else {
                view = convertView;
            }

            ListItem item = getItem(position);
            ((TextView)view.findViewById(android.R.id.text1)).setText(item.getDisplayName());
            ((TextView)view.findViewById(android.R.id.text2)).setText(item.isCollection() ? "" : ""+item.getContentLength());

            return view;
        }
    }

    public static class MakeFolderDialogFragment extends DialogFragment {

        private static final String CREDENTIALS = "example.credentials";
        private static final String CURRENT_DIR = "example.current.dir";

        private Credentials credentials;
        private String currentDir;

        public static MakeFolderDialogFragment newInstance(Credentials credentials, String currentDir) {
            MakeFolderDialogFragment fragment = new MakeFolderDialogFragment();

            Bundle args = new Bundle();
            args.putParcelable(CREDENTIALS, credentials);
            args.putString(CURRENT_DIR, currentDir);
            fragment.setArguments(args);

            return fragment;
        }

        public MakeFolderDialogFragment() {
            super();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            credentials = getArguments().getParcelable(CREDENTIALS);
            currentDir = getArguments().getString(CURRENT_DIR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final View view = getActivity().getLayoutInflater().inflate(R.layout.make_folder_layout, null);
            ((EditText) view.findViewById(R.id.example_edit_text_in_dialog)).setHint(R.string.example_make_folder_hint);
            return new AlertDialog.Builder(getActivity())
                    .setView(view)
                    .setTitle(R.string.example_make_folder_title)
                    .setPositiveButton(R.string.example_make_folder_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            String name = ((EditText) view.findViewById(R.id.example_edit_text_in_dialog)).getText().toString();
                            dialog.dismiss();
                            MakeFolderFragment.newInstance(credentials, currentDir, name).show(getFragmentManager(), "makeFolder");
                        }
                    })
                    .setNegativeButton(R.string.example_make_folder_negative_button, null)
                    .create();
        }
    }

    public static class DeleteItemDialogFragment extends ContextMenuDialogFragment {

        public static DeleteItemDialogFragment newInstance(Credentials credentials, ListItem listItem) {
            return newInstance(new DeleteItemDialogFragment(), credentials, listItem);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(listItem.isCollection() ? R.string.example_delete_folder_title : R.string.example_delete_file_title)
                    .setMessage(listItem.getDisplayName())
                    .setPositiveButton(R.string.example_delete_item_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            DeleteItemFragment.newInstance(credentials, listItem.getFullPath(), listItem.getDisplayName(),
                                                           listItem.isCollection()).show(getFragmentManager(), "deleteItem");
                        }
                    })
                    .setNegativeButton(R.string.example_delete_item_negative_button, null)
                    .create();
        }
    }

    public static class RenameMoveDialogFragment extends ContextMenuDialogFragment {

        public static RenameMoveDialogFragment newInstance(Credentials credentials, ListItem listItem) {
            return newInstance(new RenameMoveDialogFragment(), credentials, listItem);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final View view = getActivity().getLayoutInflater().inflate(R.layout.make_folder_layout, null);
            ((EditText) view.findViewById(R.id.example_edit_text_in_dialog)).setHint(R.string.example_move_rename_item_hint);
            return new AlertDialog.Builder(getActivity())
                    .setView(view)
                    .setTitle(R.string.example_move_rename_item_title)
                    .setPositiveButton(R.string.example_move_rename_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            String name = ((EditText) view.findViewById(R.id.example_edit_text_in_dialog)).getText().toString();
                            dialog.dismiss();
                            RenameMoveItemFragment.newInstance(credentials, listItem.getFullPath(),
                                                               name /* assume this is full path */ ).show(getFragmentManager(), "renameMoveItem");
                        }
                    })
                    .setNegativeButton(R.string.example_move_rename_negative_button, null)
                    .create();
        }
    }

    public static class ShowPublicUrlDialogFragment extends ContextMenuDialogFragment {

        public static ShowPublicUrlDialogFragment newInstance(Credentials credentials, ListItem listItem) {
            return newInstance(new ShowPublicUrlDialogFragment(), credentials, listItem);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.example_publish_show_url_title)
                    .setMessage(listItem.getPublicUrl())
                    .setPositiveButton(R.string.example_publish_show_url_positive_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick (DialogInterface dialog, int which) {
                            dialog.dismiss();
                            MakeItemPublicFragment.newInstance(credentials, listItem.getFullPath(), false).show(getFragmentManager(), "makeItemPublic");
                        }
                    })
                    .setNegativeButton(R.string.example_publish_show_url_negative_button, null)
                    .create();
        }
    }
}
