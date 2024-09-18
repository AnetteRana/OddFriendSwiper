package com.example.oddfriendswiper

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OddFriendSwiperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // This is where we show the face parts
                    RandomFace(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Arrays containing the drawable resources for heads, eyes, and mouths
val heads = arrayOf(R.drawable.heads1, R.drawable.heads2, R.drawable.heads3, R.drawable.heads4)
val eyes = arrayOf(R.drawable.eyes1, R.drawable.eyes2, R.drawable.eyes3, R.drawable.eyes4, R.drawable.eyes5)
val mouths = arrayOf(R.drawable.mouths1, R.drawable.mouths2, R.drawable.mouths3)

// word.(s)
val adjectives = arrayOf("happy", "sad", "furious", "excited", "nervous", "hungry")
val verbs = arrayOf("eat", "build", "destroy", "create", "imagine", "fix")
val nouns = arrayOf("cars", "houses", "ideas", "robots", "computers", "friends")

// image composable + button
@Composable
fun RandomFace(modifier: Modifier = Modifier) {

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
        }) {
            Text("Next")
        }
}
}

@Preview(showBackground = true)
@Composable
fun RandomFacePreview() {
    OddFriendSwiperTheme{
        RandomFace()
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