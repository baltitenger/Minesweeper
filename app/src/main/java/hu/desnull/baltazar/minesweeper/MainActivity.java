package hu.desnull.baltazar.minesweeper;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private double mineProb = 0.2;
    private int numCorrectFlags, numIncorrectFlags, numMines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        init();
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
                button.setTag(R.string.tileid, coordsToId(x, y));
                button.setTag(R.string.ismarked, false);
                button.setTag(R.string.isseen, false);
                if (new Random().nextDouble() < mineProb) {
                    numMines++;
                    button.setTag(R.string.ismine, true);
                } else {
                    button.setTag(R.string.ismine, false);
                }
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
        minecountTextView.setText(getResources().getString(
                R.string.minecount, numMines));
    }

    private int coordsToId(int x, int y) {
        return y * width + x;
    }

    private Point idToCoords(int id) {
        return new Point(id % width, id / width);
    }

    private int getNeighbourCount(int id) {
        Point target = idToCoords(id);
        int neighbours = 0;
        for (int y = Math.max(0, target.y - 1); y <= target.y + 1 && y < height; ++y) {
            for (int x = Math.max(0, target.x - 1); x <= target.x + 1 && x < height; ++x) {
                if (!(x == target.x && y == target.y) && (boolean) (buttons[coordsToId(x, y)].getTag(R.string.ismine))) {
                    ++neighbours;
                }
            }
        }
        return neighbours;
    }

    private void propagateZeroes(int id) {
        Point target = idToCoords(id);
        for (int y = Math.max(0, target.y - 1); y <= target.y + 1 && y < height; ++y) {
            for (int x = Math.max(0, target.x - 1); x <= target.x + 1 && x < height; ++x) {
                int chkid = coordsToId(x, y);
                if ((x == target.x && y == target.y) || ((boolean) buttons[chkid].getTag(R.string.isseen))) {
                    continue;
                }
                Button button = buttons[chkid];
                button.setBackgroundColor(Color.LTGRAY);
                button.setTag(R.string.isseen, true);
                button.setOnClickListener(null);
                button.setOnLongClickListener(null);
                if (getNeighbourCount(chkid) == 0) {
                    propagateZeroes(chkid);
                } else {
                    button.setText(String.valueOf(getNeighbourCount(chkid)));
                }
            }
        }
    }

    public void onClick(View v) {
        if ((boolean) v.getTag(R.string.ismarked)) {
            return;
        }
        Button button = (Button) v;
        int id = (int) button.getTag(R.string.tileid);
        boolean ismine = (boolean) button.getTag(R.string.ismine);
        if (ismine) {
            TextView endtext = findViewById(R.id.end);
            endtext.setText(getResources().getString(R.string.lose));
            endtext.setVisibility(VISIBLE);
            for (Button b: buttons) {
                b.setOnClickListener(null);
                b.setOnLongClickListener(null);
                if ((boolean) b.getTag(R.string.ismine)) {
                    if ((boolean) b.getTag(R.string.ismarked)) {
                        b.setText(getString(R.string.foundmine));
                    } else {
                        b.setText(getString(R.string.mine));
                    }
                }
            }
        } else {
            button.setOnClickListener(null);
            button.setOnLongClickListener(null);
            button.setBackgroundColor(Color.LTGRAY);
            button.setTag(R.string.isseen, true);
            int neighbours = getNeighbourCount(id);
            if (neighbours == 0) {
                propagateZeroes(id);
            } else {
                button.setText(String.valueOf(neighbours));
            }
        }
    }

    public boolean onLongClick(View v) {
        Button button = (Button) v;
        if ((boolean) button.getTag(R.string.ismarked)) {
            button.setText("");
            button.setTag(R.string.ismarked, false);
            if ((boolean) button.getTag(R.string.ismine)) {
                --numCorrectFlags;
            } else {
                --numIncorrectFlags;
            }
        } else {
            button.setText(R.string.flag);
            button.setTag(R.string.ismarked, true);
            if ((boolean) button.getTag(R.string.ismine)) {
                ++numCorrectFlags;
                if (numCorrectFlags == numMines && numIncorrectFlags == 0) {
                    TextView endtext = findViewById(R.id.end);
                    endtext.setText(getResources().getString(R.string.win));
                    endtext.setVisibility(VISIBLE);
                    for (Button b : buttons) {
                        b.setOnClickListener(null);
                        b.setOnLongClickListener(null);
                        if ((boolean) b.getTag(R.string.ismine)) {
                            b.setText(R.string.foundmine);
                        }
                    }
                }
            } else {
                ++numIncorrectFlags;
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
