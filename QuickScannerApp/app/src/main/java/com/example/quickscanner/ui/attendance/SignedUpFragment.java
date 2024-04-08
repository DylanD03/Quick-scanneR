package com.example.quickscanner.ui.attendance;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.quickscanner.controller.FirebaseAttendanceController;
import com.example.quickscanner.controller.FirebaseUserController;
import com.example.quickscanner.databinding.FragmentAttendanceBinding;
import com.example.quickscanner.model.User;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class SignedUpFragment extends Fragment {

    private ListView listView;
    private SignUpAdapter adapter;
    private FirebaseAttendanceController fbAttendanceController;
    FirebaseUserController fbUserController;
    private ArrayList<User> signUpDataList;
    private TextView emptySignUp;
    private FragmentAttendanceBinding binding;
    private ListenerRegistration signUpListenerReg;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAttendanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = binding.userListview;
        emptySignUp = binding.emptySignUp;
        emptySignUp.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);
        signUpDataList = new ArrayList<>();
        adapter = new SignUpAdapter(getContext(), signUpDataList);
        listView.setAdapter(adapter);

        fbAttendanceController = new FirebaseAttendanceController();
        fbUserController = new FirebaseUserController();

        Bundle bundle = this.getArguments();
        if (bundle == null) {
            Log.d("SignedUpFragment", "Bundle is null");
        }
        if (bundle != null) {
            String eventId = bundle.getString("eventID", "");
            signUpListenerReg = fbAttendanceController.setupSignUpListListener(eventId, signUpDataList, adapter,emptySignUp,listView);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (signUpListenerReg != null) {
            signUpListenerReg.remove();
            signUpListenerReg = null;
        }
    }
}