package com.lanzou.cloud.ui.question;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lanzou.cloud.R;
import com.lanzou.cloud.base.BaseActivity;
import com.lanzou.cloud.data.Question;
import com.lanzou.cloud.databinding.ActivityQuestionBinding;

import java.util.ArrayList;
import java.util.List;

public class QuestionActivity extends BaseActivity {

    private ActivityQuestionBinding binding;

    private final List<Question> questions = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding  = ActivityQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.header.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView rv = binding.questionRv;
        rv.setLayoutManager(new LinearLayoutManager(this));

        addLeft("文件下载在哪里？");
        addRight("没有授权储存权限保存在" + getExternalFilesDir("Download")
                + "\n授权了保存在外部根目录/Download");
        addLeft("为啥重新打开软件上传下载记录没了");
        addRight("目前下载有记录，上传没有");
        addLeft("为啥软件没有图标？");
        addRight("懒得搞了");
        addLeft("文件上传下载有问题？");
        addRight("由于就几天时间写的，所以难免有问题，重新下载上传就好了，不行就提issue");
        addLeft("为啥软件功能这么少？");
        addRight("懒得做了，主要功能做好就行");
        addLeft("为啥打开软件就要获取权限？");
        addRight("为了省事，省的后面判断有没有权限");
        addLeft("我的账号会被上传到云端吗？");
        addRight("不会的，全部源代码开源，都保存在本地");
        addLeft("软件会自动更新吗？");
        addRight("暂时会，可能后面会移除自动更新，国内网络可能被墙");
        addLeft("我要分享文件出去怎么办？");
        addRight("已支持分享100M+文件，不过新注册用户有限制，下载一次可能就会失效，需要对方也下载本App");
        addLeft("好的，我已了解");
        addRight("--------------------------------------");
        addRight("软件仅供交流学习使用，请勿用于其他用途");
        addRight("作者：Yu2002s");

        rv.setAdapter(new QuestionAdapter(questions));
    }

    private static class QuestionAdapter extends RecyclerView.Adapter<QuestionViewHolder> {

        private final List<Question> questions;

        public QuestionAdapter(List<Question> questions) {
            this.questions = questions;
        }

        @NonNull
        @Override
        public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int resId = viewType == Question.LEFT ? R.layout.item_list_left : R.layout.item_list_right;
            View view = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);
            return new QuestionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
            ((TextView)((FrameLayout)holder.itemView).getChildAt(0)).setText(questions.get(position).getContent());
        }

        @Override
        public int getItemCount() {
            return questions.size();
        }

        @Override
        public int getItemViewType(int position) {
            return questions.get(position).getType();
        }
    }

    private static class QuestionViewHolder extends RecyclerView.ViewHolder {
        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private void addLeft(String content) {
        questions.add(new Question(Question.LEFT, content));
    }
    private void addRight(String content) {
        questions.add(new Question(Question.RIGHT, content));
    }
}
