package hu.desnull.baltazar.minesweeper;

public class ButtonData {
    public int id;
    public boolean isMine;
    public boolean isMarked;
    public boolean isRevealed;

    public ButtonData(int id) {
        this.id = id;
    }
}
