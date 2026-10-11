package com.example.ecodriver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ecodriver.ui.theme.EvenDarkerGrey

@Composable
fun EcoDriverTopBar(modifier: Modifier = Modifier){
    Row (
        modifier = modifier.fillMaxWidth().statusBarsPadding().height(56.dp).background(color = EvenDarkerGrey),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start

    ) {
        /*
        Image(
            painter = painterResource(id = R.drawable.ecodrive_logo),
            contentDescription = "PlaceHolder EcoDrive Logo",
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))

         */
        Text(
            text = "EcoDriver",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White

        )
    }


}
