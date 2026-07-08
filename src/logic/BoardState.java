package logic;

import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Pawn;
import piece.Piece;
import piece.Queen;
import piece.Rook;

import java.util.ArrayList;
import java.util.List;

/**
 * The single source of truth for chess rules. Holds the board occupancy plus the
 * auxiliary state (side to move, castling rights, en-passant target) needed to
 * generate fully legal moves, apply and undo them, and report game status.
 *
 * This class is deliberately independent of Swing so it can be reused unchanged
 * by both the GUI ({@code GamePanel}) and the AI ({@code ai.ChessAI}).
 *
 * Encoding: squares[row][col] holds a signed piece code. 0 is empty, positive is
 * White, negative is Black. Types: PAWN=1, KNIGHT=2, BISHOP=3, ROOK=4, QUEEN=5,
 * KING=6. Row 0 is the top (Black's back rank); row 7 is the bottom (White's).
 */
public class BoardState {
    public static final int WHITE = 1;
    public static final int BLACK = -1;

    public static final int PAWN = 1, KNIGHT = 2, BISHOP = 3, ROOK = 4, QUEEN = 5, KING = 6;

    public final int[][] squares = new int[8][8];
    public int sideToMove;

    public boolean whiteKingSide, whiteQueenSide, blackKingSide, blackQueenSide;
    public int epRow = -1, epCol = -1; // square a pawn may move onto to capture en passant

    private static final int[][] KNIGHT_OFFSETS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };
    private static final int[][] KING_OFFSETS = {
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}
    };
    private static final int[][] ROOK_DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final int[][] BISHOP_DIRS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    public BoardState(int sideToMove) {
        this.sideToMove = sideToMove;
    }

    /** Builds a board matching the GUI's piece list. */
    public static BoardState fromPieces(List<Piece> pieces, int sideToMove, int epRow, int epCol) {
        BoardState b = new BoardState(sideToMove);
        for (Piece p : pieces) {
            int type = typeOf(p);
            int sign = (p.color == main.GamePanel.WHITE) ? WHITE : BLACK;
            b.squares[p.row][p.col] = type * sign;
        }
        b.epRow = epRow;
        b.epCol = epCol;
        b.deriveCastlingRights(pieces);
        return b;
    }

    /**
     * Builds a board from a FEN string (placement, side, castling and en-passant
     * fields are honoured; the clock fields are ignored). Used mainly by the
     * correctness tests to set up well-known reference positions.
     */
    public static BoardState fromFEN(String fen) {
        String[] parts = fen.trim().split("\\s+");
        BoardState b = new BoardState(WHITE);
        String[] ranks = parts[0].split("/");
        for (int row = 0; row < 8; row++) {
            int col = 0;
            for (char ch : ranks[row].toCharArray()) {
                if (Character.isDigit(ch)) {
                    col += ch - '0';
                } else {
                    b.squares[row][col++] = charToCode(ch);
                }
            }
        }
        b.sideToMove = parts.length > 1 && parts[1].equals("b") ? BLACK : WHITE;
        String castling = parts.length > 2 ? parts[2] : "-";
        b.whiteKingSide = castling.contains("K");
        b.whiteQueenSide = castling.contains("Q");
        b.blackKingSide = castling.contains("k");
        b.blackQueenSide = castling.contains("q");
        if (parts.length > 3 && !parts[3].equals("-")) {
            b.epCol = parts[3].charAt(0) - 'a';
            b.epRow = 8 - (parts[3].charAt(1) - '0');
        }
        return b;
    }

    private static int charToCode(char ch) {
        int sign = Character.isUpperCase(ch) ? WHITE : BLACK;
        int type = switch (Character.toLowerCase(ch)) {
            case 'p' -> PAWN;
            case 'n' -> KNIGHT;
            case 'b' -> BISHOP;
            case 'r' -> ROOK;
            case 'q' -> QUEEN;
            case 'k' -> KING;
            default -> 0;
        };
        return type * sign;
    }

    private static int typeOf(Piece p) {
        if (p instanceof Pawn) return PAWN;
        if (p instanceof Knight) return KNIGHT;
        if (p instanceof Bishop) return BISHOP;
        if (p instanceof Rook) return ROOK;
        if (p instanceof Queen) return QUEEN;
        return KING;
    }

    private void deriveCastlingRights(List<Piece> pieces) {
        // A castling right survives only while the king and the relevant rook are
        // still on their home squares and have never moved.
        for (Piece p : pieces) {
            if (p.moved) continue;
            if (p instanceof King) {
                if (p.color == main.GamePanel.WHITE && p.col == 4 && p.row == 7) {
                    whiteKingSide = whiteQueenSide = true;
                } else if (p.color == main.GamePanel.BLACK && p.col == 4 && p.row == 0) {
                    blackKingSide = blackQueenSide = true;
                }
            }
        }
        // Rooks gate the individual sides; clear a side if its rook is not home/unmoved.
        whiteKingSide &= rookHome(pieces, main.GamePanel.WHITE, 7, 7);
        whiteQueenSide &= rookHome(pieces, main.GamePanel.WHITE, 0, 7);
        blackKingSide &= rookHome(pieces, main.GamePanel.BLACK, 7, 0);
        blackQueenSide &= rookHome(pieces, main.GamePanel.BLACK, 0, 0);
    }

    private boolean rookHome(List<Piece> pieces, int color, int col, int row) {
        for (Piece p : pieces) {
            if (p instanceof Rook && p.color == color && p.col == col && p.row == row && !p.moved) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    public static int type(int code) {
        return Math.abs(code);
    }

    public static int colorOf(int code) {
        return Integer.signum(code);
    }

    private static boolean inBoard(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    // ---------------------------------------------------------------------
    // Move generation
    // ---------------------------------------------------------------------

    /** Pseudo-legal moves: correct piece geometry but may leave the king in check. */
    public List<Move> generatePseudoLegal(int color) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int code = squares[r][c];
                if (code == 0 || colorOf(code) != color) continue;
                switch (type(code)) {
                    case PAWN -> genPawn(r, c, color, moves);
                    case KNIGHT -> genStep(r, c, color, KNIGHT_OFFSETS, code, moves);
                    case BISHOP -> genSlide(r, c, color, BISHOP_DIRS, code, moves);
                    case ROOK -> genSlide(r, c, color, ROOK_DIRS, code, moves);
                    case QUEEN -> {
                        genSlide(r, c, color, ROOK_DIRS, code, moves);
                        genSlide(r, c, color, BISHOP_DIRS, code, moves);
                    }
                    case KING -> {
                        genStep(r, c, color, KING_OFFSETS, code, moves);
                        genCastles(r, c, color, moves);
                    }
                }
            }
        }
        return moves;
    }

    /** Fully legal moves for {@code color}: pseudo-legal moves that don't expose the king. */
    public List<Move> generateLegal(int color) {
        List<Move> legal = new ArrayList<>();
        for (Move m : generatePseudoLegal(color)) {
            makeMove(m);
            if (!isKingInCheck(color)) legal.add(m);
            undoMove(m);
        }
        return legal;
    }

    private void genPawn(int r, int c, int color, List<Move> moves) {
        int dir = -color;                       // White (=+1) moves toward row 0
        int startRow = (color == WHITE) ? 6 : 1;
        int promoRow = (color == WHITE) ? 0 : 7;
        int code = PAWN * color;

        int nr = r + dir;
        if (inBoard(nr, c) && squares[nr][c] == 0) {
            addPawnMove(r, c, nr, c, color, promoRow, false, moves);
            int nr2 = r + 2 * dir;
            if (r == startRow && squares[nr2][c] == 0) {
                moves.add(new Move(r, c, nr2, c, code, 0, false, false));
            }
        }
        for (int dc = -1; dc <= 1; dc += 2) {
            int nc = c + dc;
            if (!inBoard(nr, nc)) continue;
            int target = squares[nr][nc];
            if (target != 0 && colorOf(target) == -color) {
                addPawnMove(r, c, nr, nc, color, promoRow, false, moves);
            } else if (nr == epRow && nc == epCol) {
                moves.add(new Move(r, c, nr, nc, code, 0, true, false));
            }
        }
    }

    private void addPawnMove(int r, int c, int nr, int nc, int color, int promoRow,
                             boolean ep, List<Move> moves) {
        int code = PAWN * color;
        if (nr == promoRow) {
            for (int promo : new int[]{QUEEN, ROOK, BISHOP, KNIGHT}) {
                moves.add(new Move(r, c, nr, nc, code, promo, ep, false));
            }
        } else {
            moves.add(new Move(r, c, nr, nc, code, 0, ep, false));
        }
    }

    private void genStep(int r, int c, int color, int[][] offsets, int code, List<Move> moves) {
        for (int[] o : offsets) {
            int nr = r + o[0], nc = c + o[1];
            if (!inBoard(nr, nc)) continue;
            int target = squares[nr][nc];
            if (target == 0 || colorOf(target) == -color) {
                moves.add(new Move(r, c, nr, nc, code, 0, false, false));
            }
        }
    }

    private void genSlide(int r, int c, int color, int[][] dirs, int code, List<Move> moves) {
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (inBoard(nr, nc)) {
                int target = squares[nr][nc];
                if (target == 0) {
                    moves.add(new Move(r, c, nr, nc, code, 0, false, false));
                } else {
                    if (colorOf(target) == -color) {
                        moves.add(new Move(r, c, nr, nc, code, 0, false, false));
                    }
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }
    }

    private void genCastles(int r, int c, int color, List<Move> moves) {
        int homeRow = (color == WHITE) ? 7 : 0;
        if (r != homeRow || c != 4) return;
        if (isSquareAttacked(homeRow, 4, -color)) return; // cannot castle out of check

        boolean kingSide = (color == WHITE) ? whiteKingSide : blackKingSide;
        boolean queenSide = (color == WHITE) ? whiteQueenSide : blackQueenSide;
        int code = KING * color;

        if (kingSide
                && squares[homeRow][5] == 0 && squares[homeRow][6] == 0
                && !isSquareAttacked(homeRow, 5, -color)
                && !isSquareAttacked(homeRow, 6, -color)) {
            moves.add(new Move(homeRow, 4, homeRow, 6, code, 0, false, true));
        }
        if (queenSide
                && squares[homeRow][1] == 0 && squares[homeRow][2] == 0 && squares[homeRow][3] == 0
                && !isSquareAttacked(homeRow, 3, -color)
                && !isSquareAttacked(homeRow, 2, -color)) {
            moves.add(new Move(homeRow, 4, homeRow, 2, code, 0, false, true));
        }
    }

    // ---------------------------------------------------------------------
    // Attack / check detection
    // ---------------------------------------------------------------------

    /** True if {@code (row,col)} is attacked by any piece of {@code byColor}. */
    public boolean isSquareAttacked(int row, int col, int byColor) {
        // Pawns
        int pawnDir = -byColor; // direction that byColor's pawns move
        int pr = row - pawnDir; // square a byColor pawn would sit on to attack (row,col)
        for (int dc = -1; dc <= 1; dc += 2) {
            int pc = col + dc;
            if (inBoard(pr, pc) && squares[pr][pc] == PAWN * byColor) return true;
        }
        // Knights
        for (int[] o : KNIGHT_OFFSETS) {
            int nr = row + o[0], nc = col + o[1];
            if (inBoard(nr, nc) && squares[nr][nc] == KNIGHT * byColor) return true;
        }
        // King
        for (int[] o : KING_OFFSETS) {
            int nr = row + o[0], nc = col + o[1];
            if (inBoard(nr, nc) && squares[nr][nc] == KING * byColor) return true;
        }
        // Sliding: rook/queen
        if (slideHits(row, col, byColor, ROOK_DIRS, ROOK)) return true;
        // Sliding: bishop/queen
        return slideHits(row, col, byColor, BISHOP_DIRS, BISHOP);
    }

    private boolean slideHits(int row, int col, int byColor, int[][] dirs, int straightType) {
        for (int[] d : dirs) {
            int nr = row + d[0], nc = col + d[1];
            while (inBoard(nr, nc)) {
                int code = squares[nr][nc];
                if (code != 0) {
                    if (colorOf(code) == byColor) {
                        int t = type(code);
                        if (t == QUEEN || t == straightType) return true;
                    }
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }
        return false;
    }

    public int[] findKing(int color) {
        int king = KING * color;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (squares[r][c] == king) return new int[]{r, c};
            }
        }
        return null;
    }

    public boolean isKingInCheck(int color) {
        int[] k = findKing(color);
        if (k == null) return false;
        return isSquareAttacked(k[0], k[1], -color);
    }

    // ---------------------------------------------------------------------
    // Make / undo
    // ---------------------------------------------------------------------

    public void makeMove(Move m) {
        m.prevWK = whiteKingSide;
        m.prevWQ = whiteQueenSide;
        m.prevBK = blackKingSide;
        m.prevBQ = blackQueenSide;
        m.prevEpRow = epRow;
        m.prevEpCol = epCol;

        int color = colorOf(m.piece);

        // Record and remove the captured piece.
        if (m.isEnPassant) {
            m.capturedRow = m.fromRow;
            m.capturedCol = m.toCol;
            m.captured = squares[m.capturedRow][m.capturedCol];
            squares[m.capturedRow][m.capturedCol] = 0;
        } else {
            m.capturedRow = m.toRow;
            m.capturedCol = m.toCol;
            m.captured = squares[m.toRow][m.toCol];
        }

        // Move the piece (handling promotion).
        squares[m.fromRow][m.fromCol] = 0;
        squares[m.toRow][m.toCol] = (m.promotion != 0) ? m.promotion * color : m.piece;

        // Move the rook when castling.
        if (m.isCastle) {
            int row = m.fromRow;
            if (m.toCol == 6) {           // king side
                squares[row][5] = squares[row][7];
                squares[row][7] = 0;
            } else {                      // queen side
                squares[row][3] = squares[row][0];
                squares[row][0] = 0;
            }
        }

        updateCastlingRights(m);

        // En-passant target: only a double pawn push creates one.
        if (type(m.piece) == PAWN && Math.abs(m.toRow - m.fromRow) == 2) {
            epRow = (m.fromRow + m.toRow) / 2;
            epCol = m.fromCol;
        } else {
            epRow = epCol = -1;
        }

        sideToMove = -color;
    }

    private void updateCastlingRights(Move m) {
        // King move forfeits both of its rights.
        if (type(m.piece) == KING) {
            if (colorOf(m.piece) == WHITE) whiteKingSide = whiteQueenSide = false;
            else blackKingSide = blackQueenSide = false;
        }
        // A rook leaving a corner (or being captured on it) forfeits that side.
        clearRightForSquare(m.fromRow, m.fromCol);
        clearRightForSquare(m.capturedRow, m.capturedCol);
    }

    private void clearRightForSquare(int r, int c) {
        if (r == 7 && c == 7) whiteKingSide = false;
        else if (r == 7 && c == 0) whiteQueenSide = false;
        else if (r == 0 && c == 7) blackKingSide = false;
        else if (r == 0 && c == 0) blackQueenSide = false;
    }

    public void undoMove(Move m) {
        int color = colorOf(m.piece);

        squares[m.fromRow][m.fromCol] = m.piece;
        squares[m.toRow][m.toCol] = m.isEnPassant ? 0 : m.captured;

        if (m.isEnPassant) {
            squares[m.capturedRow][m.capturedCol] = m.captured;
        }

        if (m.isCastle) {
            int row = m.fromRow;
            if (m.toCol == 6) {
                squares[row][7] = squares[row][5];
                squares[row][5] = 0;
            } else {
                squares[row][0] = squares[row][3];
                squares[row][3] = 0;
            }
        }

        whiteKingSide = m.prevWK;
        whiteQueenSide = m.prevWQ;
        blackKingSide = m.prevBK;
        blackQueenSide = m.prevBQ;
        epRow = m.prevEpRow;
        epCol = m.prevEpCol;
        sideToMove = color;
    }

    // ---------------------------------------------------------------------
    // Status
    // ---------------------------------------------------------------------
    public enum Status {ONGOING, CHECKMATE, STALEMATE}

    public Status status(int color) {
        if (!generateLegal(color).isEmpty()) return Status.ONGOING;
        return isKingInCheck(color) ? Status.CHECKMATE : Status.STALEMATE;
    }
}
