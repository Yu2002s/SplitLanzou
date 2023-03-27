package com.lanzou.split;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lanzou.split.adapter.FileAdapter;
import com.lanzou.split.base.BaseActivity;
import com.lanzou.split.data.LanzouFile;
import com.lanzou.split.data.LanzouPage;
import com.lanzou.split.data.Upload;
import com.lanzou.split.databinding.ActivityMainBinding;
import com.lanzou.split.databinding.DialogCreateFolderBinding;
import com.lanzou.split.network.Repository;
import com.lanzou.split.service.DownloadService;
import com.lanzou.split.service.UploadService;
import com.lanzou.split.ui.folder.FolderSelectorActivity;
import com.lanzou.split.ui.resolve.ResolveFileActivity;
import com.lanzou.split.ui.setting.SettingActivity;
import com.lanzou.split.ui.transmission.TransmissionListActivity;
import com.lanzou.split.ui.web.WebActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import okhttp3.Response;

public class MainActivity extends BaseActivity implements ServiceConnection {
    private ActivityMainBinding binding;

    private final LinkedList<LanzouPage> lanzouPages = new LinkedList<>();

    private final List<LanzouFile> lanzouFiles = new ArrayList<>();

    private final FileAdapter fileAdapter = new FileAdapter(lanzouFiles);

    private LanzouPage currentPage = new LanzouPage();

    private UploadService uploadService;
    private DownloadService downloadService;

    private ActivityResultLauncher<Intent> uploadLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        bindService(new Intent(this, UploadService.class), this, BIND_AUTO_CREATE);
        bindService(new Intent(this, DownloadService.class), this, BIND_AUTO_CREATE);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);

        RecyclerView recyclerView = binding.fileRecyclerView;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(fileAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition();
                    if (currentPage.isCompleted() && currentPage.isNotNull()
                            && lastVisibleItemPosition >= fileAdapter.getItemCount() - 6) {
                        // loadMore
                        currentPage.setCompleted(false);
                        loadMoreFiles();
                    }
                }
            }
        });

        if (!Repository.getInstance().isLogin()) {
            binding.btnLogin.setVisibility(View.VISIBLE);
        } else {
            getFiles();
        }

        ActivityResultLauncher<Intent> launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        binding.btnLogin.setVisibility(View.INVISIBLE);
                        // 开始加载文件
                        getFiles();
                        // 选择上传目录
                        showUploadDialog();
                    }
                });

        binding.btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WebActivity.class);
            intent.putExtra("url", LanzouApplication.HOST_LOGIN);
            launcher.launch(intent);
        });

        ActivityResultLauncher<String[]> uploadLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(), result -> {
                    if (result == null) {
                        return;
                    }
                    for (Uri uri : result) {
                        uploadService.uploadFile(uri.toString(), currentPage);
                    }
                });

        binding.fab.setOnClickListener(view -> {
            Repository repository = Repository.getInstance();
            if (repository.getUploadPath() == null) {
                // 未选择
                showUploadDialog();
            } else {
                // 开始去上传文件了
                uploadLauncher.launch(new String[]{"*/*"});
            }
        });

        fileAdapter.setOnItemClickListener((position, view) -> {
            LanzouFile lanzouFile = lanzouFiles.get(position);
            long folderId = lanzouFile.getFolderId();
            if (folderId == 0) {
                // 文件
                downloadService.addDownload(lanzouFile.getFileId(), lanzouFile.getName_all());
                return;
            }
            getSupportActionBar().setTitle(lanzouFile.getName());
            getFiles(folderId, lanzouFile.getName());
        });

        binding.refreshLayout.setOnRefreshListener(this::refresh);

        this.uploadLauncher = registerForActivityResult(new ActivityResultContract<Intent, Long>() {
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, Intent intent) {
                return intent;
            }

            @Override
            public Long parseResult(int i, @Nullable Intent intent) {
                if (intent == null) {
                    return 0L;
                }
                return intent.getLongExtra("id", 0);
            }
        }, result -> {
            if (result == 0) {
                return;
            }
            Repository.getInstance().updateUploadPath(result);
            Toast.makeText(MainActivity.this, "已选择缓存路径", Toast.LENGTH_SHORT).show();
        });

        requestPermission();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (service instanceof UploadService.UploadBinder) {
            uploadService = ((UploadService.UploadBinder) service).getService();
        } else if (service instanceof DownloadService.DownloadBinder) {
            downloadService = ((DownloadService.DownloadBinder) service).getService();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void onBackPressed() {
        if (lanzouPages.size() == 1) {
            super.onBackPressed();
            return;
        }
        int position = lanzouPages.size() - 2;
        LanzouPage lanzouPage = lanzouPages.get(position);
        currentPage = lanzouPage;
        lanzouFiles.clear();
        if (lanzouPage.getFiles() != null) {
            lanzouFiles.addAll(lanzouPage.getFiles());
        }
        fileAdapter.notifyDataSetChanged();
        lanzouPages.remove(position + 1);
        getSupportActionBar().setTitle(currentPage.getName());
    }

    private void getFiles() {
        getFiles(-1, "根目录");
    }

    private void refresh() {
        lanzouPages.remove(currentPage);
        getFiles(currentPage.getFolderId(), currentPage.getName());
    }

    private void getFiles(long folderId, String name) {
        binding.progressBar.setVisibility(View.VISIBLE);
        currentPage = new LanzouPage();
        currentPage.setFolderId(folderId);
        currentPage.setName(name);
        lanzouPages.add(currentPage);
        if (!lanzouFiles.isEmpty()) {
            lanzouFiles.clear();
            fileAdapter.notifyDataSetChanged();
        }
        new Thread(() -> {
            List<LanzouFile> files = Repository.getInstance().getFiles(folderId, 1);
            if (files != null && !files.isEmpty()) {
                if (files.size() < 18) {
                    currentPage.setNull(true);
                }
                lanzouFiles.addAll(files);
                currentPage.addFiles(lanzouFiles);
            } else {
                currentPage.setNull(true);
            }
            runOnUiThread(() -> {
                binding.refreshLayout.setRefreshing(false);
                binding.progressBar.setVisibility(View.INVISIBLE);
                currentPage.setCompleted(true);
                fileAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void loadMoreFiles() {
        currentPage.nextPage();
        new Thread(() -> {
            List<LanzouFile> files = Repository.getInstance()
                    .getFiles(currentPage.getFolderId(), currentPage.getPage());
            if (files == null || files.isEmpty()) {
                currentPage.setNull(true);
                return;
            }
            if (files.size() < 18) {
                currentPage.setNull(true);
            }
            currentPage.setCompleted(true);
            lanzouFiles.addAll(files);
            int size = lanzouFiles.size();
            int start = size - files.size();
            runOnUiThread(() -> fileAdapter.notifyItemRangeInserted(start, start));
        }).start();
    }

    @Override
    protected void onApplyInsertBottom(int bottom) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)
                binding.fab.getLayoutParams();
        layoutParams.bottomMargin += bottom;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        } else if (id == R.id.create_folder) {
            createFolder();
            return true;
        } else if (id == R.id.transmission_list) {
            startActivity(new Intent(this, TransmissionListActivity.class));
        } else if (id == R.id.resolve_file) {
            startActivity(new Intent(this, ResolveFileActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    private void createFolder() {
        DialogCreateFolderBinding folderBinding = DialogCreateFolderBinding.inflate(getLayoutInflater());
        new AlertDialog.Builder(this)
                .setTitle("新建文件夹")
                .setView(folderBinding.getRoot())
                .setPositiveButton("新建", (dialog, which) -> {
                    String name = folderBinding.editName.getText().toString();
                    String desc = folderBinding.editDesc.getText().toString();
                    new Thread(() -> {
                        Long id = Repository.getInstance()
                                .createFolder(currentPage.getFolderId(), name, desc);
                        runOnUiThread(() -> {
                            if (id != null) {
                                LanzouFile lanzouFile = new LanzouFile();
                                lanzouFile.setName(name);
                                lanzouFile.setFolderId(id);
                                lanzouFiles.add(0, lanzouFile);
                                fileAdapter.notifyItemInserted(0);
                                binding.fileRecyclerView.scrollToPosition(0);
                                Toast.makeText(MainActivity.this, "文件夹已新建", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "新建文件夹失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).start();

                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showUploadDialog() {
        // 未选择上传目录
        new AlertDialog.Builder(this)
                .setTitle("选择缓存位置")
                .setMessage("选择缓存上传文件的位置，这是必须设置项，注意，此目录必须为不使用目录，因为将会上传大量缓存文件到此处")
                .setNegativeButton("先不选", null)
                .setPositiveButton("去选择", (dialog, which) -> uploadLauncher.launch(new Intent(MainActivity.this, FolderSelectorActivity.class))).show();

    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Toast.makeText(this, "请授权此权限", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception ignore) {
                }
            }
        } else {
            int granted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions, 1);
            }
        }
    }
}