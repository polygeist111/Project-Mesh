package com.greybox.projectmesh.buttonStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// This is a pre-defined button with white background and black text
@Composable
fun WhiteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = false
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White, // Background color
            contentColor = Color.Black    // Text color
        ),
        border = BorderStroke(1.dp, Color.Black), // Black border
        shape = RoundedCornerShape(8.dp), // Optional: Rounded corners
        modifier = modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Text(text = text)
    }
}