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

class MainActivity : ComponentActivity() {

    // declare TTS variable
    private lateinit var textToSpeech: TextToSpeech
    private val supportedLanguages = listOf(Locale.US, Locale.UK, Locale.CANADA, Locale.GERMANY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    RandomFace(
                        modifier = Modifier.padding(innerPadding),
                        onSpeak = { sentence ->
                            // select a random language
                            val selectedLanguage = supportedLanguages.random()
                            val result = textToSpeech.setLanguage(selectedLanguage)
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                Log.e("TTS", "Selected language not supported!")
                                return@RandomFace
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
                        }
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
}

// Arrays containing the drawable resources for heads, eyes, and mouths
val heads = arrayOf(R.drawable.heads1, R.drawable.heads2, R.drawable.heads3, R.drawable.heads4)
val eyes = arrayOf(R.drawable.eyes1, R.drawable.eyes2, R.drawable.eyes3, R.drawable.eyes4, R.drawable.eyes5)
val mouths = arrayOf(R.drawable.mouths1, R.drawable.mouths2, R.drawable.mouths3)

// word.(s)
val adjectives = arrayOf("healthy", "rich", "famous", "old", "left-winged", "much better than you", "future thinking", "stubborn", "scared of other humans", "romantic", "tired of liars", "hard working", "stupid", "happy", "sad", "bad ass", "hardcore", "excited", "nervous", "hungry", "tired of living", "high", "high ranking in my cult", "very fond of Taylor Swift", "bored with the internet", "lonely", "good looking", "popular now, because")
val verbs = arrayOf("eat", "build", "destroy", "create", "imagine", "fix", "have ChatGPT generate images of", "made a game about", "went back to school to learn about", "take artistic photos of", "have made it my life mission to help", "have started praying to", "go to cosplay conventions, dressed like", "am warming up to the idea of", "have a youtube channel about", "have a tattoo of", "enjoy slapping", "am saving up to buy", "got caught with")
val nouns = arrayOf("cars", "houses", "ideas", "robots", "your mom", "friends", "the wizard", "rude children", "sad cats", "a portal to another dimension", "a cape of invisibility", "a stupid programming language, like kotlin", "TROGDOR the BURNINATOR", "our lord and saviour, Jesus Christ")

// image composable + button
@Composable
fun RandomFace(modifier: Modifier = Modifier, onSpeak: (String) -> Unit) {

    // State variables for face parts
    // mutableStateOf makes the variable reactive,
    //meaning when it changes, the UI will recompose
    var head by remember { mutableStateOf(getRandomFacePart(heads))}
    var leftEye by remember { mutableStateOf(getRandomFacePart(eyes))}
    var rightEye by remember { mutableStateOf(getRandomFacePart(eyes))}
    var mouth by remember { mutableStateOf(getRandomFacePart(mouths))}

    // state variable for the sentence displayed
    var sentence by remember { mutableStateOf(generateRandomSentence())}

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        // sentence displayed here
        Text(sentence)

        Spacer(modifier = Modifier.height(16.dp))

        Box(
        modifier = modifier
            .size(300.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center // Center everything
    ) {
        // Head (this will be at the back)
        Image(
            painter = painterResource(id = head),
            contentDescription = "Head",
            modifier = Modifier.size(200.dp)
        )

        // Eyes (they will be on top of the head, side by side, adjusted vertically)
        Row(
            modifier = Modifier
                .align(Alignment.Center) // Align to the center of the Box
                .offset(y = -50.dp), // Move the eyes above the head (adjust this value)
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

        // Mouth (positioned below the eyes, closer to the head)
        Image(
            painter = painterResource(id = mouth),
            contentDescription = "Mouth",
            modifier = Modifier
                .align(Alignment.Center) // Align to the center of the Box
                .offset(y = 40.dp) // Move the mouth below the head
                .size(80.dp)
        )
    }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add the "next"-button
        Button(onClick = {
            // update state variables with new random states
            head = getRandomFacePart(heads)
            leftEye = getRandomFacePart(eyes)
            rightEye = getRandomFacePart(eyes)
            mouth = getRandomFacePart(mouths)

            sentence = generateRandomSentence()

            // TTS
            onSpeak(sentence)

        }) {
            Text("Next")
        }
}
}

// Function to get random image
fun getRandomFacePart(parts: Array<Int>): Int{
    return parts.random()
}

fun generateRandomSentence(): String{
    val adjective = adjectives.random()
    val verb = verbs.random()
    val noun = nouns.random()
    return "I'm so $adjective I $verb $noun!"
}

@Preview(showBackground = true)
@Composable
fun RandomFacePreview() {
    OddFriendSwiperTheme{
        RandomFace(onSpeak = {})
    }
}