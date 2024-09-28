package com.example.oddfriendswiper

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import kotlin.random.Random
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.oddfriendswiper.ui.theme.OddFriendSwiperTheme
import java.util.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import android.view.View
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import androidx.core.content.FileProvider
import android.content.Intent



// --- Constants and Data Classes ---
data class Stats(var likes: Int = 0, var dislikes: Int = 0, var neutrals: Int = 0)

val heads = arrayOf(R.drawable.heads1, R.drawable.heads2, R.drawable.heads3, R.drawable.heads4)
val eyes = arrayOf(R.drawable.eyes1, R.drawable.eyes2, R.drawable.eyes3, R.drawable.eyes4, R.drawable.eyes5)
val mouths = arrayOf(R.drawable.mouths1, R.drawable.mouths2, R.drawable.mouths3)

// --- Utility Functions ---

// function for loading from file
fun loadWordsFromJson(context: Context): Map<String, List<String>>{
    val json: String
    try{
        val inputStream = context.assets.open("words.json")
        json = inputStream.bufferedReader().use{it.readText()}
        val jsonObject = JSONObject(json)

        val adjectives = jsonObject.getJSONArray("adjectives").toList()
        val verbs = jsonObject.getJSONArray("verbs").toList()
        val nouns = jsonObject.getJSONArray("nouns").toList()

        return mapOf("adjectives" to adjectives, "verbs" to verbs, "nouns" to nouns)
    } catch (ex: IOException){
        ex.printStackTrace()
        return emptyMap()
    }
}

fun JSONArray.toList(): List<String>{
    val list = mutableListOf<String>()
    for (i in 0 until this.length()){
        list.add(this.getString(i))
    }
    return list
}

//function for swipe detection
fun Modifier.swipeable(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
): Modifier {
    return pointerInput(Unit) {
        var totalDragAmount = 0f
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount -> totalDragAmount += dragAmount },
            onDragEnd = {
                if (totalDragAmount > 100f) {
                    onSwipeRight()
                } else if (totalDragAmount < -100f) {
                    onSwipeLeft()
                }
                totalDragAmount = 0f // Reset for next gesture
            }
        )
    }
}

// Function to get random image
fun getRandomFacePart(parts: Array<Int>): Int {
    return parts.random()
}

// Function to generate a random sentence and return its parts
fun generateRandomSentence(adjectives: List<String>,verbs: List<String>, nouns: List<String>): Triple<String, String, String> {
    // safeguard against empty lists
    if (adjectives.isEmpty() || verbs.isEmpty() || nouns.isEmpty()){
        throw IllegalArgumentException("One or more word lists are empty")
    }

    val adjective = adjectives.random()
    val verb = verbs.random()
    val noun = nouns.random()
    return Triple(adjective, verb, noun)
}

// Function to update the stats for each word or image
fun updateStats(word: String, statsMap: MutableMap<String, Stats>, action: String) {
    // Retrieve existing stats or create new ones
    val stats = statsMap[word] ?: Stats()

    // Update stats based on the action
    when (action) {
        "like" -> stats.likes++
        "dislike" -> stats.dislikes++
        "neutral" -> stats.neutrals++
    }

    // Save the updated stats back to the map
    statsMap[word] = stats
}


// --- Main Activity ---
class MainActivity : ComponentActivity() {

    // initialize maps to track stats
    val adjectiveStats = mutableMapOf<String, Stats>()
    val verbStats = mutableMapOf<String, Stats>()
    val nounStats = mutableMapOf<String, Stats>()
    val imageStats = mutableMapOf<String, Stats>()

    // declare TTS variable
    private lateinit var textToSpeech: TextToSpeech
    private val supportedLanguages = listOf(Locale.US, Locale.UK, Locale.CANADA, Locale.GERMANY)

    // Declare the word lists
    private var adjectives: List<String> = emptyList()
    private var verbs: List<String> = emptyList()
    private var nouns: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // from json file
        val wordsMap = loadWordsFromJson(this)
        val adjectives = wordsMap["adjectives"] ?: emptyList()
        val verbs = wordsMap["verbs"] ?: emptyList()
        val nouns = wordsMap["nouns"] ?: emptyList()

        // Log the sizes of the lists to verify they are loaded correctly
        Log.d("MainActivity", "Adjectives loaded: ${adjectives.size}")
        Log.d("MainActivity", "Verbs loaded: ${verbs.size}")
        Log.d("MainActivity", "Nouns loaded: ${nouns.size}")

        if (adjectives.isEmpty() || verbs.isEmpty() || nouns.isEmpty()) {
            Toast.makeText(this, "Failed to load word lists. Please check your JSON file.", Toast.LENGTH_LONG).show()
            return
        }

        // initialize TTS
        textToSpeech = TextToSpeech(this){ status ->
            if (status == TextToSpeech.SUCCESS){
                val result = textToSpeech.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "The Language specified is not supported!")
                    Toast.makeText(this, "TTS language not supported", Toast.LENGTH_LONG).show()
                } else {
                    Log.i("TTS", "TextToSpeech Initialized Successfully!")
                }
            } else {
                Log.e("TTS", "Initialization Failed!")
                Toast.makeText(this, "TTS Initialization Failed!", Toast.LENGTH_LONG).show()
            }
        }

        enableEdgeToEdge()
        setContent {
            OddFriendSwiperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // This is where we show the face parts
                    MyCanvas(
                        modifier = Modifier.padding(innerPadding),
                        onSpeak = { sentence ->
                            // select a random language
                            val selectedLanguage = supportedLanguages.random()
                            val result = textToSpeech.setLanguage(selectedLanguage)
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.e("TTS", "Selected language not supported!")
                                return@MyCanvas
                            }

                            // select a random voice from selected language
                            val availableVoices = textToSpeech.voices.filter { it.locale == selectedLanguage }
                            val selectedVoice = availableVoices.randomOrNull()
                            selectedVoice?.let {
                                textToSpeech.voice = it
                                Log.i("TTS", "Selected voice: ${it.name}")
                            } ?: Log.e("TTS", "No available voices for the selected language!")


                            val randomPitch = Random.nextDouble(0.4, 2.0).toFloat()
                            val randomRate = Random.nextDouble(0.9, 1.4).toFloat()

                            textToSpeech.setPitch(randomPitch)
                            textToSpeech.setSpeechRate(randomRate)

                            textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, null)
                        },
                        onSwipeLeft = { noun, adjective, verb, head, leftEye, rightEye, mouth -> // Handle swipe left (dislike)
                            updateStats(noun, nounStats, "dislike")
                            updateStats(adjective, adjectiveStats, "dislike")
                            updateStats(verb, verbStats, "dislike")
                            updateStats(head.toString(), imageStats, "dislike")
                            updateStats(leftEye.toString(), imageStats, "dislike")
                            updateStats(rightEye.toString(), imageStats, "dislike")
                            updateStats(mouth.toString(), imageStats, "dislike")
                        },
                        onSwipeRight = { noun, adjective, verb, head, leftEye, rightEye, mouth -> // Handle swipe right (like)
                            updateStats(noun, nounStats, "like")
                            updateStats(adjective, adjectiveStats, "like")
                            updateStats(verb, verbStats, "like")
                            updateStats(head.toString(), imageStats, "like")
                            updateStats(leftEye.toString(), imageStats, "like")
                            updateStats(rightEye.toString(), imageStats, "like")
                            updateStats(mouth.toString(), imageStats, "like")
                        },
                        onNeutral = { noun, adjective, verb, head, leftEye, rightEye, mouth -> // Handle "neutral" action when pressing Next button
                            updateStats(noun, nounStats, "neutral")
                            updateStats(adjective, adjectiveStats, "neutral")
                            updateStats(verb, verbStats, "neutral")
                            updateStats(head.toString(), imageStats, "neutral")
                            updateStats(leftEye.toString(), imageStats, "neutral")
                            updateStats(rightEye.toString(), imageStats, "neutral")
                            updateStats(mouth.toString(), imageStats, "neutral")
                        },
                        adjectives = adjectives,
                        verbs = verbs,
                        nouns = nouns,
                        createBitmap = ::createBitmapFromCanvasView,
                        shareBitmap = ::shareBitmap
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (this::textToSpeech.isInitialized){
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

    // create image
    fun createBitmapFromCanvasView(view: View): Bitmap{

        val topPosition = view.height /6

        val bitmap = Bitmap.createBitmap(view.width, view.width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.translate(0f, -topPosition.toFloat())

        view.draw(canvas)
        return bitmap
    }

    fun shareBitmap(context: Context, bitmap: Bitmap){
        try {
            // Save bitmap to cache directory
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // Create directory if not exists
            val file = File(cachePath, "friend_image.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()

            // Create content URI
            val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            // Create share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission for the other app to access this URI
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share your friend via"))
        } catch (e: Exception) {
            Log.e("ShareError", "Error sharing bitmap: ${e.message}")
            Toast.makeText(context, "Failed to share image.", Toast.LENGTH_SHORT).show()
        }
    }
}

// --- Composables ---
@Composable
fun DisplayFace(head: Int, leftEye: Int, rightEye: Int, mouth: Int) {
    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // Head (this will be at the back)
        Image(
            painter = painterResource(id = head),
            contentDescription = "Head",
            modifier = Modifier.size(200.dp)
        )

        // Eyes (side by side)
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -50.dp), // Move the eyes above the head
            horizontalArrangement = Arrangement.spacedBy(40.dp) // Spacing between the eyes
        ) {
            // Left eye
            Image(
                painter = painterResource(id = leftEye),
                contentDescription = "Left Eye",
                modifier = Modifier
                    .size(50.dp)
                    .offset(y = 40.dp, x = 20.dp)
            )

            // Right eye
            Image(
                painter = painterResource(id = rightEye),
                contentDescription = "Right Eye",
                modifier = Modifier
                    .size(50.dp)
                    .offset(y = 40.dp, x = -20.dp)
            )
        }

        // Mouth (positioned below the eyes)
        Image(
            painter = painterResource(id = mouth),
            contentDescription = "Mouth",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 40.dp)
                .size(80.dp)
        )
    }
}

@Composable
fun MyCanvas(
    modifier: Modifier = Modifier,
    onSpeak: (String) -> Unit,
    onSwipeLeft:(String, String, String, Int, Int, Int, Int)->Unit,
    onSwipeRight:(String, String, String, Int, Int, Int, Int)->Unit,
    onNeutral:(String, String, String, Int, Int, Int, Int)->Unit,
    adjectives: List<String>,
    verbs: List<String>,
    nouns: List<String>,
    createBitmap: (View) ->Bitmap,
    shareBitmap: (Context,Bitmap) -> Unit
) {

    // State variables for face parts
    var head by remember { mutableStateOf(getRandomFacePart(heads)) }
    var leftEye by remember { mutableStateOf(getRandomFacePart(eyes)) }
    var rightEye by remember { mutableStateOf(getRandomFacePart(eyes)) }
    var mouth by remember { mutableStateOf(getRandomFacePart(mouths)) }

    // state variable for the sentence parts
    var sentenceParts by remember { mutableStateOf(generateRandomSentence(adjectives, verbs, nouns)) }
    val (adjective, verb, noun) = sentenceParts

    // Full sentence
    val sentence = "I'm so $adjective I $verb $noun!"

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // sentence displayed here
        Text(sentence)
        Spacer(modifier = Modifier.height(16.dp))

        // Use DisplayFace and add the swipeable modifier
        Box(
            modifier = Modifier
                .size(300.dp)
                .swipeable(
                    onSwipeLeft = {
                        onSwipeLeft(noun, adjective, verb, head, leftEye, rightEye, mouth)
                    },
                    onSwipeRight = {
                        onSwipeRight(noun, adjective, verb, head, leftEye, rightEye, mouth)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            DisplayFace(head = head, leftEye = leftEye, rightEye = rightEye, mouth = mouth)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // mute button TODO

        // "next"-button
        Button(onClick = {
            // update state variables with new random states
            head = getRandomFacePart(heads)
            leftEye = getRandomFacePart(eyes)
            rightEye = getRandomFacePart(eyes)
            mouth = getRandomFacePart(mouths)

            // Update sentence parts and speak
            sentenceParts = generateRandomSentence(adjectives, verbs, nouns)
            onSpeak("I'm so ${sentenceParts.first} I ${sentenceParts.second} ${sentenceParts.third}!")
        }) {
            Text("Next")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // share button
        val context = LocalContext.current
        val view = LocalView.current
        Button(onClick = {
            val bitmap = createBitmap(view)
            shareBitmap(context, bitmap)
        }){
            Text("Share")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RandomFacePreview() {
    val sampleAdjectives = listOf("fake")
    val sampleVerbs = listOf("lie about")
    val sampleNouns = listOf("everything")

    // Dummy implementations for the preview
    val dummyCreateBitmap: (View) -> Bitmap = { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }
    val dummyShareBitmap: (Context, Bitmap) -> Unit = { _, _ -> }

    OddFriendSwiperTheme {
        MyCanvas(
            onSpeak = {},
            onSwipeLeft = { _, _, _, _, _, _, _ ->},
            onSwipeRight = { _, _, _, _, _, _, _ ->},
            onNeutral = { _, _, _, _, _, _, _ ->},
        adjectives = sampleAdjectives,
        verbs = sampleVerbs,
        nouns = sampleNouns,
        createBitmap = dummyCreateBitmap,
        shareBitmap = dummyShareBitmap
        )
    }
}
