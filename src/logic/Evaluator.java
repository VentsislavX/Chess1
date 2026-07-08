package logic;

/**
 * Static evaluation of a position from White's perspective (positive favours
 * White). Combines material value with piece-square tables so the search prefers
 * active development, central control and king safety over pure material.
 */
public final class Evaluator {

    private static final int[] VALUE = {0, 100, 320, 330, 500, 900, 20000};

    // Piece-square tables are written from White's point of view with row 0 at the
    // top. Black's contribution mirrors the table vertically.
    private static final int[][] PAWN_PST = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {50, 50, 50, 50, 50, 50, 50, 50},
            {10, 10, 20, 30, 30, 20, 10, 10},
            {5, 5, 10, 25, 25, 10, 5, 5},
            {0, 0, 0, 20, 20, 0, 0, 0},
            {5, -5, -10, 0, 0, -10, -5, 5},
            {5, 10, 10, -20, -20, 10, 10, 5},
            {0, 0, 0, 0, 0, 0, 0, 0}
    };
    private static final int[][] KNIGHT_PST = {
            {-50, -40, -30, -30, -30, -30, -40, -50},
            {-40, -20, 0, 0, 0, 0, -20, -40},
            {-30, 0, 10, 15, 15, 10, 0, -30},
            {-30, 5, 15, 20, 20, 15, 5, -30},
            {-30, 0, 15, 20, 20, 15, 0, -30},
            {-30, 5, 10, 15, 15, 10, 5, -30},
            {-40, -20, 0, 5, 5, 0, -20, -40},
            {-50, -40, -30, -30, -30, -30, -40, -50}
    };
    private static final int[][] BISHOP_PST = {
            {-20, -10, -10, -10, -10, -10, -10, -20},
            {-10, 0, 0, 0, 0, 0, 0, -10},
            {-10, 0, 5, 10, 10, 5, 0, -10},
            {-10, 5, 5, 10, 10, 5, 5, -10},
            {-10, 0, 10, 10, 10, 10, 0, -10},
            {-10, 10, 10, 10, 10, 10, 10, -10},
            {-10, 5, 0, 0, 0, 0, 5, -10},
            {-20, -10, -10, -10, -10, -10, -10, -20}
    };
    private static final int[][] ROOK_PST = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {5, 10, 10, 10, 10, 10, 10, 5},
            {-5, 0, 0, 0, 0, 0, 0, -5},
            {-5, 0, 0, 0, 0, 0, 0, -5},
            {-5, 0, 0, 0, 0, 0, 0, -5},
            {-5, 0, 0, 0, 0, 0, 0, -5},
            {-5, 0, 0, 0, 0, 0, 0, -5},
            {0, 0, 0, 5, 5, 0, 0, 0}
    };
    private static final int[][] QUEEN_PST = {
            {-20, -10, -10, -5, -5, -10, -10, -20},
            {-10, 0, 0, 0, 0, 0, 0, -10},
            {-10, 0, 5, 5, 5, 5, 0, -10},
            {-5, 0, 5, 5, 5, 5, 0, -5},
            {0, 0, 5, 5, 5, 5, 0, -5},
            {-10, 5, 5, 5, 5, 5, 0, -10},
            {-10, 0, 5, 0, 0, 0, 0, -10},
            {-20, -10, -10, -5, -5, -10, -10, -20}
    };
    private static final int[][] KING_PST = {
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-30, -40, -40, -50, -50, -40, -40, -30},
            {-20, -30, -30, -40, -40, -30, -30, -20},
            {-10, -20, -20, -20, -20, -20, -20, -10},
            {20, 20, 0, 0, 0, 0, 20, 20},
            {20, 30, 10, 0, 0, 10, 30, 20}
    };

    private Evaluator() {
    }

    /** Score in centipawns from White's perspective. */
    public static int evaluate(BoardState b) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int code = b.squares[r][c];
                if (code == 0) continue;
                int type = BoardState.type(code);
                int color = BoardState.colorOf(code);
                int material = VALUE[type];
                int positional = pst(type, r, c, color);
                score += color * (material + positional);
            }
        }
        return score;
    }

    private static int pst(int type, int row, int col, int color) {
        int r = (color == BoardState.WHITE) ? row : 7 - row;
        return switch (type) {
            case BoardState.PAWN -> PAWN_PST[r][col];
            case BoardState.KNIGHT -> KNIGHT_PST[r][col];
            case BoardState.BISHOP -> BISHOP_PST[r][col];
            case BoardState.ROOK -> ROOK_PST[r][col];
            case BoardState.QUEEN -> QUEEN_PST[r][col];
            case BoardState.KING -> KING_PST[r][col];
            default -> 0;
        };
    }
}
