package com.example.oddfriendswiper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

// image composable
@Composable
fun RandomFace(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center // Center everything
    ) {
        // Head (this will be at the back)
        Image(
            painter = painterResource(id = getRandomFacePart(heads)),
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
                painter = painterResource(id = getRandomFacePart(eyes)),
                contentDescription = "Left Eye",
                modifier = Modifier
                    .size(50.dp)
                    .offset(y = 40.dp, x = 20.dp)
            )

            // Right eye
            Image(
                painter = painterResource(id = getRandomFacePart(eyes)),
                contentDescription = "Right Eye",
                modifier = Modifier
                    .size(50.dp)
                    .offset(y = 40.dp, x = -20.dp)
            )
        }

        // Mouth (positioned below the eyes, closer to the head)
        Image(
            painter = painterResource(id = getRandomFacePart(mouths)),
            contentDescription = "Mouth",
            modifier = Modifier
                .align(Alignment.Center) // Align to the center of the Box
                .offset(y = 40.dp) // Move the mouth below the head (adjust this value)
                .size(80.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RandomFacePreview() {
    OddFriendSwiperTheme{
        RandomFace()
    }
}

// Arrays containing the drawable resources for heads, eyes, and mouths
val heads = arrayOf(R.drawable.heads1, R.drawable.heads2, R.drawable.heads3, R.drawable.heads4)
val eyes = arrayOf(R.drawable.eyes1, R.drawable.eyes2, R.drawable.eyes3, R.drawable.eyes4, R.drawable.eyes5)
val mouths = arrayOf(R.drawable.mouths1, R.drawable.mouths2, R.drawable.mouths3)

// Function to get random image
fun getRandomFacePart(parts: Array<Int>): Int{
    return parts.random()
}