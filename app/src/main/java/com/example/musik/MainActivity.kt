package com.example.musik
import android.Manifest.permission.*
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.*
import kotlinx.coroutines.delay

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);enableEdgeToEdge();setContent{Root()}}
}

@Composable fun MusikTheme(mode:Int,content:@Composable()->Unit){
 val dark=mode==2||(mode==0&&isSystemInDarkTheme())
 val cs=if(dark)darkColorScheme(primary=Color(0xFF80CBC4),background=Color.Black,surface=Color.Black,
  surfaceVariant=Color(0xFF161616),surfaceContainer=Color(0xFF0C0C0C))
 else lightColorScheme(primary=Color(0xFF00796B))
 MaterialTheme(cs,content=content)
}

@Composable fun Root(){
 val sp=LocalContext.current.getSharedPreferences("s",0)
 var mode by remember{mutableIntStateOf(sp.getInt("mode",0))}
 MusikTheme(mode){Surface(Modifier.fillMaxSize()){Perm{App(mode){mode=(mode+1)%3;sp.edit().putInt("mode",mode).apply()}}}}
}

@Composable fun Perm(content:@Composable()->Unit){
 val c=LocalContext.current
 val need=if(Build.VERSION.SDK_INT>=33)arrayOf(READ_MEDIA_AUDIO,POST_NOTIFICATIONS) else arrayOf(READ_EXTERNAL_STORAGE)
 fun ok()=ContextCompat.checkSelfPermission(c,need[0])==PackageManager.PERMISSION_GRANTED
 var granted by remember{mutableStateOf(ok())}
 val l=rememberLauncherForActivityResult(RequestMultiplePermissions()){granted=ok()}
 LaunchedEffect(Unit){if(!granted)l.launch(need)}
 if(granted)content() else Box(Modifier.fillMaxSize(),Alignment.Center){Button({l.launch(need)}){Text("Izinkan akses musik")}}
}

fun fmt(ms:Long)="%d:%02d".format(ms/60000,ms/1000%60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(mode:Int,onTheme:()->Unit){
 val c=LocalContext.current
 val songs=remember{loadSongs(c)}
 val pl=remember{Playlists(c)}
 var ctrl by remember{mutableStateOf<MediaController?>(null)}
 var nowId by remember{mutableStateOf<String?>(null)}
 var playing by remember{mutableStateOf(false)}
 var pos by remember{mutableLongStateOf(0L)}
 var showNow by remember{mutableStateOf(false)}
 var tab by remember{mutableIntStateOf(0)}
 var detail by remember{mutableStateOf<String?>(null)}

 DisposableEffect(Unit){
  val f=MediaController.Builder(c,SessionToken(c,ComponentName(c,PlaybackService::class.java))).buildAsync()
  f.addListener({ctrl=f.get()},ContextCompat.getMainExecutor(c))
  onDispose{MediaController.releaseFuture(f)}
 }
 DisposableEffect(ctrl){
  val p=ctrl?:return@DisposableEffect onDispose{}
  nowId=p.currentMediaItem?.mediaId;playing=p.isPlaying
  val l=object:Player.Listener{override fun onEvents(q:Player,e:Player.Events){nowId=q.currentMediaItem?.mediaId;playing=q.isPlaying}}
  p.addListener(l);onDispose{p.removeListener(l)}
 }
 LaunchedEffect(ctrl){while(true){pos=ctrl?.currentPosition?:0L;delay(250)}}
 BackHandler(showNow||detail!=null){if(showNow)showNow=false else detail=null}

 fun play(l:List<Song>,i:Int){ctrl?.run{setMediaItems(l.map{it.item()},i,0L);prepare();play()}}
 val cur=songs.find{it.id.toString()==nowId}
 val dl=detail?.let{d->val n=d.drop(2);when(d[0]){
  'a'->songs.filter{it.album==n};'r'->songs.filter{it.artist==n};'g'->songs.filter{it.genre==n}
  else->pl.map[n].orEmpty().mapNotNull{id->songs.find{it.id==id}}}}

 Box{
 Scaffold(topBar={TopAppBar(title={Text(detail?.drop(2)?:"Musik",maxLines=1)},
   navigationIcon={if(detail!=null)IconButton({detail=null}){Icon(Icons.Default.ArrowBack,null)}},
   actions={IconButton(onTheme){Icon(when(mode){0->Icons.Default.SettingsBrightness;1->Icons.Default.LightMode;else->Icons.Default.DarkMode},null)}})},
  bottomBar={if(cur!=null)Mini(cur,ctrl,playing,pos){showNow=true}}){pad->
  Column(Modifier.padding(pad)){
   if(dl!=null) SongList(dl,nowId,pl,detail?.takeIf{it.startsWith("p:")}?.drop(2)){play(dl,it)}
   else{
    ScrollableTabRow(tab,edgePadding=0.dp){listOf("Lagu","Album","Artis","Genre","Playlist").forEachIndexed{i,t->Tab(tab==i,{tab=i},text={Text(t)})}}
    when(tab){
     0->SongList(songs,nowId,pl,null){play(songs,it)}
     1->Groups(songs.groupBy{it.album},Icons.Default.Album){detail="a:$it"}
     2->Groups(songs.groupBy{it.artist},Icons.Default.Person){detail="r:$it"}
     3->Groups(songs.groupBy{it.genre},Icons.Default.Category){detail="g:$it"}
     else->PlaylistTab(pl){detail="p:$it"}
    }
   }
  }
 }
 if(showNow&&cur!=null)Now(cur,ctrl,playing,pos){showNow=false}
 }
}

@Composable fun SongList(list:List<Song>,nowId:String?,pl:Playlists,from:String?,onPlay:(Int)->Unit){
 LazyColumn{itemsIndexed(list){i,s->
  var m by remember{mutableStateOf(false)}
  ListItem(headlineContent={Text(s.title,maxLines=1,overflow=TextOverflow.Ellipsis,color=if(s.id.toString()==nowId)MaterialTheme.colorScheme.primary else Color.Unspecified)},
   supportingContent={Text("${s.artist} • ${s.album}",maxLines=1,overflow=TextOverflow.Ellipsis)},
   leadingContent={Icon(Icons.Default.MusicNote,null)},
   trailingContent={Box{IconButton({m=true}){Icon(Icons.Default.MoreVert,null)}
    DropdownMenu(m,{m=false}){
     if(pl.map.isEmpty())DropdownMenuItem({Text("Belum ada playlist")},{m=false},enabled=false)
     pl.map.keys.sorted().forEach{n->DropdownMenuItem({Text("Tambah ke $n")},{pl.add(n,s.id);m=false})}
     if(from!=null)DropdownMenuItem({Text("Hapus dari playlist")},{pl.remove(from,s.id);m=false})
    }}},
   modifier=Modifier.clickable{onPlay(i)})
 }}
}

@Composable fun Groups(g:Map<String,List<Song>>,icon:ImageVector,open:(String)->Unit){
 LazyColumn{items(g.keys.sorted()){k->
  ListItem(headlineContent={Text(k,maxLines=1)},supportingContent={Text("${g[k]?.size} lagu")},
   leadingContent={Icon(icon,null)},modifier=Modifier.clickable{open(k)})}}
}

@Composable fun PlaylistTab(pl:Playlists,open:(String)->Unit){
 var dlg by remember{mutableStateOf(false)};var name by remember{mutableStateOf("")}
 Column{
  ListItem(headlineContent={Text("Playlist baru")},leadingContent={Icon(Icons.Default.Add,null)},modifier=Modifier.clickable{dlg=true})
  LazyColumn{items(pl.map.keys.sorted()){n->
   ListItem(headlineContent={Text(n)},supportingContent={Text("${pl.map[n]?.size?:0} lagu")},
    leadingContent={Icon(Icons.Default.QueueMusic,null)},
    trailingContent={IconButton({pl.delete(n)}){Icon(Icons.Default.Delete,null)}},modifier=Modifier.clickable{open(n)})}}
 }
 if(dlg)AlertDialog({dlg=false},confirmButton={TextButton({pl.create(name.trim());name="";dlg=false}){Text("Buat")}},
  title={Text("Playlist baru")},text={OutlinedTextField(name,{name=it},singleLine=true,label={Text("Nama")})})
}

@Composable fun Mini(s:Song,ctrl:MediaController?,playing:Boolean,pos:Long,open:()->Unit){
 Surface(tonalElevation=3.dp,modifier=Modifier.clickable{open()}){Column(Modifier.navigationBarsPadding()){
  LinearProgressIndicator({(pos.toFloat()/maxOf(s.dur,1)).coerceIn(0f,1f)},Modifier.fillMaxWidth())
  Row(Modifier.padding(horizontal=16.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){Text(s.title,maxLines=1,fontWeight=FontWeight.Bold,overflow=TextOverflow.Ellipsis);Text(s.artist,maxLines=1,style=MaterialTheme.typography.bodySmall)}
   IconButton({ctrl?.seekToPrevious()}){Icon(Icons.Default.SkipPrevious,null)}
   IconButton({if(playing)ctrl?.pause() else ctrl?.play()}){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null)}
   IconButton({ctrl?.seekToNext()}){Icon(Icons.Default.SkipNext,null)}
  }}}
}

@Composable fun Now(s:Song,ctrl:MediaController?,playing:Boolean,pos:Long,close:()->Unit){
 var lines by remember(s.id){mutableStateOf<List<Line>?>(null)}
 LaunchedEffect(s.id){lines=Lyrics.get(s)}
 val ls=rememberLazyListState()
 val idx=lines?.indexOfLast{it.ms<=pos}?:-1
 LaunchedEffect(idx){if(idx>=0)ls.animateScrollToItem(maxOf(idx-2,0))}
 Surface(Modifier.fillMaxSize()){Column(Modifier.systemBarsPadding().padding(horizontal=24.dp)){
  IconButton(close){Icon(Icons.Default.KeyboardArrowDown,null)}
  Text(s.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,maxLines=1)
  Text(s.artist,color=MaterialTheme.colorScheme.primary)
  Box(Modifier.weight(1f).fillMaxWidth()){
   val ll=lines
   when{
    ll==null->Text("Memuat lirik…",Modifier.align(Alignment.Center))
    ll.isEmpty()->Text("Lirik tersinkron tidak ditemukan",Modifier.align(Alignment.Center))
    else->LazyColumn(state=ls,contentPadding=PaddingValues(vertical=32.dp)){itemsIndexed(ll){i,l->
     Text(l.text.ifBlank{"♪"},style=MaterialTheme.typography.titleLarge,
      fontWeight=if(i==idx)FontWeight.Bold else FontWeight.Normal,
      color=MaterialTheme.colorScheme.onSurface.copy(alpha=if(i==idx)1f else 0.35f),
      modifier=Modifier.fillMaxWidth().clickable{ctrl?.seekTo(l.ms)}.padding(vertical=8.dp))}}
   }
  }
  Slider(pos.toFloat().coerceIn(0f,maxOf(s.dur,1).toFloat()),{ctrl?.seekTo(it.toLong())},valueRange=0f..maxOf(s.dur,1).toFloat())
  Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(fmt(pos));Text(fmt(s.dur))}
  Row(Modifier.fillMaxWidth().padding(bottom=16.dp),Arrangement.SpaceEvenly,Alignment.CenterVertically){
   IconButton({ctrl?.seekToPrevious()},Modifier.size(56.dp)){Icon(Icons.Default.SkipPrevious,null,Modifier.size(36.dp))}
   FilledIconButton({if(playing)ctrl?.pause() else ctrl?.play()},Modifier.size(72.dp)){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,Modifier.size(40.dp))}
   IconButton({ctrl?.seekToNext()},Modifier.size(56.dp)){Icon(Icons.Default.SkipNext,null,Modifier.size(36.dp))}
  }
 }}
}
