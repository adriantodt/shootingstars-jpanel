package net.adriantodt.asteroidsjpanel

import java.awt.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random.Default.nextFloat
import java.awt.Color.getHSBColor as hsbColor

class RenderPanel : JPanel() {
    private val frameLock = Semaphore(1)
    private val frameCount = AtomicLong()
    private val framerate = AtomicLong()
    private val colors = CopyOnWriteArrayList<Pair<Color, AtomicInteger>>()

    init {
        border = BorderFactory.createLineBorder(Color.black)
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate({
                frameLock.release()
                repaint()
            }, 0, 16666, TimeUnit.MICROSECONDS)

        val lastFrameCount = AtomicLong()
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate({
                val now = frameCount.get()
                val last = lastFrameCount.getAndSet(now)
                framerate.set(now - last)
            }, 0, 1000, TimeUnit.MILLISECONDS)
    }

    override fun getPreferredSize() = Dimension(1280, 720)

    override fun paintComponent(g: Graphics) {
        if (!frameLock.tryAcquire()) return
        frameCount.incrementAndGet()

        colors.add(hsbColor(nextFloat(), nextFloat(), nextFloat()) to AtomicInteger())

        (g as? Graphics2D)?.apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        }

        with(g) {
            color = Color.BLACK
            fillRect(0, 0, width, height)

            val h = size.height
            val w = size.width

            val hw = h + w
            val ratio = hw / (255 * 3f)
            colors.forEach { pair ->
                val (it, lifetime) = pair
                val n = lifetime.incrementAndGet()

                val entropy = ((it.red + it.green + it.blue) * ratio).roundToInt()

                val spawnX: Int
                val spawnY: Int

                if (entropy < h) {
                    spawnX = 0
                    spawnY = h - entropy
                } else {
                    spawnX = entropy - h
                    spawnY = 0
                }
                val size = (it.blue + it.green) / 128 + 1
                val speed = ((it.red * 4 + it.green * 2 + it.blue) / 256 + 1)

                val curX = spawnX + n * speed
                val curY = spawnY + n * speed

                if (curX > w || curY > h) {
                    colors.remove(pair)
                } else {

                    generateSequence(it, Color::darker).take(size * 2).forEachIndexed { i, c ->
                        color = c
                        fillRect((curX - i * speed), (curY - i * speed), size - i /2, size - i/2)
                    }
                }
            }

            // Last thing to render, the framerate
            color = Color(0x5D6265)
            drawString(framerate.get().toString() + " FPS", 2, 12)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                with(JFrame("Shooting Stars")) {
                    defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                    setSize(1280, 720)
                    add(RenderPanel())
                    pack()
                    setLocationRelativeTo(null)
                    isVisible = true
                }
            }
        }
    }
}