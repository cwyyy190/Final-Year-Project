import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

@SuppressLint("StaticFieldLeak")
object TTSManager : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingSpeakQueue: MutableList<String> = mutableListOf()
    private var context: Context? = null

    private var speechRate: Float = 1.0f
    var isTtsEnabled: Boolean = true

    fun init(context: Context) {
        if (!isTtsEnabled) return
        if (tts == null) {
            this.context = context.applicationContext
            tts = TextToSpeech(this.context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

            // Speak any pending text that was queued before initialization
//            if (isInitialized) {
//                pendingSpeakQueue.forEach { text ->
//                    tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
//                }
//                pendingSpeakQueue.clear()
//            }
        } else {
            isInitialized = false
        }
    }

    fun speak(text: String) {
        if (!isTtsEnabled) return
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            pendingSpeakQueue.add(text)
        }
    }

    fun speakHelp(){
        speakWithPause(
            "Swipe Left or Right to back to previous page <pause> " +
                    "Draw circle to start detection <pause> " +
                    "Swipe up to upload image <pause> " +
                    "Draw a tick to save object <pause> " +
                    "Draw Z to user page"
            ,1000
        )
    }

    fun speakWithPause(text: String, pauseDuration: Long) {
        val phrases = text.split("<pause>")
        val handler = Handler(Looper.getMainLooper())
        phrases.forEachIndexed { index, phrase ->
            handler.postDelayed({
                tts?.speak(phrase.trim(), TextToSpeech.QUEUE_ADD, null, null)
            }, index * pauseDuration)
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(speechRate)
    }

    fun updateSpeechRateBasedOnSpinner(spinnerValue: Int) {
        when (spinnerValue) {
            0 -> setSpeechRate(0.7f) // Slow speed
            1 -> setSpeechRate(1.0f) // Normal speed
            2 -> setSpeechRate(1.2f) // Fast speed
        }
    }

    fun enableTts(context: Context) {
        isTtsEnabled = true
        init(context)
    }

    fun disableTts() {
        isTtsEnabled = false
        shutdown()
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
