package com.example.oddfriendswiper

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- Constants and Data Classes ---

val heads = arrayOf(R.drawable.heads1, R.drawable.heads2, R.drawable.heads3, R.drawable.heads4)
val eyes = arrayOf(
    R.drawable.eyes1,
    R.drawable.eyes2,
    R.drawable.eyes3,
    R.drawable.eyes4,
    R.drawable.eyes5
)
val mouths = arrayOf(R.drawable.mouths1, R.drawable.mouths2, R.drawable.mouths3)

object TTSManager : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech? = null
    var isInitialized = false
    private var initializationCallbacks = mutableListOf<() -> Unit>()

    fun init(context: Context, callback: () -> Unit) {
        if (isInitialized) {
            callback()
            return
        }

        initializationCallbacks.add(callback)
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSManager", "The Language specified is not supported!")
            } else {
                Log.i("TTSManager", "TextToSpeech Initialized Successfully!")
                isInitialized = true
                initializationCallbacks.forEach { it() }
                initializationCallbacks.clear()
            }
        } else {
            Log.e("TTSManager", "Initialization Failed!")
        }
    }

    suspend fun speak(sentence: String) {
        withContext(Dispatchers.IO) {
            textToSpeech?.let { tts ->
                if (tts.isSpeaking) {
                    tts.stop() // stop ongoing speech
                }
                tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, null)
            } ?: Log.e("TTSManager", "TTS not initialized")
        }
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}

// --- Utility Functions ---

// function for loading from file
fun loadWordsFromJson(context: Context): Map<String, List<String>> {
    val json: String
    try {
        val inputStream = context.assets.open("words.json")
        json = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)

        val adjectives = jsonObject.getJSONArray("adjectives").toList()
        val verbs = jsonObject.getJSONArray("verbs").toList()
        val nouns = jsonObject.getJSONArray("nouns").toList()

        return mapOf("adjectives" to adjectives, "verbs" to verbs, "nouns" to nouns)
    } catch (ex: IOException) {
        ex.printStackTrace()
        return emptyMap()
    }
}

fun JSONArray.toList(): List<String> {
    val list = mutableListOf<String>()
    for (i in 0 until this.length()) {
        list.add(this.getString(i))
    }
    return list
}

// Function to get random image
fun getRandomFacePart(parts: Array<Int>): Int {
    return parts.random()
}

// Function to generate a random sentence and return its parts
fun generateRandomSentence(
    adjectives: List<String>,
    verbs: List<String>,
    nouns: List<String>
): Triple<String, String, String> {
    // safeguard against empty lists
    if (adjectives.isEmpty() || verbs.isEmpty() || nouns.isEmpty()) {
        throw IllegalArgumentException("One or more word lists are empty")
    }

    val adjective = adjectives.random()
    val verb = verbs.random()
    val noun = nouns.random()
    return Triple(adjective, verb, noun)
}

// --- Main Activity ---
class MainActivity : ComponentActivity() {
    // declare TTS variable
    private val supportedLanguages = listOf(Locale.US, Locale.UK, Locale.CANADA, Locale.GERMANY)

    // Declare the word lists
    private lateinit var adjectives: List<String>
    private lateinit var verbs: List<String>
    private lateinit var nouns: List<String>

    // create image
    fun createBitmapFromCanvasView(view: View): Bitmap {

        val topPosition = view.height / 6

        val bitmap = Bitmap.createBitmap(view.width, view.width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.translate(0f, -topPosition.toFloat())

        view.draw(canvas)
        return bitmap
    }
    fun shareBitmap(context: Context, bitmap: Bitmap) {
        try {
            // Save bitmap to cache directory
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // Create directory if not exists
            val file = File(cachePath, "frienda_image.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()

            // Create content URI
            val contentUri: Uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load words from JSON
        val wordsMap = loadWordsFromJson(this)
        adjectives = wordsMap["adjectives"] ?: emptyList()
        verbs = wordsMap["verbs"] ?: emptyList()
        nouns = wordsMap["nouns"] ?: emptyList()

        // Log the sizes of the lists to verify they are loaded correctly
        Log.d("MainActivity", "Adjectives loaded: ${adjectives.size}")
        Log.d("MainActivity", "Verbs loaded: ${verbs.size}")
        Log.d("MainActivity", "Nouns loaded: ${nouns.size}")

        if (adjectives.isEmpty() || verbs.isEmpty() || nouns.isEmpty()) {
            Toast.makeText(
                this,
                "Failed to load word lists. Please check your JSON file.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
/*
        if (adjectives.isNotEmpty() || verbs.isNotEmpty() || nouns.isNotEmpty()) {
            //isLoading = false // data successfully loaded
        } else {
            Toast.makeText(
                this,
                "Failed to load word lists. Please check your JSON file.",
                Toast.LENGTH_LONG
            ).show()
        }
*/

        enableEdgeToEdge()

        // initialize TTS Manager
        TTSManager.init(this){
            // callback after TTS is initialized
            Log.i("MainActivity", "TTS Initialized Successfully!")
        }

        setContent {
            OddFriendSwiperTheme {
                val navController = rememberNavController()

                // set up navigation host
                NavHost(navController = navController, startDestination = "start") {
                    composable("start") {
                        StartScreen(onNavigationToMainMenu = {
                            if(TTSManager.isInitialized) {
                                navController.navigate("mainMenu")
                            } else{
                                Toast.makeText(
                                    this@MainActivity,
                                    "TTS is not initialized yet!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    }
                    composable("mainMenu") {
                        MainMenuScreen(
                            adjectives = adjectives,
                            verbs = verbs,
                            nouns = nouns,
                            supportedLanguages = supportedLanguages,
                            createBitmap = ::createBitmapFromCanvasView,
                            shareBitmap = ::shareBitmap
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        TTSManager.shutdown()
        super.onDestroy()
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
    adjectives: List<String>,
    verbs: List<String>,
    nouns: List<String>,
    createBitmap: (View) -> Bitmap,
    shareBitmap: (Context, Bitmap) -> Unit
) {

    // State variables for face parts
    var head by remember { mutableStateOf(getRandomFacePart(heads)) }
    var leftEye by remember { mutableStateOf(getRandomFacePart(eyes)) }
    var rightEye by remember { mutableStateOf(getRandomFacePart(eyes)) }
    var mouth by remember { mutableStateOf(getRandomFacePart(mouths)) }

    // state variable for the sentence parts
    var sentenceParts by remember {
        mutableStateOf(
            generateRandomSentence(
                adjectives,
                verbs,
                nouns
            )
        )
    }
    val (adjective, verb, noun) = sentenceParts

    // Full sentence
    val sentence = "I'm so $adjective I $verb $noun!"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // sentence displayed here
        Text(sentence)
        Spacer(modifier = Modifier.height(16.dp))

        // Use DisplayFace and add the swipeable modifier
        Box(
            modifier = Modifier
                .size(300.dp),
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
        }) {
            Text("Share")
        }
    }
}

@Composable
fun StartScreen(onNavigationToMainMenu: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0f) }

    // simulate loading
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            progress += 0.02f
            delay(20) // frames i think 🤔
        }
        isLoading = false // once loading is complete, shop showing bar
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(progress = progress)
        } else {
            Button(onClick = { onNavigationToMainMenu() }) {
                Text("Make friends!")
            }
        }
    }
}

@Composable
fun MainMenuScreen(
    adjectives: List<String>,
    verbs: List<String>,
    nouns: List<String>,
    supportedLanguages: List<Locale>,
    createBitmap: (View) -> Bitmap,
    shareBitmap: (Context, Bitmap) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        //if(isLoading){LoadingScreen()}
        // This is where we show the face parts
        MyCanvas(
            modifier = Modifier.padding(innerPadding),
            onSpeak = { sentence ->
               coroutineScope.launch {
                   // can handle language selection here
                   TTSManager.speak(sentence)
               }
            },
            adjectives = adjectives,
            verbs = verbs,
            nouns = nouns,
            createBitmap = createBitmap,
            shareBitmap = shareBitmap
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RandomFacePreview() {
    val sampleAdjectives = listOf("fake")
    val sampleVerbs = listOf("lie about")
    val sampleNouns = listOf("everything")

    // Dummy implementations for the preview
    val dummyCreateBitmap: (View) -> Bitmap =
        { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }
    val dummyShareBitmap: (Context, Bitmap) -> Unit = { _, _ -> }

    OddFriendSwiperTheme {
        MyCanvas(
            onSpeak = {},
            adjectives = sampleAdjectives,
            verbs = sampleVerbs,
            nouns = sampleNouns,
            createBitmap = dummyCreateBitmap,
            shareBitmap = dummyShareBitmap
        )
    }
}
