package com.msqhealth.main.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.msqhealth.main.R;
import com.msqhealth.main.model.CartItem;
import com.msqhealth.main.model.Product;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.lang3.text.WordUtils;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyListingDetailsAdapter extends RecyclerView.Adapter<MyListingDetailsAdapter.ViewHolder> {

    private final List<Product> mValues;
    private Activity activity;
    private DatabaseReference mDatabase;

    public MyListingDetailsAdapter(List<Product> items, Activity activity) {
        this.mValues = items;
        this.activity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.promotions_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        holder.mItem = mValues.get(position);
        holder.mConsumableView.setText(mValues.get(position).consumables);
        holder.mDescriptionView.setText(WordUtils.capitalizeFully(mValues.get(position).description.toLowerCase()));
        holder.mPricingUnitView.setText(String.valueOf(mValues.get(position).unit_of_messuremeant.toLowerCase()));
        Glide.with(activity).load(mValues.get(position).trueImageUrl).into(holder.mProductImage);


        DecimalFormat df = new DecimalFormat("##.00");

        holder.mPricingUnitView.setText(mValues.get(position).unit_of_messuremeant.toLowerCase());
        holder.mPriceView.setText(String.valueOf("R" + String.valueOf(df.format(mValues.get(position).price)).replace(",", ".")));
        holder.mCodeView.setText(mValues.get(position).code);

        hideOtherViews(holder);

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("carts").child("pending");

        //Button to decrement the items in the cart
        holder.mDecrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                quant[0] = (long) dataSnapshot.child("quantity").getValue();
                int quantity = Integer.parseInt(holder.mQuantity.getText().toString());

                if (quantity >= 2) {
                    quantity--;
                    System.out.println("GET QUANTITY: " + quantity);
                    holder.mQuantity.setText(String.valueOf(quantity));
                }
            }
        });

        //Button to increment the items in the cart
        holder.mIncrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int[][] quantity = {{Integer.parseInt(holder.mQuantity.getText().toString())}};
                quantity[0][0]++;
                System.out.println("GET QUANTITY: " + quantity[0][0]);
                holder.mQuantity.setText(String.valueOf(quantity[0][0]));
            }
        });

        mDatabase.child(mValues.get(position).getCode()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long quant = 0;
                if (dataSnapshot.exists()) {
                    quant = (long) dataSnapshot.child("quantity").getValue();
                    final int[][] quantity = {{Integer.parseInt(holder.mQuantity.getText().toString())}};

                    holder.mQuantity.setText(String.valueOf(quant));

                    //Button to decrement the items in the cart
//                    holder.mIncrementButton.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View view) {
//                            quantity[0][0]++;
//                            System.out.println("GET QUANTITY: " + quantity[0][0]);
//                            holder.mQuantity.setText(String.valueOf(quantity[0][0]));
//                        }
//                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mDatabase.child(mValues.get(position).code).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    holder.btnAddToCart.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (holder.mQuantity.getText().toString() == "") {
                                holder.mQuantity.setError("Please add quantity...");
                                return;
                            }

                            CartItem item = dataSnapshot.getValue(CartItem.class);
                            try {
                                if (holder.mQuantity.getText() != null) {
                                    item.setQuantity(item.getQuantity() + Integer.parseInt(holder.mQuantity.getText().toString()));
                                } else {
                                    item.setQuantity(item.getQuantity() + 1);
                                    return;
                                }
                            } catch (Exception ex) {
                                holder.mQuantity.setError("Please add a quantity");
                                return;
                            }
                            Map<String, Object> postValues = item.toMap();

                            Map<String, Object> childUpdates = new HashMap<>();
                            childUpdates.put("/carts/pending/" + mValues.get(position).getCode(), postValues);
                            FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).updateChildren(childUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Snackbar snack = Snackbar.make(holder.mView, "Product added to cart",
                                                Snackbar.LENGTH_INDEFINITE).setDuration(2000);
                                        snack.getView().setBackgroundColor(ContextCompat.getColor(activity, android.R.color.holo_green_dark));
                                        View view = snack.getView();
                                        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                                        params.gravity = Gravity.BOTTOM;
                                        view.setLayoutParams(params);
                                        snack.show();
                                    } else {
                                        task.getException().printStackTrace();
                                    }
                                }
                            });
                        }
                    });
                } else {
                    holder.btnAddToCart.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (holder.mQuantity.getText().toString() == "") {
                                holder.mQuantity.setError("Please add quantity...");
                                return;
                            }
                            CartItem cartItem = new CartItem();
                            cartItem.setOwner_id(user.getUid());
                            cartItem.setProduct(holder.mItem);
                            cartItem.setQuantity(1);
                            try {
                                if (holder.mQuantity.getText() != null) {
                                    cartItem.setQuantity(Integer.parseInt(holder.mQuantity.getText().toString()));
                                } else {
                                    cartItem.setQuantity(1);
                                }
                            } catch (Exception ex) {
                                holder.mQuantity.setError("Please add a quantity");
                                return;
                            }

                            Map<String, Object> postValues = cartItem.toMap();


                            if (isNetworkAvailable()) {

                                Map<String, Object> childUpdates = new HashMap<>();
                                childUpdates.put("/carts/pending/" + mValues.get(position).getCode(), postValues);

                                FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid()).updateChildren(childUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Snackbar snack = Snackbar.make(holder.mView, "Product added to cart",
                                                    Snackbar.LENGTH_INDEFINITE).setDuration(1000);
                                            snack.getView().setBackgroundColor(ContextCompat.getColor(activity, android.R.color.holo_green_dark));
                                            View view = snack.getView();
                                            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                                            params.gravity = Gravity.BOTTOM;
                                            view.setLayoutParams(params);
                                            snack.show();
//                                        Toast.makeText(activity, "Item added to cart", Toast.LENGTH_SHORT).show();
                                        } else {
                                            task.getException().printStackTrace();
                                        }
                                    }
                                });

                            } else if (!isNetworkAvailable()) {

                                Snackbar snack = Snackbar.make(holder.mView, "No Connection Available, please check your internet settings and try again.",
                                        Snackbar.LENGTH_INDEFINITE).setDuration(1000);
                                snack.getView().setBackgroundColor(ContextCompat.getColor(activity, android.R.color.holo_red_dark));
                                View view = snack.getView();
                                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                                params.gravity = Gravity.TOP;
                                view.setLayoutParams(params);
                                snack.show();
                            }

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (databaseError != null) {
                    databaseError.toException().printStackTrace();
                    Toast.makeText(activity, databaseError.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void showDialog() {
        View mView = LayoutInflater.from(activity).inflate(R.layout.deleted_custom_dialog, null);

        TextView successView = mView.findViewById(R.id.tv_success);
        android.support.v7.app.AlertDialog.Builder aBuilder = new android.support.v7.app.AlertDialog.Builder(activity, R.style.CustomDialog);
        aBuilder.setView(mView);

        successView.setText("Successfully Added to Cart!");

        final android.support.v7.app.AlertDialog alert = aBuilder.create();
        alert.show();

        // Hide after some seconds
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (alert.isShowing()) {
                    alert.dismiss();
                }
            }
        };
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
            }
        });

        handler.postDelayed(runnable, 1500);

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView mProductImage;
        public final TextView mConsumableView;
        public final TextView mPriceView, mDescriptionView, mPricingUnitView, mPercentageView, mCodeView, tvStartDate, tvEndDate, tvHyphen;
        public final Button btnAddToCart;
        public final EditText mQuantity;
        RelativeLayout mRelativeLayout;
        public final ImageButton mIncrementButton;
        public final ImageButton mDecrementButton;
        public final ImageView mOrangeRibbon;
        public Product mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mProductImage = (ImageView) view.findViewById(R.id.img_product);
            mConsumableView = (TextView) view.findViewById(R.id.tv_consumable);
            mPriceView = (TextView) view.findViewById(R.id.tv_price);
            mPricingUnitView = view.findViewById(R.id.info_unit);
            mCodeView = view.findViewById(R.id.tv_code);
            btnAddToCart = view.findViewById(R.id.addToCart);
            mQuantity = view.findViewById(R.id.cart_item_quantity);
            mPercentageView = view.findViewById(R.id.tv_percentage);
            mRelativeLayout = view.findViewById(R.id.relative_layout);
            mDecrementButton = view.findViewById(R.id.btn_decrement);
            mIncrementButton = view.findViewById(R.id.btn_increment);
            mDescriptionView = (TextView) view.findViewById(R.id.tv_description);
            mOrangeRibbon = view.findViewById(R.id.orange_ribbon);
            tvStartDate = view.findViewById(R.id.tv_start_date);
            tvEndDate = view.findViewById(R.id.tv_end_date);
            tvHyphen = view.findViewById(R.id.hyphen);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mDescriptionView.getText() + "'";
        }


    }

    public void hideOtherViews(ViewHolder holder) {
        holder.mPercentageView.setVisibility(View.GONE);
        holder.mOrangeRibbon.setVisibility(View.GONE);
        holder.tvStartDate.setVisibility(View.GONE);
        holder.tvEndDate.setVisibility(View.GONE);
        holder.tvHyphen.setVisibility(View.GONE);
    }
}
