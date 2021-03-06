package io.branch.branchster;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import io.branch.branchster.fragment.InfoFragment;
import io.branch.branchster.util.MonsterImageView;
import io.branch.branchster.util.MonsterObject;
import io.branch.branchster.util.MonsterPreferences;
import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

public class MonsterViewerActivity extends FragmentActivity implements InfoFragment.OnFragmentInteractionListener {
    static final int SEND_SMS = 12345;

    private static String TAG = MonsterViewerActivity.class.getSimpleName();
    public static final String MY_MONSTER_OBJ_KEY = "my_monster_obj_key";
    BranchUniversalObject branchUniversalObject;
    TextView monsterUrl;
    View progressBar;

    MonsterImageView monsterImageView_;
    MonsterObject myMonsterObject;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monster_viewer);

        monsterImageView_ = (MonsterImageView) findViewById(R.id.monster_img_view);
        monsterUrl = (TextView) findViewById(R.id.shareUrl);
        progressBar = findViewById(R.id.progress_bar);

        // Change monster
        findViewById(R.id.cmdChange).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), MonsterCreatorActivity.class);
                startActivity(i);
                finish();
            }
        });

        // More info
        findViewById(R.id.infoButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                InfoFragment infoFragment = InfoFragment.newInstance();
                ft.replace(R.id.container, infoFragment).addToBackStack("info_container").commit();
            }
        });

        //Share monster
        findViewById(R.id.share_btn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                shareMyMonster();
            }
        });
         branchUniversalObject = new BranchUniversalObject();
        try {
            initUI();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initUI() throws JSONException {
        myMonsterObject = getIntent().getParcelableExtra(MY_MONSTER_OBJ_KEY);

        if (myMonsterObject != null) {
            String monsterName = getString(R.string.monster_name);

            if (!TextUtils.isEmpty(myMonsterObject.getMonsterName())) {
                monsterName = myMonsterObject.getMonsterName();
            }
            if(myMonsterObject.monsterMetaData().containsKey(MonsterPreferences.KEY_FACE_INDEX)) {
            ;
                JSONObject statemetaData = new JSONObject();
                statemetaData.put("State", myMonsterObject.monsterMetaData().get(MonsterPreferences.KEY_FACE_INDEX));
                Branch.getInstance().userCompletedAction("monster_view", statemetaData);
            }
            ((TextView) findViewById(R.id.txtName)).setText(monsterName);
            String description = MonsterPreferences.getInstance(this).getMonsterDescription();

            if (!TextUtils.isEmpty(myMonsterObject.getMonsterDescription())) {
                description = myMonsterObject.getMonsterDescription();
            }

            ((TextView) findViewById(R.id.txtDescription)).setText(description);

            // set my monster image
            monsterImageView_.setMonster(myMonsterObject);
            branchUniversalObject.setTitle(Objects.requireNonNull(myMonsterObject.prepareBranchDict().get(MonsterPreferences.KEY_MONSTER_NAME)));
            branchUniversalObject.setContentDescription(myMonsterObject.prepareBranchDict().get(MonsterPreferences.KEY_MONSTER_DESCRIPTION));
            branchUniversalObject.setContentImageUrl(Objects.requireNonNull(myMonsterObject.prepareBranchDict().get(MonsterPreferences.KEY_MONSTER_IMAGE)));
            branchUniversalObject.setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC);
            LinkProperties linkProperties = new LinkProperties()
                    .setChannel("test")
                    .setFeature("sharing")
                    .addControlParameter("$always_deeplink", "true");
            branchUniversalObject.generateShortUrl(MonsterViewerActivity.this, linkProperties, new Branch.BranchLinkCreateListener() {
                @Override
                public void onLinkCreate(String url, BranchError error) {
                    ((TextView) findViewById(R.id.shareUrl)).setText(url);

                    progressBar.setVisibility(View.GONE);
                }
            });

        } else {
            Log.e(TAG, "Monster is null. Unable to view monster");
        }
    }

    /**
     * Method to share my custom monster with sharing with Branch Share sheet
     */
    private void shareMyMonster() {
     //   new AsyncLink().execute();
        LinkProperties linkProperties = new LinkProperties()
                .setChannel("SMS")
                .setFeature("sharing")
                .addControlParameter("$always_deeplink", "true");

//urlb=branchUniversalObject.getShortUrl(MonsterViewerActivity.this,linkProperties,new Branch.BranchListResponseListener);

        branchUniversalObject.generateShortUrl(MonsterViewerActivity.this, linkProperties, new Branch.BranchLinkCreateListener() {
                    @Override
                    public void onLinkCreate(String url, BranchError error) {
                        if (error != null) {
                            Log.d("errorbranch", error.toString());
                            progressBar.setVisibility(View.GONE);
                            //   AsyncLink.this.cancel(true);
                        } else {
                            //   urlb = url;
                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("text/plain");
                            i.putExtra(Intent.EXTRA_TEXT, String.format("Check out my Branchster named %s at %s", myMonsterObject.getMonsterName(), url));
                            startActivityForResult(i, SEND_SMS);

                            progressBar.setVisibility(View.GONE);
                            Log.d(TAG + " onPostExecute", "" + url);

                        }
                    }
                });
      //  new AsyncLink().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
         // TODO: Replace with Branch-generated shortUrl



    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (SEND_SMS == requestCode) {
            if (RESULT_OK == resultCode) {
                // TODO: Track successful share via Branch.
            }
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Exit")
                    .setMessage("Are you sure you want to exit?")
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).create().show();
        }
    }


    @Override
    public void onFragmentInteraction() {
        //no-op
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        try {
            initUI();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
