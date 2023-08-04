package com.lanzou.split;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.split.adapter.MainPageAdapter;
import com.lanzou.split.data.LanzouFile;
import com.lanzou.split.data.LanzouPage;
import com.lanzou.split.databinding.ActivityMainBinding;
import com.lanzou.split.databinding.DialogCreateFolderBinding;
import com.lanzou.split.network.Repository;
import com.lanzou.split.service.DownloadFileService;
import com.lanzou.split.service.DownloadService;
import com.lanzou.split.service.UploadService;
import com.lanzou.split.ui.file.FileFragment;
import com.lanzou.split.ui.resolve.ResolveFileActivity;
import com.lanzou.split.ui.selector.FileSelectorActivity;
import com.lanzou.split.ui.setting.SettingActivity;
import com.lanzou.split.ui.transmission.TransmissionListActivity;
import com.lanzou.split.utils.UpdateUtils;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private ActivityMainBinding binding;

    private UploadService uploadService;
    private DownloadService downloadService;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        bindService(new Intent(this, UploadService.class), this, BIND_AUTO_CREATE);
        bindService(new Intent(this, DownloadService.class), this, BIND_AUTO_CREATE);
        // startService(new Intent(this, DownloadFileService.class));

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);

        ViewPager2 viewPager2 = binding.viewpager2;
        viewPager2.setUserInputEnabled(false);
        viewPager2.setOffscreenPageLimit(3);
        viewPager2.setAdapter(new MainPageAdapter(getSupportFragmentManager(), getLifecycle()));

        binding.bottomNav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    binding.viewpager2.setCurrentItem(0, false);
                    break;
                case R.id.nav_transmission:
                    binding.viewpager2.setCurrentItem(1, false);
                    break;
                case R.id.nav_me:
                    binding.viewpager2.setCurrentItem(2, false);
                    break;
            }
            invalidateMenu();
            return true;
        });

        binding.bottomNav.post(() -> viewPager2.setPadding(0, 0, 0, binding.bottomNav.getMeasuredHeight()));

        ActivityResultLauncher<Intent> selectFileLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    Intent data = result.getData();
                    if (data != null && result.getResultCode() == RESULT_OK) {
                        ArrayList<CharSequence> files = data.getCharSequenceArrayListExtra("files");
                        if (files == null || files.isEmpty()) {
                            // 选择的文件为空时
                            return;
                        }
                        for (CharSequence uri : files) {
                            Fragment fragment = getSupportFragmentManager().getFragments().get(0);
                            if (fragment instanceof FileFragment) {
                                LanzouPage currentPage = ((FileFragment) fragment).getCurrentPage();
                                uploadService.uploadFile(uri.toString(), currentPage);
                            }
                        }
                    }
                });


        binding.fab.setOnClickListener(view -> {
            Repository repository = Repository.getInstance();
            if (repository.getUploadPath() == null) {
                // 未选择
                Toast.makeText(this, "请前往设置，设置缓存路径", Toast.LENGTH_SHORT).show();
            } else {
                // 开始去上传文件了
                selectFileLauncher.launch(new Intent(MainActivity.this, FileSelectorActivity.class));
            }

        });

        requestPermission();
        UpdateUtils.checkUpdate(this);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        int selectedItemId = binding.bottomNav.getSelectedItemId();
        menu.findItem(R.id.create_folder).setVisible(selectedItemId == R.id.nav_home);
        menu.findItem(R.id.action_settings).setVisible(selectedItemId == R.id.nav_me);

        return super.onPrepareOptionsMenu(menu);
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
        Thread thread = new Thread(() -> {
            String name = folderBinding.editName.getText().toString();
            String desc = folderBinding.editDesc.getText().toString();
            Fragment fragment = getSupportFragmentManager().getFragments().get(0);
            if (fragment instanceof FileFragment) {
                LanzouPage currentPage = ((FileFragment) fragment).getCurrentPage();
                Long id = Repository.getInstance()
                        .createFolder(currentPage.getFolderId(), name, desc);
                runOnUiThread(() -> {
                    if (id != null) {
                        LanzouFile lanzouFile = new LanzouFile();
                        lanzouFile.setName(name);
                        lanzouFile.setFolderId(id);
                        ((FileFragment) fragment).addLanzouFile(lanzouFile);
                        Toast.makeText(MainActivity.this, "文件夹已新建", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "新建文件夹失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        new MaterialAlertDialogBuilder(this)
                .setTitle("新建文件夹")
                .setView(folderBinding.getRoot())
                .setPositiveButton("新建", (dialog, which) -> {
                    thread.start();
                })
                .setNegativeButton("取消", null)
                .show();
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