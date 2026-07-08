package ai;

import logic.BoardState;
import logic.Evaluator;
import logic.Move;

import java.util.List;

/**
 * Minimax search with alpha-beta pruning. The search explores the game tree to a
 * fixed depth, scoring leaves with {@link Evaluator}, and returns the move that is
 * best for the side to move. Alpha-beta pruning discards branches that cannot
 * influence the result, letting the same depth be reached far more cheaply than a
 * plain minimax.
 */
public class ChessAI {
    private static final int INF = 1_000_000;
    private static final int MATE = 100_000;

    private final int maxDepth;

    public ChessAI(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    /** Returns the best move for {@code state.sideToMove}, or null if none exist. */
    public Move findBestMove(BoardState state) {
        int color = state.sideToMove;
        List<Move> moves = state.generateLegal(color);
        if (moves.isEmpty()) return null;

        Move best = null;
        int alpha = -INF, beta = INF;
        // White maximizes the evaluation; Black minimizes it.
        int bestScore = (color == BoardState.WHITE) ? -INF : INF;

        for (Move m : moves) {
            state.makeMove(m);
            int score = search(state, maxDepth - 1, alpha, beta, -color);
            state.undoMove(m);

            if (color == BoardState.WHITE) {
                if (score > bestScore) {
                    bestScore = score;
                    best = m;
                }
                alpha = Math.max(alpha, bestScore);
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    best = m;
                }
                beta = Math.min(beta, bestScore);
            }
        }
        return best;
    }

    private int search(BoardState state, int depth, int alpha, int beta, int color) {
        List<Move> moves = state.generateLegal(color);

        if (moves.isEmpty()) {
            // Checkmate is scored relative to depth so shorter mates are preferred.
            if (state.isKingInCheck(color)) {
                int mate = MATE + depth;
                return (color == BoardState.WHITE) ? -mate : mate;
            }
            return 0; // stalemate
        }
        if (depth == 0) {
            return Evaluator.evaluate(state);
        }

        if (color == BoardState.WHITE) {
            int value = -INF;
            for (Move m : moves) {
                state.makeMove(m);
                value = Math.max(value, search(state, depth - 1, alpha, beta, -color));
                state.undoMove(m);
                alpha = Math.max(alpha, value);
                if (alpha >= beta) break; // beta cut-off
            }
            return value;
        } else {
            int value = INF;
            for (Move m : moves) {
                state.makeMove(m);
                value = Math.min(value, search(state, depth - 1, alpha, beta, -color));
                state.undoMove(m);
                beta = Math.min(beta, value);
                if (beta <= alpha) break; // alpha cut-off
            }
            return value;
        }
    }
}
