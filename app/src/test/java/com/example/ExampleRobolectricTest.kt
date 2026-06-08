package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.chess.model.ChessColor
import com.example.chess.model.GameStatus
import com.example.chess.model.Position
import com.example.chess.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Chess", appName)
  }

  @Test
  fun `test MainActivity launch`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
    val activity = controller.get()
    assertNotNull(activity)
  }

  @Test
  fun `chess engine initialization and initial board state`() {
    val viewModel = GameViewModel()
    val state = viewModel.uiState.value

    assertEquals(ChessColor.WHITE, state.activeColor)
    assertEquals(GameStatus.ACTIVE, state.status)
    assertNull(state.selectedPosition)
    assertTrue(state.validMovesForSelected.isEmpty())
    assertTrue(state.moveHistory.isEmpty())
    assertTrue(state.capturedByWhite.isEmpty())
    assertTrue(state.capturedByBlack.isEmpty())

    // Check White Pawn at e2 (row 6, col 4) and Black Pawn at e7 (row 1, col 4)
    val e2Piece = state.board[6][4]
    assertNotNull(e2Piece)
    assertEquals(com.example.chess.model.PieceType.PAWN, e2Piece?.type)
    assertEquals(ChessColor.WHITE, e2Piece?.color)

    val e7Piece = state.board[1][4]
    assertNotNull(e7Piece)
    assertEquals(com.example.chess.model.PieceType.PAWN, e7Piece?.type)
    assertEquals(ChessColor.BLACK, e7Piece?.color)
  }

  @Test
  fun `select and move pawn e2 to e4 then black moves d7 to d5`() {
    val viewModel = GameViewModel()

    // 1. Click e2 pawn (row 6, col 4)
    viewModel.handleSquareClick(Position(6, 4))
    var state = viewModel.uiState.value
    assertEquals(Position(6, 4), state.selectedPosition)
    // Pawns should have 2 options: e3 (5, 4) and e4 (4, 4)
    assertTrue(state.validMovesForSelected.contains(Position(5, 4)))
    assertTrue(state.validMovesForSelected.contains(Position(4, 4)))

    // 2. Click e4 (row 4, col 4)
    viewModel.handleSquareClick(Position(4, 4))
    state = viewModel.uiState.value
    assertNull(state.selectedPosition)
    assertEquals(ChessColor.BLACK, state.activeColor)
    assertEquals(1, state.moveHistory.size)
    assertEquals("e4", state.moveHistory.last().notation)

    // 3. Click d7 pawn (row 1, col 3)
    viewModel.handleSquareClick(Position(1, 3))
    state = viewModel.uiState.value
    assertEquals(Position(1, 3), state.selectedPosition)

    // 4. Click d5 (row 3, col 3)
    viewModel.handleSquareClick(Position(3, 3))
    state = viewModel.uiState.value
    assertEquals(ChessColor.WHITE, state.activeColor)
    assertEquals(2, state.moveHistory.size)
    assertEquals("d5", state.moveHistory.last().notation)
  }

  @Test
  fun `undo and reset gameplay successfully`() {
    val viewModel = GameViewModel()

    // Move e2 to e4
    viewModel.handleSquareClick(Position(6, 4))
    viewModel.handleSquareClick(Position(4, 4))
    
    // Move d7 to d5
    viewModel.handleSquareClick(Position(1, 3))
    viewModel.handleSquareClick(Position(3, 3))

    assertTrue(viewModel.canUndo())
    viewModel.undoLastMove()

    var state = viewModel.uiState.value
    assertEquals(ChessColor.BLACK, state.activeColor)
    assertEquals(1, state.moveHistory.size)

    viewModel.resetGame()
    state = viewModel.uiState.value
    assertEquals(ChessColor.WHITE, state.activeColor)
    assertTrue(state.moveHistory.isEmpty())
  }
}
