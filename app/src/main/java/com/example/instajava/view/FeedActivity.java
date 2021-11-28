package com.example.instajava.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.instajava.R;
import com.example.instajava.adapter.PostAdapter;
import com.example.instajava.databinding.ActivityFeedBinding;
import com.example.instajava.model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Map;

public class FeedActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private ArrayList<Post> posts;
    private FirebaseFirestore firestore;

    private ActivityFeedBinding binding;

    private PostAdapter postAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        posts = new ArrayList<>();

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        getData();

        binding.recyclerViewFeed.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(posts);
        binding.recyclerViewFeed.setAdapter(postAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.option_menu,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.add_post){
            startActivity(new Intent(FeedActivity.this, UploadActivity.class));
        }else if(item.getItemId() == R.id.log_out){
            auth.signOut();
            startActivity(new Intent(FeedActivity.this, MainActivity.class));
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void getData(){
        firestore.collection("Posts").orderBy("date", Query.Direction.DESCENDING).addSnapshotListener((value, error) -> {
            if (error != null){
                Toast.makeText(FeedActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }

            if (value != null){
                for (DocumentSnapshot snapshot : value.getDocuments()){
                    Map<String, Object> data = snapshot.getData();

                    String userEmail = (String) data.get("useremail");
                    String comment = (String) data.get("comment");
                    String downloadURL = (String) data.get("downloadurl");

                    Post post = new Post(userEmail, comment, downloadURL);
                    posts.add(post);
                }

                postAdapter.notifyDataSetChanged();
            }
        });
    }
}