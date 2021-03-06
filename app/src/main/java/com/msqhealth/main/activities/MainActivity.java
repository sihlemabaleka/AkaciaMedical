package com.msqhealth.main.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.msqhealth.main.R;
import com.msqhealth.main.dialogs.CartDialogs;
import com.msqhealth.main.fragments.ProductFragment;
import com.msqhealth.main.fragments.PromotionalContentFragment;
import com.msqhealth.main.helpers.PrefManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    List<Map<String, Object>> items = new ArrayList<>();

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private PrefManager prefManager;
    private DatabaseReference mDatabase, mDatabaseUsers;
    private FirebaseAuth mAuth;
    private int cart_count = 0;
    private int count_cart = 0;
    private TextView mCartCountTextView;
    private FirebaseUser user;

    private RelativeLayout mCartRelativeLayout;

    ImageButton btnCart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main_menu);

        //Initialising and setting my toolBar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_main);
        myToolbar.setTitle("MSQ Health");
        setSupportActionBar(myToolbar);
        //Setting up the back button click listener
        myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btnCart = findViewById(R.id.cart);
        //Setting up the icon for the drop down menu, which is called options menu.
        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_more_vert_black_24dp);
        myToolbar.setOverflowIcon(drawable);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());


        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("users");

        mCartCountTextView = findViewById(R.id.actionbar_notifcation_textview);
        mCartRelativeLayout = findViewById(R.id.cart_layout);

        user = FirebaseAuth.getInstance().getCurrentUser();
        mAuth = FirebaseAuth.getInstance();

        if (user != null) {
            mDatabaseUsers.child(user.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        cart_count = (int) dataSnapshot.child("carts").child("pending").getChildrenCount();
                        if (cart_count == 0) {
                            mCartCountTextView.setVisibility(View.GONE);
                        } else {
                            mCartCountTextView.setVisibility(View.VISIBLE);
                            mCartCountTextView.setText(String.valueOf(cart_count));

                        }

                        btnCart.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                CartDialogs dialog = new CartDialogs();
                                dialog.show(getSupportFragmentManager(), "Checkout");
                            }
                        });
                        mCartCountTextView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                CartDialogs dialog = new CartDialogs();
                                dialog.show(getSupportFragmentManager(), "Checkout");
                            }
                        });

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        if (!isNetworkAvailable()) {

            Snackbar snack = Snackbar.make(findViewById(R.id.linear), "No Connection Available, please check your internet settings and try again.",
                    Snackbar.LENGTH_INDEFINITE).setDuration(5000);
            snack.getView().setBackgroundColor(ContextCompat.getColor(getApplicationContext(), android.R.color.holo_red_dark));
            View view = snack.getView();
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
            params.gravity = Gravity.TOP;
            view.setLayoutParams(params);
            snack.show();
        }




    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_profile) {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_terms_and_conditions) {
            Intent intent = new Intent(MainActivity.this, AboutUs.class);
            startActivity(intent);
            return true;
        }
//        if (id == R.id.action_publications) {
//            Intent intent = new Intent(MainActivity.this, PublicationsActivity.class);
//            startActivity(intent);
//            return true;
//        }
        if (id == R.id.action_log_out) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return new ProductFragment();
                case 1:
                    return new PromotionalContentFragment();
                default:
                    return new ProductFragment();
            }

        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Browse";
                case 1:
                    return "Promotions";
            }
            return null;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     *
     * @param ip
     * @param userName
     * @param pass
     */
    public void connnectingwithFTP(final String ip, final String userName, final String pass)  {

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    //Your code goes here
                    boolean status = false;
                    try {
                        FTPClient mFtpClient = new FTPClient();
                        mFtpClient.setConnectTimeout(10 * 1000);
                        mFtpClient.connect(InetAddress.getByName(ip));
                        status = mFtpClient.login(userName, pass);
                        Log.e("isFTPConnected", String.valueOf(status));
                        if (FTPReply.isPositiveCompletion(mFtpClient.getReplyCode())) {
                            mFtpClient.setFileType(FTP.ASCII_FILE_TYPE);
                            mFtpClient.enterLocalPassiveMode();
                            FTPFile[] mFileArray = mFtpClient.listFiles();
                            Log.e("Size", String.valueOf(mFileArray.length));
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
