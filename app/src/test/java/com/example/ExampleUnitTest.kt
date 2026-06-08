package com.example

import com.example.chess.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testCastling_WhenPreconditionsMet_ShouldBeLegal() {
        // Setup a custom board where paths for White kingside and queenside are clear
        val board = ChessEngine.createInitialBoard().map { it.toMutableList() }
        
        // Clear squares between White king (7,4) and roops (7,0) / (7,7)
        board[7][1] = null // b1 Bishop/Knight is originally at different col anyway but let's clear the exact columns:
        board[7][1] = null // Knight (b1)
        board[7][2] = null // Bishop (c1)
        board[7][3] = null // Queen (d1)
        board[7][5] = null // Bishop (f1)
        board[7][6] = null // Knight (g1)

        val finalBoard = board.map { it.toList() }

        // Fetch legal moves for White King at (7, 4)
        val legalMoves = ChessEngine.getLegalMoves(Position(7, 4), finalBoard, emptyList())

        // White King should be able to castle kingside (7, 6) and queenside (7, 2)
        assertTrue("Kingside castling (7, 6) should be legal", legalMoves.contains(Position(7, 6)))
        assertTrue("Queenside castling (7, 2) should be legal", legalMoves.contains(Position(7, 2)))
    }

    @Test
    fun testCastling_WithBlockedPath_ShouldNotBeLegal() {
        // Setup initial board (paths are fully blocked)
        val initialBoard = ChessEngine.createInitialBoard()
        val legalMoves = ChessEngine.getLegalMoves(Position(7, 4), initialBoard, emptyList())

        assertFalse("Kingside castling should be illegal when blocked", legalMoves.contains(Position(7, 6)))
        assertFalse("Queenside castling should be illegal when blocked", legalMoves.contains(Position(7, 2)))
    }

    @Test
    fun testCastling_WhenKingOrRookHaveMoved_ShouldNotBeLegal() {
        val board = ChessEngine.createInitialBoard().map { it.toMutableList() }
        board[7][5] = null // Clear f1
        board[7][6] = null // Clear g1
        val finalBoard = board.map { it.toList() }

        // Case A: King has already moved previously
        val historyWithKingMoved = listOf(
            ChessMoveRecord(
                player = ChessColor.WHITE,
                pieceType = PieceType.KING,
                from = Position(7, 4),
                to = Position(7, 5),
                capturedPiece = null,
                isCheck = false,
                notation = "Kf1"
            )
        )
        val legalMovesA = ChessEngine.getLegalMoves(Position(7, 4), finalBoard, historyWithKingMoved)
        assertFalse("Castling should be forbidden if king has moved previously", legalMovesA.contains(Position(7, 6)))

        // Case B: Kingside Rook has already moved previously
        val historyWithRookMoved = listOf(
            ChessMoveRecord(
                player = ChessColor.WHITE,
                pieceType = PieceType.ROOK,
                from = Position(7, 7),
                to = Position(7, 6),
                capturedPiece = null,
                isCheck = false,
                notation = "Rg1"
            )
        )
        val legalMovesB = ChessEngine.getLegalMoves(Position(7, 4), finalBoard, historyWithRookMoved)
        assertFalse("Castling should be forbidden if rook has moved previously", legalMovesB.contains(Position(7, 6)))
    }

    @Test
    fun testCastling_MoveExecution_ShouldPlacePiecesCorrectly() {
        val board = ChessEngine.createInitialBoard().map { it.toMutableList() }
        board[7][5] = null
        board[7][6] = null
        val finalBoard = board.map { it.toList() }

        // Execute Kingside castling
        val postCastleBoard = ChessEngine.makeBoardMove(Position(7, 4), Position(7, 6), finalBoard)

        // The King should be at (7, 6)
        val king = postCastleBoard[7][6]
        assertNotNull("King should exist at g1 (7, 6)", king)
        assertEquals(PieceType.KING, king?.type)

        // The Rook should now be at (7, 5)
        val rook = postCastleBoard[7][5]
        assertNotNull("Rook should have jumped to f1 (7, 5)", rook)
        assertEquals(PieceType.ROOK, rook?.type)

        // The old Rook and King positions must be cleared
        assertNull("Old king square e1 (7, 4) must be empty", postCastleBoard[7][4])
        assertNull("Old rook square h1 (7, 7) must be empty", postCastleBoard[7][7])
    }

    @Test
    fun testCastling_NotationFormatting_IsCorrect() {
        val kingPiece = ChessPiece(PieceType.KING, ChessColor.WHITE)

        val kingsideNotation = ChessEngine.generateNotation(
            piece = kingPiece,
            from = Position(7, 4),
            to = Position(7, 6),
            isCapture = false,
            isCheck = false,
            isCheckmate = false
        )
        assertEquals("O-O", kingsideNotation)

        val queensideNotation = ChessEngine.generateNotation(
            piece = kingPiece,
            from = Position(7, 4),
            to = Position(7, 2),
            isCapture = false,
            isCheck = true,
            isCheckmate = false
        )
        assertEquals("O-O-O+", queensideNotation)
    }
}
