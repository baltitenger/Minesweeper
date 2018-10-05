package hu.desnull.baltazar.minesweeper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener {
    private float mineProb;
    private int width, height, numCorrectFlags, numIncorrectFlags, numMines;
    private boolean ended;

    private static final int propagateDelay = 20;
    private static final int vibrateBomb = 30;
    private static final int vibrateFlag = 50;
    private static final int maxBombVibrates = 8;
    private static final int minBombVibrates = 3;

    private SharedPreferences sharedPrefs;
    private List<Tile> tiles;
    private LinearLayout board;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        tiles = new LinkedList<>();
        board = findViewById(R.id.board);
        PreferenceManager.setDefaultValues(this, R.xml.preferences,false);
        loadPrefs();
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()) {
            case R.id.action_preferences:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPrefs();
    }

    private void loadPrefs() {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mineProb = Integer.parseInt(sharedPrefs.getString(getString(R.string.pref_mine_probability), "")) / 100f;
        width = height = Integer.parseInt(sharedPrefs.getString(getString(R.string.pref_board_size), ""));
    }

    private void init() {
        ended = false;
        findViewById(R.id.end).setVisibility(INVISIBLE);
        numCorrectFlags = numIncorrectFlags = numMines = 0;
        board.removeAllViews();
        for (int y = 0; y < height; ++y) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int x = 0; x < width; ++x) {
                Tile tile;
                if (coordsToId(x, y) > tiles.size() - 1) {
                    tile = new Tile(this, coordsToId(x, y));
                    tiles.add(tile);
                } else {
                    tile = tiles.get(coordsToId(x, y));
                    LinearLayout parent = (LinearLayout) tile.getParent();
                    if (parent != null) {
                        parent.removeAllViews();
                    }
                    tile.init();
                }
                if (new Random().nextDouble() < mineProb) {
                    numMines++;
                    tile.mine = true;
                } else {
                    tile.mine = false;
                }
                tile.setOnClickListener(this);
                tile.setOnLongClickListener(this);
                row.addView(tile, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, 1));
            }
            row.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            row.setDividerDrawable(getResources().getDrawable(R.drawable.divider));
            board.addView(row, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, 1));
        }
        board.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        board.setDividerDrawable(getResources().getDrawable(R.drawable.divider));
        TextView minecountTextView = findViewById(R.id.minecount);
        minecountTextView.setText(getString(R.string.minecount, 0, numMines));
    }

    private int coordsToId(int x, int y) {
        return y * width + x;
    }

    private Point idToCoords(int id) {
        return new Point(id % width, id / width);
    }

    private Set<Integer> getNeighbours(int id) {
        Set<Integer> neighbours = new HashSet<>();
        Point target = idToCoords(id);
        for (int y = Math.max(0, target.y - 1); y <= target.y + 1 && y < height; ++y) {
            for (int x = Math.max(0, target.x - 1); x <= target.x + 1 && x < height; ++x) {
                if (!(x == target.x && y == target.y)) {
                    neighbours.add(coordsToId(x, y));
                }
            }
        }
        return neighbours;
    }

    private int getNeighbourMineCount(int id) {
        int neighbourMineCount = 0;
        for (int neighbourId : getNeighbours(id)) {
            if (tiles.get(neighbourId).mine) {
                ++neighbourMineCount;
            }
        }
        return neighbourMineCount;
    }

    private int getNeighbourMarkCount(int id) {
        int neighbourMarkCount = 0;
        for (int neighbourId : getNeighbours(id)) {
            if (tiles.get(neighbourId).marked) {
                ++neighbourMarkCount;
            }
        }
        return neighbourMarkCount;
    }

    public int reveal(Tile tile, boolean propagate) {
        tile.setBackgroundColor(Color.LTGRAY);
        tile.revealed = true;
        int neighbours = getNeighbourMineCount(tile.id);
        if (neighbours != 0) {
            if (sharedPrefs.getBoolean(getString(R.string.pref_live_neighbour_count), false)) {
                neighbours -= getNeighbourMarkCount(tile.id);
            }
            tile.setText(String.valueOf(neighbours));
        }
        if (propagate && neighbours == 0 && sharedPrefs.getBoolean(getString(R.string.pref_propagate_zeroes), false)) {
            new propagateZeroesTask().execute(tile.id);
        }
        return neighbours;
    }

    private class propagateZeroesTask extends AsyncTask<Integer, Void, Boolean> {
        Set<Integer> nextPropagation;
        Set<Integer> toReveal;

        @Override
        protected Boolean doInBackground(Integer... ids) {
            nextPropagation = new HashSet<>(Arrays.asList(ids));
            Set <Integer> nextReveal = new HashSet<>();
            do {
                Set<Integer> propagateNow = new HashSet<>(nextPropagation);
                nextPropagation.clear();
                for (int propageteId : propagateNow) {
                    for (int revealId : getNeighbours(propageteId)) {
                        Tile tile = tiles.get(revealId);
                        if (!(tile.marked || tile.revealed)) {
                            if (tile.mine) {
                                return true;
                            }
                            nextReveal.add(revealId);
                            if (getNeighbourMineCount(revealId) == 0) {
                                nextPropagation.add(revealId);
                            }
                        }
                    }
                }
                toReveal = new HashSet<>(nextReveal);
                nextReveal.clear();
                publishProgress();
                if (sharedPrefs.getBoolean(getString(R.string.pref_visual_propagation), false)) {
                    try {
                        Thread.sleep(propagateDelay);
                    } catch (InterruptedException ignore) {}
                }
            } while (!nextPropagation.isEmpty());
            return false;
        }

        @Override
        protected void onPostExecute(Boolean lost) {
            if (lost) {
                lose();
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            for (int revealId : toReveal) {
                reveal(tiles.get(revealId), false);
            }
        }
    }

    private void win() {
        ended = true;
        TextView endtext = findViewById(R.id.end);
        endtext.setText(R.string.win);
        endtext.setVisibility(VISIBLE);
        for (Tile tile : tiles) {
            if (tile.mine) {
                tile.setText(R.string.foundmine);
            }
        }
    }

    private void lose() {
        ended = true;
        TextView minecount = findViewById(R.id.minecount);
        minecount.setText(getString(R.string.minecount, numCorrectFlags, numMines));
        int numExplosions = minBombVibrates + ((maxBombVibrates - minBombVibrates) * (numMines - numCorrectFlags) / numMines);
        long[] vibratePattern = new long[numExplosions * 2 - 1];
        Arrays.fill(vibratePattern, vibrateBomb);
        vibrator.vibrate(vibratePattern, -1);
        TextView endtext = findViewById(R.id.end);
        endtext.setText(R.string.lose);
        endtext.setVisibility(VISIBLE);
        for (Tile tile : tiles) {
            if (tile.mine) {
                if (tile.marked) {
                    tile.setText(R.string.foundmine);
                } else {
                    tile.setText(R.string.mine);
                }
            } else if (tile.marked) {
                tile.setTextColor(Color.RED);
            }
        }
    }

    public void onClick(View v) {
        Tile tile = (Tile) v;
        if (ended || tile.revealed || tile.marked) {
            return;
        }
        if (tile.mine) {
            lose();
        } else {
            reveal(tile, true);
        }
    }

    public boolean onLongClick(View v) {
        Tile tile = (Tile) v;
        if (ended || tile.revealed) {
            return true;
        }
        vibrator.vibrate(vibrateFlag);
        if (tile.marked) {
            tile.setText("");
            tile.marked = false;
            if (tile.mine) {
                --numCorrectFlags;
            } else {
                --numIncorrectFlags;
            }
        } else {
            tile.setText(R.string.flag);
            tile.marked = true;
            if (tile.mine) {
                ++numCorrectFlags;
            } else {
                ++numIncorrectFlags;
            }
        }
        if (sharedPrefs.getBoolean(getString(R.string.pref_live_neighbour_count), false)) {
            Set<Integer> toPropagate = new HashSet<>();
            for (int neighbourId : getNeighbours(tile.id)) {
                if (tiles.get(neighbourId).revealed) {
                    if (reveal(tiles.get(neighbourId), false) == 0) {
                        toPropagate.add(neighbourId);
                    }
                }
            }
            new propagateZeroesTask().execute(toPropagate.toArray(new Integer[0]));
        }
        TextView minecount = findViewById(R.id.minecount);
        minecount.setText(getString(R.string.minecount, numCorrectFlags + numIncorrectFlags, numMines));
        if (numCorrectFlags == numMines && numIncorrectFlags == 0) {
            win();
        }
        return true;
    }

    public void reset(View view) {
        init();
    }

    public void quit(View view) {
        finish();
    }
}
