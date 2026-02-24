package piece;

import main.GamePanel;

public class Rook extends Piece{
    public Rook(int color, int col, int row) {
        super(color, col, row);
        if (color == GamePanel.WHITE) {
            image = getImage("/piece/w-rook");
        }
        else {
            image = getImage("/piece/b-rook");
        }
    }
    public boolean canMove(int targetCol, int targetRow) {
        if(isWithinTheBoard(targetCol, targetRow) && isSameSquare(targetCol, targetRow) == false) {
            //Rook се движи ако се намира в същия ред или в същата колона
            if(targetCol == preCol || targetRow == preRow) {
                if(isValidSquare(targetCol, targetRow)
                        && pieceOnTheStraightLine(targetCol, targetRow) == false) {
                    return true;
                }
            }
        }
        return false;
    }
}
