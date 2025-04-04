package com.lanzou.cloud.ui.file;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.cloud.LanzouApplication;
import com.lanzou.cloud.R;
import com.lanzou.cloud.adapter.FileAdapter;
import com.lanzou.cloud.adapter.PathAdapter;
import com.lanzou.cloud.adapter.SimpleListAdapter;
import com.lanzou.cloud.data.LanzouFile;
import com.lanzou.cloud.data.LanzouPage;
import com.lanzou.cloud.data.LanzouSimpleResponse;
import com.lanzou.cloud.data.SimpleItem;
import com.lanzou.cloud.databinding.FragmentFileBinding;
import com.lanzou.cloud.event.FileActionListener;
import com.lanzou.cloud.network.Repository;
import com.lanzou.cloud.service.DownloadService;
import com.lanzou.cloud.ui.file.imple.FileActionImpl;
import com.lanzou.cloud.ui.folder.FolderSelectorActivity;
import com.lanzou.cloud.ui.web.WebActivity;

import java.util.ArrayList;
import java.util.List;

public class FileFragment extends Fragment implements ServiceConnection {

    private FragmentFileBinding binding;

    private final AbstractFileAction fileAction = new FileActionImpl();

    private final List<LanzouFile> lanzouFiles = fileAction.getSource();

    private final List<LanzouPage> lanzouPages = fileAction.getLanzouPages();

    private int selectCount = 0;

    private FileAdapter fileAdapter;

    private final PathAdapter pathAdapter = new PathAdapter(lanzouPages);

    private DownloadService downloadService;

    private ActivityResultLauncher<Intent> moveLauncher;

    public boolean isMultiMode() {
        return selectCount > 0;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireContext().bindService(new Intent(requireContext(),
                DownloadService.class), this, Context.BIND_AUTO_CREATE);
        setHasOptionsMenu(true);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloadService = ((DownloadService.DownloadBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = binding.fileRecyclerView;
        StaggeredGridLayoutManager staggeredGridLayoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        fileAdapter = new FileAdapter(lanzouFiles);
        recyclerView.setAdapter(fileAdapter);

        RecyclerView pathRecyclerView = binding.pathRecyclerview;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        pathRecyclerView.setLayoutManager(linearLayoutManager);
        pathRecyclerView.setAdapter(pathAdapter);
        pathAdapter.setListener((position, view1) -> fileAction.navigateTo(position));

        fileAction.bindView(recyclerView, new FileActionListener() {

            @Override
            public void onPageChange() {
                pathAdapter.notifyDataSetChanged();
                pathRecyclerView.post(() -> pathRecyclerView.smoothScrollToPosition(lanzouPages.size() - 1));
            }

            @Override
            public void onPreLoadFile() {
                binding.progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFileLoaded() {
                binding.getRoot().setRefreshing(false);
                binding.progressBar.setVisibility(View.INVISIBLE);
            }
        });

        if (!Repository.getInstance().isLogin()) {
            binding.btnLogin.setVisibility(View.VISIBLE);
        } else {
            fileAction.getFiles();
        }

        ActivityResultLauncher<Intent> uploadPathLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        long id = data.getLongExtra("id", -1);
                        Repository.getInstance().updateUploadPath(id);
                        Toast.makeText(requireContext(), "已选择缓存路径", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        moveLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getData() != null) {
                LanzouFile lanzouFile = result.getData().getParcelableExtra("lanzouFile");
                new Thread(() -> {
                    long id = result.getData().getLongExtra("id", -1);
                    if (lanzouFile == null) {
                        moveFiles(id);
                    } else {
                        moveFile(lanzouFile, id);
                    }
                }).start();
            }
        });

        ActivityResultLauncher<Intent> loginLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        binding.btnLogin.setVisibility(View.INVISIBLE);
                        // 开始加载文件
                        fileAction.getFiles();
                        // 选择上传目录

                        new AlertDialog.Builder(requireContext())
                                .setCancelable(false)
                                .setTitle("选择缓存位置")
                                .setMessage("选择缓存上传文件的位置，这是必须设置项，注意，此目录必须为不使用目录，因为将会上传大量缓存文件到此处")
                                .setNegativeButton("先不选", null)
                                .setPositiveButton("去选择", (dialog, which) ->
                                        uploadPathLauncher.launch(new Intent(requireContext(), FolderSelectorActivity.class))).show();
                    }
                });

        binding.btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), WebActivity.class);
            intent.putExtra("url", LanzouApplication.HOST_LOGIN);
            loginLauncher.launch(intent);
        });

        fileAdapter.setOnItemClickListener((position, v) -> {
            LanzouFile lanzouFile = fileAdapter.getItem(position);
            if (isMultiMode()) {
                v.setSelected(!v.isSelected());
                lanzouFile.setSelected(v.isSelected());
                if (v.isSelected()) {
                    selectCount++;
                } else {
                    selectCount--;
                }
                changeSelect();
                return;
            }
            long folderId = lanzouFile.getFolderId();
            if (folderId == 0) {
                // 文件
                showFileActionDialog(lanzouFile, position);
                return;
            }
            // requireActivity().setTitle(lanzouFile.getName());
            fileAction.getFiles(lanzouFile.getFolderId(), lanzouFile.getName());
        });

        fileAdapter.setLongClickListener((position, v) -> {
            v.setSelected(!v.isSelected());
            LanzouFile lanzouFile = fileAdapter.getItem(position);
            lanzouFile.setSelected(v.isSelected());
            if (v.isSelected()) {
                selectCount++;
            } else {
                selectCount--;
            }
            changeSelect();
        });

        binding.getRoot().setOnRefreshListener(() -> {
            selectCount = 0;
            changeSelect();
            fileAction.refresh();
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isMultiMode()) {
                    clearSelect();
                } else {
                    fileAction.onBackPressed();
                }
            }
        });
    }

    public void onBackPressed() {
        fileAction.onBackPressed();
    }

    public LanzouPage getCurrentPage() {
        return fileAction.getCurrentPage();
    }

    public void addLanzouFile(LanzouFile lanzouFile) {
        lanzouFiles.add(0, lanzouFile);
        fileAdapter.notifyItemInserted(0);
        binding.fileRecyclerView.scrollToPosition(0);
    }

    private void changeSelect() {
        // 当选中内容为空时，取消全部选中，则退出多选模式
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        if (!isMultiMode()) {
            actionBar.setTitle(getString(R.string.app_name));
            actionBar.setDisplayHomeAsUpEnabled(false);
            requireActivity().invalidateOptionsMenu();
        } else if (selectCount == 1) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("已选择(" + selectCount + ")");
            requireActivity().invalidateOptionsMenu();
        } else {
            actionBar.setTitle("已选择(" + selectCount + ")");
        }
    }

    private void clearSelect() {
        selectCount = 0;
        for (int i = 0; i < lanzouFiles.size(); i++) {
            LanzouFile lanzouFile = lanzouFiles.get(i);
            if (lanzouFile.isSelected()) {
                lanzouFile.setSelected(false);
                fileAdapter.notifySelect(i);
            }
        }
        changeSelect();
    }

    private void moveFile(LanzouFile lanzouFile, long id) {
        LanzouSimpleResponse response = Repository.getInstance().moveFile(lanzouFile.getFileId(), id);
        if (response.getStatus() == 1) {
            requireActivity().runOnUiThread(() -> {
                int position = lanzouFiles.indexOf(lanzouFile);
                fileAdapter.deleteItem(position);
                fileAction.getCurrentPage().getFiles().remove(position);
            });
        } else {
            Looper.prepare();
            Toast.makeText(requireContext(), "移动文件失败", Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }

    private void moveFiles(long id) {
        for (int i = lanzouFiles.size() -1; i >= 0; i--) {
            LanzouFile file = lanzouFiles.get(i);
            if (file.isSelected() && !file.isFolder()) {
                LanzouSimpleResponse response = Repository.getInstance().moveFile(file.getFileId(), id);
                final int position = i;
                requireActivity().runOnUiThread(() -> {
                    if (response.getStatus() == 1) {
                        fileAdapter.deleteItem(position);
                        fileAction.getCurrentPage().getFiles().remove(position);
                    }
                });
            }
        }
        requireActivity().runOnUiThread(this::clearSelect);
    }

    private void showFileActionDialog(LanzouFile lanzouFile, int itemPosition) {
        List<SimpleItem> list = new ArrayList<>();
        list.add(new SimpleItem("下载", R.drawable.baseline_get_app_24));
        list.add(new SimpleItem("分享", R.drawable.baseline_share_24));
        list.add(new SimpleItem("移动", R.drawable.baseline_sync_alt_24));
        list.add(new SimpleItem("删除", R.drawable.baseline_delete_outline_24));
        GridView gridView = new GridView(requireContext());
        gridView.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        gridView.setNumColumns(2);
        gridView.setAdapter(new SimpleListAdapter(list));
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(gridView)
                .create();
        dialog.show();
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            switch (position) {
                case 0:
                    downloadService.addDownload(lanzouFile.getFileId(), lanzouFile.getName_all());
                    break;
                case 1:
                    fileAction.shareFile(itemPosition);
                    break;
                case 2:
                    fileAction.moveFile(moveLauncher, lanzouFile);
                    break;
                case 3:
                    fileAction.deleteFile(itemPosition);
                    break;
            }
        });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean multiMode = isMultiMode();
        menu.findItem(R.id.delete).setVisible(multiMode);
        menu.findItem(R.id.download).setVisible(multiMode);
        menu.findItem(R.id.move).setVisible(multiMode);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuItem search = menu.add("搜索");
        SearchView searchView = new SearchView(requireContext());
        search.setActionView(searchView);
        search.setIcon(R.drawable.baseline_search_24);
        search.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItem.SHOW_AS_ACTION_ALWAYS);
        searchView.setQueryHint("输入关键字搜索");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                fileAdapter.getFilter().filter(newText);
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            clearSelect();
        } else if (item.getItemId() == R.id.delete) {
            fileAction.deleteFiles(this::clearSelect);
        } else if (item.getItemId() == R.id.download) {
            for (LanzouFile lanzouFile : lanzouFiles) {
                if (lanzouFile.isSelected() && !lanzouFile.isFolder()) {
                    downloadService.addDownload(lanzouFile.getFileId(), lanzouFile.getName_all());
                }
            }
            clearSelect();
        } else if (item.getItemId() == R.id.move) {
            fileAction.moveFiles(moveLauncher);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        requireContext().unbindService(this);
    }
}
