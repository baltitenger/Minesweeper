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
    private double mineprob = 0.2;
    private int foundmines, foundnotmines, minecount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        init();
    }

    public void init() {
        ((FrameLayout) findViewById(R.id.board_container)).removeAllViews();
        findViewById(R.id.end).setVisibility(INVISIBLE);
        foundmines = foundnotmines = minecount = 0;
        buttons = new Button[width * height];
        board = new LinearLayout(context);
        board.setOrientation(LinearLayout.VERTICAL);
        ((FrameLayout) findViewById(R.id.board_container)).addView(board,
                MATCH_PARENT, MATCH_PARENT);
        for (int y = 0; y < height; y++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int x = 0; x < width; x++) {
                Button button = new Button(context);
                button.setTag(R.string.tileid, coordsToId(x, y));
                button.setTag(R.string.ismarked, false);
                button.setTag(R.string.isseen, false);
                if (new Random().nextDouble() < mineprob) {
                    minecount++;
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
        ((TextView) findViewById(R.id.minecount)).setText(getResources().getString(
                R.string.minecount, minecount));
    }

    public int coordsToId(int x, int y) {
        return y * width + x;
    }

    public Point idToCoords(int id) {
        return new Point(id % width, id / width);
    }

    public int getNeighbourCount(int id) {
        int targetx = idToCoords(id).x;
        int targety = idToCoords(id).y;
        int neighbours = 0;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                if (x == 0 && y == 0) { continue; }
                int chkx = targetx + x;
                int chky = targety + y;
                if (chkx >= 0 && chky >= 0 && chkx < width && chky < height
                        && (boolean) (buttons[coordsToId(chkx, chky)].getTag(R.string.ismine))) {
                    neighbours++;
                }
            }
        }
        return neighbours;
    }

    public void propagatezeroes(int id) {
        int targetx = idToCoords(id).x;
        int targety = idToCoords(id).y;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                int chkx = targetx + x;
                int chky = targety + y;
                int chkid = coordsToId(chkx, chky);
                if ((x == 0 && y == 0) || !(chkx >= 0 && chky >= 0 && chkx < width && chky < height)
                        || ((Boolean) buttons[chkid].getTag(R.string.isseen))) { continue; }
                Button button = buttons[chkid];
                button.setBackgroundColor(Color.LTGRAY);
                button.setTag(R.string.isseen, true);
                button.setOnClickListener(null);
                button.setOnLongClickListener(null);
                if (getNeighbourCount(chkid) == 0) {
                    propagatezeroes(chkid);
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
                propagatezeroes(id);
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
                foundmines--;
            } else {
                foundnotmines--;
            }
        } else {
            button.setText(R.string.flag);
            button.setTag(R.string.ismarked, true);
            if ((boolean) button.getTag(R.string.ismine)) {
                foundmines++;
                if (foundmines == minecount && foundnotmines == 0) {
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
                foundnotmines++;
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
