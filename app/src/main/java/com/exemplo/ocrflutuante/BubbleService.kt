package com.exemplo.ocrflutuante

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.*
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.*

class BubbleService : Service() {
 private lateinit var wm:WindowManager
 private var bubble:View?=null; private var selection:SelectionView?=null
 private var projection:MediaProjection?=null
 private var reader:ImageReader?=null
 private val handler=Handler(Looper.getMainLooper())
 private val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
 private val channel="ocr_capture"
 override fun onBind(intent:Intent?)=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  nm.createNotificationChannel(NotificationChannel(channel,"Captura de texto",NotificationManager.IMPORTANCE_LOW))
  val pending=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
  val notification=Notification.Builder(this,channel).setContentTitle("OCR Flutuante ativo").setContentText("Toque na bolinha para copiar texto").setSmallIcon(android.R.drawable.ic_menu_crop).setContentIntent(pending).build()
  startForeground(1,notification)
  if(projection==null) {
   val data=if(Build.VERSION.SDK_INT>=33) intent?.getParcelableExtra("data",Intent::class.java) else intent?.getParcelableExtra<Intent>("data")
   if(data==null) {stopSelf();return START_NOT_STICKY}
   val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
   projection=mgr.getMediaProjection(intent.getIntExtra("code",Activity.RESULT_CANCELED),data)
   projection?.registerCallback(object:MediaProjection.Callback(){override fun onStop(){handler.post {stopSelf()} }},handler)
  }
  wm=getSystemService(WINDOW_SERVICE) as WindowManager
  if(bubble==null) showBubble()
  return START_NOT_STICKY
 }
 private fun params(width:Int,height:Int,flags:Int)=WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,flags,android.graphics.PixelFormat.TRANSLUCENT).apply {gravity=Gravity.TOP or Gravity.START}
 private fun showBubble(){
  val btn=TextView(this).apply {text="OCR"; textSize=16f; setTextColor(Color.WHITE); gravity=Gravity.CENTER; background=android.graphics.drawable.GradientDrawable().apply {setColor(Color.rgb(32,105,219));cornerRadius=100f}; elevation=10f}
  val p=params(150,150,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);p.x=20;p.y=300
  var downX=0f;var downY=0f;var startX=0;var startY=0
  btn.setOnTouchListener { _,e->when(e.action){
   MotionEvent.ACTION_DOWN->{downX=e.rawX;downY=e.rawY;startX=p.x;startY=p.y;true}
   MotionEvent.ACTION_MOVE->{p.x=startX+(e.rawX-downX).toInt();p.y=startY+(e.rawY-downY).toInt();wm.updateViewLayout(btn,p);true}
   MotionEvent.ACTION_UP->{if(hypot((e.rawX-downX).toDouble(),(e.rawY-downY).toDouble())<25) showSelection();true}
   else->false} }
  bubble=btn;wm.addView(btn,p)
 }
 private fun showSelection(){if(selection!=null)return
  bubble?.visibility=View.GONE
  val v=SelectionView(this);selection=v
  wm.addView(v,params(-1,-1,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN))
 }
 inner class SelectionView(ctx:Context):View(ctx){
  private val paint=Paint(3);private var a:PointF?=null;private var b:PointF?=null
  override fun onDraw(c:Canvas){super.onDraw(c);paint.color=0x55000000;c.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
   val p=a;val q=b;if(p!=null&&q!=null){val r=RectF(min(p.x,q.x),min(p.y,q.y),max(p.x,q.x),max(p.y,q.y));paint.color=Color.TRANSPARENT;paint.xfermode=PorterDuffXfermode(PorterDuff.Mode.CLEAR);c.drawRect(r,paint);paint.xfermode=null;paint.color=Color.CYAN;paint.style=Paint.Style.STROKE;paint.strokeWidth=4f;c.drawRect(r,paint);paint.style=Paint.Style.FILL}}
  override fun onTouchEvent(e:MotionEvent):Boolean {when(e.action){MotionEvent.ACTION_DOWN->{a=PointF(e.x,e.y);b=PointF(e.x,e.y);invalidate();return true}
   MotionEvent.ACTION_MOVE->{b=PointF(e.x,e.y);invalidate();return true}
   MotionEvent.ACTION_UP->{b=PointF(e.x,e.y);val p=a!!;val q=b!!;val r=Rect(min(p.x,q.x).toInt(),min(p.y,q.y).toInt(),max(p.x,q.x).toInt(),max(p.y,q.y).toInt());closeSelection();if(r.width()>10&&r.height()>10)handler.postDelayed({capture(r)},250);return true}}
   return true}
 }
 private fun closeSelection(){selection?.let{wm.removeView(it)};selection=null;bubble?.visibility=View.VISIBLE}
 private fun capture(rect:Rect){
  val metrics=resources.displayMetrics;val w=metrics.widthPixels;val h=metrics.heightPixels
  val p=projection?:return
  val ir=ImageReader.newInstance(w,h,android.graphics.PixelFormat.RGBA_8888,2);reader=ir
  val display=p.createVirtualDisplay("ocr",w,h,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,ir.surface,null,handler)
  var attempts=0
  fun finish(){display.release();ir.close();if(reader===ir)reader=null}
  fun poll(){val img=ir.acquireLatestImage();if(img==null){if(++attempts<15)handler.postDelayed({poll()},100) else {finish();toast("Não foi possível capturar a tela")};return}
   try{val plane=img.planes[0];val buffer=plane.buffer;val stride=plane.rowStride/plane.pixelStride
    val padded=Bitmap.createBitmap(stride,h,Bitmap.Config.ARGB_8888);padded.copyPixelsFromBuffer(buffer)
    val bounds=Rect(rect);bounds.intersect(0,0,w,h)
    if(bounds.isEmpty){padded.recycle();finish();return}
    val crop=Bitmap.createBitmap(padded,bounds.left,bounds.top,bounds.width(),bounds.height());padded.recycle();finish()
    recognizer.process(InputImage.fromBitmap(crop,0)).addOnSuccessListener {result->val t=result.text.trim();if(t.isNotEmpty()){(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(ClipData.newPlainText("Texto OCR",t));toast("Texto copiado!")}else toast("Nenhum texto encontrado")}.addOnFailureListener{toast("Erro ao reconhecer texto")}.addOnCompleteListener{crop.recycle()}
   }catch(ex:Exception){finish();toast("Falha na captura") }finally{img.close()}
  }
  handler.postDelayed({poll()},250)
 }
 private fun toast(s:String){handler.post{Toast.makeText(this,s,Toast.LENGTH_SHORT).show()}}
 override fun onDestroy(){selection?.let{wm.removeView(it)};bubble?.let{wm.removeView(it)};reader?.close();projection?.stop();recognizer.close();super.onDestroy()}
}