package com.example.chess.model

enum class ChessColor {
    WHITE, BLACK;

    fun opponent(): ChessColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7

    fun toAlgebraic(): String {
        val files = "abcdefgh"
        val ranks = "87654321"
        return if (isValid()) "${files[col]}${ranks[row]}" else "??"
    }
}

data class ChessPiece(
    val type: PieceType,
    val color: ChessColor
) {
    fun getSymbol(): String {
        // We use the robust solid filled Unicode symbols so they possess symmetric visual weights,
        // and we will tint them appropriately in the UI.
        return when (type) {
            PieceType.KING -> "♚"
            PieceType.QUEEN -> "♛"
            PieceType.ROOK -> "♜"
            PieceType.BISHOP -> "♝"
            PieceType.KNIGHT -> "♞"
            PieceType.PAWN -> "♟"
        }
    }
}

enum class GameStatus {
    ACTIVE,
    CHECKMATE,
    STALEMATE,
    DRAW_INSUFFICIENT_MATERIAL
}

data class ChessMoveRecord(
    val player: ChessColor,
    val pieceType: PieceType,
    val from: Position,
    val to: Position,
    val capturedPiece: ChessPiece?,
    val isCheck: Boolean,
    val notation: String
)
