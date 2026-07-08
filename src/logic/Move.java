package logic;

/**
 * Immutable-by-convention description of a single move on the board, plus the
 * information required to undo it. Coordinates use row 0 at the top (Black home
 * rank) and row 7 at the bottom (White home rank), matching the GUI.
 */
public class Move {
    public final int fromRow, fromCol, toRow, toCol;

    /** Signed code of the moving piece (see {@link BoardState}). */
    public final int piece;

    /** Promotion target type (unsigned piece type) or 0 when not a promotion. */
    public final int promotion;

    public final boolean isEnPassant;
    public final boolean isCastle;

    // --- State captured for undo ---
    int captured;          // signed code of the captured piece, 0 if none
    int capturedRow, capturedCol;
    boolean prevWK, prevWQ, prevBK, prevBQ;
    int prevEpRow, prevEpCol;

    public Move(int fromRow, int fromCol, int toRow, int toCol,
                int piece, int promotion, boolean isEnPassant, boolean isCastle) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.piece = piece;
        this.promotion = promotion;
        this.isEnPassant = isEnPassant;
        this.isCastle = isCastle;
    }

    public boolean sameSquares(int fr, int fc, int tr, int tc) {
        return fromRow == fr && fromCol == fc && toRow == tr && toCol == tc;
    }

    @Override
    public String toString() {
        String s = "" + (char) ('a' + fromCol) + (8 - fromRow)
                + (char) ('a' + toCol) + (8 - toRow);
        if (promotion != 0) s += "=" + "?PNBRQK".charAt(promotion);
        return s;
    }
}
