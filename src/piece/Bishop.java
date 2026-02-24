package piece;

import main.GamePanel;

public class Bishop extends Piece {
    public Bishop(int color, int col, int row) {
        super(color, col, row);
        if (color == GamePanel.WHITE) {
            image = getImage("/piece/w-bishop");
        } else {
            image = getImage("/piece/b-bishop");
        }
    }

    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinTheBoard(targetCol, targetRow) && isSameSquare(targetCol, targetRow) == false) {
            //За да се движи по диагонал, разликата между сменената колона и ред винаги трябва да са еднакви
            if(Math.abs(targetCol - preCol) == Math.abs(targetRow - preRow)) {
                if(isValidSquare(targetCol, targetRow) && pieceOnTheDiagonal(targetCol, targetRow) == false) {
                    return true;
                }
            }
        }
        return false;
    }
}
