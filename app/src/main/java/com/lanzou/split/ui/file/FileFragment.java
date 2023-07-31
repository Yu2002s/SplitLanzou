package com.lanzou.split.ui.file;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.LocusId;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.text.InputFilter;
import android.text.LoginFilter;
import android.util.AndroidException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lanzou.split.LanzouApplication;
import com.lanzou.split.MainActivity;
import com.lanzou.split.R;
import com.lanzou.split.adapter.FileAdapter;
import com.lanzou.split.adapter.PathAdapter;
import com.lanzou.split.adapter.SimpleListAdapter;
import com.lanzou.split.data.LanzouFile;
import com.lanzou.split.data.LanzouPage;
import com.lanzou.split.data.LanzouSimpleResponse;
import com.lanzou.split.data.LanzouUrl;
import com.lanzou.split.data.SimpleItem;
import com.lanzou.split.databinding.FragmentFileBinding;
import com.lanzou.split.event.FileActionListener;
import com.lanzou.split.event.OnItemClickListener;
import com.lanzou.split.event.OnItemLongClickListener;
import com.lanzou.split.network.Repository;
import com.lanzou.split.service.DownloadService;
import com.lanzou.split.ui.LinearItemDecoration;
import com.lanzou.split.ui.file.imple.FileActionImpl;
import com.lanzou.split.ui.folder.FolderSelectorActivity;
import com.lanzou.split.ui.web.WebActivity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FileFragment extends Fragment implements ServiceConnection {

    private FragmentFileBinding binding;

    private final AbstractFileAction fileAction = new FileActionImpl();

    private final List<LanzouFile> lanzouFiles = fileAction.getSource();

    private final List<LanzouPage> lanzouPages = fileAction.getLanzouPages();

    private final List<LanzouFile> selectedFiles = new ArrayList<>();

    private FileAdapter fileAdapter;

    private final PathAdapter pathAdapter = new PathAdapter(lanzouPages);

    private ClipboardManager clipboardManager;

    private DownloadService downloadService;

    private ActivityResultLauncher<Intent> moveLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireContext().bindService(new Intent(requireContext(),
                DownloadService.class), this, Context.BIND_AUTO_CREATE);
        clipboardManager = ((ClipboardManager) requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE));
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
        // recyclerView.addItemDecoration(new LinearItemDecoration());
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

        moveLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getData() != null) {
                    LanzouFile lanzouFile = result.getData().getParcelableExtra("lanzouFile");
                    if (lanzouFile == null) return;
                    new Thread(() -> {
                        long id = result.getData().getLongExtra("id", -1);
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
                    }).start();
                }
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
            if (selectedFiles.size() > 0) {
                v.setSelected(!v.isSelected());
                lanzouFile.setSelected(v.isSelected());
                if (v.isSelected()) {
                    selectedFiles.add(lanzouFile);
                } else {
                    selectedFiles.remove(lanzouFile);
                }
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

        fileAdapter.setLongClickListener(new OnItemLongClickListener() {
            @Override
            public void onItemLongClick(int position, View v) {
                v.setSelected(!v.isSelected());
                LanzouFile lanzouFile = fileAdapter.getItem(position);
                lanzouFile.setSelected(v.isSelected());
                if (v.isSelected()) {
                    selectedFiles.add(lanzouFile);
                } else {
                    selectedFiles.remove(lanzouFile);
                }
            }
        });

        binding.getRoot().setOnRefreshListener(fileAction::refresh);
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        MenuItem search = menu.add("搜索");
        SearchView searchView = new SearchView(requireContext());
        search.setActionView(searchView);
        search.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        requireContext().unbindService(this);
    }
}
