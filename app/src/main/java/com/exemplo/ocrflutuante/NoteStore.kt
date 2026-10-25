package com.exemplo.ocrflutuante
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object NoteStore {
 private const val KEY="notes"
 private const val WINDOW=5*60*1000L
 fun add(ctx:Context,text:String) {
  val prefs=ctx.getSharedPreferences("ocr_notes",Context.MODE_PRIVATE)
  val notes=JSONArray(prefs.getString(KEY,"[]"))
  val now=System.currentTimeMillis()
  val last=if(notes.length()>0)notes.getJSONObject(0) else null
  if(last!=null && now-last.optLong("updated")<WINDOW) {
   last.put("body",last.optString("body")+"\n\n"+text)
   last.put("updated",now)
  } else {
   val title=text.replace("\n"," ").trim().take(65).ifBlank{"Sem título"}
   val n=JSONObject().put("title",title).put("body",text).put("updated",now)
   val all=JSONArray().put(n)
   for(i in 0 until notes.length())all.put(notes.getJSONObject(i))
   prefs.edit().putString(KEY,all.toString()).apply()
   return
  }
  prefs.edit().putString(KEY,notes.toString()).apply()
 }
 fun list(ctx:Context)=JSONArray(ctx.getSharedPreferences("ocr_notes",Context.MODE_PRIVATE).getString(KEY,"[]"))
 fun save(ctx:Context,notes:JSONArray){ctx.getSharedPreferences("ocr_notes",Context.MODE_PRIVATE).edit().putString(KEY,notes.toString()).apply()}
 fun newSession(ctx:Context){val notes=list(ctx);if(notes.length()>0){notes.getJSONObject(0).put("updated",0);save(ctx,notes)}}
}