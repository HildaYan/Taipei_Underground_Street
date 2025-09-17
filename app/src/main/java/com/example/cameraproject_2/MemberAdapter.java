package com.example.cameraproject_2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<User> users;
    private List<String> selectedMembers;
    public MemberAdapter(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        this.selectedMembers = new ArrayList<>();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User user = users.get(position);
        holder.textViewMember.setText("(" + user.getId() + ") " + user.getUsername());
        holder.checkBox.setChecked(selectedMembers.contains(user.getId())); // 使用 userId
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedMembers.contains(user.getId())) {
                    selectedMembers.add(user.getId());
                }
            } else {
                selectedMembers.remove(user.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public List<String> getSelectedMembers() {
        return new ArrayList<>(selectedMembers);
    }
    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMember;
        CheckBox checkBox;
        MemberViewHolder(View itemView) {
            super(itemView);
            textViewMember = itemView.findViewById(R.id.text_member);
            checkBox = itemView.findViewById(R.id.checkbox_member);
        }
    }
}