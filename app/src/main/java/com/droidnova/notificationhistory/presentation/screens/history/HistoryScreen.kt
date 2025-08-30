package com.droidnova.notificationhistory.presentation.screens.history

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.data.db.NotificationEntity
import com.droidnova.notificationhistory.data.model.NotificationModel
import java.nio.file.WatchEvent

@Composable
fun HistoryScreen(mainViewmodel: MainViewModel) {
    val  packages = mainViewmodel.apps.collectAsState()


    Scaffold(){
        innerPadding ->
        HistoryScreenContent(modifier = Modifier.padding(innerPadding), packages = packages.value)
    }
}

@Composable
fun HistoryScreenContent(modifier: Modifier, packages: List<NotificationModel>) {

    LazyColumn(modifier= modifier) {
        if (packages.isEmpty()){
            item {
                Text("No History Found")
            }
        }else{
            items(packages){ item->
                Log.e("Mantsha", "HistoryScreenContent: ${item}")
                ItemHistoryCard(item)
            }
        }
    }
}

@Composable
fun ItemHistoryCard(model: NotificationModel){
    Card(
        modifier = Modifier.padding(8.dp).fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            AppIcon(drawable = model.appIcon)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = model.appName)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "${model.receivedAt}")
        }
        Text(text = model.title)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = model.text)
    }
}
@Composable
fun AppIcon(drawable: Drawable?) {
    drawable?.let {
        val bitmap: ImageBitmap = it.toBitmap().asImageBitmap()
        Image(
            modifier = Modifier.size(24.dp),
            bitmap = bitmap,
            contentDescription = null
        )
    }
}