package com.lanzou.split.ui.selector;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.PagerTitleStrip;

import com.lanzou.split.adapter.FileSelectorAdapter;
import com.lanzou.split.data.FileInfo;
import com.lanzou.split.databinding.FragmentFileSelectorBinding;
import com.lanzou.split.event.OnItemClickListener;
import com.lanzou.split.event.Searchable;
import com.lanzou.split.ui.LinearItemDecoration;
import com.lanzou.split.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileSelectorFragment extends Fragment implements Searchable {

    public static final int TYPE_ALL = 0;
    public static final int TYPE_APP = 1;

    public static final int TYPE_APK = 2;

    public static final int TYPE_IMAGE = 3;

    public static final int TYPE_AUDIO = 4;

    public static final int TYPE_VIDEO = 5;

    public static final int TYPE_DOCUMENT = 6;

    public static FileSelectorFragment newInstance(int type) {
        Bundle args = new Bundle();
        args.putInt("type", type);
        FileSelectorFragment fragment = new FileSelectorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private boolean isFirst = true;

    private FragmentFileSelectorBinding binding;

    private final List<FileInfo> files = new ArrayList<>();

    private final FileSelectorAdapter fileSelectorAdapter = new FileSelectorAdapter(files);

    public List<FileInfo> selectedFiles;

    private String searchWorld = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectedFiles = ((FileSelectorActivity) requireActivity()).getSelectedFiles();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFileSelectorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = binding.fileRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        // recyclerView.addItemDecoration(new LinearItemDecoration());
        recyclerView.setAdapter(fileSelectorAdapter);

        fileSelectorAdapter.setOnItemClickListener((position, view1) -> {
            FileInfo fileInfo = fileSelectorAdapter.getItem(position);
            if (view1.isSelected()) {
                selectedFiles.add(fileInfo);
            } else {
                selectedFiles.remove(fileInfo);
            }
        });

        binding.getRoot().setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                files.clear();
                fileSelectorAdapter.notifyDataSetChanged();
                getFiles();
            }
        });
    }

    private FileSelectorActivity getParent() {
        return (FileSelectorActivity) getActivity();
    }

    private void searchFile() {
        fileSelectorAdapter.getFilter().filter(searchWorld);
    }

    @Override
    public void onSearch(String keyWorld) {
        if (searchWorld.equals(keyWorld)) {
            return;
        }
        searchWorld = keyWorld;
        if (isFirst) {
            return;
        }
        searchFile();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirst) {
            isFirst = false;
            getFiles();
        } else {
            checkSelect();
            searchFile();
        }
    }

    private void getFiles() {
        int type = requireArguments().getInt("type", TYPE_ALL);

        new Thread(() -> {
            switch (type) {
                case TYPE_ALL:
                    queryFile();
                    break;
                case TYPE_APP:
                    getApps();
                    break;
                case TYPE_APK:
                    getApks();
                    break;
                case TYPE_IMAGE:
                    getImages();
                    break;
                case TYPE_AUDIO:
                    getAudios();
                    break;
                case TYPE_VIDEO:
                    getVideos();
                    break;
                case TYPE_DOCUMENT:
                    getDocuments();
                    break;
                default:
                    break;
            }
            if (!"".equals(searchWorld)) {
                searchFile();
            }
        }).start();
    }

    private void getApks() {
        String selection = "mime_type = ?";
        queryFile(null, selection, new String[]{"application/vnd.android.package-archive"});
    }

    private void getImages() {
        queryFile(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null);
    }

    private void getAudios() {
        queryFile(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null);
    }

    private void getVideos() {
        queryFile(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null);
    }

    private void getDocuments() {
        String selection = "mime_type = ? and mime_type = ? and mime_type = ?";
        String[] selectionArgs = {"application/msword", "application/vnd.ms-excel", "application/vnd.ms-powerpoint"};
        queryFile(null, selection, selectionArgs);
    }

    public void queryFile() {
        queryFile(null, null, null);
    }

    private void queryFile(@Nullable Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        Uri contentUri = uri == null ? MediaStore.Files.getContentUri("external") : uri;
        String dateModified = MediaStore.Files.FileColumns.DATE_MODIFIED;
        String sel = "mime_type != ? and mime_type != ?";
        if (selection != null) {
            sel += " and " + selection;
        }
        String[] args = {"application/octet-stream", ""};
        if (selectionArgs != null) {
            String[] defaultArgs = args;
            args = new String[defaultArgs.length + selectionArgs.length];
            for (int i = 0; i < args.length; i++) {
                if (i < 2) {
                    args[i] = defaultArgs[i];
                } else {
                    args[i] = selectionArgs[i - 2];
                }
            }
        }
        Cursor cursor = contentResolver.query(contentUri, null, sel, args, dateModified + " desc");
        int nameIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
        while (cursor.moveToNext()) {
            String path = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
            if (path.startsWith("/storage/emulated/0/Android")) {
                continue;
            }
            long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            // String uriString = ContentUris.withAppendedId(contentUri, id).toString();
            String name = cursor.getString(nameIndex);
            String mimeType = cursor.getString(cursor.getColumnIndexOrThrow("mime_type"));
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            Long time = cursor.getLong(cursor.getColumnIndexOrThrow(dateModified));
            Long size = cursor.getLong(cursor.getColumnIndexOrThrow("_size"));
            String fileName = name == null ? title + "." + extension : name;
            FileInfo fileInfo = new FileInfo(fileName, path, size);
            fileInfo.setExtension(extension);
            fileInfo.setTime(time);
            fileInfo.setId(id);
            fileInfo.setFileDesc(FileUtils.toSize(fileInfo.getLength()));
            selectFile(fileInfo);
            files.add(fileInfo);
        }
        cursor.close();
        notifyData();
    }

    private void selectFile(FileInfo fileInfo) {
        if (selectedFiles.contains(fileInfo)) {
            fileInfo.setSelected(true);
        }
    }

    private void checkSelect() {
        if (requireArguments().getInt("type") == TYPE_APP) {
            return;
        }
        for (int i = 0; i < files.size(); i++) {
            FileInfo fileInfo = files.get(i);
            if (selectedFiles.contains(fileInfo)) {
                if (!fileInfo.isSelected()) {
                    fileInfo.setSelected(true);
                    fileSelectorAdapter.notifySelect(i);
                }
            } else if (fileInfo.isSelected()) {
                fileInfo.setSelected(false);
                fileSelectorAdapter.notifySelect(i);
            }
        }
    }

    private void getApps() {
        PackageManager packageManager = requireContext().getPackageManager();
        List<PackageInfo> packages = packageManager.getInstalledPackages(0);
        for (PackageInfo packageInfo : packages) {
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            String label = applicationInfo.loadLabel(packageManager).toString();
            long length = new File(applicationInfo.sourceDir).length();
            FileInfo fileInfo = new FileInfo(label, applicationInfo.sourceDir, length);
            fileInfo.setTime(packageInfo.lastUpdateTime);
            fileInfo.setPkgName(packageInfo.packageName);
            fileInfo.setExtension("apk");
            fileInfo.setFileDesc(FileUtils.toSize(length) + " - " + packageInfo.versionName);
            selectFile(fileInfo);
            files.add(fileInfo);
        }
        Collections.sort(files);
        notifyData();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyData() {
        requireActivity().runOnUiThread(() -> {
            fileSelectorAdapter.notifyDataSetChanged();
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.getRoot().setRefreshing(false);
        });

    }
}
