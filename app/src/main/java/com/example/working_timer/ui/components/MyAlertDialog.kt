package com.example.working_timer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.working_timer.util.BackgroundColor

/**
 * 3つのボタンを持つアラートダイアログ
 */
@Composable
fun MyAlertDialogWithThreeButton(
    title: String,
    message: String,
    positiveButtonText: String,
    neutralButtonText: String,
    negativeButtonText: String,
    onPositiveClick: () -> Unit,
    onNeutralClick: () -> Unit,
    onNegativeClick: () -> Unit
) {
    Dialog(
        // ダイアログ外をタップしたらなにも起こらないようにするべき
        onDismissRequest = onNeutralClick
    ) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = typography.headlineMedium
                )


                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    style = typography.bodyLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color = BackgroundColor)
                        .padding(vertical = 24.dp)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    TextButton(
                        onClick = onNegativeClick,
                    ) {
                        Text(
                            text = negativeButtonText,
                            style = typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = onNeutralClick,
                    ) {
                        Text(
                            text = neutralButtonText,
                            style = typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = onPositiveClick,
                    ) {
                        Text(
                            text = positiveButtonText,
                            style = typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MyAlertDialogPreview() {
    MyAlertDialogWithThreeButton(
        title = "タイトル",
        message = "メッセージ",
        positiveButtonText = "はい",
        neutralButtonText = "キャンセル",
        negativeButtonText = "いいえ",
        onPositiveClick = {},
        onNeutralClick = {},
        onNegativeClick = {}
    )
}