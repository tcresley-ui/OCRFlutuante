package com.exemplo.ocrflutuante
import android.service.quicksettings.TileService
import android.content.Intent
import android.os.Build
class OcrTileService:TileService(){
 override fun onClick(){
  super.onClick()
  val i=Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("toggle",true)
  if(Build.VERSION.SDK_INT>=34)startActivityAndCollapse(android.app.PendingIntent.getActivity(this,11,i,android.app.PendingIntent.FLAG_IMMUTABLE))
  else { @Suppress("DEPRECATION") startActivityAndCollapse(i) }
 }
}