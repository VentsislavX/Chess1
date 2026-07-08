package test;

import logic.BoardState;
import logic.Move;

import java.util.List;

/**
 * Perft (performance test) move-path enumeration used to validate the move
 * generator. It walks the full legal game tree to a given depth and counts the
 * leaf nodes; the totals for well-known reference positions are published and
 * exact, so any bug in castling, en passant, promotion or check handling shows up
 * as a mismatched count. This is how correctness was validated across edge cases.
 *
 * Run: java test.Perft
 */
public class Perft {

    private record Case(String name, String fen, long[] expected) {
    }

    private static final Case[] CASES = {
            new Case("Startpos",
                    "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                    new long[]{20, 400, 8902, 197281}),
            new Case("Kiwipete",
                    "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
                    new long[]{48, 2039, 97862}),
            new Case("Position 3 (en passant)",
                    "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
                    new long[]{14, 191, 2812, 43238}),
            new Case("Position 4 (promotion)",
                    "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
                    new long[]{6, 264, 9467}),
            new Case("Position 5",
                    "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
                    new long[]{44, 1486, 62379}),
    };

    static long perft(BoardState b, int depth) {
        List<Move> moves = b.generateLegal(b.sideToMove);
        if (depth == 1) return moves.size();
        long nodes = 0;
        for (Move m : moves) {
            b.makeMove(m);
            nodes += perft(b, depth - 1);
            b.undoMove(m);
        }
        return nodes;
    }

    public static void main(String[] args) {
        boolean allPass = true;
        for (Case c : CASES) {
            System.out.println(c.name + ":");
            for (int depth = 1; depth <= c.expected.length; depth++) {
                BoardState b = BoardState.fromFEN(c.fen);
                long got = perft(b, depth);
                long want = c.expected[depth - 1];
                boolean ok = got == want;
                allPass &= ok;
                System.out.printf("  depth %d: %,d  (expected %,d)  %s%n",
                        depth, got, want, ok ? "OK" : "FAIL");
            }
        }
        System.out.println(allPass ? "\nAll perft tests passed." : "\nPERFT FAILURES DETECTED.");
        if (!allPass) System.exit(1);
    }
}
