package com.exemplo.ocrflutuante

import android.app.*
import android.content.*
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
 private val captureCode = 501
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,64,32,32) }
  layout.addView(TextView(this).apply { text="OCR Flutuante\n\nAutorize a sobreposição e a captura de tela. Toque na bolinha para selecionar texto."; textSize=19f })
  layout.addView(Button(this).apply { text="Ativar botão flutuante"; setOnClickListener { start() } })
  layout.addView(Button(this).apply { text="Parar"; setOnClickListener { stopService(Intent(this@MainActivity,BubbleService::class.java)) } })
  setContentView(layout)
 }
 private fun start() {
  if (!Settings.canDrawOverlays(this)) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))); Toast.makeText(this,"Autorize e volte para ativar",Toast.LENGTH_LONG).show(); return }
  val manager=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
  startActivityForResult(manager.createScreenCaptureIntent(),captureCode)
 }
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) { super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==captureCode && resultCode==RESULT_OK && data!=null) {
   val intent=Intent(this,BubbleService::class.java).putExtra("code",resultCode).putExtra("data",data)
   if(Build.VERSION.SDK_INT>=26) startForegroundService(intent) else startService(intent)
   moveTaskToBack(true)
  }
 }
}