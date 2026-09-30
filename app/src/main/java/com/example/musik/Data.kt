package com.example.musik
import android.content.*
import android.provider.MediaStore.Audio.Media as M
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.common.*
import kotlinx.coroutines.*
import org.json.*
import java.net.*

data class Song(val id:Long,val title:String,val artist:String,val album:String,val genre:String,val dur:Long){
 val uri get()=ContentUris.withAppendedId(M.EXTERNAL_CONTENT_URI,id)
}
fun Song.item()=MediaItem.Builder().setMediaId(id.toString()).setUri(uri)
 .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).build()).build()

fun loadSongs(c:Context):List<Song>{
 val out=ArrayList<Song>()
 fun cl(s:String?,d:String)=if(s.isNullOrBlank()||s=="<unknown>")d else s
 c.contentResolver.query(M.EXTERNAL_CONTENT_URI,arrayOf(M._ID,M.TITLE,M.ARTIST,M.ALBUM,M.GENRE,M.DURATION),
  "${M.IS_MUSIC}!=0",null,"${M.TITLE} COLLATE NOCASE")?.use{cu->
  while(cu.moveToNext()) out+=Song(cu.getLong(0),cl(cu.getString(1),"Tanpa judul"),cl(cu.getString(2),"Artis tidak dikenal"),
   cl(cu.getString(3),"Album tidak dikenal"),cl(cu.getString(4),"Genre tidak dikenal"),cu.getLong(5))
 }
 return out
}

class Playlists(c:Context){
 private val sp=c.getSharedPreferences("pl",0)
 val map=mutableStateMapOf<String,List<Long>>()
 init{ val j=JSONObject(sp.getString("d","{}")!!); j.keys().forEach{k->val a=j.getJSONArray(k);map[k]=List(a.length()){a.getLong(it)}} }
 private fun save()=sp.edit().putString("d",JSONObject(map.mapValues{JSONArray(it.value)}).toString()).apply()
 fun create(n:String){ if(n.isNotBlank()&&n !in map){map[n]=emptyList();save()} }
 fun delete(n:String){map.remove(n);save()}
 fun add(n:String,id:Long){ val l=map[n].orEmpty(); if(id !in l){map[n]=l+id;save()} }
 fun remove(n:String,id:Long){map[n]=map[n].orEmpty()-id;save()}
}

data class Line(val ms:Long,val text:String)
object Lyrics{
 private val cache=HashMap<Long,List<Line>>()
 private val re=Regex("""\[(\d+):(\d+)(?:\.(\d+))?](.*)""")
 suspend fun get(s:Song):List<Line> = cache[s.id]?:withContext(Dispatchers.IO){
  try{
   fun e(x:String)=URLEncoder.encode(x,"UTF-8")
   val u="https://lrclib.net/api/get?track_name=${e(s.title)}&artist_name=${e(s.artist)}&album_name=${e(s.album)}&duration=${s.dur/1000}"
   val c=URL(u).openConnection() as HttpURLConnection
   c.setRequestProperty("User-Agent","Musik/1.0");c.connectTimeout=8000;c.readTimeout=8000
   val t=JSONObject(c.inputStream.bufferedReader().readText()).optString("syncedLyrics")
   t.lines().mapNotNull{l->re.matchEntire(l.trim())?.let{m->
    val(mm,ss,f,tx)=m.destructured
    Line(mm.toLong()*60000+ss.toLong()*1000+(f+"000").take(3).toLong(),tx.trim())}}
  }catch(e:Exception){emptyList()}
 }.also{cache[s.id]=it}
}
