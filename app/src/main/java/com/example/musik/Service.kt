package com.example.musik
import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.widget.RemoteViews
import androidx.media3.common.*
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.*

@OptIn(UnstableApi::class)
class PlaybackService:MediaSessionService(){
 private var session:MediaSession?=null
 override fun onCreate(){
  super.onCreate()
  val rf=object:DefaultRenderersFactory(this){
   override fun buildAudioSink(context:Context,enableFloatOutput:Boolean,enableAudioTrackPlaybackParams:Boolean):AudioSink=
    DefaultAudioSink.Builder(context).setEnableFloatOutput(enableFloatOutput)
     .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
     .setAudioProcessors(arrayOf<AudioProcessor>(SpectrumTap())).build()
  }
  val p=ExoPlayer.Builder(this,rf).setAudioAttributes(AudioAttributes.DEFAULT,true).setHandleAudioBecomingNoisy(true).build()
  p.addListener(object:Player.Listener{
   override fun onEvents(pl:Player,e:Player.Events){
    PlayerWidget.push(this@PlaybackService,pl.mediaMetadata.title?.toString(),pl.mediaMetadata.artist?.toString(),pl.isPlaying)
   }})
  session=MediaSession.Builder(this,p).setSessionActivity(
   PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)).build()
 }
 override fun onGetSession(i:MediaSession.ControllerInfo)=session
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  session?.player?.let{when(intent?.action){
   "pp"->if(it.isPlaying)it.pause() else it.play()
   "next"->it.seekToNext(); "prev"->it.seekToPrevious(); else->Unit}}
  return super.onStartCommand(intent,flags,startId)
 }
 override fun onDestroy(){session?.run{player.release();release()};session=null;super.onDestroy()}
}

class PlayerWidget:AppWidgetProvider(){
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray)=push(c,null,null,false)
 companion object{
  fun push(c:Context,t:String?,a:String?,playing:Boolean){
   val v=RemoteViews(c.packageName,R.layout.widget_player)
   v.setTextViewText(R.id.wt,t?:"VibeMusic");v.setTextViewText(R.id.wa,a?:"Belum memutar")
   v.setImageViewResource(R.id.wpp,if(playing)android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
   fun pi(act:String)=PendingIntent.getService(c,act.hashCode(),Intent(c,PlaybackService::class.java).setAction(act),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
   v.setOnClickPendingIntent(R.id.wp,pi("prev"));v.setOnClickPendingIntent(R.id.wpp,pi("pp"));v.setOnClickPendingIntent(R.id.wn,pi("next"))
   v.setOnClickPendingIntent(R.id.wroot,PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE))
   AppWidgetManager.getInstance(c).updateAppWidget(ComponentName(c,PlayerWidget::class.java),v)
  }
 }
}
