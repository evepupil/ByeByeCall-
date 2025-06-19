package com.example.telprotect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class KeywordAdapter extends RecyclerView.Adapter<KeywordAdapter.KeywordViewHolder> {
    
    private List<String> keywordList;
    private OnKeywordDeleteListener listener;

    public interface OnKeywordDeleteListener {
        void onKeywordDelete(String keyword);
    }

    public KeywordAdapter(List<String> keywordList, OnKeywordDeleteListener listener) {
        this.keywordList = keywordList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public KeywordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_keyword, parent, false);
        return new KeywordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KeywordViewHolder holder, int position) {
        String keyword = keywordList.get(position);
        holder.keywordTextView.setText(keyword);
        
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onKeywordDelete(keyword);
            }
        });
    }

    @Override
    public int getItemCount() {
        return keywordList.size();
    }

    public void updateKeywords(List<String> newKeywords) {
        this.keywordList = newKeywords;
        notifyDataSetChanged();
    }

    static class KeywordViewHolder extends RecyclerView.ViewHolder {
        TextView keywordTextView;
        ImageButton deleteButton;

        KeywordViewHolder(View itemView) {
            super(itemView);
            keywordTextView = itemView.findViewById(R.id.keywordTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
} 