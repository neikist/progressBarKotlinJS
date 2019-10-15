import kotlin.browser.window
import kotlin.js.Date
import kotlin.math.min

fun main() {
    println("start")
    val progressBar = ProgressBar(ProgressBar.Parameters(20, 100, onePercentIncrementSpeed = 200), object : ProgressListener{
        override fun onProgress(timeDiff: Double, value: Int) {
            println("progress time diff $timeDiff and value $value")
        }

        override fun onFinish() {
            println("finish")
        }

        override fun onPause() {
            println("pause")
        }

        override fun onCancel() {
            println("cancel")
        }

    }).startProgress()

    window.setTimeout({
        progressBar.pauseProgress()
    }, 8_000)

    window.setTimeout({
        progressBar.startProgress()
    }, 16_000)

    window.setTimeout({
        progressBar.cleanAndStopProgress()
    }, 24_000)

    window.setTimeout({
        progressBar.startProgress()
    }, 32_000)


}

class ProgressBar(val parameters: Parameters, listener: ProgressListener) {

    private var started = false

    private val onePercentSize: Double = (parameters.finish - parameters.start) / 100.0

    private val listeners: MutableList<ProgressListener> = mutableListOf()
    private var currentTime: Double = .0
    private var timer: Int? = null

    private var currentValue: Int = parameters.start

    private var lastInterruptedValue: Int = currentValue
    private var lastStartTime = .0

    init {
        addProgressListener(listener)
    }

    fun startProgress(): ProgressBar {
        if (!started) {
            started = true
            lastStartTime = Date.now()
            timer = window.setInterval({
                val prevTime = currentTime
                currentTime = Date.now()
                val diff = if (prevTime == .0) .0 else currentTime - prevTime
                val percentFromLastStart = (currentTime - lastStartTime) / parameters.onePercentIncrementSpeed
                currentValue = kotlin.math.floor(lastInterruptedValue + percentFromLastStart * onePercentSize).toInt()

                listeners.forEach { it.onProgress(diff, min(currentValue, parameters.finish)) }
                if (currentValue >= parameters.finish) {
                    listeners.forEach { it.onFinish() }
                    cleanAndStopWithoutListeners()
                }
            }, parameters.updateInterval)
        }
        return this
    }

    fun pauseProgress(): Unit {
        if (started) {
            started = false
            timer?.run { window.clearTimeout(this) }
            lastInterruptedValue = currentValue
            listeners.forEach { it.onPause() }
        }
    }

    fun cleanAndStopProgress(): Unit {
        if (started) {
            cleanAndStopWithoutListeners()
            listeners.forEach { it.onCancel() }
        }
    }

    fun addProgressListener(listener: ProgressListener): Unit {
        listeners.add(listener)
    }

    fun removeProgressListener(listener: ProgressListener): Unit {
        listeners.remove(listener)
    }

    private fun cleanAndStopWithoutListeners(){
        timer?.run { window.clearTimeout(this) }
        started = false
        timer = null
        currentTime = .0
        lastInterruptedValue = 0
    }

    data class Parameters(
            val start: Int,
            val finish: Int,
            val updateInterval: Int = 16,
            val onePercentIncrementSpeed: Int = 2_000
    )
}

interface ProgressListener {
    fun onProgress(timeDiff: Double, value: Int)
    fun onFinish()
    fun onPause()
    fun onCancel()
}

class OnlyChangeListener(val lambda: (Double, Int)->Unit) : ProgressListener{
    override fun onCancel() = Unit
    override fun onFinish() = Unit
    override fun onPause() = Unit
    override fun onProgress(timeDiff: Double, value: Int) = lambda(timeDiff, value)
}
