package hu.desnull.baltazar.minesweeper;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;

public class Tile extends androidx.appcompat.widget.AppCompatButton {
    public int id;
    public boolean mine;
    public boolean marked;
    public boolean revealed;
    private final ColorStateList defaultColors;

    public Tile(Context context, int id) {
        super(context);
        this.id = id;
        defaultColors = this.getTextColors();
        init();
    }

    public void init() {
        this.setHapticFeedbackEnabled(false);
        this.setBackgroundColor(Color.GRAY);
        this.setPadding(1, 1, 1, 1);
        this.setText("");
        this.setTextColor(defaultColors);
        mine = marked = revealed = false;
    }
}
