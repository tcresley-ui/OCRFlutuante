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
  layout.addView(Button(this).apply { text="Minhas anotações"; setOnClickListener { showNotes() } })
  layout.addView(Button(this).apply { text="Nova anotação na próxima captura"; setOnClickListener { NoteStore.newSession(this@MainActivity); Toast.makeText(this@MainActivity,"Próxima captura iniciará nova nota",Toast.LENGTH_SHORT).show() } })
  setContentView(layout)
  if(intent.getBooleanExtra("toggle",false)) { if(BubbleService.active) stopService(Intent(this,BubbleService::class.java)) else start() }
 }
 private fun showNotes(){
  val notes=NoteStore.list(this)
  val names=Array(notes.length()){i->notes.getJSONObject(i).optString("title")}
  AlertDialog.Builder(this).setTitle("Minhas anotações").setItems(names){_,index->
   val note=notes.getJSONObject(index)
   val edit=android.widget.EditText(this).apply{setText(note.optString("body"));minLines=4}
   AlertDialog.Builder(this).setTitle(note.optString("title")).setView(edit)
    .setPositiveButton("Salvar"){_,_->note.put("body",edit.text.toString());NoteStore.save(this,notes)}
    .setNeutralButton("Título"){_,_->
      val title=android.widget.EditText(this).apply{setText(note.optString("title"))}
      AlertDialog.Builder(this).setTitle("Editar título").setView(title).setPositiveButton("Salvar"){_,_->note.put("title",title.text.toString());NoteStore.save(this,notes)}.setNegativeButton("Cancelar",null).show()
    }.setNegativeButton("Fechar",null).show()
  }.setPositiveButton("Fechar",null).show()
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