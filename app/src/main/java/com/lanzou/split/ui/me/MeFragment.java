package com.lanzou.split.ui.me;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.lanzou.split.data.User;
import com.lanzou.split.databinding.FragmentMeBinding;
import com.lanzou.split.network.Repository;
import com.lanzou.split.ui.dialog.UserDialog;
import com.lanzou.split.ui.resolve.ResolveFileActivity;

public class MeFragment extends Fragment {

    private FragmentMeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserDialog userDialog = new UserDialog(v.getContext());
                userDialog.setOnDismissListener(dialog -> getUser());
                userDialog.show();
            }
        });

        binding.btnResolve.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ResolveFileActivity.class))
        );
    }

    private void getUser() {
        Repository repository = Repository.getInstance();
        User user = repository.getSavedUser();
        binding.tvUsername.setText(user == null ? "未登录" : user.getUsername());
    }

    @Override
    public void onResume() {
        super.onResume();
        getUser();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
