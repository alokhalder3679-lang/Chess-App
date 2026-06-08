package com.example.chess.model

object ChessEngine {

    fun createInitialBoard(): List<List<ChessPiece?>> {
        val board = MutableList(8) { MutableList<ChessPiece?>(8) { null } }

        // Place major backrank black pieces (row 0)
        board[0][0] = ChessPiece(PieceType.ROOK, ChessColor.BLACK)
        board[0][1] = ChessPiece(PieceType.KNIGHT, ChessColor.BLACK)
        board[0][2] = ChessPiece(PieceType.BISHOP, ChessColor.BLACK)
        board[0][3] = ChessPiece(PieceType.QUEEN, ChessColor.BLACK)
        board[0][4] = ChessPiece(PieceType.KING, ChessColor.BLACK)
        board[0][5] = ChessPiece(PieceType.BISHOP, ChessColor.BLACK)
        board[0][6] = ChessPiece(PieceType.KNIGHT, ChessColor.BLACK)
        board[0][7] = ChessPiece(PieceType.ROOK, ChessColor.BLACK)

        // Place black pawns (row 1)
        for (col in 0..7) {
            board[1][col] = ChessPiece(PieceType.PAWN, ChessColor.BLACK)
        }

        // Place white pawns (row 6)
        for (col in 0..7) {
            board[6][col] = ChessPiece(PieceType.PAWN, ChessColor.WHITE)
        }

        // Place major backrank white pieces (row 7)
        board[7][0] = ChessPiece(PieceType.ROOK, ChessColor.WHITE)
        board[7][1] = ChessPiece(PieceType.KNIGHT, ChessColor.WHITE)
        board[7][2] = ChessPiece(PieceType.BISHOP, ChessColor.WHITE)
        board[7][3] = ChessPiece(PieceType.QUEEN, ChessColor.WHITE)
        board[7][4] = ChessPiece(PieceType.KING, ChessColor.WHITE)
        board[7][5] = ChessPiece(PieceType.BISHOP, ChessColor.WHITE)
        board[7][6] = ChessPiece(PieceType.KNIGHT, ChessColor.WHITE)
        board[7][7] = ChessPiece(PieceType.ROOK, ChessColor.WHITE)

        return board.map { it.toList() }.toList()
    }

    fun getPseudoLegalMoves(from: Position, board: List<List<ChessPiece?>>): List<Position> {
        val piece = board[from.row][from.col] ?: return emptyList()
        val moves = mutableListOf<Position>()

        when (piece.type) {
            PieceType.PAWN -> {
                val direction = if (piece.color == ChessColor.WHITE) -1 else 1
                val startRow = if (piece.color == ChessColor.WHITE) 6 else 1

                val forwardOne = Position(from.row + direction, from.col)
                if (forwardOne.isValid() && board[forwardOne.row][forwardOne.col] == null) {
                    moves.add(forwardOne)

                    val forwardTwo = Position(from.row + 2 * direction, from.col)
                    if (from.row == startRow && forwardTwo.isValid() && board[forwardTwo.row][forwardTwo.col] == null) {
                        moves.add(forwardTwo)
                    }
                }

                // Diagonal Captures
                val diagonals = listOf(
                    Position(from.row + direction, from.col - 1),
                    Position(from.row + direction, from.col + 1)
                )
                for (diag in diagonals) {
                    if (diag.isValid()) {
                        val target = board[diag.row][diag.col]
                        if (target != null && target.color == piece.color.opponent()) {
                            moves.add(diag)
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val offsets = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for ((dr, dc) in offsets) {
                    val target = Position(from.row + dr, from.col + dc)
                    if (target.isValid()) {
                        val targetPiece = board[target.row][target.col]
                        if (targetPiece == null || targetPiece.color == piece.color.opponent()) {
                            moves.add(target)
                        }
                    }
                }
            }

            PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN -> {
                val directions = when (piece.type) {
                    PieceType.BISHOP -> listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
                    PieceType.ROOK -> listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
                    // Queen
                    else -> listOf(
                        Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
                        Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
                    )
                }
                for ((dr, dc) in directions) {
                    var r = from.row + dr
                    var c = from.col + dc
                    while (Position(r, c).isValid()) {
                        val target = Position(r, c)
                        val targetPiece = board[r][c]
                        if (targetPiece == null) {
                            moves.add(target)
                        } else {
                            if (targetPiece.color == piece.color.opponent()) {
                                moves.add(target)
                            }
                            break // Hit a piece, stop sliding in this direction
                        }
                        r += dr
                        c += dc
                    }
                }
            }

            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val target = Position(from.row + dr, from.col + dc)
                        if (target.isValid()) {
                            val targetPiece = board[target.row][target.col]
                            if (targetPiece == null || targetPiece.color == piece.color.opponent()) {
                                moves.add(target)
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    /**
     * Compute what squares a piece controls or attacks. Used for check-checking.
     * Pawns attack diagonally, even if currently empty.
     */
    fun getPieceAttackedSquares(from: Position, board: List<List<ChessPiece?>>): List<Position> {
        val piece = board[from.row][from.col] ?: return emptyList()
        val attacks = mutableListOf<Position>()

        when (piece.type) {
            PieceType.PAWN -> {
                val direction = if (piece.color == ChessColor.WHITE) -1 else 1
                val left = Position(from.row + direction, from.col - 1)
                val right = Position(from.row + direction, from.col + 1)
                if (left.isValid()) attacks.add(left)
                if (right.isValid()) attacks.add(right)
            }

            PieceType.KNIGHT -> {
                val offsets = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for ((dr, dc) in offsets) {
                    val target = Position(from.row + dr, from.col + dc)
                    if (target.isValid()) attacks.add(target)
                }
            }

            PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN -> {
                val directions = when (piece.type) {
                    PieceType.BISHOP -> listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
                    PieceType.ROOK -> listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
                    else -> listOf(
                        Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
                        Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
                    )
                }
                for ((dr, dc) in directions) {
                    var r = from.row + dr
                    var c = from.col + dc
                    while (Position(r, c).isValid()) {
                        attacks.add(Position(r, c))
                        if (board[r][c] != null) {
                            break // Hit block
                        }
                        r += dr
                        c += dc
                    }
                }
            }

            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val target = Position(from.row + dr, from.col + dc)
                        if (target.isValid()) attacks.add(target)
                    }
                }
            }
        }
        return attacks
    }

    fun findKingPosition(color: ChessColor, board: List<List<ChessPiece?>>): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    return Position(r, c)
                }
            }
        }
        return null
    }

    fun isInCheck(color: ChessColor, board: List<List<ChessPiece?>>): Boolean {
        val kingPos = findKingPosition(color, board) ?: return false
        val opponentColor = color.opponent()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color == opponentColor) {
                    val attacks = getPieceAttackedSquares(Position(r, c), board)
                    if (attacks.contains(kingPos)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun isSquareUnderAttack(pos: Position, attackerColor: ChessColor, board: List<List<ChessPiece?>>): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color == attackerColor) {
                    val attacks = getPieceAttackedSquares(Position(r, c), board)
                    if (attacks.contains(pos)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun makeBoardMove(from: Position, to: Position, board: List<List<ChessPiece?>>): List<List<ChessPiece?>> {
        val newBoard = board.map { it.toMutableList() }.toMutableList()
        var piece = newBoard[from.row][from.col]
        
        // Handle Castling moves
        if (piece != null && piece.type == PieceType.KING) {
            if (from == Position(7, 4) && to == Position(7, 6)) {
                // White Kingside
                val rook = newBoard[7][7]
                newBoard[7][7] = null
                newBoard[7][5] = rook
            } else if (from == Position(7, 4) && to == Position(7, 2)) {
                // White Queenside
                val rook = newBoard[7][0]
                newBoard[7][0] = null
                newBoard[7][3] = rook
            } else if (from == Position(0, 4) && to == Position(0, 6)) {
                // Black Kingside
                val rook = newBoard[0][7]
                newBoard[0][7] = null
                newBoard[0][5] = rook
            } else if (from == Position(0, 4) && to == Position(0, 2)) {
                // Black Queenside
                val rook = newBoard[0][0]
                newBoard[0][0] = null
                newBoard[0][3] = rook
            }
        }
        
        // Handle Pawn Promotion (Auto-promote to Queen for visual and mechanical standard-completeness)
        if (piece != null && piece.type == PieceType.PAWN) {
            if ((piece.color == ChessColor.WHITE && to.row == 0) || (piece.color == ChessColor.BLACK && to.row == 7)) {
                piece = ChessPiece(PieceType.QUEEN, piece.color)
            }
        }
        
        newBoard[from.row][from.col] = null
        newBoard[to.row][to.col] = piece
        return newBoard.map { it.toList() }.toList()
    }

    fun getLegalMoves(
        from: Position, 
        board: List<List<ChessPiece?>>,
        moveHistory: List<ChessMoveRecord> = emptyList()
    ): List<Position> {
        val piece = board[from.row][from.col] ?: return emptyList()
        val pseudoMoves = getPseudoLegalMoves(from, board)
        val normalLegalMoves = pseudoMoves.filter { to ->
            val tempBoard = makeBoardMove(from, to, board)
            !isInCheck(piece.color, tempBoard)
        }.toMutableList()

        // Castling options for King
        if (piece.type == PieceType.KING) {
            val color = piece.color

            if (color == ChessColor.WHITE && from == Position(7, 4)) {
                // Check Kingside Castling
                val kingHasMoved = moveHistory.any { it.player == ChessColor.WHITE && it.pieceType == PieceType.KING }
                val kingsideRookHasMoved = moveHistory.any { it.player == ChessColor.WHITE && it.from == Position(7, 7) }
                if (!kingHasMoved && !kingsideRookHasMoved) {
                    val rookPiece = board[7][7]
                    if (rookPiece != null && rookPiece.type == PieceType.ROOK && rookPiece.color == ChessColor.WHITE) {
                        if (board[7][5] == null && board[7][6] == null) {
                            if (!isInCheck(ChessColor.WHITE, board) &&
                                !isSquareUnderAttack(Position(7, 5), ChessColor.BLACK, board) &&
                                !isSquareUnderAttack(Position(7, 6), ChessColor.BLACK, board)
                            ) {
                                normalLegalMoves.add(Position(7, 6))
                            }
                        }
                    }
                }

                // Check Queenside Castling
                val queensideRookHasMoved = moveHistory.any { it.player == ChessColor.WHITE && it.from == Position(7, 0) }
                if (!kingHasMoved && !queensideRookHasMoved) {
                    val rookPiece = board[7][0]
                    if (rookPiece != null && rookPiece.type == PieceType.ROOK && rookPiece.color == ChessColor.WHITE) {
                        if (board[7][1] == null && board[7][2] == null && board[7][3] == null) {
                            if (!isInCheck(ChessColor.WHITE, board) &&
                                !isSquareUnderAttack(Position(7, 3), ChessColor.BLACK, board) &&
                                !isSquareUnderAttack(Position(7, 2), ChessColor.BLACK, board)
                            ) {
                                normalLegalMoves.add(Position(7, 2))
                            }
                        }
                    }
                }
            } else if (color == ChessColor.BLACK && from == Position(0, 4)) {
                // Check Kingside Castling
                val kingHasMoved = moveHistory.any { it.player == ChessColor.BLACK && it.pieceType == PieceType.KING }
                val kingsideRookHasMoved = moveHistory.any { it.player == ChessColor.BLACK && it.from == Position(0, 7) }
                if (!kingHasMoved && !kingsideRookHasMoved) {
                    val rookPiece = board[0][7]
                    if (rookPiece != null && rookPiece.type == PieceType.ROOK && rookPiece.color == ChessColor.BLACK) {
                        if (board[0][5] == null && board[0][6] == null) {
                            if (!isInCheck(ChessColor.BLACK, board) &&
                                !isSquareUnderAttack(Position(0, 5), ChessColor.WHITE, board) &&
                                !isSquareUnderAttack(Position(0, 6), ChessColor.WHITE, board)
                            ) {
                                normalLegalMoves.add(Position(0, 6))
                            }
                        }
                    }
                }

                // Check Queenside Castling
                val queensideRookHasMoved = moveHistory.any { it.player == ChessColor.BLACK && it.from == Position(0, 0) }
                if (!kingHasMoved && !queensideRookHasMoved) {
                    val rookPiece = board[0][0]
                    if (rookPiece != null && rookPiece.type == PieceType.ROOK && rookPiece.color == ChessColor.BLACK) {
                        if (board[0][1] == null && board[0][2] == null && board[0][3] == null) {
                            if (!isInCheck(ChessColor.BLACK, board) &&
                                !isSquareUnderAttack(Position(0, 3), ChessColor.WHITE, board) &&
                                !isSquareUnderAttack(Position(0, 2), ChessColor.WHITE, board)
                            ) {
                                normalLegalMoves.add(Position(0, 2))
                            }
                        }
                    }
                }
            }
        }

        return normalLegalMoves
    }

    fun hasAnyLegalMoves(
        color: ChessColor, 
        board: List<List<ChessPiece?>>,
        moveHistory: List<ChessMoveRecord> = emptyList()
    ): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color == color) {
                    val legalMoves = getLegalMoves(Position(r, c), board, moveHistory)
                    if (legalMoves.isNotEmpty()) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Determines the algebraic notation for a move (e.g. "e4", "Nf3", "Bxf7+")
     */
    fun generateNotation(
        piece: ChessPiece,
        from: Position,
        to: Position,
        isCapture: Boolean,
        isCheck: Boolean,
        isCheckmate: Boolean
    ): String {
        val files = "abcdefgh"
        val ranks = "87654321"

        // Castling checks for notation
        val isCastlingKingside = piece.type == PieceType.KING &&
                ((from == Position(7, 4) && to == Position(7, 6)) || (from == Position(0, 4) && to == Position(0, 6)))

        val isCastlingQueenside = piece.type == PieceType.KING &&
                ((from == Position(7, 4) && to == Position(7, 2)) || (from == Position(0, 4) && to == Position(0, 2)))

        if (isCastlingKingside) {
            return if (isCheckmate) "O-O#" else if (isCheck) "O-O+" else "O-O"
        }
        if (isCastlingQueenside) {
            return if (isCheckmate) "O-O-O#" else if (isCheck) "O-O-O+" else "O-O-O"
        }

        val pieceSymbol = when (piece.type) {
            PieceType.KING -> "K"
            PieceType.QUEEN -> "Q"
            PieceType.ROOK -> "R"
            PieceType.BISHOP -> "B"
            PieceType.KNIGHT -> "N"
            PieceType.PAWN -> ""
        }

        val toSquare = "${files[to.col]}${ranks[to.row]}"
        val fromFile = files[from.col].toString()

        val moveStr = if (piece.type == PieceType.PAWN) {
            if (isCapture) {
                "${fromFile}x${toSquare}"
            } else {
                toSquare
            }
        } else {
            if (isCapture) {
                "${pieceSymbol}x${toSquare}"
            } else {
                "${pieceSymbol}${toSquare}"
            }
        }

        return when {
            isCheckmate -> "$moveStr#"
            isCheck -> "$moveStr+"
            else -> moveStr
        }
    }

    /**
     * Very basic verification of insufficient material
     * (e.g., only two kings remaining)
     */
    fun isInsufficientMaterial(board: List<List<ChessPiece?>>): Boolean {
        var whitePiecesCount = 0
        var blackPiecesCount = 0
        var whiteKnightOrBishopCount = 0
        var blackKnightOrBishopCount = 0
        var otherPiecesCount = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c] ?: continue
                when (piece.color) {
                    ChessColor.WHITE -> {
                        whitePiecesCount++
                        if (piece.type == PieceType.KNIGHT || piece.type == PieceType.BISHOP) {
                            whiteKnightOrBishopCount++
                        } else if (piece.type != PieceType.KING) {
                            otherPiecesCount++
                        }
                    }
                    ChessColor.BLACK -> {
                        blackPiecesCount++
                        if (piece.type == PieceType.KNIGHT || piece.type == PieceType.BISHOP) {
                            blackKnightOrBishopCount++
                        } else if (piece.type != PieceType.KING) {
                            otherPiecesCount++
                        }
                    }
                }
            }
        }

        if (otherPiecesCount > 0) return false

        // Only kings left
        if (whitePiecesCount == 1 && blackPiecesCount == 1) return true

        // King and Bishop vs King or King and Knight vs King
        if (whitePiecesCount == 2 && whiteKnightOrBishopCount == 1 && blackPiecesCount == 1) return true
        if (blackPiecesCount == 2 && blackKnightOrBishopCount == 1 && whitePiecesCount == 1) return true

        return false
    }
}
