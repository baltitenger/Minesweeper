package hu.desnull.baltazar.minesweeper;

import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hu.desnull.baltazar.minesweeper.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener {

    private float mineProb;
    private int width, height, numCorrectFlags, numIncorrectFlags, numMines;
    private boolean ended, liveNeighs, doPropagate, visualProp;

    private static final int propagateDelay = 20;
    private static final int vibrateBomb = 30;
    private static final int vibrateFlag = 50;
    private static final int maxBombVibrates = 8;
    private static final int minBombVibrates = 3;

    private ArrayList<Tile> tiles;
    private Vibrator vibrator;
    private Random random;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        tiles = new ArrayList<>();
        random = new Random();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        loadPrefs();

        init();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPrefs();
    }

    private void loadPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mineProb = Integer.parseInt(prefs.getString(getString(R.string.pref_mine_prob), "")) / 100f;
        width = height = Integer.parseInt(prefs.getString(getString(R.string.pref_board_size), ""));
        doPropagate = prefs.getBoolean(getString(R.string.pref_propagate_zeroes), false);
        visualProp = prefs.getBoolean(getString(R.string.pref_visual_propagation), false);
        liveNeighs = prefs.getBoolean(getString(R.string.pref_live_neighbour_count), false);
    }

    private void init() {
        ended = false;
        binding.end.setVisibility(View.INVISIBLE);
        numCorrectFlags = numIncorrectFlags = numMines = 0;
        binding.board.removeAllViews();
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
                if (random.nextDouble() < mineProb) {
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
            row.setDividerDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.divider, null));
            binding.board.addView(row, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, 1));
        }
        binding.board.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        binding.board.setDividerDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.divider, null));
        binding.minecount.setText(getString(R.string.minecount, 0, numMines));
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
        return (int)getNeighbours(id).stream().filter(i -> tiles.get(i).mine).count();
    }

    private int getNeighbourMarkCount(int id) {
        return (int)getNeighbours(id).stream().filter(i -> tiles.get(i).marked).count();
    }

    public int reveal(Tile tile, boolean propagate) {
        int mode = this.getResources().getConfiguration().uiMode;
        if ((mode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
            tile.setBackgroundColor(Color.DKGRAY);
        else
            tile.setBackgroundColor(Color.LTGRAY);
        tile.revealed = true;
        int neighbours = getNeighbourMineCount(tile.id);
        if (neighbours != 0) {
            if (liveNeighs) {
                neighbours -= getNeighbourMarkCount(tile.id);
            }
            tile.setText(String.valueOf(neighbours));
        }
        if (propagate && neighbours == 0 && doPropagate) {
            propagateZeroes(tile.id);
        }
        return neighbours;
    }

    private void propagateZeroes(Integer... ids) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            Set<Integer> nextPropagation = new HashSet<>(Arrays.asList(ids));
            do {
                Set<Integer> toReveal = new HashSet<>();
                Set<Integer> propagateNow = new HashSet<>(nextPropagation);
                nextPropagation.clear();
                for (int propagateId : propagateNow) {
                    for (int revealId : getNeighbours(propagateId)) {
                        Tile tile = tiles.get(revealId);
                        if (!(tile.marked || tile.revealed)) {
                            if (tile.mine) {
                                handler.post(this::lose);
                                return;
                            }
                            toReveal.add(revealId);
                            if (getNeighbourMineCount(revealId) == 0) {
                                nextPropagation.add(revealId);
                            }
                        }
                    }
                }
                handler.post(() -> {
                    for (int revealId : toReveal) {
                        reveal(tiles.get(revealId), false);
                    }
                });
                if (visualProp) {
                    try {
                        Thread.sleep(propagateDelay);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Uncaught", e);
                    }
                }
            } while (!nextPropagation.isEmpty());
        });
    }

    private void win() {
        ended = true;
        binding.end.setText(R.string.win);
        binding.end.setVisibility(VISIBLE);
        for (Tile tile : tiles) {
            if (tile.mine) {
                tile.setText(R.string.foundmine);
            }
        }
    }

    private void lose() {
        ended = true;
        binding.minecount.setText(getString(R.string.minecount, numCorrectFlags, numMines));
        int numExplosions = minBombVibrates + ((maxBombVibrates - minBombVibrates) * (numMines - numCorrectFlags) / numMines);
        long[] vibratePattern = new long[numExplosions * 2 - 1];
        Arrays.fill(vibratePattern, vibrateBomb);
        vibrator.vibrate(vibratePattern, -1);
        binding.end.setText(R.string.lose);
        binding.end.setVisibility(VISIBLE);
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
        if (liveNeighs) {
            Set<Integer> toPropagate = new HashSet<>();
            for (int neighbourId : getNeighbours(tile.id)) {
                if (tiles.get(neighbourId).revealed) {
                    if (reveal(tiles.get(neighbourId), false) == 0) {
                        toPropagate.add(neighbourId);
                    }
                }
            }
            propagateZeroes(toPropagate.toArray(new Integer[0]));
        }
        binding.minecount.setText(getString(R.string.minecount, numCorrectFlags + numIncorrectFlags, numMines));
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
