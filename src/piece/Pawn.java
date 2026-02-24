package piece;

import main.GamePanel;

public class Pawn extends Piece {
    public Pawn(int color, int col, int row) {
        super(color, col, row);
        if (color == GamePanel.WHITE) {
            image = getImage("/piece/w-pawn");
        }
        else {
            image = getImage("/piece/b-pawn");
        }
    }
    public boolean canMove(int targetCol, int targetRow) {
        if (isWithinTheBoard(targetCol, targetRow) && isSameSquare(targetCol, targetRow) == false) {
            //Определяме ходовете спрямо цвета на фигурата
            int moveValue;
            if(color == GamePanel.WHITE) {
                moveValue = -1;
            }
            else {
                moveValue = 1;
            }
            //Проверяваме дали има фигура пред пешката
            hittingPiece = getHittingPiece(targetCol, targetRow);

            //Движение само с 1 поле
            if(targetCol == preCol && targetRow == preRow + moveValue && hittingPiece == null) {
                return true;
            }

            //Движение с 2 полета
            if(targetCol == preCol && targetRow == preRow + moveValue * 2 && hittingPiece == null
                    && moved == false && pieceOnTheStraightLine(targetCol, targetRow) == false) {
                return true;
            }

            //Движение по диагонал когато може да вземе фигура
            if(Math.abs(targetCol - preCol) == 1 && targetRow == preRow + moveValue
                    && hittingPiece != null && hittingPiece.color != color) {
                return true;
            }
        }
        return false;
    }
}
