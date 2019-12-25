package com.example.decryptmusic.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.decryptmusic.Models.Story;
import com.example.decryptmusic.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.recyclerview.widget.RecyclerView;

public class LeftRecyclerViewAdapter extends RecyclerView.Adapter<LeftRecyclerViewAdapter.LeftTextViewHolder> {
    private final LayoutInflater mLayoutInflater;
    private final Context mContext;
    private Map<String, List<Story>> storyMap;
    private List<String> mAlbums;
    private TextView txt_album;
    private RightRecyclerViewAdapter rAdapter;

    public LeftRecyclerViewAdapter(Context context,RightRecyclerViewAdapter rAdapter, Map<String, List<Story>> storyMap, TextView txt_album) {
        this.rAdapter = rAdapter;
        this.storyMap = storyMap;
        mAlbums = new ArrayList<String>(storyMap.keySet());
        this.txt_album = txt_album;
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public LeftTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LeftTextViewHolder(mLayoutInflater.inflate(R.layout.item_text, parent, false));
    }

    @Override
    public void onBindViewHolder(final LeftTextViewHolder holder, final int position) {
        holder.mTextView.setText(mAlbums.get(position));
        holder.itemView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String album_name = mAlbums.get(position);
                txt_album.setText(album_name);
                rAdapter.setStroies(storyMap.get(album_name));
                rAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAlbums == null ? 0 : mAlbums.size();
    }

    public static class LeftTextViewHolder extends RecyclerView.ViewHolder {
        TextView mTextView;

        public LeftTextViewHolder(View view) {
            super(view);
            mTextView = view.findViewById(R.id.txt_item);
        }
    }
}