package hu.desnull.baltazar.minesweeper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Random;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener{
    private int width = 10;
    private int height= 10;
    private Button[] buttons;
    private LinearLayout board;
    private Context context;
    private double mineProb = 0.15;
    private int numCorrectFlags, numIncorrectFlags, numMines;
    private Vibrator vibrator;
    private static final int vibrateBomb = 30;
    private static final int vibrateFlag = 50;
    private static final int maxBombVibrates = 8;
    private static final int minBombVibrates = 3;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
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
                Intent intent = new Intent(context, SettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void init() {
        FrameLayout boardContainer = findViewById(R.id.board_container);
        boardContainer.removeAllViews();
        findViewById(R.id.end).setVisibility(INVISIBLE);
        numCorrectFlags = numIncorrectFlags = numMines = 0;
        buttons = new Button[width * height];
        board = new LinearLayout(context);
        board.setOrientation(LinearLayout.VERTICAL);
        boardContainer.addView(board, MATCH_PARENT, MATCH_PARENT);
        for (int y = 0; y < height; ++y) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int x = 0; x < width; ++x) {
                Button button = new Button(context);
                ButtonData buttonData = new ButtonData(coordsToId(x, y));
                if (new Random().nextDouble() < mineProb) {
                    numMines++;
                    buttonData.isMine = true;
                } else {
                    buttonData.isMine = false;
                }
                button.setHapticFeedbackEnabled(false);
                button.setTag(R.string.buttonData, buttonData);
                button.setOnClickListener(this);
                button.setOnLongClickListener(this);
                button.setBackgroundColor(Color.GRAY);
                button.setPadding(0, 0, 0, 0);
                buttons[coordsToId(x, y)] = button;
                row.addView(button, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, 1.0f / width));
            }
            row.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            row.setDividerDrawable(getResources().getDrawable(R.drawable.divider));
            board.addView(row, new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, 1.0f / height));
        }
        board.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        board.setDividerDrawable(getResources().getDrawable(R.drawable.divider));
        board.setBackgroundColor(Color.DKGRAY);
        TextView minecountTextView = findViewById(R.id.minecount);
        minecountTextView.setText(getString(R.string.minecount, 0, numMines));
    }

    private int coordsToId(int x, int y) {
        return y * width + x;
    }

    private Point idToCoords(int id) {
        return new Point(id % width, id / width);
    }

    private ButtonData getButtonData(Button button) {
        return (ButtonData) button.getTag(R.string.buttonData);
    }

    private ButtonData getButtonData(int id) {
        return getButtonData(buttons[id]);
    }

    private ButtonData getButtonData(int x, int y) {
        return getButtonData(coordsToId(x, y));
    }

    private int getNeighbourCount(int id) {
        Point target = idToCoords(id);
        int neighbours = 0;
        for (int y = Math.max(0, target.y - 1); y <= target.y + 1 && y < height; ++y) {
            for (int x = Math.max(0, target.x - 1); x <= target.x + 1 && x < height; ++x) {
                if (!(x == target.x && y == target.y) &&  getButtonData(x, y).isMine) {
                    ++neighbours;
                }
            }
        }
        return neighbours;
    }

    private class propagateZeroesTask extends AsyncTask<Void, Void, Void> {
        int mId;
        public propagateZeroesTask(int id){
            mId = id;
            this.execute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Point target = idToCoords(mId);
            for (int y = Math.max(0, target.y - 1); y <= target.y + 1 && y < height; ++y) {
                for (int x = Math.max(0, target.x - 1); x <= target.x + 1 && x < height; ++x) {
                    int chkid = coordsToId(x, y);
                    ButtonData buttonData = getButtonData(chkid);
                    if ((x == target.x && y == target.y) || buttonData.isRevealed || buttonData.isMarked) {
                        continue;
                    }
                    if (sharedPrefs.getBoolean(getString(R.string.pref_visual_propagation), false)) {
                        SystemClock.sleep(10);
                    }
                    Button button = buttons[chkid];
                    button.setBackgroundColor(Color.LTGRAY);
                    buttonData.isRevealed = true;
                    button.setOnClickListener(null);
                    button.setOnLongClickListener(null);
                    if (getNeighbourCount(chkid) == 0) {
                        new propagateZeroesTask(chkid);
                    } else {
                        button.setText(String.valueOf(getNeighbourCount(chkid)));
                    }
                }
            }
            return null;
        }
    }

    public void onClick(View v) {
        Button button = (Button) v;
        ButtonData buttonData = getButtonData(button);
        if (buttonData.isRevealed || buttonData.isMarked) {
            return;
        }
        if (buttonData.isMine) {
            TextView minecount = findViewById(R.id.minecount);
            minecount.setText(getString(R.string.minecount, numCorrectFlags, numMines));
            int numExplosions = minBombVibrates + ((maxBombVibrates - minBombVibrates) * (numMines - numCorrectFlags) / numMines);
            long[] vibratePattern = new long[numExplosions * 2 - 1];
            Arrays.fill(vibratePattern, vibrateBomb);
            vibrator.vibrate(vibratePattern, -1);
            TextView endtext = findViewById(R.id.end);
            endtext.setText(R.string.lose);
            endtext.setVisibility(VISIBLE);
            for (Button b: buttons) {
                b.setOnClickListener(null);
                b.setOnLongClickListener(null);
                ButtonData bd = getButtonData(b);
                if (bd.isMine) {
                    if (bd.isMarked) {
                        b.setText(R.string.foundmine);
                    } else {
                        b.setText(R.string.mine);
                    }
                } else if (bd.isMarked) {
                    b.setTextColor(Color.RED);
                }
            }
        } else {
            button.setOnClickListener(null);
            button.setOnLongClickListener(null);
            button.setBackgroundColor(Color.LTGRAY);
            buttonData.isRevealed = true;
            int neighbours = getNeighbourCount(buttonData.id);
            if (neighbours == 0) {
                if (sharedPrefs.getBoolean(getString(R.string.pref_propagate_zeroes), true)) {
                    new propagateZeroesTask(buttonData.id);
                }
            } else {
                button.setText(String.valueOf(neighbours));
            }
        }
    }

    public boolean onLongClick(View v) {
        vibrator.vibrate(vibrateFlag);
        Button button = (Button) v;
        ButtonData buttonData = getButtonData(button);
        if (buttonData.isMarked) {
            button.setText("");
            buttonData.isMarked = false;
            if (buttonData.isMine) {
                --numCorrectFlags;
            } else {
                --numIncorrectFlags;
            }
        } else {
            button.setText(R.string.flag);
            buttonData.isMarked = true;
            if (buttonData.isMine) {
                ++numCorrectFlags;
            } else {
                ++numIncorrectFlags;
            }
        }
        TextView minecount = findViewById(R.id.minecount);
        minecount.setText(getString(R.string.minecount, numCorrectFlags + numIncorrectFlags, numMines));
        if (numCorrectFlags == numMines && numIncorrectFlags == 0) {
            TextView endtext = findViewById(R.id.end);
            endtext.setText(R.string.win);
            endtext.setVisibility(VISIBLE);
            for (Button b : buttons) {
                b.setOnClickListener(null);
                b.setOnLongClickListener(null);
                if (getButtonData(b).isMine) {
                    b.setText(R.string.foundmine);
                }
            }
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
