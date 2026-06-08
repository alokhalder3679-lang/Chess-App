package com.example.chess.viewmodel

import androidx.lifecycle.ViewModel
import com.example.chess.model.ChessColor
import com.example.chess.model.ChessEngine
import com.example.chess.model.ChessMoveRecord
import com.example.chess.model.ChessPiece
import com.example.chess.model.GameStatus
import com.example.chess.model.Position
import com.example.chess.ui.ChessSoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ChessUiState(
    val board: List<List<ChessPiece?>> = ChessEngine.createInitialBoard(),
    val activeColor: ChessColor = ChessColor.WHITE,
    val selectedPosition: Position? = null,
    val validMovesForSelected: List<Position> = emptyList(),
    val moveHistory: List<ChessMoveRecord> = emptyList(),
    val capturedByWhite: List<ChessPiece> = emptyList(), // Black pieces captured by White
    val capturedByBlack: List<ChessPiece> = emptyList(), // White pieces captured by Black
    val status: GameStatus = GameStatus.ACTIVE,
    val isWhiteInCheck: Boolean = false,
    val isBlackInCheck: Boolean = false
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    // History stack of states for instant, bug-free undos
    private val stateHistoryStack = mutableListOf<ChessUiState>()

    fun handleSquareClick(pos: Position) {
        try {
            val currentState = _uiState.value
            if (currentState.status != GameStatus.ACTIVE) return

            val clickedPiece = currentState.board[pos.row][pos.col]

            // Case 1: No piece selected yet
            if (currentState.selectedPosition == null) {
                if (clickedPiece != null && clickedPiece.color == currentState.activeColor) {
                    // Select own piece, calculate legal options
                    val legalMoves = ChessEngine.getLegalMoves(pos, currentState.board, currentState.moveHistory)
                    _uiState.update {
                        it.copy(
                            selectedPosition = pos,
                            validMovesForSelected = legalMoves
                        )
                    }
                }
            }
            // Case 2: A piece is already selected
            else {
                val fromPos = currentState.selectedPosition
                
                // Subcase A: Clicking a valid target destination square (execute move!)
                if (currentState.validMovesForSelected.contains(pos)) {
                    executeMove(fromPos, pos)
                } 
                // Subcase B: Clicking another piece belonging to the active player (switch selection)
                else if (clickedPiece != null && clickedPiece.color == currentState.activeColor) {
                    val legalMoves = ChessEngine.getLegalMoves(pos, currentState.board, currentState.moveHistory)
                    _uiState.update {
                        it.copy(
                            selectedPosition = pos,
                            validMovesForSelected = legalMoves
                        )
                    }
                } 
                // Subcase C: Clicking any invalid square (deselect selection)
                else {
                    _uiState.update {
                        it.copy(
                            selectedPosition = null,
                            validMovesForSelected = emptyList()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GameViewModel", "Error in handleSquareClick at pos $pos", e)
        }
    }

    private fun executeMove(from: Position, to: Position) {
        val prevState = _uiState.value
        // Save the previous state to the undo history stack
        synchronized(stateHistoryStack) {
            stateHistoryStack.add(prevState)
        }

        val movingPiece = prevState.board[from.row][from.col] ?: return
        val captured = prevState.board[to.row][to.col]

        val newBoard = ChessEngine.makeBoardMove(from, to, prevState.board)
        val opponentColor = prevState.activeColor.opponent()

        // Scan game status changes
        val opponentInCheck = ChessEngine.isInCheck(opponentColor, newBoard)
        val opponentHasMoves = ChessEngine.hasAnyLegalMoves(opponentColor, newBoard, prevState.moveHistory)

        val newStatus = when {
            opponentInCheck && !opponentHasMoves -> GameStatus.CHECKMATE
            !opponentInCheck && !opponentHasMoves -> GameStatus.STALEMATE
            ChessEngine.isInsufficientMaterial(newBoard) -> GameStatus.DRAW_INSUFFICIENT_MATERIAL
            else -> GameStatus.ACTIVE
        }

        // List captured trophies
        val updatedCapturedByWhite = if (captured != null && captured.color == ChessColor.BLACK) {
            prevState.capturedByWhite + captured
        } else {
            prevState.capturedByWhite
        }

        val updatedCapturedByBlack = if (captured != null && captured.color == ChessColor.WHITE) {
            prevState.capturedByBlack + captured
        } else {
            prevState.capturedByBlack
        }

        // Generate clean notation
        val isCheckmate = (newStatus == GameStatus.CHECKMATE)
        val notation = ChessEngine.generateNotation(
            piece = movingPiece,
            from = from,
            to = to,
            isCapture = (captured != null),
            isCheck = opponentInCheck,
            isCheckmate = isCheckmate
        )

        val moveRecord = ChessMoveRecord(
            player = prevState.activeColor,
            pieceType = movingPiece.type,
            from = from,
            to = to,
            capturedPiece = captured,
            isCheck = opponentInCheck,
            notation = notation
        )

        val whiteCheck = ChessEngine.isInCheck(ChessColor.WHITE, newBoard)
        val blackCheck = ChessEngine.isInCheck(ChessColor.BLACK, newBoard)

        _uiState.update {
            it.copy(
                board = newBoard,
                activeColor = opponentColor,
                selectedPosition = null,
                validMovesForSelected = emptyList(),
                moveHistory = prevState.moveHistory + moveRecord,
                capturedByWhite = updatedCapturedByWhite,
                capturedByBlack = updatedCapturedByBlack,
                status = newStatus,
                isWhiteInCheck = whiteCheck,
                isBlackInCheck = blackCheck
            )
        }

        // Play appropriate sound effects depending on move outcome
        when {
            newStatus == GameStatus.CHECKMATE -> {
                ChessSoundPlayer.playCheckmate()
            }
            newStatus == GameStatus.STALEMATE || newStatus == GameStatus.DRAW_INSUFFICIENT_MATERIAL -> {
                ChessSoundPlayer.playCheckmate()
            }
            opponentInCheck -> {
                ChessSoundPlayer.playCheck()
            }
            captured != null -> {
                ChessSoundPlayer.playCapture()
            }
            else -> {
                ChessSoundPlayer.playMove()
            }
        }
    }

    fun resetGame() {
        try {
            synchronized(stateHistoryStack) {
                stateHistoryStack.clear()
            }
            _uiState.value = ChessUiState()
            ChessSoundPlayer.playMove() // play nice soft knock on reset
        } catch (e: Exception) {
            android.util.Log.e("GameViewModel", "Error in resetGame", e)
        }
    }

    fun undoLastMove() {
        try {
            var prevState: ChessUiState? = null
            synchronized(stateHistoryStack) {
                if (stateHistoryStack.isNotEmpty()) {
                    prevState = stateHistoryStack.removeAt(stateHistoryStack.lastIndex)
                }
            }
            prevState?.let {
                _uiState.value = it
                ChessSoundPlayer.playMove() // play soft knock on undo
            }
        } catch (e: Exception) {
            android.util.Log.e("GameViewModel", "Error in undoLastMove", e)
        }
    }

    fun canUndo(): Boolean {
        return synchronized(stateHistoryStack) {
            stateHistoryStack.isNotEmpty()
        }
    }
}
