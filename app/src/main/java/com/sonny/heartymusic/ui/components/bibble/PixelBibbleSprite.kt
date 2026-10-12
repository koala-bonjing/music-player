package com.sonny.heartymusic.ui.components.bibble

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/*
 * Bibble animation engine - pure Kotlin (no Android / Compose), renders into an ARGB IntArray.
 *
 * How it works
 * ------------
 * The 57x76 master sprite is cut into parts: hair-top, head (with swappable eyes + mouth patches),
 * torso, two arms, two feet. Every frame each part gets its own whole-pixel offset (and the arms a rotation),
 * so Bibble keeps crisp pixel-art edges while moving like a puppet: breathing, head lag, hair sway,
 * squash & stretch on hops, blinking, glancing, chewing, and floating particles.
 */

internal const val SPR_W = 57
internal const val SPR_H = 76

/** Transparent room around the sprite for hops, hearts, Zzz, music notes and the "!" bubble. */
internal const val MARGIN_X = 10
internal const val MARGIN_TOP = 16
internal const val MARGIN_BOTTOM = 3
internal const val OUT_W = SPR_W + MARGIN_X * 2
internal const val OUT_H = SPR_H + MARGIN_TOP + MARGIN_BOTTOM

internal enum class BibbleAction { IDLE, WALK, DANCE, SLEEP, LOVE, SNACK, SURPRISE, DANGLING }

private enum class Eyes { OPEN, HALF, CLOSED, HAPPY, LOOK_L, LOOK_R }
private enum class Mouth { SMILE, OPEN_O, OPEN_SMALL }

// ---------------------------------------------------------------------------------------------
// Layers
// ---------------------------------------------------------------------------------------------

private class Layer(val x0: Int, val y0: Int, val w: Int, val h: Int, val px: IntArray)

private fun argb(c: Char): Int = BIBBLE_ARGB[c] ?: 0

private fun layerOf(
    x0: Int, y0: Int, x1: Int, y1: Int,
    keep: (Int, Int) -> Boolean = { _, _ -> true }
): Layer {
    val w = x1 - x0
    val h = y1 - y0
    val px = IntArray(w * h)
    for (yy in 0 until h) for (xx in 0 until w) {
        val sx = x0 + xx
        val sy = y0 + yy
        if (keep(sx, sy)) px[yy * w + xx] = argb(BIBBLE_BASE_ROWS[sy][sx])
    }
    return Layer(x0, y0, w, h, px)
}

private object Parts {
    // Head = rows 11..42. Overlaps the hair-top band by two rows so the hair can move without leaving a gap.
    // The right-hand fingertips (x >= 47, y >= 39) belong to the right arm layer, so they are cut out here.
    val headRest = layerOf(0, 11, SPR_W, 43) { x, y -> !(x >= 47 && y >= 39) }
    val hairTop = layerOf(0, 0, SPR_W, 13)

    // Torso = rows 43..70, plus 3 synthetic "neck" rows above it so the head can stretch upward.
    // The outer arm pixels are cut out (arms are separate layers); a short shoulder stub stays.
    val torso: Layer = run {
        val y0 = 40
        val y1 = 71
        val px = IntArray(SPR_W * (y1 - y0))
        for (sy in y0 until y1) for (x in 0 until SPR_W) {
            if (sy < 43) {
                // neck: only where the chin row (42) has pixels, so no collar sticks out behind the head
                if (BIBBLE_BASE_ROWS[42][x] != '.') px[(sy - y0) * SPR_W + x] = argb(BIBBLE_BASE_ROWS[43][x])
            } else {
                val armOuter = sy <= 53 && (x <= 9 || x >= 47)
                if (!armOuter) px[(sy - y0) * SPR_W + x] = argb(BIBBLE_BASE_ROWS[sy][x])
            }
        }
        Layer(0, y0, SPR_W, y1 - y0, px)
    }

    val footL = layerOf(14, 71, 25, 76)
    val footMid = layerOf(25, 71, 31, 76)
    val footR = layerOf(31, 71, 44, 76)
    val armL = layerOf(0, 43, 13, 54)
    val armR = layerOf(44, 39, SPR_W, 54)

    private val headCache = HashMap<Int, Layer>()

    fun head(eyes: Eyes, mouth: Mouth): Layer =
        headCache.getOrPut(eyes.ordinal * 8 + mouth.ordinal) { makeHead(eyes, mouth) }

    private fun makeHead(eyes: Eyes, mouth: Mouth): Layer {
        val base = headRest
        val px = base.px.copyOf()
        fun paint(p: SpritePatch) {
            for (j in p.rows.indices) for (i in p.rows[j].indices) {
                val c = p.rows[j][i]
                if (c == '.') continue
                val lx = p.x + i - base.x0
                val ly = p.y + j - base.y0
                if (lx in 0 until base.w && ly in 0 until base.h) px[ly * base.w + lx] = argb(c)
            }
        }
        if (eyes != Eyes.OPEN) BIBBLE_EYE_PATCHES[eyes.name]?.let { paint(it.first); paint(it.second) }
        if (mouth != Mouth.SMILE) BIBBLE_MOUTH_PATCHES[mouth.name]?.let { paint(it) }
        return Layer(base.x0, base.y0, base.w, base.h, px)
    }
}

// ---------------------------------------------------------------------------------------------
// Tiny sprites (particles + props)
// ---------------------------------------------------------------------------------------------

private val ART_HEART_S = arrayOf(".h.h.", "hhhhh", "hhhhh", ".hhh.", "..h..")
private val ART_Z_S = arrayOf("AAAA", "..A.", ".A..", "AAAA")
private val ART_Z_L = arrayOf("AAAAA", "...A.", "..A..", ".A...", "AAAAA")
private val ART_NOTE = arrayOf("..NNN", "..N.N", "..N..", "..N..", "NNN..", "NNN..")
private val ART_BANG = arrayOf("KK", "KK", "KK", "KK", "..", "KK")

private val ART_CHEST_HEART_BIG = arrayOf(
    ".hhh...hhh.",
    "hhhhh.hhhhh",
    "wwwwwwwwwww",
    ".wwwwwwwww.",
    "..wwwwwww..",
    "...wwwww...",
    "....www....",
    ".....w....."
)
private val ART_CHEST_HEART_SMALL = arrayOf(
    ".hh.hh.",
    "hhhhhhh",
    "wwwwwww",
    ".wwwww.",
    "..www..",
    "...w..."
)

// strawberry: 9x8 with seeds ('y'); stages 0..3 are progressively bitten from the right
private val ART_BERRY_FULL = arrayOf(
    "..g.g.g..",
    ".ggggggg.",
    ".sssssss.",
    ".sysssys.",
    ".ssssyss.",
    "..sysss..",
    "...sss...",
    "....s...."
)
private val ART_BERRY: Array<Array<String>> = Array(4) { k ->
    val keep = 8 - 2 * k   // keep columns 0..keep
    Array(ART_BERRY_FULL.size) { j -> String(CharArray(9) { i -> if (i <= keep) ART_BERRY_FULL[j][i] else '.' }) }
}

private fun drawArt(dst: IntArray, art: Array<String>, x: Int, y: Int) {
    for (j in art.indices) for (i in art[j].indices) {
        val c = art[j][i]
        if (c == '.') continue
        val dx = x + i
        val dy = y + j
        if (dx in 0 until OUT_W && dy in 0 until OUT_H) dst[dy * OUT_W + dx] = argb(c)
    }
}

// ---------------------------------------------------------------------------------------------
// Blitting
// ---------------------------------------------------------------------------------------------

private fun blit(dst: IntArray, l: Layer, ox: Int, oy: Int) {
    for (y in 0 until l.h) {
        val dy = oy + l.y0 + y
        if (dy < 0 || dy >= OUT_H) continue
        for (x in 0 until l.w) {
            val c = l.px[y * l.w + x]
            if (c == 0) continue
            val dx = ox + l.x0 + x
            if (dx in 0 until OUT_W) dst[dy * OUT_W + dx] = c
        }
    }
}

/** Nearest-neighbour rotation of [l] about its source pivot; the pivot lands on (dstX, dstY). Positive = clockwise. */
private fun blitRot(dst: IntArray, l: Layer, pivX: Float, pivY: Float, dstX: Float, dstY: Float, deg: Float) {
    val th = deg * (PI.toFloat() / 180f)
    val c = cos(th)
    val s = sin(th)
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    val cx = floatArrayOf(l.x0.toFloat(), (l.x0 + l.w).toFloat(), l.x0.toFloat(), (l.x0 + l.w).toFloat())
    val cy = floatArrayOf(l.y0.toFloat(), l.y0.toFloat(), (l.y0 + l.h).toFloat(), (l.y0 + l.h).toFloat())
    for (k in 0 until 4) {
        val rx = cx[k] - pivX
        val ry = cy[k] - pivY
        val ex = dstX + rx * c - ry * s
        val ey = dstY + rx * s + ry * c
        minX = min(minX, ex); maxX = max(maxX, ex)
        minY = min(minY, ey); maxY = max(maxY, ey)
    }
    for (y in floor(minY).toInt()..ceil(maxY).toInt()) {
        if (y < 0 || y >= OUT_H) continue
        for (x in floor(minX).toInt()..ceil(maxX).toInt()) {
            if (x < 0 || x >= OUT_W) continue
            val vx = (x + 0.5f) - dstX
            val vy = (y + 0.5f) - dstY
            val sx = pivX + vx * c + vy * s
            val sy = pivY - vx * s + vy * c
            val lx = floor(sx - l.x0).toInt()
            val ly = floor(sy - l.y0).toInt()
            if (lx < 0 || ly < 0 || lx >= l.w || ly >= l.h) continue
            val col = l.px[ly * l.w + lx]
            if (col != 0) dst[y * OUT_W + x] = col
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pose + particles
// ---------------------------------------------------------------------------------------------

private class Pose {
    var rootDx = 0; var rootDy = 0
    var headDx = 0; var headDy = 0
    var hairDx = 0; var hairDy = 0
    var torsoDx = 0; var torsoDy = 0
    var armL = 0f; var armR = 0f   // degrees raised from the spread-out rest pose (negative = lowered)
    var armDy = 0
    var footLDx = 0; var footLDy = 0
    var footRDx = 0; var footRDy = 0
    var eyes = Eyes.OPEN
    var mouth = Mouth.SMILE
    var blush = false
    var allowBlink = true
    var heartBig = false        // LOVE: which chest-heart size to show (heartbeat)
    var showChestHeart = false
    var berryStage = -1         // SNACK: -1 = no strawberry
    var berryY = 44
    var showBang = false
    var bangDy = 0

    fun reset() {
        rootDx = 0; rootDy = 0; headDx = 0; headDy = 0; hairDx = 0; hairDy = 0
        torsoDx = 0; torsoDy = 0; armL = 0f; armR = 0f; armDy = 0
        footLDx = 0; footLDy = 0; footRDx = 0; footRDy = 0
        eyes = Eyes.OPEN; mouth = Mouth.SMILE; blush = false; allowBlink = true
        heartBig = false; showChestHeart = false; berryStage = -1; berryY = 44
        showBang = false; bangDy = 0
    }
}

private enum class PKind { HEART, Z, NOTE }

private class Particle(
    val kind: PKind,
    var x: Float,
    var y: Float,
    val vy: Float,
    val life: Float,
    val phase: Float,
    var age: Float = 0f
)

private const val TAU = (2.0 * PI).toFloat()
private fun sinT(t: Float, period: Float, phase: Float = 0f): Float = sin(TAU * t / period + phase)
private fun Float.r(): Int = roundToInt()

// ---------------------------------------------------------------------------------------------
// Engine
// ---------------------------------------------------------------------------------------------

internal class BibbleEngine(seed: Long = 20260612L) {
    private val rnd = Random(seed)
    private val pose = Pose()
    private val particles = ArrayList<Particle>()

    private var lastT = 0f
    private var lastAction: BibbleAction? = null
    private var actionT0 = 0f
    private var spawnAt = 0f
    private var spawnSide = 0

    private var blinkAt = 1.6f
    private var blinkStart = -10f
    private var blinkDouble = false
    private var glanceAt = 3.2f
    private var glanceUntil = -1f
    private var glanceDir = 0
    private var hopAt = 5f
    private var hopStart = -10f
    private var tick = 0

    /** Render one frame (OUT_W x OUT_H ARGB) for time [t] seconds. */
    fun render(t: Float, action: BibbleAction, out: IntArray) {
        update(t, action)
        out.fill(0)
        val p = pose
        val ox = MARGIN_X + p.rootDx
        val oy = MARGIN_TOP + p.rootDy

        blit(out, Parts.footMid, ox, oy)
        blit(out, Parts.torso, ox + p.torsoDx, oy + p.torsoDy)
        blit(out, Parts.footL, ox + p.footLDx, oy + p.footLDy)
        blit(out, Parts.footR, ox + p.footRDx, oy + p.footRDy)

        val hx = ox + p.headDx
        val hy = oy + p.headDy
        blit(out, Parts.head(p.eyes, p.mouth), hx, hy)
        blit(out, Parts.hairTop, hx + p.hairDx, hy + p.hairDy)

        if (p.blush) {
            val blushC = argb('R')
            for (i in 0 until 3) for (j in 0 until 2) {
                put(out, hx + 13 + i, hy + 35 + j, blushC)
                put(out, hx + 40 + i, hy + 35 + j, blushC)
            }
        }

        // arms: left arm extends left of its pivot (clockwise = up); right arm extends right (counter-clockwise = up)
        val tx = ox + p.torsoDx
        val ty = oy + p.torsoDy + p.armDy
        blitRot(out, Parts.armL, 11f, 47f, tx + 11f, ty + 47f, p.armL)
        blitRot(out, Parts.armR, 45f, 47f, tx + 45f, ty + 47f, -p.armR)

        // props
        if (p.showChestHeart) {
            if (p.heartBig) drawArt(out, ART_CHEST_HEART_BIG, tx + 23, ty + 47)
            else drawArt(out, ART_CHEST_HEART_SMALL, tx + 25, ty + 48)
        }
        if (p.berryStage in 0..3) drawArt(out, ART_BERRY[p.berryStage], hx + 23, hy + p.berryY)
        if (p.showBang) drawArt(out, ART_BANG, MARGIN_X + 27, 2 + p.bangDy)

        // particles
        for (q in particles) {
            val left = q.life - q.age
            if (left < 0.35f && (floor(q.age * 18f).toInt() % 2 == 0)) continue   // pixel flicker instead of alpha fade
            val sway = sin(q.age * 3.2f + q.phase) * (if (q.kind == PKind.Z) 2.2f else 3f)
            val px = (q.x + sway).r()
            val py = q.y.r()
            when (q.kind) {
                PKind.HEART -> drawArt(out, ART_HEART_S, px, py)
                PKind.NOTE -> drawArt(out, ART_NOTE, px, py)
                PKind.Z -> drawArt(out, if (q.age < q.life * 0.45f) ART_Z_S else ART_Z_L, px, py)
            }
        }
    }

    private fun put(dst: IntArray, x: Int, y: Int, c: Int) {
        if (x in 0 until OUT_W && y in 0 until OUT_H) dst[y * OUT_W + x] = c
    }

    // -------------------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------------------

    private fun update(t: Float, action: BibbleAction) {
        val dt = (t - lastT).coerceIn(0f, 0.1f)
        lastT = t
        tick++
        if (action != lastAction) {
            lastAction = action
            actionT0 = t
            spawnAt = t + 0.4f
            hopAt = t + 4f + rnd.nextFloat() * 4f
        }
        val tA = t - actionT0
        val p = pose
        p.reset()

        when (action) {
            BibbleAction.IDLE -> idle(p, t)
            BibbleAction.WALK -> walk(p, t)
            BibbleAction.DANCE -> dance(p, t)
            BibbleAction.SLEEP -> sleep(p, t)
            BibbleAction.LOVE -> love(p, t)
            BibbleAction.SNACK -> snack(p, t, tA)
            BibbleAction.SURPRISE -> surprise(p, t, tA)
            BibbleAction.DANGLING -> dangling(p, t)
        }

        // blinking + glancing (only while the base expression is "eyes open")
        if (p.eyes == Eyes.OPEN && p.allowBlink) p.eyes = liveEyes(t)

        spawnParticles(t, action)
        for (q in particles) {
            q.age += dt
            q.y += q.vy * dt
        }
        particles.removeAll { it.age >= it.life }
    }

    private fun liveEyes(t: Float): Eyes {
        if (t >= blinkAt) {
            blinkStart = t
            blinkAt = t + 2.2f + rnd.nextFloat() * 3.4f
            blinkDouble = rnd.nextFloat() < 0.2f
        }
        val bt = t - blinkStart
        blinkPhase(bt)?.let { return it }
        if (blinkDouble) blinkPhase(bt - 0.32f)?.let { return it }

        if (t >= glanceAt) {
            glanceUntil = t + 0.5f + rnd.nextFloat() * 0.7f
            glanceDir = if (rnd.nextBoolean()) -1 else 1
            glanceAt = t + 3f + rnd.nextFloat() * 4f
        }
        if (t < glanceUntil) return if (glanceDir < 0) Eyes.LOOK_L else Eyes.LOOK_R
        return Eyes.OPEN
    }

    private fun blinkPhase(bt: Float): Eyes? = when {
        bt < 0f -> null
        bt < 0.05f -> Eyes.HALF
        bt < 0.14f -> Eyes.CLOSED
        bt < 0.20f -> Eyes.HALF
        else -> null
    }

    private fun spawnParticles(t: Float, action: BibbleAction) {
        if (t < spawnAt) return
        when (action) {
            BibbleAction.LOVE -> {
                spawnAt = t + 0.45f
                particles += Particle(
                    PKind.HEART, MARGIN_X + 14f + rnd.nextFloat() * 28f, MARGIN_TOP + 4f,
                    -10f, 1.7f, rnd.nextFloat() * 6f
                )
            }
            BibbleAction.SLEEP -> {
                spawnAt = t + 2.0f
                particles += Particle(PKind.Z, MARGIN_X + 44f, MARGIN_TOP + 10f, -6.5f, 3.3f, 0f)
            }
            BibbleAction.DANCE -> {
                spawnAt = t + 1.0f
                spawnSide = 1 - spawnSide
                val x = if (spawnSide == 0) 1f else (OUT_W - 7f)
                particles += Particle(PKind.NOTE, x, MARGIN_TOP + 34f, -9f, 1.8f, rnd.nextFloat() * 6f)
            }
            else -> spawnAt = t + 1f
        }
    }

    // -------------------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------------------

    /** Breathing, weight shift, hair sway and a tiny arm drift - the base of every "alive" pose. */
    private fun breathe(p: Pose, t: Float, speed: Float = 1f) {
        val b = sinT(t * speed, 3.4f)
        p.headDy = (-0.9f * b).r()
        p.hairDy = (0.7f * sinT(t * speed, 3.4f, -0.9f)).r()
        p.hairDx = (1.2f * sinT(t * speed, 2.6f, 1.0f)).r()
        p.headDx = (1.2f * sinT(t * speed, 6.8f, -0.3f)).r()
        p.torsoDx = (0.7f * sinT(t * speed, 6.8f)).r()
        val a = 3f * sinT(t * speed, 3.4f, 0.6f)
        p.armL = 3f + a
        p.armR = 3f - a
    }

    private fun idle(p: Pose, t: Float) {
        breathe(p, t)
        // an occasional happy little hop
        if (t >= hopAt) {
            hopStart = t
            hopAt = t + 8f + rnd.nextFloat() * 7f
        }
        val u = (t - hopStart) / 0.5f
        if (u in 0f..1f) {
            val h = 4f * u * (1f - u)
            p.rootDy = -(3f * h).r()
            p.armL += 22f * h
            p.armR += 22f * h
            p.hairDy -= (1.5f * h).r()
            if (u < 0.12f || u > 0.88f) { p.headDy += 1; p.torsoDy = 1 }
            if (h > 0.9f) p.mouth = Mouth.OPEN_SMALL
        }
    }

    private fun walk(p: Pose, t: Float) {
        val step = sinT(t, 1.0f)                 // one full cycle = two steps
        p.rootDy = -(abs(step)).r()               // bob on every step
        p.torsoDx = (1.5f * step).r()
        p.headDx = (1.8f * sinT(t, 1.0f, -0.5f)).r()
        p.hairDx = (1.5f * sinT(t, 1.0f, -1.3f)).r()
        p.hairDy = (0.8f * sinT(t, 0.5f, -1f)).r()
        p.armL = 10f * step
        p.armR = -10f * step
        if (step > 0.25f) { p.footLDy = -2; p.footLDx = 1 }
        if (step < -0.25f) { p.footRDy = -2; p.footRDx = -1 }
        if (abs(step) < 0.2f) p.headDy = 1
    }

    private fun dance(p: Pose, t: Float) {
        val hop = abs(sin(PI.toFloat() * t / 0.5f))    // 0..1, one hop per beat (120 bpm)
        val sway = sinT(t, 1.0f)
        p.rootDy = -(3f * hop).r()
        if (hop < 0.15f) { p.headDy += 1; p.torsoDy = 1 }        // squash on landing
        if (hop > 0.85f) p.headDy -= 1                           // stretch at the top
        p.torsoDx = (2f * sway).r()
        p.headDx = (3f * sinT(t, 1.0f, -0.6f)).r()
        p.hairDx = (2f * sinT(t, 1.0f, -1.4f)).r()
        p.hairDy = (-1.2f * sinT(t, 0.5f, -1.0f)).r()
        p.armL = 28f + 26f * sinT(t, 1.0f)
        p.armR = 28f + 26f * sinT(t, 1.0f, PI.toFloat())
        p.armDy = if (hop < 0.15f) 1 else 0
        if (sway > 0.4f) p.footLDy = -1
        if (sway < -0.4f) p.footRDy = -1
        val bar = t % 4f
        if (bar in 2.5f..3.5f) p.eyes = Eyes.HAPPY           // closes eyes and grooves once a bar
        if (t % 2f < 0.25f) p.mouth = Mouth.OPEN_SMALL        // little "ooh"
    }

    private fun sleep(p: Pose, t: Float) {
        val b = sinT(t, 4.6f)
        val nod = max(0f, sinT(t, 7.3f)).pow(8)
        p.eyes = Eyes.CLOSED
        p.headDy = 1 + (0.9f * b).r() + (2f * nod).r()
        p.headDx = -1 + (0.6f * sinT(t, 9.2f)).r()
        p.hairDy = 1 + (0.6f * sinT(t, 4.6f, -0.8f)).r()
        p.hairDx = 0
        p.armL = -7f + 2f * b
        p.armR = -7f + 2f * b
        p.mouth = if (b < -0.3f) Mouth.OPEN_SMALL else Mouth.SMILE
    }

    private fun love(p: Pose, t: Float) {
        breathe(p, t, 1.2f)
        p.eyes = Eyes.HAPPY
        p.blush = true
        p.headDx = (2f * sinT(t, 2.2f)).r()
        p.headDy = (-0.8f * sinT(t, 1.1f)).r()
        p.torsoDx = (1f * sinT(t, 2.2f, -0.6f)).r()
        p.armL = 18f + 4f * sinT(t, 1.1f)
        p.armR = 18f + 4f * sinT(t, 1.1f)
        p.showChestHeart = true
        p.heartBig = sinT(t, 1.25f) > 0.15f        // heartbeat
    }

    private fun snack(p: Pose, t: Float, tA: Float) {
        breathe(p, t, 1.1f)
        val cycle = 6.4f
        val c = tA % cycle
        val bites = floatArrayOf(1.0f, 2.3f, 3.6f, 4.9f)
        var eaten = 0
        for (b in bites) if (c >= b) eaten++
        p.berryStage = if (eaten >= 4) -1 else eaten
        var lift = 0f
        for (b in bites) {
            val u = c - b
            val v = if (u < 0f) (u + 0.3f) / 0.3f else 1f - (u / 0.35f)
            lift = max(lift, v.coerceIn(0f, 1f))
        }
        p.berryY = 44 - (7f * lift).r()
        var chewing = false
        for (b in bites) if (c >= b + 0.35f && c < b + 1.0f) chewing = true
        if (c >= bites.last() + 0.35f && c < cycle - 0.2f) chewing = true
        val beat = ((tA * 6f).toInt() % 2 == 0)
        if (lift > 0.8f) p.mouth = Mouth.OPEN_O
        else if (chewing) { p.mouth = if (beat) Mouth.OPEN_SMALL else Mouth.SMILE; p.eyes = Eyes.HAPPY; if (beat) p.headDy += 1 }
        p.armL = 12f
        p.armR = 12f
    }

    private fun surprise(p: Pose, t: Float, tA: Float) {
        p.mouth = Mouth.OPEN_O
        p.allowBlink = false
        p.showBang = tA < 2.2f
        p.bangDy = (1.2f * sinT(tA, 0.5f)).r()
        when {
            tA < 0.12f -> {                                   // anticipation: crouch
                p.headDy = 2; p.torsoDy = 1; p.hairDy = 1
            }
            tA < 0.55f -> {                                   // jump
                val u = (tA - 0.12f) / 0.43f
                val h = 4f * u * (1f - u)
                p.rootDy = -(7f * h).r()
                p.headDy = -(2f * h).r()
                p.hairDy = -2
                p.armL = 10f + 50f * h
                p.armR = 10f + 50f * h
            }
            tA < 0.7f -> { p.headDy = 2; p.torsoDy = 1; p.hairDy = 1 }   // land
            else -> {
                val k = tA - 0.7f
                val damp = exp(-k * 4f)
                val w = sin(k * 30f) * damp
                p.headDy = (1.5f * w).r()
                p.hairDy = (-1.5f * w).r()
                p.armL = 8f + 10f * damp
                p.armR = 8f + 10f * damp
                if (tA < 1.4f) p.hairDx = ((tick % 2) * 2 - 1)        // tiny tremble of surprise
                if (tA > 1.6f) { p.allowBlink = true; p.mouth = Mouth.OPEN_SMALL }
            }
        }
    }

    private fun dangling(p: Pose, t: Float) {
        p.torsoDx = (3f * sinT(t, 1.4f)).r()
        p.headDx = (-2f * sinT(t, 1.4f, 0.3f)).r()
        p.hairDx = (2f * sinT(t, 1.4f, -0.8f)).r()
        p.hairDy = (1f * sinT(t, 0.7f, -0.5f)).r()
        p.headDy = (-0.8f * sinT(t, 2.8f)).r()
        p.armL = 62f + 4f * sinT(t, 1.4f)
        p.armR = 62f - 4f * sinT(t, 1.4f)
        val kick = sinT(t, 0.7f)
        p.footLDy = (2f * kick).r()
        p.footRDy = (-2f * kick).r()
        p.mouth = Mouth.OPEN_SMALL
    }
}

/** Public animation "frames" - kept for source compatibility; each one now selects a living animation. */
enum class PixelBibbleFrame {
    IDLE, BLINK, WALK_1, WALK_2, DANCE_1, DANCE_2, SLEEP, LOVE, SNACK, DANGLING, SURPRISE
}

/** char -> ARGB. 'A'..'V' = the sprite palette, w/h = hearts, s/g = strawberry. */
internal val BIBBLE_ARGB: Map<Char, Int> = mapOf(
    '.' to 0,
    'A' to (0xFFAE7FF0L).toInt(), // lavender (spots, ear edges)
    'B' to (0xFFFB7AFFL).toInt(), // bright pink-violet (main hair)
    'C' to (0xFF3E8687L).toInt(), // dark teal (body shadow, lower edge)
    'D' to (0xFF9CE3E0L).toInt(), // light mint (face highlight)
    'E' to (0xFF5BA8A7L).toInt(), // mid teal (shading)
    'F' to (0xFF72ADB5L).toInt(), // steel teal (shading)
    'G' to (0xFF84C3CCL).toInt(), // soft blue-teal
    'H' to (0xFF94CCD4L).toInt(), // soft blue-teal (lighter)
    'I' to (0xFF80DFE2L).toInt(), // bright aqua (main body fur)
    'J' to (0xFF7ED1D7L).toInt(), // aqua (body mid-tone)
    'K' to (0xFFC535DEL).toInt(), // deep magenta (hair shadow)
    'L' to (0xFF70C0C1L).toInt(), // teal (shading)
    'M' to (0xFF98A3DAL).toInt(), // periwinkle (spot shading)
    'N' to (0xFFDD62F8L).toInt(), // pink-violet (hair mid-tone)
    'O' to (0xFF5A5C9AL).toInt(), // slate purple (ear and outline accents)
    'P' to (0xFF638FA5L).toInt(), // blue-gray (shading)
    'Q' to (0xFFCEEDEEL).toInt(), // very light mint (muzzle and chest highlight)
    'R' to (0xFFE7A1F9L).toInt(), // light pink-lavender (hair highlight, blush)
    'S' to (0xFF492E86L).toInt(), // deep purple (dark accents, feet, closed eyelids)
    'T' to (0xFF000474L).toInt(), // navy (iris)
    'U' to (0xFF000000L).toInt(), // black (pupil)
    'V' to (0xFFC7C8DDL).toInt(), // pale gray-lavender (eye white, teeth)
    'w' to (0xFFFF1493L).toInt(), // deep pink heart
    'h' to (0xFFFF69B4L).toInt(), // light pink heart
    's' to (0xFFEF4444L).toInt(), // strawberry red
    'g' to (0xFF10B981L).toInt(), // strawberry green leaf
    'y' to (0xFFFFE08AL).toInt(), // strawberry seeds
)

/** The master 57x76 sprite. */
internal val BIBBLE_BASE_ROWS: List<String> = listOf(
    ".........................................................",
    ".........................................................",
    ".........................................................",
    "..........................B..............................",
    "......................RRRBBBBBBBR.R......................",
    "..................BBBBBBBBBBBBBBBBBBRR...................",
    "................RBBBBBBBBBBBBBBBBBBBBBR..................",
    "..............RRBBBBBBBBBBBBBBBBBBBBBBBRR................",
    "............RRBBBBBBBBBBBBBBBBBBBBBBBBBBR................",
    "............RBBBBBBBNBBBBBBBBBBBBBBBBBBBBR...............",
    "...........RBBBBBBNNNNBBBBBBBBBBBBBBBBBBBBRR.............",
    "...........BBBBBBBNNNNNBBBBBBBNNBBBBBBBBBBBR.............",
    "..........RBBBBBBBNNNNNNNBBBBNNNNNNNNNNNNNNRR............",
    "........RRBBBBNNBBNNNNNNNNNNNKKKKKKKKKKKNNAAR............",
    ".........BBBBNKKKNNNKKKKKKNNKKKKKKKKKKKKKAAAR............",
    ".........BBNNNKKKKKKKKKKKKKKKKKKKKMMMFPKKKAAR............",
    "........RNNNNKKKKKKKKKKKKKKKKKKMGFGGGGFOKKAAA............",
    ".......RNNNNKKKKKOOKKKKKKKKAMMMGFFFFLLGGMKAAAA...........",
    "........NNKKKKSOPFGMMAGMHMMHHHGFFFOOPPFGGAAAAA...........",
    "........NKKKKSOFFGGGGGHHHDDDDHHFPPPCPCPHHMAAAA...........",
    ".......BKKKKSPFFFFGGGGHHDDDDDDDGLLGJHHHGGJMKKKA..........",
    "........KKKSSFFFPCCFFLGHDDDDDDDJJHDDGGHDJJFKKAA..........",
    ".......RKKKSPFFPPEFFFLGDDDDDDDDIJDGPCCFLJGGOAAAA.........",
    "........NKSSPFPFGGGGFLHDDDDDDDDJIPVQQMSCEGGGPSOOOA.......",
    "........NSSOFGGLFHLGLFGDDDDDDDDJFQQQQQQSFGHHLTTOOO.......",
    "........RKOOELFPSOFPPFGHDDDQQDDJHPSSQQQVCDHHJOSOO........",
    "........AAOPFFPSVQQQQFLHDDQQQQDJOTTOSQQQPJJHIPOO.........",
    ".......AOSCEGFCMQQMTTOEGDDQQQQDJTTUSTOQQHJDDDG...........",
    "......AOTUSEGFCQQVTTSSCLDDQQQQDGTUUUTTQQVLDDDD...........",
    ".....OSSSSOELJFQQPTUUTTLHDQQQQDLTUUUTSQQVHDDDDD..........",
    ".......OOOPLJIHQQOTUUUTEHQQQQQDJOTUTTMQQDDDHHHJD.........",
    ".........OEGJIHQQPTTUUTEHDQQQQDDHSOPHQQQDDHHHHHH.........",
    ".........FLHDDGVQVTTTTSFLHDVVDDDDDDDDDDDDDHHHGGL.........",
    ".........GGHHHGHQQMOPFHJJFRRRADDDDDDDHHHDHGGGGGG.........",
    ".........FFGHDQQQQDDDDDDHSKRKSDDDDDEPFFFLGGGGLEG.........",
    "........GFFGDDDDDDDDDDDJHPSOSEDDDDGCCCPPFFFFFEE..........",
    "........GEEGHHHHHGFEFGGGGLECELGLFCUCEEEEEEEEEEF..........",
    "........LEELGLFFFPCTSCEEEECCCPCSUUCHGGLEEEEEEEV..........",
    "........VEEEFFFEEPPECUUUUSVVVVSUUSDHHGFEEEECCF...........",
    ".........LEEEEEEEFFFFPUUUUUVVUSSOHGHGFEEECCCG............",
    "..........FEECCEEEFFFFFPSUSVVVMGGFFFFEECCCCG......F......",
    "...........ECCCCPEEFFFFFGGGGHHLFEEFEPCCCCP........G......",
    "............FCCCCCCPEEEEPPEEECCCEEECCCCCC........LF..GHF.",
    ".....FL.......CCCCCCCCCCCCCCCCCCCCCCCCCCCCLHD...FHHGHEC..",
    ".HH..PG......FPCCCCCCCCCCCCCCCCCCCEEEEPPELLLGGHJLDHHHH...",
    ".PFHHGHGFHHGLEECCCCCCCCCCCCCCCCEELJGGGMFLLLJGHDDHGFHHHHHM",
    "...PDGHHGGLFLJLLPPPCELLLFEPOOOALJJDHGHJJGHJJIDDDDHGHHFCC.",
    "VHGGHLFFHDHGGIJGMFLFAMGJJGAANNNAIDHHHMHJJHIIDDDHHVDDJL...",
    "FEFFHHLLHHDGGJJGGGMAAMHDIJAANNNAJDDVRRMJJIIDDLFFFPFFEFGM.",
    "..FFHHDGGHHGGJJJJHAARRDIIIARNNNAJIDRRRAJLJJGLEECCP....C..",
    ".GGECCCCFEEFLJJJJHARRRIIIIMANAAMIIDVAAADIEEEEEECF........",
    "..C....FECCEEEJLIHARRMIIIIJMAMMIIIIDVMHJHLEEEEEG.........",
    "........FCCCCLLEIJRAAMJIIIIJMMMIIIIIDDIHAPCCCF...........",
    "..........FECCCLJJAAAAJIIIIIIIMHIIIIIIIMAAP..............",
    "............G.ELGGAAAMIIIIIIIIMMIIIIIIIMAAF..............",
    "..............EMMMMMMJIIIIIIIIAAMIIIIIIIMAF..............",
    ".............LFAMMGJJIIIIIIIIHAAAAAMIIIIIGLH.............",
    ".............LMAMJJJJJJJIIIIIMAAAARRMIIIIJGG.............",
    ".............MAMDDJJJJJAJIIIIMAAARRRAIIIIJMM.............",
    ".............AAGIDIJLMAAMJJJJHMAMRRRRHIIIJJM.............",
    "............MAALIIIJGAAAAJJJJIIMIVMHVMIAHJJJ.............",
    "............MAFLJJJJAAANAMJJJIJHIIIIIMMAAJJJ.............",
    "............FMEEJJJJAAANRAIJJIMHIIIIIHAAAALLG............",
    "............GFPPLLJGAAAAAMJJJJAAIJIIIJAAAALF.............",
    "............GFFPELLLMAAAAJJJJAAAAMJIJJAAAFMM.............",
    ".............FFPEEEFMAAMLJJJGAAAAAMJLLMAAEMM.............",
    ".............FFFLEPMMMFLLLLLMAAAAAAELEFPFLF..............",
    ".............LFELLEOAFEEELLLFAAAAAAEECCELLF..............",
    "..............LEEEECOCEEEEEEPAAAAAOCCCEELLH..............",
    "...............EEEEPCCCCEEECOOOOOOCCCCEEE................",
    "...............HCCCCSCCCCECCCOOOSCSCCSOPG................",
    "................OAAOOOSSCH.HG..CTTSOOAAAM................",
    "...............AAAAAAASTP.......TSKAAAAAA................",
    "...............AAAAAAAOSM.......SOAAAAAAAA...............",
    "...............OSAASAASSPFFFFFFPOSAAOOAOO................",
    "................M...OO.............O.OO..................",
)

internal class SpritePatch(val x: Int, val y: Int, val rows: List<String>)

/** Eye variants: name -> (left eye patch, right eye patch). Painted over the head layer. */
internal val BIBBLE_EYE_PATCHES: Map<String, Pair<SpritePatch, SpritePatch>> = mapOf(
    "HALF" to Pair(
        SpritePatch(14, 24, listOf("HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD", "CSSSSSSSSSD", "FQQPTUUTTLH", "HQQOTUUUTEH", "HQQPTTUUTEH", "GVQVTTTTSFL", "GHQQMOPFHJJ", "QQQQDDDDDDH")),
        SpritePatch(30, 24, listOf("DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DSSSSSSSSSSL", "DLTUUUTSQQVH", "DJOTUTTMQQDD", "DDHSOPHQQQDD", "DDDDDDDDDDDD", "DDDDDDDHHHDH", "DDDDDEPFFFLG"))
    ),
    "CLOSED" to Pair(
        SpritePatch(14, 24, listOf("HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD", "HSSHHDDDSSD", "HHHSSSSSDDD", "HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD")),
        SpritePatch(30, 24, listOf("DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DSSDDDDDHSSH", "DDDSSSSSSHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH"))
    ),
    "HAPPY" to Pair(
        SpritePatch(14, 24, listOf("HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHSSSDDDD", "HHSSHDDSSDD", "HSHHHDDDDSD", "HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD", "HHHHHDDDDDD")),
        SpritePatch(30, 24, listOf("DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDSSSSHHHH", "DDSSDDDDSSHH", "DSDDDDDDHHSH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH", "DDDDDDDDHHHH"))
    ),
    "LOOK_L" to Pair(
        SpritePatch(14, 24, listOf("GLFHLGLFGDD", "FPSOFPPFGHD", "PSVQQQQFLHD", "CMQQTTQOEGD", "CQQTTQSSCLD", "FQQTUUTTQLH", "HQQTUUUTQEH", "HQQTTUUTQEH", "GVQTTTTQSFL", "GHQQMOPFHJJ", "QQQQDDDDDDH")),
        SpritePatch(30, 24, listOf("DJFQQQQQQSFG", "DJHPSSQQQVCD", "DJTTQOSQQQPJ", "DTTUQTQOQQHJ", "DTUUUTTQQQVL", "DTUUUTQSQQVH", "DJTUTTQMQQDD", "DDHSOPHQQQDD", "DDDDDDDDDDDD", "DDDDDDDHHHDH", "DDDDDEPFFFLG"))
    ),
    "LOOK_R" to Pair(
        SpritePatch(14, 24, listOf("GLFHLGLFGDD", "FPSOFPPFGHD", "PSVQQQQFLHD", "CMQQMQTTEGD", "CQQVQTTSCLD", "FQQPQTUUTTH", "HQQOQTUUUTH", "HQQPQTTUUTH", "GVQVQTTTTFL", "GHQQMOPFHJJ", "QQQQDDDDDDH")),
        SpritePatch(30, 24, listOf("DJFQQQQQQSFG", "DJHPSSQQQVCD", "DJOQTTSQQQPJ", "DJQTTUQTQQHJ", "DGQTUUUTTQVL", "DLQTUUUTQQVH", "DJOQTUTTQQDD", "DDHSOPHQQQDD", "DDDDDDDDDDDD", "DDDDDDDHHHDH", "DDDDDEPFFFLG"))
    ),
)

/** Mouth variants painted over the head layer (the default smile is part of the master sprite). */
internal val BIBBLE_MOUTH_PATCHES: Map<String, SpritePatch> = mapOf(
    "OPEN_O" to SpritePatch(20, 36, listOf("EFGGGGLECELGLFCE", "EECEEEESSSPCEEEC", "ECEEEESUUUSEEEED", "FFPEEESUUUSEEEHG", "FFFFPEESSSEEGGFF", "FFFFFGGGGHHLFEEF")),
    "OPEN_SMALL" to SpritePatch(20, 36, listOf("EFGGGGLECELGLFCE", "EECEEEECCCPCEEEC", "ECEEEEESUSEEEEED", "FFPEEEESUSEEEEHG", "FFFFPEEEEEEEGGFF", "FFFFFGGGGHHLFEEF")),
)

/**
 * Kept so any other code that used the old palette map still compiles.
 * Key 'A'..'V' = sprite colors, 'w'/'h' = hearts, 's'/'g'/'y' = strawberry.
 */
val BIBBLE_57X76_PALETTE: Map<Char, Color> =
    BIBBLE_ARGB.mapValues { (_, argb) -> if (argb == 0) Color.Transparent else Color(argb) }

/** 30 fps pixel-art cadence; the engine renders a frame only when this step changes. */
private const val FRAME_NANOS = 33_333_333L

private fun PixelBibbleFrame.toAction(): BibbleAction = when (this) {
    PixelBibbleFrame.IDLE, PixelBibbleFrame.BLINK -> BibbleAction.IDLE
    PixelBibbleFrame.WALK_1, PixelBibbleFrame.WALK_2 -> BibbleAction.WALK
    PixelBibbleFrame.DANCE_1, PixelBibbleFrame.DANCE_2 -> BibbleAction.DANCE
    PixelBibbleFrame.SLEEP -> BibbleAction.SLEEP
    PixelBibbleFrame.LOVE -> BibbleAction.LOVE
    PixelBibbleFrame.SNACK -> BibbleAction.SNACK
    PixelBibbleFrame.DANGLING -> BibbleAction.DANGLING
    PixelBibbleFrame.SURPRISE -> BibbleAction.SURPRISE
}

/**
 * A living, animated Bibble.
 *
 * Drop-in replacement for the old frame-swapping version: same name and parameters. Pass the *mood* you
 * want (IDLE, DANCE_1/2, SLEEP, LOVE, SNACK, WALK_1/2, DANGLING, SURPRISE) and keep it there; Bibble
 * animates continuously on his own (breathing, blinking, glancing, hair sway, hops, particles).
 * You no longer need a timer that alternates DANCE_1 / DANCE_2 - both map to the same continuous dance.
 *
 * NOTE: the canvas is now (57 + 20) x (76 + 19) sprite pixels - a transparent margin around the character
 * leaves room for hops, hearts, "Zzz", music notes and the "!" bubble. Position it a little differently
 * (or use Modifier.offset) if you were anchoring the old 57x76 box.
 */
@Composable
fun PixelBibbleSprite(
    frame: PixelBibbleFrame,
    pixelSize: Dp = 1.3.dp,
    modifier: Modifier = Modifier
) {
    val action by rememberUpdatedState(frame.toAction())
    val engine = remember { BibbleEngine() }
    val buffer = remember { IntArray(OUT_W * OUT_H) }
    val bitmap = remember { Bitmap.createBitmap(OUT_W, OUT_H, Bitmap.Config.ARGB_8888) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var version by remember { mutableStateOf(0) }

    LaunchedEffect(engine) {
        val start = withFrameNanos { it }
        var lastStep = -1L
        while (true) {
            val now = withFrameNanos { it }
            val step = (now - start) / FRAME_NANOS
            if (step != lastStep) {
                lastStep = step
                engine.render((now - start) / 1_000_000_000f, action, buffer)
                bitmap.setPixels(buffer, 0, OUT_W, 0, 0, OUT_W, OUT_H)
                version = step.toInt()      // invalidates only the draw phase, no recomposition of children
            }
        }
    }

    Canvas(modifier = modifier.size(pixelSize * OUT_W, pixelSize * OUT_H)) {
        @Suppress("UNUSED_EXPRESSION")
        version   // read here so a new frame triggers a redraw
        drawImage(
            image = image,
            srcSize = IntSize(OUT_W, OUT_H),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.None      // nearest-neighbour = crisp pixels
        )
    }
}
