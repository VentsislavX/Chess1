package main;

import ai.ChessAI;
import logic.BoardState;
import logic.Move;
import piece.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing controller and view. Rendering and mouse handling live here, but all
 * chess rules and move legality are delegated to {@link logic.BoardState}, the
 * single source of truth shared with the AI. White is the human player; Black is
 * driven by {@link ai.ChessAI}.
 */
public class GamePanel extends JPanel implements Runnable {
    public static final int WIDTH = 1100;
    public static final int HEIGHT = 800;
    final int FPS = 60;
    Thread gameThread;
    Board board = new Board();
    Mouse mouse = new Mouse();

    //PIECES
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    Piece activeP;

    //COLOR (W/B)
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    int currentColor = WHITE;

    //RULES / STATE
    int epRow = -1, epCol = -1;           // en-passant target left by the last move
    boolean gameOver = false;
    String statusMessage = "White to move";
    List<Move> currentLegal = new ArrayList<>();   // legal moves for the piece being dragged

    //AI (plays Black)
    final ChessAI ai = new ChessAI(3);
    volatile Move aiMove = null;
    volatile boolean aiThinking = false;

    //BOOLEANS
    boolean canMove;
    boolean validSquare;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        setPieces();
        copyPieces(pieces, simPieces);
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setPieces() {
        //WHITE
        pieces.add(new Pawn(WHITE, 0, 6));
        pieces.add(new Pawn(WHITE, 1, 6));
        pieces.add(new Pawn(WHITE, 2, 6));
        pieces.add(new Pawn(WHITE, 3, 6));
        pieces.add(new Pawn(WHITE, 4, 6));
        pieces.add(new Pawn(WHITE, 5, 6));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 7, 6));

        pieces.add(new Rook(WHITE, 0, 7));
        pieces.add(new Knight(WHITE, 1, 7));
        pieces.add(new Bishop(WHITE, 2, 7));
        pieces.add(new Queen(WHITE, 3, 7));
        pieces.add(new King(WHITE, 4, 7));
        pieces.add(new Bishop(WHITE, 5, 7));
        pieces.add(new Knight(WHITE, 6, 7));
        pieces.add(new Rook(WHITE, 7, 7));

        //BLACK
        pieces.add(new Pawn(BLACK, 0, 1));
        pieces.add(new Pawn(BLACK, 1, 1));
        pieces.add(new Pawn(BLACK, 2, 1));
        pieces.add(new Pawn(BLACK, 3, 1));
        pieces.add(new Pawn(BLACK, 4, 1));
        pieces.add(new Pawn(BLACK, 5, 1));
        pieces.add(new Pawn(BLACK, 6, 1));
        pieces.add(new Pawn(BLACK, 7, 1));

        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Knight(BLACK, 1, 0));
        pieces.add(new Bishop(BLACK, 2, 0));
        pieces.add(new Queen(BLACK, 3, 0));
        pieces.add(new King(BLACK, 4, 0));
        pieces.add(new Bishop(BLACK, 5, 0));
        pieces.add(new Knight(BLACK, 6, 0));
        pieces.add(new Rook(BLACK, 7, 0));
    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        target.addAll(source);
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;
        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;
            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    // -----------------------------------------------------------------
    // Engine bridge
    // -----------------------------------------------------------------
    private int engineColor(int guiColor) {
        return guiColor == WHITE ? BoardState.WHITE : BoardState.BLACK;
    }

    private BoardState currentBoard() {
        return BoardState.fromPieces(pieces, engineColor(currentColor), epRow, epCol);
    }

    // -----------------------------------------------------------------
    // Game loop step
    // -----------------------------------------------------------------
    private void update() {
        if (gameOver) return;

        if (currentColor == BLACK) {
            handleAi();
            return;
        }
        handleHuman();
    }

    private void handleHuman() {
        if (mouse.pressed) {
            if (activeP == null) {
                for (Piece piece : pieces) {
                    if (piece.color == currentColor
                            && piece.col == mouse.x / Board.SQUARE_SIZE
                            && piece.row == mouse.y / Board.SQUARE_SIZE) {
                        activeP = piece;
                        // Compute legal destinations once, from the resting position.
                        currentLegal = currentBoard().generateLegal(engineColor(currentColor));
                        break;
                    }
                }
            } else {
                simulate();
            }
        }

        if (!mouse.pressed && activeP != null) {
            Move chosen = matchMove(activeP.preRow, activeP.preCol, activeP.row, activeP.col);
            if (chosen != null) {
                applyMove(chosen, activeP);
                endTurn();
            } else {
                activeP.resetPosition();
            }
            activeP = null;
            canMove = false;
        }
    }

    private void handleAi() {
        if (aiMove != null) {
            Piece moving = findPieceAt(aiMove.fromRow, aiMove.fromCol, null);
            applyMove(aiMove, moving);
            aiMove = null;
            endTurn();
        } else if (!aiThinking) {
            aiThinking = true;
            final BoardState snapshot = currentBoard();
            new Thread(() -> {
                aiMove = ai.findBestMove(snapshot);
                aiThinking = false;
            }).start();
        }
    }

    private void simulate() {
        canMove = false;
        validSquare = false;

        activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activeP.col = activeP.getCol(activeP.x);
        activeP.row = activeP.getRow(activeP.y);

        if (matchMove(activeP.preRow, activeP.preCol, activeP.row, activeP.col) != null) {
            canMove = true;
            validSquare = true;
        }
    }

    // -----------------------------------------------------------------
    // Applying a legal move to the visual piece list
    // -----------------------------------------------------------------
    private Move matchMove(int fromRow, int fromCol, int toRow, int toCol) {
        Move fallback = null;
        for (Move m : currentLegal) {
            if (m.sameSquares(fromRow, fromCol, toRow, toCol)) {
                if (m.promotion == 0 || m.promotion == BoardState.QUEEN) return m;
                fallback = m; // a promotion; keep looking for the queen default
            }
        }
        return fallback;
    }

    private Piece findPieceAt(int row, int col, Piece exclude) {
        for (Piece p : pieces) {
            if (p != exclude && p.row == row && p.col == col) return p;
        }
        return null;
    }

    private void applyMove(Move m, Piece moving) {
        // Remove any captured piece (regular capture or en passant).
        int capRow = m.isEnPassant ? m.fromRow : m.toRow;
        int capCol = m.toCol;
        Piece captured = findPieceAt(capRow, capCol, moving);
        if (captured != null) pieces.remove(captured);

        // Relocate the moving piece.
        moving.col = m.toCol;
        moving.row = m.toRow;
        moving.updatePositon();

        // Move the rook when castling.
        if (m.isCastle) {
            int rookFromCol = (m.toCol == 6) ? 7 : 0;
            int rookToCol = (m.toCol == 6) ? 5 : 3;
            Piece rook = findPieceAt(m.toRow, rookFromCol, null);
            if (rook != null) {
                rook.col = rookToCol;
                rook.updatePositon();
            }
        }

        // Promote by swapping the pawn for the chosen piece.
        int promo = m.promotion;
        if (moving instanceof Pawn && (m.toRow == 0 || m.toRow == 7)) {
            if (promo == 0) promo = askPromotion(moving.color);
            pieces.remove(moving);
            pieces.add(createPromoted(promo, moving.color, m.toCol, m.toRow));
        }

        // Record a new en-passant target only for a double pawn push.
        if (moving instanceof Pawn && Math.abs(m.toRow - m.fromRow) == 2) {
            epRow = (m.fromRow + m.toRow) / 2;
            epCol = m.fromCol;
        } else {
            epRow = epCol = -1;
        }

        copyPieces(pieces, simPieces);
    }

    private Piece createPromoted(int type, int color, int col, int row) {
        Piece p = switch (type) {
            case BoardState.ROOK -> new Rook(color, col, row);
            case BoardState.BISHOP -> new Bishop(color, col, row);
            case BoardState.KNIGHT -> new Knight(color, col, row);
            default -> new Queen(color, col, row);
        };
        p.moved = true;
        return p;
    }

    private int askPromotion(int color) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(this, "Promote pawn to:", "Promotion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        return switch (choice) {
            case 1 -> BoardState.ROOK;
            case 2 -> BoardState.BISHOP;
            case 3 -> BoardState.KNIGHT;
            default -> BoardState.QUEEN;
        };
    }

    // -----------------------------------------------------------------
    // Turn / status bookkeeping
    // -----------------------------------------------------------------
    private void endTurn() {
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;

        BoardState.Status status = currentBoard().status(engineColor(currentColor));
        String side = (currentColor == WHITE) ? "White" : "Black";
        switch (status) {
            case CHECKMATE -> {
                String winner = (currentColor == WHITE) ? "Black" : "White";
                statusMessage = "Checkmate - " + winner + " wins";
                gameOver = true;
            }
            case STALEMATE -> {
                statusMessage = "Stalemate - draw";
                gameOver = true;
            }
            case ONGOING -> {
                boolean check = currentBoard().isKingInCheck(engineColor(currentColor));
                statusMessage = side + " to move" + (check ? " (check)" : "");
            }
        }
    }

    // -----------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        board.draw(g2);
        for (Piece piece : pieces) {
            piece.draw(g2);
        }
        if (activeP != null) {
            if (canMove) {
                g2.setColor(Color.red);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE,
                        Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }
            activeP.draw(g2);
        }

        g2.setColor(Color.white);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 28));
        g2.drawString(statusMessage, 820, 60);
        if (aiThinking) {
            g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g2.drawString("Black is thinking...", 820, 100);
        }
    }
}
