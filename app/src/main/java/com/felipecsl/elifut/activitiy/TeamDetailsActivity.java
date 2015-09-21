package com.felipecsl.elifut.activitiy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import com.felipecsl.elifut.R;
import com.felipecsl.elifut.adapter.ViewPagerAdapter;
import com.felipecsl.elifut.fragment.TeamDetailsFragment;
import com.felipecsl.elifut.fragment.TeamPlayersFragment;
import com.felipecsl.elifut.models.Club;
import com.felipecsl.elifut.models.League;
import com.felipecsl.elifut.models.Nation;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import icepick.Icepick;
import icepick.State;
import retrofit.Response;
import rx.android.schedulers.AndroidSchedulers;

public class TeamDetailsActivity extends ElifutActivity {
  private static final String EXTRA_COUNTRY = "EXTRA_COUNTRY";
  private static final String EXTRA_NAME = "EXTRA_NAME";
  private static final String TAG = TeamDetailsActivity.class.getSimpleName();

  @Bind(R.id.toolbar) Toolbar toolbar;
  @Bind(R.id.viewpager) ViewPager viewPager;
  @Bind(R.id.tabs) TabLayout tabLayout;

  @State Nation nation;
  @State String coachName;
  @State Club club;
  @State League league;

  public static Intent newIntent(Context context, Nation nation, String name) {
    return new Intent(context, TeamDetailsActivity.class)
        .putExtra(EXTRA_COUNTRY, nation)
        .putExtra(EXTRA_NAME, name);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_team_details);
    ButterKnife.bind(this);
    daggerComponent().inject(this);
    Icepick.restoreInstanceState(this, savedInstanceState);
    setSupportActionBar(toolbar);

    if (savedInstanceState == null) {
      Intent intent = getIntent();
      coachName = intent.getStringExtra(EXTRA_NAME);
      nation = intent.getParcelableExtra(EXTRA_COUNTRY);
    }

    if (savedInstanceState == null) {
      loadClub();
    } else {
      loadLeague();
    }
  }

  private void setupViewPager() {
    ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
    adapter.addFragment(TeamDetailsFragment.newInstance(club, coachName, nation, league),
        getString(R.string.infos));
    adapter.addFragment(TeamPlayersFragment.newInstance(club), getString(R.string.players));
    viewPager.setAdapter(adapter);
    tabLayout.setupWithViewPager(viewPager);
  }

  private void loadClub() {
    service.randomClub(nation.id())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new SimpleResponseObserver<Club>() {
          @Override public void onError(Throwable e) {
            Toast.makeText(
                TeamDetailsActivity.this, "Failed to load club", Toast.LENGTH_SHORT).show();
            Log.w(TAG, e);
          }

          @Override public void onNext(Response<Club> response) {
            if (response.isSuccess()) {
              club = response.body();
              loadLeague();
            } else {
              Log.w(TAG, "Failed to load club");
            }
          }
        });
  }

  private void loadLeague() {
    service.league(club.league_id())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new SimpleResponseObserver<League>() {
          @Override public void onError(Throwable e) {
            Toast.makeText(TeamDetailsActivity.this, "Failed to load league infos",
                Toast.LENGTH_SHORT).show();
            Log.w(TAG, e);
          }

          @Override public void onNext(Response<League> response) {
            if (response.isSuccess()) {
              league = response.body();
              toolbar.setTitle(club.shortName());
              setupViewPager();
            } else {
              Log.w(TAG, "Failed to load league infos");
            }
          }
        });
  }

  public void setToolbarColor(int primaryColor, int secondaryColor) {
    toolbar.setBackgroundColor(primaryColor);
    tabLayout.setBackgroundColor(primaryColor);
    tabLayout.setSelectedTabIndicatorColor(secondaryColor);
    getWindow().setStatusBarColor(primaryColor);
  }

  @OnClick(R.id.fab) public void onClickNext() {
    if (league == null) {
      // League not loaded yet...
      return;
    }
    startActivity(LeagueDetailsActivity.newIntent(this, league, club));
  }
}