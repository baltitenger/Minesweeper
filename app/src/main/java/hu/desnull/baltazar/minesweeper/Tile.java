package hu.desnull.baltazar.minesweeper;

import android.content.Context;
import android.graphics.Color;

public class Tile extends android.support.v7.widget.AppCompatButton {
    public int id;
    public boolean mine;
    public boolean marked;
    public boolean revealed;

    public Tile(Context context, int id) {
        super(context);
        this.id = id;
        init();
    }

    public void init() {
        this.setHapticFeedbackEnabled(false);
        this.setBackgroundColor(Color.GRAY);
        this.setPadding(1, 1, 1, 1);
        this.setText("");
        mine = marked = revealed = false;
    }
}
