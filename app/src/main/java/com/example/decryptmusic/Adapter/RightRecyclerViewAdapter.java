package com.example.decryptmusic.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.decryptmusic.Models.Story;
import com.example.decryptmusic.R;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class RightRecyclerViewAdapter extends RecyclerView.Adapter<RightRecyclerViewAdapter.RightTextViewHolder> {
    private final LayoutInflater mLayoutInflater;
    private final Context mContext;
    private List<Story> mStroies;
    private TextView txt_name;
    private Story currentStory;

    public RightRecyclerViewAdapter(Context context, TextView txt_name) {
        this.txt_name = txt_name;
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public RightTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RightTextViewHolder(mLayoutInflater.inflate(R.layout.item_text, parent, false));
    }

    @Override
    public void onBindViewHolder(final RightTextViewHolder holder, final int position) {
        holder.mTextView.setText(mStroies.get(position).getName());

        holder.itemView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                currentStory = mStroies.get(position);
                txt_name.setText(currentStory.getName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mStroies == null ? 0 : mStroies.size();
    }

    public Story getCurrentStory() {
        return currentStory;
    }

    public void setStroies(List<Story> mStroies) {
        this.mStroies = mStroies;
    }

    public static class RightTextViewHolder extends RecyclerView.ViewHolder {
        TextView mTextView;
        public RightTextViewHolder(View view) {
            super(view);
            mTextView = view.findViewById(R.id.txt_item);
        }
    }
}