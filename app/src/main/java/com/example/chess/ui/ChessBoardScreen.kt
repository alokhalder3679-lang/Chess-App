package com.example.chess.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.model.*
import com.example.chess.viewmodel.ChessUiState
import com.example.chess.viewmodel.GameViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChessBoardScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Linear gradient for a premium atmospheric dark slate background
    val premiumBackgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            Color(0xFF18181B), // Zinc 900
            Color(0xFF09090B)  // Zinc 955
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent // Controlled by background brush below
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(premiumBackgroundBrush)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HEADER SECTION (Title, Game mode label)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Branded App Logo and Title Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("app_logo_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Chess Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column {
                        Text(
                            text = "CHESS NOUVEAU",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = "Grandmaster Edition",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF818CF8),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "MATCH STATUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF71717A), // Zinc 500
                                letterSpacing = 1.5.sp
                            )
                        )
                        Text(
                            text = "Blitz Session • 5:00",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF4F4F5) // Zinc 100
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sound Mute Toggle Button
                        var soundEnabled by remember { mutableStateOf(ChessSoundPlayer.isSoundEnabled) }
                        IconButton(
                            onClick = {
                                ChessSoundPlayer.isSoundEnabled = !ChessSoundPlayer.isSoundEnabled
                                soundEnabled = ChessSoundPlayer.isSoundEnabled
                            },
                            modifier = Modifier
                                .testTag("mute_button")
                                .size(36.dp)
                                .background(Color(0xFF27272A), CircleShape)
                                .border(1.dp, Color(0xFF3F3F46), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = if (soundEnabled) "Mute Sound" else "Unmute Sound",
                                tint = if (soundEnabled) Color(0xFF818CF8) else Color(0xFF71717A),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        TurnIndicator(
                            activeColor = uiState.activeColor,
                            isCheck = if (uiState.activeColor == ChessColor.WHITE) uiState.isWhiteInCheck else uiState.isBlackInCheck
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TOP PLAYER CARD: Opponent Profile (Grandmaster AI)
                PlayerProfileCard(
                    name = "Grandmaster_AI",
                    elo = "2840 ELO",
                    avatarSymbol = "♚",
                    isBot = true,
                    capturedPieces = uiState.capturedByBlack,
                    capturedPieceColor = ChessColor.WHITE,
                    isCurrentTurn = uiState.activeColor == ChessColor.BLACK
                )
            }

            // 2. CHESS BOARD CONTAINER (zinc square board)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val boardSize = minOf(maxWidth, maxHeight)
                Box(
                    modifier = Modifier.size(boardSize)
                ) {
                    ChessBoardGrid(
                        uiState = uiState,
                        onSquareClick = { viewModel.handleSquareClick(it) }
                    )
                }
            }

            // 3. BOTTOM PROFILE, HISTORY & CONTROL ACTION PANEL
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // BOTTOM PLAYER CARD: User Profile (You)
                PlayerProfileCard(
                    name = "You",
                    elo = "1450 ELO",
                    avatarSymbol = "♔",
                    isBot = false,
                    capturedPieces = uiState.capturedByWhite,
                    capturedPieceColor = ChessColor.BLACK,
                    isCurrentTurn = uiState.activeColor == ChessColor.WHITE
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Move log section (scrollable)
                if (uiState.moveHistory.isNotEmpty()) {
                    MoveHistoryLog(moveHistory = uiState.moveHistory)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Control actions panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo button (Modern styled rounded frame)
                    IconButton(
                        onClick = { viewModel.undoLastMove() },
                        enabled = viewModel.canUndo(),
                        modifier = Modifier
                            .testTag("undo_button")
                            .border(
                                width = 1.dp,
                                color = if (viewModel.canUndo()) Color(0xFF3F3F46) else Color(0x1F27272A),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(
                                color = if (viewModel.canUndo()) Color(0xFF27272A) else Color(0x0CFFFFFF),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo Last Move",
                            tint = if (viewModel.canUndo()) Color(0xFFF4F4F5) else Color(0xFF52525B)
                        )
                    }

                    // Reset Match Button
                    Button(
                        onClick = { viewModel.resetGame() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5), // Premium Indigo 600
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .testTag("reset_button")
                            .height(52.dp)
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Game",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RESET GAME",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }

        // 4. GAME OVER STATE OVERLAYS & DIALOGS
        if (uiState.status != GameStatus.ACTIVE) {
            GameOverDialog(
                status = uiState.status,
                activeColor = uiState.activeColor,
                onReset = { viewModel.resetGame() }
            )
        }
    }
}

@Composable
fun PlayerProfileCard(
    name: String,
    elo: String,
    avatarSymbol: String,
    isBot: Boolean,
    capturedPieces: List<ChessPiece>,
    capturedPieceColor: ChessColor,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTurn) Color(0xFF1F2024) else Color(0xFF141517)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isCurrentTurn) Color(0xFF6366F1).copy(alpha = 0.45f) else Color(0xFF27272A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar Box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isBot) Color(0xFF27272A) else Color(0xFF6366F1).copy(alpha = 0.2f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isBot) Color(0xFF3F3F46) else Color(0xFF6366F1).copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarSymbol,
                        style = TextStyle(
                            fontSize = 20.sp,
                            shadow = Shadow(
                                color = if (isBot) Color.Transparent else Color.Black.copy(alpha = 0.3f),
                                offset = Offset(1f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        color = if (isBot) Color(0xFFEEEED2) else Color(0xFF818CF8)
                    )
                }

                // Metadata Column
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F5)
                        )
                    )
                    Text(
                        text = elo,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF71717A),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Captured Trophies Stack
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (capturedPieces.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF09090B), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF27272A), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        capturedPieces.forEach { piece ->
                            Text(
                                text = piece.getSymbol(),
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    shadow = Shadow(
                                        color = if (capturedPieceColor == ChessColor.WHITE) Color.Black.copy(alpha = 0.4f) else Color.Transparent,
                                        offset = Offset(1f, 1f),
                                        blurRadius = 1.5f
                                    )
                                ),
                                color = if (capturedPieceColor == ChessColor.WHITE) Color(0xFFFFFFFF) else Color(0xFF3F3F46)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "no captures",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF3F3F46),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TurnIndicator(
    activeColor: ChessColor,
    isCheck: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isCheck) Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isCheck) Color(0xFFEF4444).copy(alpha = 0.35f) else Color(0xFF6366F1).copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isCheck) Color(0xFFEF4444) else Color(0xFF818CF8))
            )

            Text(
                text = when {
                    isCheck && activeColor == ChessColor.WHITE -> "WHITE IN CHECK"
                    isCheck && activeColor == ChessColor.BLACK -> "BLACK IN CHECK"
                    activeColor == ChessColor.WHITE -> "WHITE TO MOVE"
                    else -> "BLACK TO MOVE"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isCheck) Color(0xFFEF4444) else Color(0xFF818CF8),
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
fun ChessBoardGrid(
    uiState: ChessUiState,
    onSquareClick: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
            .border(2.dp, Color(0xFF27272A), RoundedCornerShape(12.dp)), // Zinc 800 Border Frame
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF18181B) // Deep Zinc Base
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0..7) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (col in 0..7) {
                        val position = Position(row, col)
                        val piece = uiState.board[row][col]
                        val isSelected = uiState.selectedPosition == position
                        val isValidMoveTarget = uiState.validMovesForSelected.contains(position)

                        BoardSquare(
                            position = position,
                            piece = piece,
                            isSelected = isSelected,
                            isValidMoveTarget = isValidMoveTarget,
                            onClick = { onSquareClick(position) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoardSquare(
    position: Position,
    piece: ChessPiece?,
    isSelected: Boolean,
    isValidMoveTarget: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightSquare = (position.row + position.col) % 2 == 0
    val normalColor = if (isLightSquare) Color(0xFFE4E4E7) else Color(0xFF3F3F46) // Zinc 300 vs Zinc 700

    val backgroundColor = when {
        isSelected -> if (isLightSquare) Color(0xFFC7D2FE) else Color(0xFF4338CA) // Modern Indigo translucent blend
        isValidMoveTarget && piece != null -> Color(0xFFFCA5A5) // Translucent Red capture helper
        else -> normalColor
    }

    val squareBorderModifier = if (isSelected) {
        Modifier.border(2.dp, Color(0xFF818CF8)) // Indigo highlight frame
    } else if (isValidMoveTarget && piece != null) {
        Modifier.border(2.dp, Color(0xFFEF4444)) // Red highlight target capture frame
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor)
            .then(squareBorderModifier)
            .clickable(onClick = onClick)
            .testTag("square_${position.row}_${position.col}")
    ) {
        // Coordinate Label
        val coordinateColor = if (isLightSquare) Color(0xFF71717A) else Color(0xFFD4D4D8)

        if (position.col == 0) {
            val rankText = (8 - position.row).toString()
            Text(
                text = rankText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = coordinateColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 4.dp, top = 2.dp)
            )
        }

        if (position.row == 7) {
            val fileText = ('a' + position.col).toString()
            Text(
                text = fileText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = coordinateColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 2.dp)
            )
        }

        // Render Chess Piece Symbol
        if (piece != null) {
            Text(
                text = piece.getSymbol(),
                style = TextStyle(
                    fontSize = 35.sp,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = if (piece.color == ChessColor.WHITE) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.25f),
                        offset = Offset(1.5f, 1.5f),
                        blurRadius = 3f
                    )
                ),
                color = if (piece.color == ChessColor.WHITE) Color(0xFFFFFFFF) else Color(0xFF09090B),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Translucent Indigo/Blue highlight dot target for legal moves on empty squares
        if (isValidMoveTarget && piece == null) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1).copy(alpha = 0.7f))
                    .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.8f), CircleShape)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun MoveHistoryLog(
    moveHistory: List<ChessMoveRecord>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 64.dp)
            .background(Color(0xFF141517), shape = RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "MATCH LOG",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF71717A), // Zinc 500
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chunkedHistory = moveHistory.chunked(2)
            items(chunkedHistory.size) { index ->
                val pair = chunkedHistory[index]
                val whiteMove = pair.getOrNull(0)?.notation ?: ""
                val blackMove = pair.getOrNull(1)?.notation ?: ""
                
                Text(
                    text = "${index + 1}. $whiteMove $blackMove",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFE4E4E7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
fun GameOverDialog(
    status: GameStatus,
    activeColor: ChessColor,
    onReset: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Force response interactively */ },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFF18181B), // Zinc 900
        title = {
            Text(
                text = "GAME OVER",
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFE4E4E7),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            val message = when (status) {
                GameStatus.CHECKMATE -> {
                    val winner = activeColor.opponent().name
                    "CHECKMATE!\n$winner dominates and wins the match!"
                }
                GameStatus.STALEMATE -> "STALEMATE!\nThe active player is trapped with no legal moves. It's a draw."
                GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "DRAW!\nNeither side possesses sufficient material to drive a checkmate."
                else -> ""
            }
            Text(
                text = message,
                color = Color(0xFFA1A1AA), // Zinc 400
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5), // Indigo 600
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_reset_button")
            ) {
                Text(
                    text = "PLAY AGAIN",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
