/*
 * Copyright (c) 2026 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.euidi.verifier.presentation.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/** Colours of the Loen 2026 welcome screen, taken from the MinID+ Christmas design. */
internal object FestColors {
    val Background = Color(0xFFF9F5EC)
    val DarkGreen = Color(0xFF1C3B2B)
    val Green = Color(0xFF2F5C41)
    val Navy = Color(0xFF1E2A3B)
    val Gold = Color(0xFFD1B04E)
    val PaleGold = Color(0xFFF1E2B0)
    val Cream = Color(0xFFF6E7B8)
    val Red = Color(0xFFA33A35)
    val Sparkle = Color(0xFFF3DFA0)
}

// The drawing is made on a 600 x 440 grid and scaled to the canvas.
private const val GRID_WIDTH = 600f
private const val GRID_HEIGHT = 440f
internal const val FEST_ILLUSTRATION_ASPECT_RATIO = GRID_WIDTH / GRID_HEIGHT

/** Gold pill button with a soft glow, as in the MinID+ Christmas design. */
@Composable
internal fun FestGoldButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(68.dp)
            .shadow(
                elevation = 18.dp,
                shape = CircleShape,
                ambientColor = FestColors.Gold,
                spotColor = FestColors.Gold,
            )
            .clip(CircleShape)
            .background(if (enabled) FestColors.Gold else FestColors.Gold.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = FestColors.DarkGreen,
            fontSize = 22.sp,
        )
    }
}

/** Two people toasting next to a Christmas tree. */
@Composable
internal fun FestIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val factor = minOf(size.width / GRID_WIDTH, size.height / GRID_HEIGHT)
        scale(scale = factor, pivot = Offset.Zero) {
            drawSparkle(Offset(30f, 60f), 14f)
            drawSparkle(Offset(420f, 20f), 10f)
            drawSparkle(Offset(560f, 90f), 16f)
            drawTree()
            drawPresents()
            drawMan()
            drawWoman()
            drawMugs()
        }
    }
}

/** Three Christmas baubles hanging from the top edge. */
@Composable
internal fun FestOrnaments(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val factor = minOf(size.width / 200f, size.height / 130f)
        scale(scale = factor, pivot = Offset.Zero) {
            drawOrnament(x = 40f, stringLength = 50f, color = FestColors.Red)
            drawOrnament(x = 100f, stringLength = 75f, color = FestColors.Gold)
            drawOrnament(x = 160f, stringLength = 40f, color = FestColors.Red)
        }
    }
}

private fun DrawScope.drawOrnament(x: Float, stringLength: Float, color: Color) {
    drawLine(FestColors.Gold, Offset(x, 0f), Offset(x, stringLength), strokeWidth = 2f)
    drawRoundRect(
        color = FestColors.Gold,
        topLeft = Offset(x - 7f, stringLength - 2f),
        size = Size(14f, 10f),
        cornerRadius = CornerRadius(2f)
    )
    val center = Offset(x, stringLength + 30f)
    drawCircle(color.copy(alpha = 0.15f), radius = 30f, center = center + Offset(2f, 4f))
    drawCircle(color, radius = 28f, center = center)
    drawCircle(Color.White.copy(alpha = 0.35f), radius = 7f, center = center + Offset(-10f, -10f))
    drawCircle(Color.White.copy(alpha = 0.5f), radius = 2.5f, center = center + Offset(12f, 12f))
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, FestColors.Sparkle)
}

private fun DrawScope.triangle(top: Offset, bottomLeft: Offset, bottomRight: Offset, color: Color) {
    drawPath(
        Path().apply {
            moveTo(top.x, top.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        },
        color
    )
}

private fun DrawScope.drawTree() {
    val cx = 500f
    drawRect(FestColors.Gold, Offset(cx - 12f, 360f), Size(24f, 30f))
    triangle(Offset(cx, 200f), Offset(cx - 110f, 375f), Offset(cx + 110f, 375f), FestColors.DarkGreen)
    triangle(Offset(cx, 130f), Offset(cx - 85f, 290f), Offset(cx + 85f, 290f), FestColors.Green)
    triangle(Offset(cx, 60f), Offset(cx - 60f, 200f), Offset(cx + 60f, 200f), FestColors.DarkGreen)

    // Garlands
    val garland = Stroke(width = 3f, cap = StrokeCap.Round)
    drawArc(FestColors.Cream, 20f, 120f, false, Offset(cx - 70f, 100f), Size(140f, 70f), style = garland)
    drawArc(FestColors.Cream, 20f, 120f, false, Offset(cx - 95f, 190f), Size(190f, 80f), style = garland)
    drawArc(FestColors.Cream, 20f, 120f, false, Offset(cx - 120f, 285f), Size(240f, 80f), style = garland)

    // Baubles
    listOf(
        Offset(cx - 20f, 140f) to FestColors.Gold,
        Offset(cx + 25f, 175f) to FestColors.Red,
        Offset(cx + 35f, 230f) to FestColors.Cream,
        Offset(cx - 45f, 245f) to FestColors.Gold,
        Offset(cx - 10f, 300f) to FestColors.Red,
        Offset(cx + 55f, 320f) to FestColors.Gold,
        Offset(cx - 70f, 340f) to FestColors.Cream,
    ).forEach { (center, color) -> drawCircle(color, radius = 9f, center = center) }

    // Star
    val star = Path()
    val starCenter = Offset(cx, 52f)
    for (i in 0 until 10) {
        val angle = (-90.0 + i * 36.0) * kotlin.math.PI / 180.0
        val r = if (i % 2 == 0) 26f else 11f
        val point = Offset(
            (starCenter.x + r * kotlin.math.cos(angle)).toFloat(),
            (starCenter.y + r * kotlin.math.sin(angle)).toFloat()
        )
        if (i == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
    }
    star.close()
    drawPath(star, FestColors.Gold)
}

private fun DrawScope.drawPresents() {
    drawRect(FestColors.Red, Offset(400f, 360f), Size(50f, 40f))
    drawRect(FestColors.Cream, Offset(421f, 360f), Size(8f, 40f))
    drawRect(FestColors.Cream, Offset(540f, 365f), Size(55f, 35f))
    drawRect(FestColors.Gold, Offset(563f, 365f), Size(8f, 35f))
    drawRect(FestColors.Gold, Offset(470f, 372f), Size(25f, 20f))
}

private fun DrawScope.drawMan() {
    // Legs and shoes
    drawRect(FestColors.DarkGreen, Offset(85f, 300f), Size(32f, 95f))
    drawRect(FestColors.DarkGreen, Offset(125f, 300f), Size(32f, 95f))
    drawRoundRect(FestColors.Green, Offset(70f, 388f), Size(50f, 14f), CornerRadius(7f))
    drawRoundRect(FestColors.Green, Offset(125f, 388f), Size(50f, 14f), CornerRadius(7f))
    // Body
    drawRoundRect(FestColors.Gold, Offset(70f, 175f), Size(105f, 135f), CornerRadius(30f))
    // Raised arm towards the mugs
    drawLine(FestColors.Gold, Offset(160f, 200f), Offset(225f, 165f), strokeWidth = 28f, cap = StrokeCap.Round)
    // Scarf
    drawRoundRect(FestColors.Red, Offset(95f, 170f), Size(55f, 18f), CornerRadius(9f))
    drawRoundRect(FestColors.Red, Offset(125f, 180f), Size(16f, 50f), CornerRadius(6f))
    // Head and hair
    drawCircle(FestColors.PaleGold, radius = 36f, center = Offset(122f, 130f))
    drawArc(FestColors.DarkGreen, 180f, 180f, true, Offset(84f, 88f), Size(76f, 60f))
    drawCircle(FestColors.DarkGreen, radius = 16f, center = Offset(152f, 108f))
    // Smile and closed eyes
    val face = Stroke(width = 3f, cap = StrokeCap.Round)
    drawArc(FestColors.DarkGreen, 200f, 140f, false, Offset(103f, 122f), Size(14f, 10f), style = face)
    drawArc(FestColors.DarkGreen, 200f, 140f, false, Offset(128f, 122f), Size(14f, 10f), style = face)
    drawArc(Color.White, 0f, 180f, true, Offset(108f, 138f), Size(28f, 20f))
}

private fun DrawScope.drawWoman() {
    // Legs and shoes
    drawRect(FestColors.Gold, Offset(278f, 300f), Size(30f, 95f))
    drawRect(FestColors.Gold, Offset(315f, 300f), Size(30f, 95f))
    drawRoundRect(FestColors.DarkGreen, Offset(262f, 388f), Size(48f, 14f), CornerRadius(7f))
    drawRoundRect(FestColors.DarkGreen, Offset(315f, 388f), Size(48f, 14f), CornerRadius(7f))
    // Long hair behind the body
    drawRoundRect(FestColors.DarkGreen, Offset(278f, 100f), Size(90f, 130f), CornerRadius(40f))
    // Body
    drawRoundRect(FestColors.Green, Offset(268f, 175f), Size(100f, 135f), CornerRadius(30f))
    drawLine(FestColors.Gold, Offset(300f, 250f), Offset(330f, 250f), strokeWidth = 3f)
    // Raised arm towards the mugs
    drawLine(FestColors.Green, Offset(285f, 200f), Offset(255f, 165f), strokeWidth = 28f, cap = StrokeCap.Round)
    // Head and fringe
    drawCircle(FestColors.PaleGold, radius = 34f, center = Offset(318f, 132f))
    drawArc(FestColors.DarkGreen, 180f, 180f, true, Offset(282f, 92f), Size(74f, 62f))
    // Smile and closed eyes
    val face = Stroke(width = 3f, cap = StrokeCap.Round)
    drawArc(FestColors.DarkGreen, 200f, 140f, false, Offset(298f, 126f), Size(13f, 9f), style = face)
    drawArc(FestColors.DarkGreen, 200f, 140f, false, Offset(322f, 126f), Size(13f, 9f), style = face)
    drawArc(Color.White, 0f, 180f, true, Offset(304f, 140f), Size(26f, 18f))
    drawCircle(FestColors.Gold, radius = 4f, center = Offset(352f, 145f))
}

private fun DrawScope.drawMugs() {
    drawMug(Offset(205f, 130f))
    drawMug(Offset(250f, 125f))
    // Clink lines
    val clink = Stroke(width = 3f, cap = StrokeCap.Round)
    drawLine(FestColors.Gold, Offset(245f, 95f), Offset(240f, 80f), strokeWidth = clink.width, cap = StrokeCap.Round)
    drawLine(FestColors.Gold, Offset(258f, 92f), Offset(262f, 77f), strokeWidth = clink.width, cap = StrokeCap.Round)
    drawLine(FestColors.Gold, Offset(230f, 102f), Offset(220f, 92f), strokeWidth = clink.width, cap = StrokeCap.Round)
}

private fun DrawScope.drawMug(topLeft: Offset) {
    val handle = Stroke(width = 6f)
    drawArc(FestColors.Cream, 270f, 180f, false, topLeft + Offset(28f, 10f), Size(24f, 28f), style = handle)
    drawRoundRect(FestColors.Cream, topLeft, Size(38f, 50f), CornerRadius(6f))
    drawRoundRect(FestColors.Gold, topLeft + Offset(6f, 10f), Size(26f, 34f), CornerRadius(4f))
    drawCircle(Color.White, radius = 11f, center = topLeft + Offset(9f, 2f))
    drawCircle(Color.White, radius = 12f, center = topLeft + Offset(26f, 0f))
}

/** Gold circle with a dark green tick and a soft glow, shown when a drikkebong is accepted. */
@Composable
internal fun FestCheckBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val factor = minOf(size.width, size.height) / 200f
        scale(scale = factor, pivot = Offset.Zero) {
            val center = Offset(100f, 100f)
            drawCircle(FestColors.Gold.copy(alpha = 0.12f), radius = 78f, center = center)
            drawCircle(FestColors.Gold.copy(alpha = 0.2f), radius = 68f, center = center)
            drawCircle(FestColors.Gold, radius = 58f, center = center)
            val tick = Path().apply {
                moveTo(72f, 102f)
                lineTo(92f, 122f)
                lineTo(130f, 82f)
            }
            drawPath(
                tick,
                FestColors.DarkGreen,
                style = Stroke(width = 11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawSparkle(Offset(18f, 40f), 10f)
            drawSparkle(Offset(186f, 30f), 13f)
            drawSparkle(Offset(178f, 172f), 8f)
        }
    }
}

// The cheers drawing is made on a 400 x 260 grid.
internal const val CHEERS_ILLUSTRATION_ASPECT_RATIO = 400f / 260f

/** Two beer mugs clinking, with confetti, sparkles and a present. */
@Composable
internal fun CheersIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val factor = minOf(size.width / 400f, size.height / 260f)
        scale(scale = factor, pivot = Offset.Zero) {
            drawConfetti()
            drawSparkle(Offset(200f, 30f), 16f)
            drawSparkle(Offset(70f, 120f), 12f)
            drawSparkle(Offset(320f, 110f), 12f)
            drawSparkle(Offset(20f, 40f), 8f)
            drawSparkle(Offset(385f, 25f), 10f)
            drawSparkle(Offset(90f, 230f), 6f)
            rotate(degrees = -14f, pivot = Offset(150f, 160f)) {
                drawBigMug(topLeft = Offset(105f, 95f), handleOnLeft = true)
            }
            rotate(degrees = 14f, pivot = Offset(250f, 160f)) {
                drawBigMug(topLeft = Offset(205f, 95f), handleOnLeft = false)
            }
            drawGift(Offset(285f, 185f))
        }
    }
}

private fun DrawScope.drawBigMug(topLeft: Offset, handleOnLeft: Boolean) {
    val body = Size(90f, 115f)
    // Handle
    val handleX = if (handleOnLeft) topLeft.x - 32f else topLeft.x + body.width - 8f
    drawRoundRect(
        FestColors.DarkGreen,
        Offset(handleX, topLeft.y + 22f),
        Size(40f, 60f),
        CornerRadius(16f),
        style = Stroke(width = 13f)
    )
    // Glass with outline, beer and highlights
    drawRoundRect(FestColors.DarkGreen, topLeft, body, CornerRadius(14f))
    drawRoundRect(
        FestColors.Gold,
        topLeft + Offset(9f, 9f),
        Size(body.width - 18f, body.height - 18f),
        CornerRadius(8f)
    )
    drawRoundRect(FestColors.Cream, topLeft + Offset(20f, 26f), Size(12f, 72f), CornerRadius(6f))
    drawRoundRect(FestColors.Cream, topLeft + Offset(42f, 26f), Size(8f, 60f), CornerRadius(4f))
    drawCircle(Color.White, radius = 3f, center = topLeft + Offset(64f, 60f))
    drawCircle(Color.White, radius = 2.5f, center = topLeft + Offset(58f, 82f))
    // Foam
    listOf(10f to 0f, 30f to -8f, 52f to -4f, 74f to -6f, 88f to 2f).forEach { (x, y) ->
        drawCircle(FestColors.PaleGold, radius = 15f, center = topLeft + Offset(x, y + 6f))
    }
    drawRoundRect(FestColors.PaleGold, topLeft + Offset(4f, 6f), Size(body.width - 8f, 14f))
}

private fun DrawScope.drawGift(topLeft: Offset) {
    drawRect(FestColors.Green, topLeft + Offset(0f, 14f), Size(56f, 46f))
    drawRect(FestColors.Red, topLeft + Offset(-3f, 0f), Size(62f, 16f))
    drawRect(FestColors.Gold, topLeft + Offset(26f, 0f), Size(10f, 60f))
    val bow = Stroke(width = 4f)
    drawOval(FestColors.Gold, topLeft + Offset(12f, -16f), Size(20f, 16f), style = bow)
    drawOval(FestColors.Gold, topLeft + Offset(30f, -16f), Size(20f, 16f), style = bow)
}

private fun DrawScope.drawConfetti() {
    val pieces = listOf(
        Triple(Offset(60f, 60f), FestColors.Red, 30f),
        Triple(Offset(110f, 30f), FestColors.Green, -20f),
        Triple(Offset(150f, 55f), FestColors.Gold, 60f),
        Triple(Offset(250f, 50f), FestColors.Gold, -50f),
        Triple(Offset(290f, 25f), FestColors.Gold, 10f),
        Triple(Offset(330f, 60f), FestColors.Red, -35f),
        Triple(Offset(350f, 170f), FestColors.Red, 45f),
        Triple(Offset(40f, 200f), FestColors.Red, -30f),
        Triple(Offset(30f, 160f), FestColors.Green, 70f),
        Triple(Offset(370f, 120f), FestColors.Green, 20f),
    )
    pieces.forEach { (center, color, angle) ->
        rotate(degrees = angle, pivot = center) {
            drawRoundRect(
                color,
                center - Offset(7f, 2.5f),
                Size(14f, 5f),
                CornerRadius(2.5f)
            )
        }
    }
    drawCircle(FestColors.Gold, radius = 4f, center = Offset(230f, 20f))
    drawCircle(FestColors.Gold, radius = 3f, center = Offset(85f, 95f))
}

/** Red circle with a white cross and a soft glow, shown when a drikkebong is not accepted. */
@Composable
internal fun FestCrossBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val factor = minOf(size.width, size.height) / 200f
        scale(scale = factor, pivot = Offset.Zero) {
            val center = Offset(100f, 100f)
            drawCircle(FestColors.Red.copy(alpha = 0.12f), radius = 78f, center = center)
            drawCircle(FestColors.Red.copy(alpha = 0.2f), radius = 68f, center = center)
            drawCircle(FestColors.Red, radius = 58f, center = center)
            val cross = Stroke(width = 12f, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(78f, 78f), Offset(122f, 122f), cross.width, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(122f, 78f), Offset(78f, 122f), cross.width, cap = StrokeCap.Round)
        }
    }
}

// The no-bong drawing is made on a 400 x 260 grid.
internal const val NO_BONG_ILLUSTRATION_ASPECT_RATIO = 400f / 260f

/** Two used drink tickets with holly, next to an empty-looking beer mug. */
@Composable
internal fun NoBongIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val factor = minOf(size.width / 400f, size.height / 260f)
        scale(scale = factor, pivot = Offset.Zero) {
            rotate(degrees = -18f, pivot = Offset(140f, 150f)) {
                drawTicket(Offset(60f, 105f))
            }
            rotate(degrees = -8f, pivot = Offset(160f, 175f)) {
                drawTicket(Offset(80f, 135f))
            }
            drawHolly(Offset(95f, 205f))
            drawBigMug(topLeft = Offset(250f, 100f), handleOnLeft = false)
        }
    }
}

private fun DrawScope.drawTicket(topLeft: Offset) {
    val size = Size(170f, 85f)
    drawRoundRect(FestColors.Gold, topLeft, size, CornerRadius(10f))
    drawRoundRect(FestColors.Cream, topLeft + Offset(6f, 6f), Size(size.width - 12f, size.height - 12f), CornerRadius(6f))
    // Perforation
    for (i in 0 until 6) {
        drawCircle(FestColors.Gold, radius = 2.5f, center = topLeft + Offset(120f, 14f + i * 12f))
    }
    // Small glass on the ticket
    drawRoundRect(FestColors.DarkGreen, topLeft + Offset(55f, 25f), Size(22f, 30f), CornerRadius(4f))
    drawRoundRect(FestColors.Gold, topLeft + Offset(59f, 33f), Size(14f, 18f), CornerRadius(2f))
    // "Used" stroke over the ticket
    drawLine(FestColors.Red.copy(alpha = 0.7f), topLeft + Offset(15f, 70f), topLeft + Offset(110f, 15f), strokeWidth = 5f, cap = StrokeCap.Round)
}

private fun DrawScope.drawHolly(center: Offset) {
    val leaf = Path().apply {
        moveTo(0f, 0f)
        quadraticTo(18f, -16f, 40f, 0f)
        quadraticTo(18f, 16f, 0f, 0f)
        close()
    }
    listOf(-35f to -10f, 35f to 10f).forEach { (angle, dx) ->
        rotate(degrees = angle, pivot = center) {
            translate(left = center.x + dx - 20f, top = center.y) { drawPath(leaf, FestColors.Green) }
        }
    }
    drawCircle(FestColors.Red, radius = 7f, center = center + Offset(-6f, -4f))
    drawCircle(FestColors.Red, radius = 7f, center = center + Offset(6f, -6f))
    drawCircle(FestColors.Red, radius = 7f, center = center + Offset(0f, 6f))
}
