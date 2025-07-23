package com.lanzou.cloud;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.DialogInterface;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.cloud.adapter.MainPageAdapter;
import com.lanzou.cloud.config.SPConfig;
import com.lanzou.cloud.data.LanzouPage;
import com.lanzou.cloud.databinding.ActivityMainBinding;
import com.lanzou.cloud.network.Repository;
import com.lanzou.cloud.service.DownloadService;
import com.lanzou.cloud.service.UploadService;
import com.lanzou.cloud.ui.file.FileFragment;
import com.lanzou.cloud.ui.selector.FileSelectorActivity;
import com.lanzou.cloud.ui.selector.PhoneFileActivity;
import com.lanzou.cloud.utils.SpJavaUtils;
import com.lanzou.cloud.utils.UpdateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SplitLanzou
 *
 * @author 冬日暖雨
 * @mail jiangdongyu54@gmail.com
 * @since 2025/07/01
 */
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

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);

        ViewPager2 viewPager2 = binding.viewpager2;
        viewPager2.setUserInputEnabled(false);
        viewPager2.setOffscreenPageLimit(5);
        viewPager2.setAdapter(new MainPageAdapter(getSupportFragmentManager(), getLifecycle()));

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.fab.setVisibility(position == 1 ? View.VISIBLE : View.INVISIBLE);
            }
        });

        binding.bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    viewPager2.setCurrentItem(0, false);
                    break;
                case R.id.nav_file:
                    viewPager2.setCurrentItem(1, false);
                    break;
                case R.id.nav_transmission:
                    viewPager2.setCurrentItem(2, false);
                    break;
                case R.id.nav_me:
                    viewPager2.setCurrentItem(3, false);
                    break;
                case R.id.nav_setting:
                    viewPager2.setCurrentItem(4, false);
                    break;
            }
            invalidateMenu();
            return true;
        });

        ActivityResultCallback<ActivityResult> selectFileCallback = result -> {
            Intent data = result.getData();
            if (data != null && result.getResultCode() == RESULT_OK) {
                ArrayList<CharSequence> files = data.getCharSequenceArrayListExtra("files");
                if (files == null || files.isEmpty()) {
                    // 选择的文件为空时
                    return;
                }
                for (CharSequence uri : files) {
                    LanzouPage currentPage = getFileFragment().getCurrentPage();
                    uploadService.uploadFile(uri.toString(), currentPage.getFolderId(), currentPage.getName());
                }
            }
        };

        ActivityResultLauncher<Intent> selectFileLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), selectFileCallback);

        binding.fab.setOnClickListener(view -> showUploadDialog(selectFileLauncher));

        requestPermission();

        if (SpJavaUtils.getBoolean(SPConfig.CHECK_UPDATE, true)) {
            UpdateUtils.checkUpdate(this);
        }
    }

    /**
     * 获取 FileFragment 实例
     *
     * @return FileFragment
     */
    private FileFragment getFileFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        Fragment fragment = fragments.get(1);
        if (!(fragment instanceof FileFragment)) {
            for (Fragment f : fragments) {
                if (f instanceof FileFragment) {
                    return (FileFragment) f;
                }
            }
            throw new ClassCastException("获取 FileFragment 错误");
        }
        return (FileFragment) fragment;
    }

    private void showUploadDialog(ActivityResultLauncher<Intent> selectFileLauncher) {
        Repository repository = Repository.getInstance();

        DialogInterface.OnClickListener onClickListener = (dialog, which) -> {
            if (repository.getUploadPath() == null) {
                // 未选择
                Toast.makeText(MainActivity.this, "请前往设置，设置缓存路径", Toast.LENGTH_SHORT).show();
                return;
            }
            Class<?> clazz = null;
            switch (which) {
                case 0:
                    getFileFragment().createFolder();
                    break;
                case 1:
                    clazz = FileSelectorActivity.class;
                    break;
                case 2:
                    clazz = PhoneFileActivity.class;
                    break;
            }
            if (clazz != null) {
                selectFileLauncher.launch(new Intent(MainActivity.this, clazz));
            }
            dialog.dismiss();
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("选择操作")
                .setSingleChoiceItems(new String[]{"新建文件夹", "分类选择上传", "文件选择上传"}, -1, onClickListener)
                .show();
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
        menu.findItem(R.id.create_folder).setVisible(selectedItemId == R.id.nav_file);
        menu.findItem(R.id.detail).setVisible(selectedItemId == R.id.nav_file);
        menu.findItem(R.id.scan).setVisible(selectedItemId == R.id.nav_me);
        menu.findItem(R.id.delete).setVisible(selectedItemId == R.id.nav_home);
        menu.findItem(R.id.sort).setVisible(selectedItemId == R.id.nav_home);
        menu.findItem(R.id.delete_download).setVisible(selectedItemId == R.id.nav_transmission);
        menu.findItem(R.id.clear_download).setVisible(selectedItemId == R.id.nav_transmission);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
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