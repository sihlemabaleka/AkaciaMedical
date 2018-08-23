package com.msqhealth.main.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.msqhealth.main.R;
import com.msqhealth.main.adapters.PromotionalContentRecyclerViewAdapter;
import com.msqhealth.main.model.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Admin on 17/02/2018.
 */

public class PromotionalContentFragment extends Fragment {

    private DatabaseReference mPromotionsDatabase;
    private RecyclerView mRecyclerView;

    // TODO: Customize parameters
    private int mColumnCount = 1;


    private DatabaseReference mDatabase;
    private List<Product> productList;

    private ProgressDialog pDialog;
    private Query mQueryCurrentUser;

    private TextView mPromoTextView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PromotionalContentFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.global_list, container, false);

        pDialog = new ProgressDialog(getActivity());
        pDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        pDialog.setMessage("Just a second...");
        pDialog.show();

        mPromotionsDatabase = FirebaseDatabase.getInstance().getReference().child("products");
        mPromotionsDatabase.keepSynced(true);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("products");
        mDatabase.keepSynced(true);

        mPromoTextView = rootView.findViewById(R.id.promo_textview);

        productList = new ArrayList<>();
        // Set the adapter
        if (rootView instanceof RecyclerView) {
            Context context = rootView.getContext();
            final RecyclerView recyclerView = (RecyclerView) rootView;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            }

            Query query = mDatabase.orderByChild("PROMO").equalTo(true);
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    System.out.println("CHI<D~: " + dataSnapshot.getChildrenCount());
                    if (!dataSnapshot.exists()) {
                        mPromoTextView.setVisibility(View.VISIBLE);
                        System.out.println("HELLO COUNT");
                    }

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        System.out.println("Count: " + snapshot.getChildrenCount());
                        System.out.println("PROMO VALUE: " + snapshot.child("PROMO").getValue());

                        if (!snapshot.exists()) {
                            mPromoTextView.setVisibility(View.VISIBLE);
                            System.out.println("HELLO COUNT");
                        }

                        String percentage = (String) snapshot.child("PERCENTAGE").getValue();
                        String pricing = (String) snapshot.child("PRICING").getValue();
                        double final_price = (Double.parseDouble(pricing) - (Double.parseDouble(percentage) / 100) * Double.parseDouble(pricing));
                        DecimalFormat df = new DecimalFormat("##.00");

                        System.out.println("FINAL PRICE: " + df.format(final_price));

                        Product product = new Product(((String) snapshot.child("CODE").getValue()),
                                ((String) snapshot.child("CONSUMABLES").getValue()),
                                (String) snapshot.child("DESCRIPTION").getValue(),
                                Double.parseDouble(df.format(final_price)),
                                (String) snapshot.child("PRICING_UNIT").getValue(),
                                percentage, (String) snapshot.child("True Image").getValue(),
                                (boolean) snapshot.child("PROMO").getValue(), (String) snapshot.child("END").getValue());
                        productList.add(product);
                    }
                    pDialog.dismiss();
                    recyclerView.setAdapter(new PromotionalContentRecyclerViewAdapter(productList, getActivity()));

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        pDialog.dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pDialog.dismiss();
    }
}
