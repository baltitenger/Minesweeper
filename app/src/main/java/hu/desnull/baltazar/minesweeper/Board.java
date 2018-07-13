package hu.desnull.baltazar.minesweeper;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

public class Board extends LinearLayout implements View.OnClickListener, View.OnLongClickListener{
    private int width, height;
    private Button[] buttons;
    public int minecount;

    public Board(Context context) {
        super(context);
        init(context);
    }

    public Board(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Board(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        this.width = 7;
        this.height = 7;
        buttons = new Button[width * height];
        double mineprob = 0.2;
        int minecount = 0;
        setOrientation(LinearLayout.VERTICAL);
        for (int y = 0; y < height; y++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int x = 0; x < width; x++) {
                Button button = new Button(context);
                button.setTag(R.string.tileid, coordsToId(x, y));
                if (new Random().nextDouble() < mineprob) {
                    minecount++;
                    button.setTag(R.string.ismine, true);
                } else {
                    button.setTag(R.string.ismine, false);
                }
                button.setOnClickListener(this);
                button.setOnLongClickListener(this);
                buttons[coordsToId(x, y)] = button;
                row.addView(button, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f / width));
            }
            addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f / height));
        }
        this.minecount = minecount;
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

    public void onClick(View v) {
        Button button = (Button) v;
        int id = (int) button.getTag(R.string.tileid);
        boolean isbomb = (boolean) button.getTag(R.string.ismine);
        if (isbomb) {
            ((TextView) findViewById(R.id.end)).setText(getResources().getString(R.string.lose));
        } else {
            button.setText(String.valueOf(getNeighbourCount(id)));
        }
    }

    public boolean onLongClick(View v) {

        return true;
    }
}